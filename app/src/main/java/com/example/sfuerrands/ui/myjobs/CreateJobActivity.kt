package com.example.sfuerrands.ui.myjobs

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.sfuerrands.databinding.ActivityCreateJobBinding

class CreateJobActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateJobBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateJobBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up the toolbar with back button
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Create New Job"

        // Handle the submit button click
        binding.submitButton.setOnClickListener {
            submitJob()
        }
    }

    private fun submitJob() {
        // Get the values from the input fields
        val title = binding.titleEditText.text.toString().trim()
        val description = binding.descriptionEditText.text.toString().trim()
        val campus = binding.campusEditText.text.toString().trim()
        val priceText = binding.priceEditText.text.toString().trim()

        // Validate that all fields are filled
        if (title.isEmpty()) {
            binding.titleEditText.error = "Title is required"
            return
        }

        if (description.isEmpty()) {
            binding.descriptionEditText.error = "Description is required"
            return
        }

        if (campus.isEmpty()) {
            binding.campusEditText.error = "Campus is required"
            return
        }

        if (priceText.isEmpty()) {
            binding.priceEditText.error = "Price is required"
            return
        }

        // Try to convert price to a number
        val price = priceText.toDoubleOrNull()
        if (price == null || price <= 0) {
            binding.priceEditText.error = "Please enter a valid price"
            return
        }

        // Print the data to the console/log
        Log.d("CreateJobActivity", "=== New Job Created ===")
        Log.d("CreateJobActivity", "Title: $title")
        Log.d("CreateJobActivity", "Description: $description")
        Log.d("CreateJobActivity", "Campus: $campus")
        Log.d("CreateJobActivity", "Price Offered: $$price")
        Log.d("CreateJobActivity", "=======================")

        // Show a success message
        Toast.makeText(this, "Job created successfully!", Toast.LENGTH_LONG).show()

        // Close the activity and go back
        finish()
    }

    // Handle the back button in the toolbar
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}