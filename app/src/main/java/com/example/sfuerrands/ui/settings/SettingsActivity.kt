package com.example.sfuerrands.ui.settings

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
    }

    // back button in the toolbar
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
