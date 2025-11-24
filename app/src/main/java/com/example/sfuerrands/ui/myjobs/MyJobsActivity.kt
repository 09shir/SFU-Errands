package com.example.sfuerrands.ui.myjobs

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.example.sfuerrands.databinding.ActivityMyJobsBinding
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

// Show users jobs (Requests and Tasks)
class MyJobsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMyJobsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set up view binding
        binding = ActivityMyJobsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "My Jobs"

        // Setupo ViewPager2 with the adapter
        // ViewPager2 is for swiping between the two columns (Requests vs Tasks)
        val adapter = MyJobsPagerAdapter(this)
        binding.viewPager.adapter = adapter

        // Connect TabLayout with ViewPager2 so tabs switch when swiping
        TabLayoutMediator(binding.tabLayout, binding.viewPager) {
            tab, position ->
            tab.text = when(position) {
                0 -> "Requests"
                1 -> "Tasks"
                else -> ""
            }
        }.attach()
    }

    // Handle back button in toolbar'
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}