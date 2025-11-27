package com.example.sfuerrands.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.sfuerrands.databinding.ActivitySignupBinding
import com.example.sfuerrands.data.repository.AuthRepository
import kotlinx.coroutines.launch

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private val authRepo = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Sign Up Logic
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

                    // Success: Navigate to VerifyEmailActivity
                    val intent = Intent(this@SignUpActivity, VerifyEmailActivity::class.java)
                    intent.putExtra("displayName", name)
                    startActivity(intent)

                    // Close SignUpActivity and LoginActivity (if it's on stack)
                    finishAffinity()
                } catch (e: Exception) {
                    setLoading(false)
                    Log.d("SignUp", "Error during sign up", e)
                    toast(e.message ?: "Sign up failed")
                }
            }
        }

        // 2. Back to Login
        binding.tvGoToLogin.setOnClickListener {
            finish() // Simply closes this activity, revealing LoginActivity underneath
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progress.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnSignUp.isEnabled = !loading
        binding.tvGoToLogin.isEnabled = !loading
    }

    private fun isSfuEmail(email: String): Boolean =
        email.lowercase().matches(Regex("^[A-Z0-9._%+-]+@(?:sfu\\.ca|g\\.sfu\\.ca)$", RegexOption.IGNORE_CASE))

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}