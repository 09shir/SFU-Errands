package com.example.sfuerrands.ui.settings

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.sfuerrands.data.repository.UserRepository
import com.example.sfuerrands.databinding.ActivityEditProfileBinding
import kotlinx.coroutines.launch
import androidx.core.net.toUri

class EditProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProfileBinding
    private val viewModel: EditProfileViewModel by viewModels()
    private val repository = UserRepository()

    private val campusOptions = arrayOf("Burnaby", "Surrey", "Vancouver")
    private val selectedBooleans = BooleanArray(campusOptions.size)

    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { viewModel.setImage(it) }
        }

    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
            if (bitmap != null) {
                val uri = MediaStore.Images.Media.insertImage(
                    contentResolver,
                    bitmap,
                    "profile_pic",
                    null
                ).toUri()
                viewModel.setImage(uri)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        observeViewModel()
        setupListeners()

        lifecycleScope.launch {
            ProfileLoader.loadProfileInto(viewModel)
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbarEditProfile)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Edit Profile"
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun observeViewModel() {

        // 1. New image picked by user (highest priority)
        viewModel.imageUri.observe(this) { uri ->
            if (uri != null) {
                binding.imageProfile.setImageURI(uri)
            }
        }

        // 2. Existing Firestore image (only if user hasn't chosen a new one)
        viewModel.existingPhotoUrl.observe(this) { url ->
            if (url != null && viewModel.imageUri.value == null) {
                Glide.with(this)
                    .load(url)
                    .into(binding.imageProfile)
            }
        }

        viewModel.name.observe(this) {
            binding.edittextName.setText(it)
        }

        viewModel.email.observe(this) {
            binding.textviewEmail.text = it
        }

        viewModel.selectedCampuses.observe(this) { campuses ->
            binding.textviewSelectedCampuses.text =
                if (campuses.isEmpty()) "None selected"
                else campuses.joinToString(", ")
        }
    }

    private fun setupListeners() {
        binding.buttonChangeImage.setOnClickListener { showImagePickerDialog() }

        binding.buttonChooseCampuses.setOnClickListener { showCampusDialog() }

        binding.buttonSave.setOnClickListener {
            viewModel.setName(binding.edittextName.text.toString())
            saveToFirebase()
        }
    }

    private fun showImagePickerDialog() {
        val options = arrayOf("Gallery", "Camera")
        AlertDialog.Builder(this)
            .setTitle("Select Image From")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> checkGalleryPermission()
                    1 -> checkCameraPermission()
                }
            }
            .show()
    }

    private fun checkGalleryPermission() {
        val permission = Manifest.permission.READ_EXTERNAL_STORAGE

        when {
            ContextCompat.checkSelfPermission(this, permission) ==
                    PackageManager.PERMISSION_GRANTED ->
                galleryLauncher.launch("image/*")

            shouldShowRequestPermissionRationale(permission) ->
                showPermissionExplanation("Gallery access required.") {
                    requestPermissions(arrayOf(permission), 1001)
                }

            else -> requestPermissions(arrayOf(permission), 1001)
        }
    }

    private fun checkCameraPermission() {
        val permission = Manifest.permission.CAMERA

        when {
            ContextCompat.checkSelfPermission(this, permission) ==
                    PackageManager.PERMISSION_GRANTED ->
                cameraLauncher.launch(null)

            shouldShowRequestPermissionRationale(permission) ->
                showPermissionExplanation("Camera access required.") {
                    requestPermissions(arrayOf(permission), 1002)
                }

            else -> requestPermissions(arrayOf(permission), 1002)
        }
    }

    private fun showPermissionExplanation(msg: String, onPositive: () -> Unit) {
        AlertDialog.Builder(this)
            .setMessage(msg)
            .setPositiveButton("Allow") { _, _ -> onPositive() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showCampusDialog() {
        val saved = viewModel.selectedCampuses.value ?: emptyList()

        for (i in campusOptions.indices) {
            selectedBooleans[i] = campusOptions[i] in saved
        }

        AlertDialog.Builder(this)
            .setTitle("Select Campuses")
            .setMultiChoiceItems(campusOptions, selectedBooleans) { _, index, isChecked ->
                selectedBooleans[index] = isChecked
            }
            .setPositiveButton("OK") { _, _ ->
                val selected = campusOptions.filterIndexed { i, _ -> selectedBooleans[i] }
                viewModel.setCampuses(selected)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveToFirebase() {
        lifecycleScope.launch {
            try {
                val name = binding.edittextName.text.toString()
                val campuses = viewModel.selectedCampuses.value ?: emptyList()
                val imageUri = viewModel.imageUri.value

                var uploadedPhotoUrl: String? = null

                if (imageUri != null) {
                    uploadedPhotoUrl = repository.uploadProfilePhoto(imageUri)
                }

                repository.updateProfile(
                    displayName = name,
                    campuses = campuses,
                    photoUrl = uploadedPhotoUrl
                )

                Toast.makeText(this@EditProfileActivity, "Profile saved", Toast.LENGTH_SHORT).show()
                finish()

            } catch (e: Exception) {
                Log.e("EditProfile", "Error saving", e)
                Toast.makeText(this@EditProfileActivity, "Error saving profile", Toast.LENGTH_LONG)
                    .show()
            }
        }
    }
}
