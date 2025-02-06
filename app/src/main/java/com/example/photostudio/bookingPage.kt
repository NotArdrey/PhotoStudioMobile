package com.example.photostudio

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class bookingPage : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_booking_page)

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigationView.setOnNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    val intent = Intent(this, LandingPage::class.java)
                    startActivity(intent)
                    true
                }
                R.id.nav_cart -> {
                    true
                }
                R.id.nav_profile -> {

                    true
                }
                else -> false
            }
        }


        findViewById<ImageView>(R.id.arrow).setOnClickListener {
            val intent = Intent(this, SoloPackagePage::class.java)
            startActivity(intent)
        }

        findViewById<ImageView>(R.id.pairArrow).setOnClickListener {
            val intent = Intent(this, pair_packagePage::class.java)
            startActivity(intent)
        }

        findViewById<ImageView>(R.id.GroupArrow).setOnClickListener {
            val intent = Intent(this, GroupPackage::class.java)
            startActivity(intent)
        }

        findViewById<ImageView>(R.id.BirthdayArrow).setOnClickListener {
            val intent = Intent(this, pre_birthdayPage::class.java)
            startActivity(intent)
        }
    }
}
