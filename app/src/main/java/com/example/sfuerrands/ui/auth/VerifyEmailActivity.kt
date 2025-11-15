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

// VerifyEmailActivity.kt
class VerifyEmailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVerifyEmailBinding
    private val authRepo = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVerifyEmailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnOpenEmail.setOnClickListener {
            // best-effort: open any mail app
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_APP_EMAIL)
            }
            startActivity(Intent.createChooser(intent, "Open email"))
        }

        binding.btnIVerified.setOnClickListener {
            setLoading(true)
            lifecycleScope.launch {
                try {
                    if (!authRepo.isEmailVerifiedFresh()) {
                        toast("Not verified yet. Check your inbox or tap Resend.")
                        setLoading(false); return@launch
                    }
                    // Create user doc only after verified
                    val name = intent.getStringExtra("displayName") ?: ""
                    authRepo.createUserDocIfMissing(name)
                    goMain()
                } catch (e: Exception) {
                    setLoading(false)
                    toast(e.message ?: "Could not finalize sign-up")
                }
            }
        }

        binding.btnResend.setOnClickListener {
            setLoading(true)
            lifecycleScope.launch {
                try {
                    authRepo.resendVerificationEmail()
                    toast("Verification email sent")
                } catch (e: Exception) {
                    toast(e.message ?: "Resend failed")
                } finally {
                    setLoading(false)
                }
            }
        }

        binding.btnSignOut.setOnClickListener {
            Firebase.auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun goMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun setLoading(b: Boolean) {
        binding.progress.visibility = if (b) View.VISIBLE else View.GONE
        binding.btnIVerified.isEnabled = !b
        binding.btnResend.isEnabled = !b
        binding.btnSignOut.isEnabled = !b
        binding.btnOpenEmail.isEnabled = !b
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}

