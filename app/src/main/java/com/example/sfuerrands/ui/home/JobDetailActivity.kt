package com.example.sfuerrands.ui.home
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
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
    private var currentMediaUrls: List<String> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Setup view binding
        binding = ActivityJobDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get job data passed from previous screen
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

        // Set up the bakc button
        binding.backButton.setOnClickListener {
            finish()
        }

        // Set up accept button
        binding.acceptButton.setOnClickListener {
            // For now just show a message and go back
            // TODO: Implement logic to accept job when databse setup
            finish()
        }

        // Media gallery setup
        mediaAdapter = MediaAdapter(emptyList()) { position, _ ->
            ImagePreviewNavigator.open(
                context = this@JobDetailActivity,
                urls = currentMediaUrls,
                startIndex = position
            )
        }

//        mediaAdapter = MediaAdapter(emptyList())
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
            // Resolve to download URLs in parallel
            lifecycleScope.launch {
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
            }
        }
    }
}