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
import com.example.sfuerrands.ui.home.RatingDialog
import com.example.sfuerrands.ui.preview.ImagePreviewNavigator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
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

    // requester reference for rating
    private var requesterRef: DocumentReference? = null
    private var runnerCompletion: Boolean = false

    private var isClaimed: Boolean = false
    private var clientCompleted: Boolean = false
    private var runnerCompletedFlag: Boolean = false


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

//        // initial completion UI
//        if (runnerCompletion) {
//            binding.completionStatusText.visibility = View.VISIBLE
//            binding.completionStatusText.text = "✓ You marked this as complete"
//            binding.markCompleteButton.isEnabled = false
//            binding.markCompleteButton.text = "Already Marked Complete"
//            binding.markCompleteButton.visibility = View.GONE
//        } else {
//            binding.completionStatusText.visibility = View.GONE
//            binding.markCompleteButton.visibility = View.VISIBLE
//            binding.markCompleteButton.isEnabled = true
//            binding.markCompleteButton.text = "Mark as Complete"
//        }

        // Convert and display photos
        convertPhotoUrls()

        // Load errand to get requester ref (for rating)
        lifecycleScope.launch {
            try {
                val errand = errandRepository.getErrandById(errandId)
                requesterRef = errand?.requesterId

                clientCompleted = errand?.clientCompletion ?: false
                runnerCompletedFlag = errand?.runnerCompletion ?: false
                isClaimed = errand?.runnerId != null

                updateCompletionUI()
            } catch (e: Exception) {}
        }

        // Button listeners
        binding.markCompleteButton.setOnClickListener {
            showCompletionWarning()
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
        mediaAdapter = MediaAdapter(downloadUrls) { position, _ ->
            ImagePreviewNavigator.open(
                context = this@TaskDetailActivity,
                urls = downloadUrls,
                startIndex = position
            )
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
                // Update runnerCompletion to true in Firestore
                errandRepository.setRunnerCompleted(errandId)

                // Refresh from DB (optional but safe)
                val refreshed = errandRepository.getErrandById(errandId)
                clientCompleted = refreshed?.clientCompletion ?: clientCompleted
                runnerCompletedFlag = refreshed?.runnerCompletion ?: true

                // Update UI based on new flags
                updateCompletionUI()

                Toast.makeText(this@TaskDetailActivity, "Marked as complete!", Toast.LENGTH_SHORT).show()

                // Show rating dialog for the requester if available
                requesterRef?.let { ref ->
                    RatingDialog.show(this@TaskDetailActivity) { rating ->
                        lifecycleScope.launch {
                            try {
                                errandRepository.updateUserRating(ref, rating)
                                Toast.makeText(this@TaskDetailActivity, "Thanks for the rating!", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(this@TaskDetailActivity, "Failed to save rating: ${e.message}", Toast.LENGTH_SHORT).show()
                            } finally {
                                finish()
                            }
                        }
                    }
                } ?: run {
                    finish()
                }

            } catch (e: Exception) {
                Toast.makeText(this@TaskDetailActivity, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
                binding.markCompleteButton.isEnabled = true
                binding.markCompleteButton.text = "Mark as Complete"
            }
        }
    }

    private fun showCompletionWarning() {
        AlertDialog.Builder(this)
            .setTitle("Confirm Completion")
            .setMessage(
                "Only mark this errand as complete if you have already received the payment " +
                        "from the requester.\n\n" +
                        "Are you sure you want to proceed?"
            )
            .setPositiveButton("Yes, I received the payment") { _, _ ->
                markAsComplete()
            }
            .setNegativeButton("Cancel", null)
            .show()
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
    private fun updateCompletionUI() {

        // Runner's own completion (this user)
        if (runnerCompletedFlag) {
            binding.completionStatusText.visibility = View.VISIBLE
            binding.completionStatusText.text = "✓ You marked this as complete"
            binding.markCompleteButton.visibility = View.GONE
            binding.markCompleteButton.isEnabled = false
            binding.markCompleteButton.text = "Already Marked Complete"
        } else {
            binding.completionStatusText.visibility = View.GONE
            binding.markCompleteButton.visibility = View.VISIBLE
            binding.markCompleteButton.isEnabled = true
            binding.markCompleteButton.text = "Mark as Complete"
        }

        // Show requester completion status
        if (clientCompleted) {
            binding.requesterCompletionStatus.visibility = View.VISIBLE
            binding.requesterCompletionStatus.text = "✓ Requester marked as complete"
        } else {
            binding.requesterCompletionStatus.visibility = View.GONE
        }

        // Runner shouldn't unclaim after requester marked complete
        binding.unclaimButton.visibility = if (!clientCompleted) View.VISIBLE else View.GONE
    }



    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}