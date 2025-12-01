package com.example.sfuerrands.ui.home

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sfuerrands.data.repository.ErrandRepository
import com.example.sfuerrands.data.repository.StorageRepository
import com.example.sfuerrands.databinding.ActivityJobDetailBinding
import com.example.sfuerrands.ui.preview.ImagePreviewNavigator
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// View when we click on a job
class JobDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityJobDetailBinding
    private lateinit var mediaAdapter: MediaAdapter
    private val storageRepository = StorageRepository()
    private val errandRepository = ErrandRepository() // Initialize Repository
    private var currentMediaUrls: List<String> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Setup view binding
        binding = ActivityJobDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Job Details"
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        // Get job data passed from previous screen
        val jobId = intent.getStringExtra("JOB_ID") // Get the ID
        val jobTitle = intent.getStringExtra("JOB_TITLE") ?: "No title"
        val jobDescription = intent.getStringExtra("JOB_DESCRIPTION") ?: "No description"
        val jobLocation = intent.getStringExtra("JOB_LOCATION") ?: "No location"
        val jobPayment = intent.getStringExtra("JOB_PAYMENT") ?: "$0.00"
        val mediaPaths = intent.getStringArrayListExtra("JOB_MEDIA_PATHS") ?: arrayListOf()

        // Display job information
        binding.detailJobTitle.text = jobTitle
        binding.detailJobDescription.text = jobDescription
        binding.detailJobLocation.text = jobLocation
        binding.detailJobPayment.text = jobPayment

        // Set up the back button
        binding.backButton.setOnClickListener {
            finish()
        }

        // Set up offer button (Updated for Offer System #41)
        binding.offerButton.setOnClickListener {
            val currentUser = Firebase.auth.currentUser

            // 1. Check if user is logged in
            if (currentUser == null) {
                Toast.makeText(this, "You must be logged in to offer help", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 2. Validate Job ID
            if (jobId.isNullOrEmpty()) {
                Toast.makeText(this, "Error: Invalid Job ID", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 3. Disable button to prevent double-clicks
            binding.offerButton.isEnabled = false
            binding.offerButton.text = "Sending Offer..."

            // 4. Perform the offer operation
            lifecycleScope.launch {
                try {
                    // Send offer via Repository instead of claiming directly
                    errandRepository.sendOffer(jobId, currentUser.uid)

                    Toast.makeText(this@JobDetailActivity, "Offer Sent!", Toast.LENGTH_SHORT).show()

                    // Close activity. The Home list will refresh automatically via the listener.
                    finish()
                } catch (e: Exception) {
                    // Handle failure
                    Toast.makeText(this@JobDetailActivity, "Failed to send offer: ${e.message}", Toast.LENGTH_LONG).show()

                    // Re-enable button on failure
                    binding.offerButton.isEnabled = true
                    binding.offerButton.text = "Offer Help"
                }
            }
        }

        // Media gallery setup
        mediaAdapter = MediaAdapter(emptyList()) { position, _ ->
            ImagePreviewNavigator.open(
                context = this@JobDetailActivity,
                urls = currentMediaUrls,
                startIndex = position
            )
        }

        binding.mediaRecycler.apply {
            layoutManager = LinearLayoutManager(
                this@JobDetailActivity,
                LinearLayoutManager.HORIZONTAL,
                false
            )
            adapter = mediaAdapter
        }

        if (mediaPaths.isEmpty()) {
            binding.mediaRecycler.visibility = View.GONE
        } else {
            binding.mediaRecycler.visibility = View.VISIBLE
            lifecycleScope.launch {
                try {
                    Firebase.auth.currentUser?.getIdToken(true)?.await()
                    val urls = mediaPaths.map { p -> async { storageRepository.resolveToUrl(p) } }.awaitAll()
                        .filterNotNull()
                    if (urls.isEmpty()) {
                        binding.mediaRecycler.visibility = View.GONE
                    } else {
                        binding.mediaRecycler.visibility = View.VISIBLE
                        currentMediaUrls = urls
                        mediaAdapter.submit(urls)
                    }
                } catch (e: Exception) {
                    binding.mediaRecycler.visibility = View.GONE
                }
            }
        }
    }
}