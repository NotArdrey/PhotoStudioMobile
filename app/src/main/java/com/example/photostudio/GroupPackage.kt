package com.example.photostudio

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class GroupPackage : AppCompatActivity() {
    private lateinit var addIcon: ImageView
    private lateinit var backButton: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()  // Optional, if you want edge-to-edge layout
        setContentView(R.layout.activity_group_package)

        // Setup back button to navigate back to the booking page in BottomNavActivity
        backButton = findViewById(R.id.backButton)
        backButton.setOnClickListener {
            val intent = Intent(this, BottomNavActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            intent.putExtra("destination", "booking") // Ensure it goes to BookingFragment
            startActivity(intent)
            finish()
        }

        // Initialize variables for group package details
        val defaultPax = 3
        val description = "Group Package"
        val packagePrice = 1000

        // Setup add icon to start PaymentPage
        addIcon = findViewById(R.id.addIcon)
        addIcon.setOnClickListener {
            val paymentIntent = Intent(this, PaymentPage::class.java)
            paymentIntent.putExtra("defaultPax", defaultPax)
            paymentIntent.putExtra("description", description)
            paymentIntent.putExtra("packagePrice", packagePrice)
            paymentIntent.putExtra("showExtraSection", true)
            startActivity(paymentIntent)
        }

        // Set up Bottom Navigation behavior
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNav.itemActiveIndicatorColor = null // Removes highlight

        // Handle Bottom Navigation item selection
        bottomNav.setOnItemSelectedListener { item ->
            val intent = Intent(this, BottomNavActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP // Prevents multiple activities

            when (item.itemId) {
                R.id.nav_home -> intent.putExtra("destination", "home")
                R.id.nav_booking -> intent.putExtra("destination", "booking")
                R.id.nav_profile -> intent.putExtra("destination", "profile")
            }

            startActivity(intent)
            finish()  // Close the current GroupPackage activity
            true // Ensures the navigation works as expected
        }
    }
}
