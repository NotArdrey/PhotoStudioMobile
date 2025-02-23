package com.example.photostudio

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class BottomNavActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bottom_nav)

        bottomNav = findViewById(R.id.bottomNavigationView)

        bottomNav.itemActiveIndicatorColor = null

        val destination = intent.getStringExtra("destination")
        when (destination) {
            "home" -> replaceFragment(LandingFragment())
            "booking" -> replaceFragment(BookingFragment())
            "profile" -> replaceFragment(AccountFragment())
            else -> replaceFragment(LandingFragment()) // Default to home
        }

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> replaceFragment(LandingFragment())
                R.id.nav_booking -> replaceFragment(BookingFragment())
                R.id.nav_profile -> replaceFragment(AccountFragment())
            }
            false
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}
