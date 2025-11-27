package com.example.sfuerrands.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.sfuerrands.MainActivity
import com.example.sfuerrands.data.repository.AuthRepository
import com.example.sfuerrands.databinding.ActivityVerifyEmailBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

class VerifyEmailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVerifyEmailBinding
    private val authRepo = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVerifyEmailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // UX Improvement: Show the user which email we are waiting on
        val currentUserEmail = Firebase.auth.currentUser?.email
        if (!currentUserEmail.isNullOrEmpty()) {
            binding.tvDescription.text = "We sent a verification link to:\n$currentUserEmail\n\nPlease click the link in your inbox, then tap the button below."
        }

        // 1. "I have verified" button
        binding.btnIVerified.setOnClickListener {
            setLoading(true)
            lifecycleScope.launch {
                try {
                    // Check if verified
                    if (!authRepo.isEmailVerifiedFresh()) {
                        toast("Not verified yet. Please check your inbox.")
                        setLoading(false)
                        return@launch
                    }

                    // Success: Create user doc and go to Main
                    val name = intent.getStringExtra("displayName") ?: ""
                    authRepo.createUserDocIfMissing(name)
                    goMain()
                } catch (e: Exception) {
                    setLoading(false)
                    toast(e.message ?: "Could not finalize sign-up")
                }
            }
        }

        // 2. Resend Link button
        binding.btnResend.setOnClickListener {
            setLoading(true)
            lifecycleScope.launch {
                try {
                    authRepo.resendVerificationEmail()
                    toast("Verification email sent!")
                } catch (e: Exception) {
                    toast(e.message ?: "Failed to resend email")
                } finally {
                    setLoading(false)
                }
            }
        }

        // 3. Sign Out text link
        binding.tvSignOut.setOnClickListener {
            Firebase.auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun goMain() {
        // Clear back stack so they can't go back to Verify screen
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progress.visibility = if (isLoading) View.VISIBLE else View.GONE

        // Disable inputs while loading
        binding.btnIVerified.isEnabled = !isLoading
        binding.btnResend.isEnabled = !isLoading
        binding.tvSignOut.isClickable = !isLoading

        // Visual feedback on opacity
        binding.btnIVerified.alpha = if (isLoading) 0.5f else 1.0f
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}