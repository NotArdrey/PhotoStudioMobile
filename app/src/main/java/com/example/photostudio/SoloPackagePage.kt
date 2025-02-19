package com.example.photostudio

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class SoloPackagePage : AppCompatActivity() {

    private lateinit var addIcon: ImageView
    private lateinit var backButton: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_solo_package_page)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        backButton = findViewById(R.id.backButton)


        backButton.setOnClickListener {
            val intent = Intent(this, bookingPage::class.java)
            startActivity(intent)
            finish()
        }

        val defaultPax = 1
        val packagePrice = 500
        val description = "Solo Package"
        addIcon = findViewById(R.id.addIcon)
        addIcon.setOnClickListener {
            val intent = Intent(this, PaymentPage::class.java)
            intent.putExtra("defaultPax", defaultPax)
            intent.putExtra("description", description)
            intent.putExtra("packagePrice", packagePrice)
            startActivity(intent)
        }
    }
}
