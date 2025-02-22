package com.example.photostudio

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class BirthdayPage : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()  // Optional, if you want edge-to-edge layout
        setContentView(R.layout.activity_pre_birthday_page)

        setupPackageButtons()
        setupBackButton()
        setupBottomNav()
    }

    private fun setupPackageButtons() {
        findViewById<ImageView>(R.id.plusFirst).setOnClickListener {
            navigateToPayment(1, 800, "1-2 Years Old Package")
        }

        findViewById<ImageView>(R.id.plusSecond).setOnClickListener {
            navigateToPayment(1, 1000, "3-4 Years Old Package")
        }

        findViewById<ImageView>(R.id.plusThird).setOnClickListener {
            navigateToPayment(1, 1500, "5-9 Years Old Package")
        }
    }

    private fun navigateToPayment(defaultPax: Int, packagePrice: Int, description: String) {
        val paymentIntent = Intent(this, PaymentPage::class.java).apply {
            putExtra("defaultPax", defaultPax)
            putExtra("description", description)
            putExtra("packagePrice", packagePrice)
        }
        startActivity(paymentIntent)
    }

    private fun setupBackButton() {
        findViewById<ImageView>(R.id.backButton).setOnClickListener {
            val intent = Intent(this, BottomNavActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            intent.putExtra("destination", "booking")
            startActivity(intent)
            finish()
        }
    }

    private fun setupBottomNav() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNav.itemActiveIndicatorColor = null // Removes highlight

        bottomNav.setOnItemSelectedListener { item ->
            val intent = Intent(this, BottomNavActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP // Prevents multiple activities

            when (item.itemId) {
                R.id.nav_home -> intent.putExtra("destination", "home")
                R.id.nav_booking -> intent.putExtra("destination", "booking")
                R.id.nav_profile -> intent.putExtra("destination", "profile")
            }

            startActivity(intent)
            finish()  // Close the current BirthdayPage
            true // Ensures the navigation works as expected
        }
    }
}
