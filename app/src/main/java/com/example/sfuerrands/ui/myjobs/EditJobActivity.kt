package com.example.sfuerrands.ui.myjobs

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.sfuerrands.data.repository.ErrandRepository
import com.example.sfuerrands.data.repository.StorageRepository
import com.example.sfuerrands.databinding.ActivityEditJobBinding
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class EditJobActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditJobBinding
    private val errandRepository = ErrandRepository()
    private val storageRepository = StorageRepository()

    // Store the errand ID so we can update/delete it
    private var errandId: String = ""

    // Store whether the errand is claimed
    private var isClaimed: Boolean = false

    // Store existing photo URLs (gs:// format from Firebase)
    private var originalPhotoUrls: List<String> = emptyList()
    
    // Store all current photos (existing + new)
    private val currentPhotos = mutableListOf<PhotoItem>()
    
    // Adapter for displaying editable photos
    private lateinit var photoAdapter: EditablePhotoAdapter
    
    // Maximum number of photos allowed
    private val maxPhotos = 3
    
    // Activity Result for picking multiple images
    private val pickImages =
        registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
            if (uris == null || uris.isEmpty()) return@registerForActivityResult
            
            // Calculate how many more photos we can add
            val remainingSlots = maxPhotos - currentPhotos.size
            if (remainingSlots <= 0) {
                Toast.makeText(this, "You can only have up to $maxPhotos photos.", Toast.LENGTH_SHORT).show()
                return@registerForActivityResult
            }
            
            // Take only as many as we have room for
            val toAdd = uris.take(remainingSlots)
            
            // Add new photos to the list
            toAdd.forEach { uri ->
                currentPhotos.add(PhotoItem.NewPhoto(uri))
            }
            
            if (uris.size > remainingSlots) {
                Toast.makeText(
                    this,
                    "Only $maxPhotos photos allowed. Added ${toAdd.size} more.",
                    Toast.LENGTH_SHORT
                ).show()
            }
            
            // Update the display
            updatePhotoDisplay()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditJobBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up the toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Edit Job"

        // --- 1. RETRIEVE DATA FROM INTENT ---
        // Get all the errand data that was passed from RequestsFragment
        errandId = intent.getStringExtra("ERRAND_ID") ?: ""
        val title = intent.getStringExtra("ERRAND_TITLE") ?: ""
        val description = intent.getStringExtra("ERRAND_DESCRIPTION") ?: ""
        val campus = intent.getStringExtra("ERRAND_CAMPUS") ?: ""
        val price = intent.getDoubleExtra("ERRAND_PRICE", 0.0)
        val location = intent.getStringExtra("ERRAND_LOCATION") ?: ""
        isClaimed = intent.getBooleanExtra("ERRAND_IS_CLAIMED", false)
        originalPhotoUrls = intent.getStringArrayListExtra("ERRAND_PHOTO_URLS") ?: emptyList()
        
        // Debug: Log photo URLs
        android.util.Log.d("EditJobActivity", "Received ${originalPhotoUrls.size} photos: $originalPhotoUrls")

        // --- 2. SET UP CAMPUS DROPDOWN ---
        val campuses = arrayOf("burnaby", "surrey", "vancouver")
        val campusAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, campuses)
        binding.campusEditText.setAdapter(campusAdapter)

        // --- 3. POPULATE FORM FIELDS ---
        // Set the text in the input fields with existing data
        binding.titleEditText.setText(title)
        binding.descriptionEditText.setText(description)
        binding.campusEditText.setText(campus, false) // false = don't filter
        binding.priceEditText.setText(price.toString())
        binding.locationEditText.setText(location)

        // --- 4. CONVERT AND DISPLAY EXISTING PHOTOS ---
        convertPhotoUrls()

        // --- 5. SET UP UI BASED ON CLAIMED STATUS ---
        setupUI()
    }
    
    // Convert Firebase Storage gs:// URLs to download URLs and set up photo display
    private fun convertPhotoUrls() {
        android.util.Log.d("EditJobActivity", "convertPhotoUrls called, originalPhotoUrls size: ${originalPhotoUrls.size}")
        
        if (originalPhotoUrls.isEmpty()) {
            // No photos to convert, set up empty display
            android.util.Log.d("EditJobActivity", "No photos to convert")
            setupPhotoDisplay()
            return
        }
        
        // Show loading state
        binding.photosLabel.text = "Loading photos..."
        binding.photosLabel.visibility = View.VISIBLE
        
        // Convert gs:// URLs to https:// download URLs
        lifecycleScope.launch {
            try {
                val storage = FirebaseStorage.getInstance()
                
                for (gsUrl in originalPhotoUrls) {
                    android.util.Log.d("EditJobActivity", "Converting: $gsUrl")
                    // Extract path from gs://bucket/path format
                    val path = gsUrl.removePrefix("gs://sfu-errand-app.firebasestorage.app/")
                    android.util.Log.d("EditJobActivity", "Path extracted: $path")
                    
                    // Get download URL
                    val downloadUrl = storage.reference.child(path).downloadUrl.await()
                    android.util.Log.d("EditJobActivity", "Download URL: $downloadUrl")
                    
                    // Add to currentPhotos list
                    currentPhotos.add(PhotoItem.ExistingPhoto(downloadUrl.toString(), gsUrl))
                }
                
                android.util.Log.d("EditJobActivity", "Converted ${currentPhotos.size} URLs, calling setupPhotoDisplay")
                setupPhotoDisplay()
                
            } catch (e: Exception) {
                android.util.Log.e("EditJobActivity", "Error converting photo URLs", e)
                Toast.makeText(
                    this@EditJobActivity,
                    "Failed to load photos: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                setupPhotoDisplay() // Show empty state
            }
        }
    }

    // Set up the RecyclerView to display editable photos
    private fun setupPhotoDisplay() {
        android.util.Log.d("EditJobActivity", "setupPhotoDisplay called, currentPhotos size: ${currentPhotos.size}")
        
        // Create adapter for editable photos
        photoAdapter = EditablePhotoAdapter(currentPhotos) { position ->
            // Remove photo at this position
            currentPhotos.removeAt(position)
            updatePhotoDisplay()
        }

        // Set up RecyclerView
        binding.photosRecyclerView.apply {
            layoutManager = GridLayoutManager(this@EditJobActivity, 3) // 3 columns
            adapter = photoAdapter
        }
        
        android.util.Log.d("EditJobActivity", "RecyclerView adapter set")
        
        // Update the display
        updatePhotoDisplay()
    }
    
    // Update photo display (label, button, visibility)
    private fun updatePhotoDisplay() {
        // Update adapter
        photoAdapter.submit(currentPhotos)
        
        // Show/hide the photos section
        if (currentPhotos.isEmpty()) {
            binding.photosRecyclerView.visibility = View.GONE
            binding.photosLabel.visibility = View.GONE
            binding.addPhotosButton.text = "Add Photos"
        } else {
            binding.photosRecyclerView.visibility = View.VISIBLE
            binding.photosLabel.visibility = View.VISIBLE
            binding.photosLabel.text = "Photos (${currentPhotos.size}/$maxPhotos)"
            
            // Update button text
            if (currentPhotos.size >= maxPhotos) {
                binding.addPhotosButton.text = "Maximum Photos Reached"
                binding.addPhotosButton.isEnabled = false
            } else {
                binding.addPhotosButton.text = "Add Photos (${currentPhotos.size}/$maxPhotos)"
                binding.addPhotosButton.isEnabled = !isClaimed
            }
        }
    }

    private fun setupUI() {
        // --- BUTTON CLICK LISTENERS ---

        // Add Photos Button - Open photo picker
        binding.addPhotosButton.setOnClickListener {
            pickImages.launch("image/*")
        }

        // Save Button - Update the errand in Firestore
        binding.saveButton.setOnClickListener {
            saveChanges()
        }

        // Delete Button - Remove the errand from Firestore
        binding.deleteButton.setOnClickListener {
            confirmDelete()
        }

        // Back Button - Just close the screen
        binding.backButton.setOnClickListener {
            finish()
        }

        // --- HANDLE CLAIMED vs UNCLAIMED STATE ---
        if (isClaimed) {
            // STATE: CLAIMED (Read-Only)
            // User cannot edit or delete a claimed errand

            binding.titleEditText.isEnabled = false
            binding.descriptionEditText.isEnabled = false
            binding.campusEditText.isEnabled = false
            binding.priceEditText.isEnabled = false
            binding.locationEditText.isEnabled = false
            binding.addPhotosButton.isEnabled = false

            binding.warningTextView.visibility = View.VISIBLE
            binding.warningTextView.text = "This errand has been claimed and can no longer be edited."

            binding.saveButton.visibility = View.GONE
            binding.deleteButton.visibility = View.GONE
            binding.backButton.visibility = View.VISIBLE

        } else {
            // STATE: UNCLAIMED (Editable)
            // User can edit and delete

            binding.titleEditText.isEnabled = true
            binding.descriptionEditText.isEnabled = true
            binding.campusEditText.isEnabled = true
            binding.priceEditText.isEnabled = true
            binding.locationEditText.isEnabled = true
            binding.addPhotosButton.isEnabled = currentPhotos.size < maxPhotos

            binding.warningTextView.visibility = View.GONE

            binding.saveButton.visibility = View.VISIBLE
            binding.deleteButton.visibility = View.VISIBLE
            binding.backButton.visibility = View.VISIBLE
        }
    }

    // Save the edited errand to Firestore
    private fun saveChanges() {
        // Get the current values from the input fields
        val newTitle = binding.titleEditText.text.toString().trim()
        val newDescription = binding.descriptionEditText.text.toString().trim()
        val newCampus = binding.campusEditText.text.toString().trim()
        val newPriceText = binding.priceEditText.text.toString().trim()
        val newLocation = binding.locationEditText.text.toString().trim()

        // Validate required fields
        if (newTitle.isEmpty()) {
            binding.titleEditText.error = "Title is required"
            return
        }

        if (newDescription.isEmpty()) {
            binding.descriptionEditText.error = "Description is required"
            return
        }

        if (newCampus.isEmpty()) {
            binding.campusEditText.error = "Campus is required"
            return
        }

        // Validate campus is one of the allowed values
        val validCampuses = listOf("burnaby", "surrey", "vancouver")
        if (!validCampuses.contains(newCampus.lowercase())) {
            binding.campusEditText.error = "Please select a valid campus"
            return
        }

        // Validate and parse price
        val newPrice = newPriceText.toDoubleOrNull()
        if (newPrice == null || newPrice < 0) {
            binding.priceEditText.error = "Please enter a valid price (0 or greater)"
            return
        }

        // Disable save button to prevent double-clicks
        binding.saveButton.isEnabled = false
        binding.saveButton.text = "Saving..."

        // Update the errand in Firestore using coroutine
        lifecycleScope.launch {
            try {
                // Step 1: Upload any new photos to Firebase Storage
                val finalPhotoUrls = mutableListOf<String>()
                
                for (photo in currentPhotos) {
                    when (photo) {
                        is PhotoItem.ExistingPhoto -> {
                            // Keep existing photo (use original gs:// URL)
                            finalPhotoUrls.add(photo.originalGsUrl)
                        }
                        is PhotoItem.NewPhoto -> {
                            // Upload new photo and get gs:// URL
                            binding.saveButton.text = "Uploading photos..."
                            val gsUrl = storageRepository.uploadErrandMedia(errandId, photo.uri)
                            finalPhotoUrls.add(gsUrl)
                        }
                    }
                }
                
                // Step 2: Create a map of fields to update
                binding.saveButton.text = "Saving..."
                val updates = mutableMapOf<String, Any?>(
                    "title" to newTitle,
                    "description" to newDescription,
                    "campus" to newCampus.lowercase(),
                    "priceOffered" to newPrice,
                    "location" to if (newLocation.isNotEmpty()) newLocation else null,
                    "photoUrls" to finalPhotoUrls
                )

                // Step 3: Update the errand in Firestore
                errandRepository.updateErrand(errandId, updates)

                // Show success message
                Toast.makeText(
                    this@EditJobActivity,
                    "Changes saved successfully!",
                    Toast.LENGTH_SHORT
                ).show()

                // Close the screen and go back to Requests list
                finish()

            } catch (e: Exception) {
                // Show error message
                Toast.makeText(
                    this@EditJobActivity,
                    "Failed to save changes: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()

                // Re-enable the save button
                binding.saveButton.isEnabled = true
                binding.saveButton.text = "Save"
            }
        }
    }

    // Show confirmation dialog before deleting
    private fun confirmDelete() {
        AlertDialog.Builder(this)
            .setTitle("Delete Errand")
            .setMessage("Are you sure you want to delete this errand? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteErrand()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // Delete the errand from Firestore
    private fun deleteErrand() {
        // Disable delete button to prevent double-clicks
        binding.deleteButton.isEnabled = false
        binding.deleteButton.text = "Deleting..."

        // Delete the errand from Firestore using coroutine
        lifecycleScope.launch {
            try {
                // Call repository to delete the errand
                errandRepository.deleteErrand(errandId)

                // Show success message
                Toast.makeText(
                    this@EditJobActivity,
                    "Errand deleted successfully!",
                    Toast.LENGTH_SHORT
                ).show()

                // Close the screen and go back to Requests list
                finish()

            } catch (e: Exception) {
                // Show error message
                Toast.makeText(
                    this@EditJobActivity,
                    "Failed to delete errand: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()

                // Re-enable the delete button
                binding.deleteButton.isEnabled = true
                binding.deleteButton.text = "Delete"
            }
        }
    }

    // Handle toolbar back button
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}