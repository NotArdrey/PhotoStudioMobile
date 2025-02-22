package com.example.photostudio

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class GroupPackage : AppCompatActivity() {

    private lateinit var addIcon: ImageView
    private lateinit var backButton: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_group_package)

        backButton = findViewById(R.id.backButton)
        backButton.setOnClickListener {
            val intent = Intent(this, bookingPage::class.java)
            startActivity(intent)
            finish()
        }

        val defaultPax = 3
        val description = "Group Package"
        val packagePrice = 1000

        addIcon = findViewById(R.id.addIcon)
        addIcon.setOnClickListener {
            val paymentIntent = Intent(this, PaymentPage::class.java)
            paymentIntent.putExtra("defaultPax", defaultPax)
            paymentIntent.putExtra("description", description)
            paymentIntent.putExtra("packagePrice", packagePrice)
            paymentIntent.putExtra("showExtraSection", true)
            startActivity(paymentIntent)
        }
    }
}
