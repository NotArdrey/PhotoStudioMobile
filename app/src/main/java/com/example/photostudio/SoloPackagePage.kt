package com.example.photostudio

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class SoloPackagePage : AppCompatActivity() {

    private lateinit var addIcon: ImageView
    private lateinit var backButton: ImageView
    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_solo_package_page)

        backButton = findViewById(R.id.backButton)
        backButton.setOnClickListener {
            finish()
        }

        addIcon = findViewById(R.id.addIcon)
        addIcon.setOnClickListener {
            val intent = Intent(this, PaymentPage::class.java)
            intent.putExtra("defaultPax", 0)
            intent.putExtra("packageType", "Solo Package")
            intent.putExtra("packagePrice", 500)
            startActivity(intent)
        }

        bottomNav = findViewById(R.id.bottomNavigationView)

        bottomNav.itemActiveIndicatorColor = null

        bottomNav.setOnItemSelectedListener { item ->
            val intent = Intent(this, BottomNavActivity::class.java)

            when (item.itemId) {
                R.id.nav_home -> intent.putExtra("destination", "home")
                R.id.nav_booking -> intent.putExtra("destination", "booking")
                R.id.nav_profile -> intent.putExtra("destination", "profile")
            }

            startActivity(intent)
            finish()
            true
        }
    }
}
