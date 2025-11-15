package com.example.sfuerrands

import android.os.Bundle
import android.view.Menu
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import androidx.lifecycle.lifecycleScope
import androidx.navigation.ui.NavigationUI
import com.example.sfuerrands.data.repository.AuthRepository


import com.example.sfuerrands.databinding.ActivityMainBinding
import com.example.sfuerrands.ui.auth.LoginActivity
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // authentication
        val authRepo = com.example.sfuerrands.data.repository.AuthRepository()
        if (!authRepo.isSignedIn()) {
            startActivity(Intent(this, com.example.sfuerrands.ui.auth.LoginActivity::class.java))
            finish()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)

        binding.appBarMain.fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null)
                .setAnchorView(R.id.fab).show()
        }
        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        // Handle nav drawer hamburger clicks
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_jobs -> {
                    // Open myJobs activity
                    val intent = Intent(this, com.example.sfuerrands.ui.myjobs.MyJobsActivity::class.java)
                    startActivity(intent)
                    drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_settings -> {
                    // Open Settings activity
                    val intent = Intent(this, com.example.sfuerrands.ui.settings.SettingsActivity::class.java)
                    startActivity(intent)
                    drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_logout -> {
                    // Show logout toast
                    Toast.makeText(this, "User logging out...", Toast.LENGTH_SHORT).show()
                    lifecycleScope.launch {
                        AuthRepository().signOut()
                        startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                        finish()
                    }
                    drawerLayout.closeDrawers()
                    true
                }
                else -> {
                    // Let the default navigation handle other items
                    NavigationUI.onNavDestinationSelected(menuItem, navController)
                    drawerLayout.closeDrawers()
                    true
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}