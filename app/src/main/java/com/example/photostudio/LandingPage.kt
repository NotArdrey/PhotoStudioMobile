package com.example.photostudio

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class LandingPage : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_landing_page)

        // Load the LandingFragment into the FragmentContainerView
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, LandingFragment())  // Attach LandingFragment as default
                .commit()
        }

        // Set up BottomNavigationView to handle fragment transitions
        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottomNavigationView)

        // Disable item highlighting (active indicator)
        bottomNavigationView.itemActiveIndicatorColor = null
        bottomNavigationView.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    replaceFragment(LandingFragment())
                    true
                }
                R.id.nav_booking -> {
                    replaceFragment(BookingFragment())
                    true
                }
                R.id.nav_profile -> {
                    replaceFragment(AccountFragment())
                    true
                }
                else -> false
            }
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}
