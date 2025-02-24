package com.example.photostudio

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class GroupPackage : AppCompatActivity() {
    private lateinit var addIcon: ImageView
    private lateinit var backButton: ImageView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_group_package)

        backButton = findViewById(R.id.backButton)
        backButton.setOnClickListener {
            val intent = Intent(this, BottomNavActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            intent.putExtra("destination", "booking")
            startActivity(intent)
            finish()
        }

        addIcon = findViewById(R.id.addIcon)
        addIcon.setOnClickListener {
            val paymentIntent = Intent(this, PaymentPage::class.java)
            paymentIntent.putExtra("extraPersonCost", 200)

            paymentIntent.putExtra("packageType", "Group Package")
            paymentIntent.putExtra("packagePrice", 1000)
            paymentIntent.putExtra("showExtraSection", true)
            startActivity(paymentIntent)
        }

    }

}