package com.example.photostudio

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class BirthdayPage : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_pre_birthday_page)

        val plusFirst: ImageView = findViewById(R.id.plusFirst)
        plusFirst.setOnClickListener {
            val defaultPax = 1
            val packagePrice = 800
            val description = "1-2 Years Old Package"

            val paymentIntent = Intent(this, PaymentPage::class.java).apply {
                putExtra("defaultPax", defaultPax)
                putExtra("description", description)
                putExtra("packagePrice", packagePrice)
            }
            startActivity(paymentIntent)
        }

        val plusSecond: ImageView = findViewById(R.id.plusSecond)
        plusSecond.setOnClickListener {
            val defaultPax = 1
            val packagePrice = 1000
            val description = "3-4 Years Old Package"

            val paymentIntent = Intent(this, PaymentPage::class.java).apply {
                putExtra("defaultPax", defaultPax)
                putExtra("description", description)
                putExtra("packagePrice", packagePrice)
            }
            startActivity(paymentIntent)
        }

        val plusThird: ImageView = findViewById(R.id.plusThird)
        plusThird.setOnClickListener {
            val defaultPax = 1
            val packagePrice = 1500
            val description = "5-9 Years Old Package"

            val paymentIntent = Intent(this, PaymentPage::class.java).apply {
                putExtra("defaultPax", defaultPax)
                putExtra("description", description)
                putExtra("packagePrice", packagePrice)
            }
            startActivity(paymentIntent)
        }

        findViewById<ImageView>(R.id.backButton).setOnClickListener {
            val intent = Intent(this, LandingPage::class.java)
            startActivity(intent)
        }
    }
}
