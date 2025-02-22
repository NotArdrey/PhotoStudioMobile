package com.example.photostudio

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView

class PairPackageActivity : AppCompatActivity() {

    private lateinit var addIcon: ImageView
    private lateinit var backButton: ImageView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_pair_package_page)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Fix Back Button
        backButton = findViewById(R.id.backButton)
        backButton.setOnClickListener {
            val intent = Intent(this, BottomNavActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            intent.putExtra("destination", "booking") // Ensure it goes to BookingFragment
            startActivity(intent)
            finish()
        }

        val defaultPax = 2
        val description = "Pair Package"
        val packagePrice = 700

        addIcon = findViewById(R.id.addIcon)
        addIcon.setOnClickListener {
            val paymentIntent = Intent(this, PaymentPage::class.java)
            paymentIntent.putExtra("defaultPax", defaultPax)
            paymentIntent.putExtra("description", description)
            paymentIntent.putExtra("packagePrice", packagePrice)
            startActivity(paymentIntent)
        }

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
            finish()
            false // Prevent highlight
        }
    }
}
