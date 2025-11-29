package com.example.sfuerrands.ui.profile

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.sfuerrands.data.models.User
import com.example.sfuerrands.databinding.ActivityRunnerProfileBinding
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch

class ProfileDisplayActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRunnerProfileBinding
    private val db = FirebaseFirestore.getInstance()

    // Track which role we are viewing
    private var isViewingRunner: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRunnerProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnClose.setOnClickListener { finish() }

        // runner or requester
        val personPath = intent.getStringExtra("PERSON_PATH")
        val role = intent.getStringExtra("ROLE")

        when (role) {
            "runner" -> {
                isViewingRunner = true
            }
            "requester" -> {
                isViewingRunner = false
            }
            else -> {
                Toast.makeText(this, "Error: No role provided", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        if (personPath != null) {
            loadUserProfile(personPath)
            return
        } else if (role == null) {
            Toast.makeText(this, "Error: No role provided", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
    }

    private fun loadUserProfile(path: String) {
        lifecycleScope.launch {
            try {
                // Use the path string to get the document
                val userRef = db.document(path)
                val snapshot = userRef.get().await()
                val user = snapshot.toObject(User::class.java)

                if (user != null) {
                    setupUI(user)
                } else {
                    binding.tvDisplayName.text = "User was deactivated"
                    if (isViewingRunner) {
                        binding.tvRoleBadge.text = "RUNNER"
                        binding.tvRoleBadge.setBackgroundColor(Color.parseColor("#4CAF50"))
                    } else {
                        binding.tvRoleBadge.text = "REQUESTER"
                        binding.tvRoleBadge.setBackgroundColor(Color.parseColor("#2196F3"))
                    }
                }

            } catch (e: Exception) {
                Toast.makeText(this@ProfileDisplayActivity, "Failed to fetch profile", Toast.LENGTH_SHORT).show()
                Log.e("ProfileDisplayActivity", "Error fetching profile", e)
            }
        }
    }

    private fun setupUI(user: User) {
        // 1. Basic Info
        binding.tvDisplayName.text = user.displayName
        binding.tvEmail.text = user.email
        binding.tvCampuses.text = if (user.campuses.isNotEmpty())
            user.campuses.joinToString(", ") else "No Campus"

        // 2. Load Photo
        if (!user.photoUrl.isNullOrEmpty()) {
            Glide.with(this)
                .load(user.photoUrl)
                .circleCrop()
                .into(binding.ivProfilePhoto)
        }

        // 3. Dynamic UI based on Role (Runner vs Requester)
        if (isViewingRunner) {
            // Show RUNNER Badge (Green)
            binding.tvRoleBadge.text = "RUNNER"
            binding.tvRoleBadge.setBackgroundColor(Color.parseColor("#4CAF50")) // Green

            // Calculate Runner Rating
            val rating = if (user.runnerRatingCount > 0) {
                user.runnerRatingSum / user.runnerRatingCount
            } else 0.0
            binding.tvRating.text = String.format("%.1f (%d jobs run)", rating, user.runnerRatingCount)

        } else {
            // Show REQUESTER Badge (Blue)
            binding.tvRoleBadge.text = "REQUESTER"
            binding.tvRoleBadge.setBackgroundColor(Color.parseColor("#2196F3")) // Blue

            // Calculate Requester Rating
            val rating = if (user.requesterRatingCount > 0) {
                user.requesterRatingSum / user.requesterRatingCount
            } else 0.0
            binding.tvRating.text = String.format("%.1f (%d jobs posted)", rating, user.requesterRatingCount)
        }
    }
}