package com.example.sfuerrands.ui.myjobs

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.sfuerrands.databinding.ActivityEditJobBinding

class EditJobActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditJobBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditJobBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up the toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Edit Job"

        // --- 1. PRE-FILL DATA ---
        // Retrieve the data sent from RequestsFragment
        val title = intent.getStringExtra("JOB_TITLE")
        val description = intent.getStringExtra("JOB_DESCRIPTION")
        val campus = intent.getStringExtra("JOB_LOCATION")
        val price = intent.getStringExtra("JOB_PAYMENT")

        // Set the text in the input fields
        binding.titleEditText.setText(title)
        binding.descriptionEditText.setText(description)
        binding.campusEditText.setText(campus)

        // Strip the '$' sign from the price string (e.g., "$5.00" -> "5.00")
        // because the input type is numberDecimal
        val cleanPrice = price?.replace("$", "")?.trim()
        binding.priceEditText.setText(cleanPrice)
        // ------------------------

        // --- 2. MOCK DATA TOGGLE ---
        // Change this boolean manually to test the two states required by the ticket:
        // true  = READ ONLY (Claimed)
        // false = EDITABLE  (Unclaimed)
        val isClaimed = false
        // ---------------------------

        setupUI(isClaimed)
    }

    private fun setupUI(isClaimed: Boolean) {
        // Setup Button Click Listeners
        binding.saveButton.setOnClickListener {
            Toast.makeText(this, "Changes Saved (UI Only)", Toast.LENGTH_SHORT).show()
            finish()
        }

        binding.deleteButton.setOnClickListener {
            Toast.makeText(this, "Job Deleted (UI Only)", Toast.LENGTH_SHORT).show()
            finish()
        }

        binding.backButton.setOnClickListener {
            finish()
        }

        // Handle "Claimed" vs "Unclaimed" State
        if (isClaimed) {
            // STATE: CLAIMED (Read-Only)

            binding.titleEditText.isEnabled = false
            binding.descriptionEditText.isEnabled = false
            binding.campusEditText.isEnabled = false
            binding.priceEditText.isEnabled = false

            binding.warningTextView.visibility = View.VISIBLE
            binding.warningTextView.text = "This errand has been claimed and can no longer be edited."

            binding.saveButton.visibility = View.GONE
            binding.deleteButton.visibility = View.GONE
            binding.backButton.visibility = View.VISIBLE

        } else {
            // STATE: UNCLAIMED (Editable)

            binding.titleEditText.isEnabled = true
            binding.descriptionEditText.isEnabled = true
            binding.campusEditText.isEnabled = true
            binding.priceEditText.isEnabled = true

            binding.warningTextView.visibility = View.GONE

            binding.saveButton.visibility = View.VISIBLE
            binding.deleteButton.visibility = View.VISIBLE
            binding.backButton.visibility = View.VISIBLE
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}