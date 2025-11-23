package com.example.sfuerrands.ui.myjobs

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sfuerrands.data.models.Errand
import com.example.sfuerrands.data.repository.ErrandRepository
import com.example.sfuerrands.data.repository.StorageRepository
import com.example.sfuerrands.databinding.ActivityCreateJobBinding
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class CreateJobActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateJobBinding
    private val errandRepository = ErrandRepository()
    private val storageRepository = StorageRepository()
    private val auth = FirebaseAuth.getInstance()

    private var selectedExpectedCompletionDate: Date? = null
    private val dateFormatter = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())

    // images
    private val selectedPhotoUris = mutableListOf<Uri>()
    private val maxPhotos: Int = 3
    private lateinit var photosAdapter: SelectedPhotoAdapter

    // Activity Result for picking multiple images
    private val pickImages =
        registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
            if (uris == null || uris.isEmpty()) return@registerForActivityResult

            // How many more can we still add
            val remainingSlots = maxPhotos - selectedPhotoUris.size
            if (remainingSlots <= 0) {
                Toast.makeText(this, "You can only add up to $maxPhotos photos.", Toast.LENGTH_SHORT).show()
                return@registerForActivityResult
            }

            // Take only as many as we have room for
            val toAdd = uris.take(remainingSlots)

            selectedPhotoUris.addAll(toAdd)

            if (uris.size > remainingSlots) {
                Toast.makeText(
                    this,
                    "Only $maxPhotos photos allowed. Added ${toAdd.size} more.",
                    Toast.LENGTH_SHORT
                ).show()
            }

            photosAdapter.submit(selectedPhotoUris)
            updatePhotosUi()
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateJobBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Create New Job"

        setupCampusDropdown()
        setupDatePicker()
        setupPhotoPicker()

        binding.submitButton.setOnClickListener {
            submitJob()
        }
    }

    private fun setupCampusDropdown() {
        val campuses = arrayOf("Burnaby", "Surrey", "Vancouver")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, campuses)
        binding.campusDropdown.setAdapter(adapter)
    }

    private fun setupDatePicker() {
        binding.expectedCompletionEditText.setOnClickListener {
            showDateTimePicker()
        }
    }

    private fun showDateTimePicker() {
        val calendar = Calendar.getInstance()

        // Show date picker first
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                // Then show time picker
                TimePickerDialog(
                    this,
                    { _, hourOfDay, minute ->
                        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                        calendar.set(Calendar.MINUTE, minute)
                        calendar.set(Calendar.SECOND, 0)

                        selectedExpectedCompletionDate = calendar.time
                        binding.expectedCompletionEditText.setText(
                            dateFormatter.format(calendar.time)
                        )
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    false
                ).show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            // Don't allow dates in the past
            datePicker.minDate = System.currentTimeMillis()
        }.show()
    }

    private fun setupPhotoPicker() {
        photosAdapter = SelectedPhotoAdapter(emptyList()) { indexToRemove ->
            // Remove from list
            if (indexToRemove in selectedPhotoUris.indices) {
                selectedPhotoUris.removeAt(indexToRemove)
                photosAdapter.submit(selectedPhotoUris)
                updatePhotosUi()
            }
        }
        binding.photosRecycler.apply {
            layoutManager =
                LinearLayoutManager(this@CreateJobActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = photosAdapter
        }

        binding.addPhotosButton.setOnClickListener {
            // Launch system picker for images
            pickImages.launch("image/*")
        }
        binding.photosRecycler.visibility = View.GONE

    }

    private fun submitJob() {
        // Disable button to prevent double submission
        binding.submitButton.isEnabled = false

        // Get the values from the input fields
        val title = binding.titleEditText.text.toString().trim()
        val description = binding.descriptionEditText.text.toString().trim()
        val campus = binding.campusDropdown.text.toString().trim()
        val location = binding.locationEditText.text.toString().trim()
        val priceText = binding.priceEditText.text.toString().trim()

        // Validate title
        if (title.isEmpty()) {
            binding.titleEditText.error = "Title is required"
            binding.submitButton.isEnabled = true
            return
        }

        // Validate description
        if (description.isEmpty()) {
            binding.descriptionEditText.error = "Description is required"
            binding.submitButton.isEnabled = true
            return
        }

        // Validate campus
        if (campus.isEmpty()) {
            binding.campusDropdown.error = "Campus is required"
            binding.submitButton.isEnabled = true
            return
        }

        val validCampuses = listOf("Burnaby", "Surrey", "Vancouver")
        if (!validCampuses.contains(campus)) {
            binding.campusDropdown.error = "Please select a valid campus"
            binding.submitButton.isEnabled = true
            return
        }

        // Validate price
        if (priceText.isEmpty()) {
            binding.priceEditText.error = "Price is required"
            binding.submitButton.isEnabled = true
            return
        }

        val price = priceText.toDoubleOrNull()
        if (price == null || price < 0) {
            binding.priceEditText.error = "Please enter a valid price (0 or greater)"
            binding.submitButton.isEnabled = true
            return
        }

        // Validate expected completion date (if provided)
        if (selectedExpectedCompletionDate != null) {
            val now = Date()
            if (selectedExpectedCompletionDate!!.before(now)) {
                Toast.makeText(this, "Expected completion date cannot be in the past", Toast.LENGTH_SHORT).show()
                binding.submitButton.isEnabled = true
                return
            }
        }

        // Get current user UID
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) {
            Toast.makeText(this, "You must be signed in to create a job", Toast.LENGTH_SHORT).show()
            binding.submitButton.isEnabled = true
            return
        }

        // Show loading state
        binding.submitButton.text = "Creating..."

        // Create DocumentReference for the current user
        val db = FirebaseFirestore.getInstance()
        val requesterRef = db.collection("users").document(currentUserId)

        // Create the Errand object
        val errand = Errand(
            requesterId = requesterRef,
            title = title,
            description = description,
            campus = campus.lowercase(), // Store as lowercase in DB
            location = location.ifEmpty { null },
            priceOffered = price,
            status = "open",
            runnerId = null,
            runnerCompletion = false,
            clientCompletion = false,
            createdAt = Timestamp.now(),
            claimedAt = null,
            expectedCompletionAt = selectedExpectedCompletionDate?.let { Timestamp(it) },
            photoUrls = emptyList()
        )

        // Save to Firestore
        lifecycleScope.launch {
            try {
                val errandId = errandRepository.createErrand(errand)

                // upload photos if any
                if (selectedPhotoUris.isNotEmpty()) {
                    val urls = selectedPhotoUris.map { uri ->
                        async { storageRepository.uploadErrandMedia(errandId, uri) }
                    }.awaitAll()

                    // update errand with photoUrls
                    errandRepository.updateErrand(
                        errandId,
                        mapOf("photoUrls" to urls)
                    )
                }

                Log.d("CreateJobActivity", "=== Errand Created Successfully ===")
                Log.d("CreateJobActivity", "Errand ID: $errandId")
                Log.d("CreateJobActivity", "Title: $title")
                Log.d("CreateJobActivity", "Requester ID: $currentUserId")
                Log.d("CreateJobActivity", "====================================")

                Toast.makeText(this@CreateJobActivity, "Job created successfully!", Toast.LENGTH_LONG).show()
                finish() // Go back to previous screen

            } catch (e: Exception) {
                Log.e("CreateJobActivity", "Error creating errand", e)
                Toast.makeText(
                    this@CreateJobActivity,
                    "Failed to create job: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()

                // Re-enable button and restore text
                binding.submitButton.isEnabled = true
                binding.submitButton.text = "Create Job"
            }
        }
    }

    // Handle the back button in the toolbar
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun updatePhotosUi() {
        binding.photosRecycler.visibility =
            if (selectedPhotoUris.isEmpty()) View.GONE else View.VISIBLE

        // (optional) also update Add Photos button state
        binding.addPhotosButton.isEnabled = selectedPhotoUris.size < maxPhotos
    }
}