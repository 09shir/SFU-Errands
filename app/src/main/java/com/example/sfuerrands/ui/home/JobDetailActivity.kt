package com.example.sfuerrands.ui.home
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.sfuerrands.databinding.ActivityJobDetailBinding

// View when we click on a job
class JobDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityJobDetailBinding

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
    }
}