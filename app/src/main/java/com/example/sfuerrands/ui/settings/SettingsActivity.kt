package com.example.sfuerrands.ui.settings

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.sfuerrands.databinding.ActivitySettingsBinding

// settings page
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up the toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Settings"

        // Inside SettingsActivity.onCreate after binding is inflated:

        binding.buttonEditProfile.setOnClickListener {
            val intent = Intent(this@SettingsActivity, EditProfileActivity::class.java)
            startActivity(intent)
        }

    }

    // back button in the toolbar
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
