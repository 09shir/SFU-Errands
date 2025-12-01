package com.example.sfuerrands.ui.myjobs

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.sfuerrands.data.repository.ErrandRepository
import com.example.sfuerrands.databinding.ActivityTaskDetailBinding
import com.example.sfuerrands.ui.home.MediaAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class TaskDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTaskDetailBinding
    private val errandRepository = ErrandRepository()
    private val auth = FirebaseAuth.getInstance()

    private var errandId: String = ""
    private var photoUrls: List<String> = emptyList()
    private var downloadUrls: List<String> = emptyList()
    private lateinit var mediaAdapter: MediaAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTaskDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Task Details"

        // Get data from intent
        errandId = intent.getStringExtra("ERRAND_ID") ?: ""
        val title = intent.getStringExtra("ERRAND_TITLE") ?: ""
        val description = intent.getStringExtra("ERRAND_DESCRIPTION") ?: ""
        val campus = intent.getStringExtra("ERRAND_CAMPUS") ?: ""
        val price = intent.getDoubleExtra("ERRAND_PRICE", 0.0)
        val location = intent.getStringExtra("ERRAND_LOCATION") ?: ""
        val runnerCompletion = intent.getBooleanExtra("ERRAND_RUNNER_COMPLETION", false)
        photoUrls = intent.getStringArrayListExtra("ERRAND_PHOTO_URLS") ?: emptyList()

        // Populate UI
        binding.titleText.text = title
        binding.descriptionText.text = description
        binding.campusText.text = "Campus: ${campus.replaceFirstChar { it.uppercase() }}"
        binding.priceText.text = "Payment: $${"%.2f".format(price)}"
        binding.locationText.text = if (location.isNotEmpty()) "Location: $location" else "Location: Not specified"

        // Show completion status
        if (runnerCompletion) {
            binding.completionStatusText.visibility = View.VISIBLE
            binding.completionStatusText.text = "You marked this as complete"
            binding.markCompleteButton.isEnabled = false
            binding.markCompleteButton.text = "Already Marked Complete"
        } else {
            binding.completionStatusText.visibility = View.GONE
        }

        // Convert and display photos
        convertPhotoUrls()

        // Button listeners
        binding.markCompleteButton.setOnClickListener {
            markAsComplete()
        }

        binding.unclaimButton.setOnClickListener {
            confirmUnclaim()
        }

        binding.backButton.setOnClickListener {
            finish()
        }
    }

    private fun convertPhotoUrls() {
        if (photoUrls.isEmpty()) {
            binding.photosLabel.visibility = View.GONE
            binding.photosRecyclerView.visibility = View.GONE
            return
        }

        binding.photosLabel.text = "Loading photos..."
        binding.photosLabel.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val storage = FirebaseStorage.getInstance()
                val urls = mutableListOf<String>()

                for (gsUrl in photoUrls) {
                    val path = gsUrl.removePrefix("gs://sfu-errand-app.firebasestorage.app/")
                    val downloadUrl = storage.reference.child(path).downloadUrl.await()
                    urls.add(downloadUrl.toString())
                }

                downloadUrls = urls
                setupPhotoDisplay()

            } catch (e: Exception) {
                Toast.makeText(this@TaskDetailActivity, "Failed to load photos", Toast.LENGTH_SHORT).show()
                binding.photosLabel.visibility = View.GONE
            }
        }
    }

    private fun setupPhotoDisplay() {
        mediaAdapter = MediaAdapter(downloadUrls) { position, photoUrl ->
            Toast.makeText(this, "Photo ${position + 1}", Toast.LENGTH_SHORT).show()
        }

        binding.photosRecyclerView.apply {
            layoutManager = GridLayoutManager(this@TaskDetailActivity, 3)
            adapter = mediaAdapter
        }

        binding.photosRecyclerView.visibility = View.VISIBLE
        binding.photosLabel.visibility = View.VISIBLE
        binding.photosLabel.text = "Photos (${downloadUrls.size})"
    }

    private fun markAsComplete() {
        binding.markCompleteButton.isEnabled = false
        binding.markCompleteButton.text = "Marking..."

        lifecycleScope.launch {
            try {
                // Update runnerCompletion to true
                errandRepository.updateErrand(
                    errandId,
                    mapOf("runnerCompletion" to true)
                )

                Toast.makeText(this@TaskDetailActivity, "Marked as complete!", Toast.LENGTH_SHORT).show()

                // Update UI
                binding.completionStatusText.visibility = View.VISIBLE
                binding.completionStatusText.text = "✓ You marked this as complete"
                binding.markCompleteButton.text = "Already Marked Complete"

            } catch (e: Exception) {
                Toast.makeText(this@TaskDetailActivity, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
                binding.markCompleteButton.isEnabled = true
                binding.markCompleteButton.text = "Mark as Complete"
            }
        }
    }

    private fun confirmUnclaim() {
        AlertDialog.Builder(this)
            .setTitle("Unclaim Task")
            .setMessage("Are you sure you want to unclaim this task? It will become available to others again.")
            .setPositiveButton("Unclaim") { _, _ ->
                unclaimTask()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun unclaimTask() {
        binding.unclaimButton.isEnabled = false
        binding.unclaimButton.text = "Unclaiming..."

        lifecycleScope.launch {
            try {
                // Reset errand to make it available again
                errandRepository.updateErrand(
                    errandId,
                    mapOf(
                        "runnerId" to null,
                        "status" to "open",
                        "claimedAt" to null,
                        "runnerCompletion" to false
                    )
                )

                Toast.makeText(this@TaskDetailActivity, "Task unclaimed successfully!", Toast.LENGTH_SHORT).show()
                finish()

            } catch (e: Exception) {
                Toast.makeText(this@TaskDetailActivity, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
                binding.unclaimButton.isEnabled = true
                binding.unclaimButton.text = "Unclaim Task"
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}