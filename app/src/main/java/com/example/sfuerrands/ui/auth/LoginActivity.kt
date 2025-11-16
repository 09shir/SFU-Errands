package com.example.sfuerrands.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.sfuerrands.MainActivity
import com.example.sfuerrands.databinding.ActivityLoginBinding
import com.example.sfuerrands.data.repository.AuthRepository
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val authRepo = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // If already signed in, skip
        if (authRepo.isSignedIn()) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSignIn.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val pass  = binding.etPassword.text.toString()

            if (email.isEmpty() || pass.isEmpty()) {
                toast("Enter email and password")
                return@setOnClickListener
            }

            setLoading(true)
            lifecycleScope.launch {
                try {
                    authRepo.signIn(email, pass)

                    val verified = authRepo.isEmailVerifiedFresh()
                    if (!verified) {
                        // Stay signed in so they can tap “Resend”, but don’t let them into Main
                        startActivity(Intent(this@LoginActivity, VerifyEmailActivity::class.java))
                        finish(); return@launch
                    }

                    // Verified → Main
                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                    finish()
                } catch (e: Exception) {
                    setLoading(false)
                    Log.d("SignIn", "Error during sign in", e)
                    toast(e.message ?: "Sign in failed")
                }
            }
        }

        binding.btnSignUp.setOnClickListener {
            val name  = binding.etName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val pass  = binding.etPassword.text.toString()

            if (name.isEmpty() || email.isEmpty() || pass.isEmpty()) {
                toast("Enter name, email, password")
                return@setOnClickListener
            }

            if (!isSfuEmail(email)) {
                toast("Please use a valid SFU email address.")
                return@setOnClickListener
            }

            setLoading(true)
            lifecycleScope.launch {
                try {
                    authRepo.signUpAndSendVerification(email, pass, name)
                    // Navigate to a VerifyEmailActivity
                    startActivity(Intent(this@LoginActivity, VerifyEmailActivity::class.java)
                        .putExtra("displayName", name))
                    finish()
                } catch (e: Exception) {
                    setLoading(false)
                    Log.d("SignUp", "Error during sign up", e)
                    toast(e.message ?: "Sign up failed")
                }
            }
        }

        binding.btnForgot.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            if (email.isEmpty()) { toast("Enter email first"); return@setOnClickListener }
            lifecycleScope.launch {
                try {
                    authRepo.sendPasswordReset(email)
                    toast("Reset email sent")
                } catch (e: Exception) {
                    toast(e.message ?: "Failed to send reset email")
                }
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progress.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnSignIn.isEnabled = !loading
        binding.btnSignUp.isEnabled = !loading
    }

    fun isSfuEmail(email: String): Boolean =
        email.lowercase().matches(Regex("^[A-Z0-9._%+-]+@(?:sfu\\.ca|g\\.sfu\\.ca)$", RegexOption.IGNORE_CASE))


    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
