package com.example.photostudio

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat


class pair_packagePage : AppCompatActivity() {

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
        backButton = findViewById(R.id.backButton)


        backButton.setOnClickListener {
            val intent = Intent(this, bookingPage::class.java)
            startActivity(intent)
            finish()
        }

        val defaultPax = 2
        val description = "Pair Package"
        val packagePrice = 700

        addIcon = findViewById(R.id.addIcon)
        addIcon.setOnClickListener {

            startActivity(Intent(this, PaymentPage::class.java))
            intent.putExtra("defaultPax", defaultPax)
            intent.putExtra("description", description)
            intent.putExtra("packagePrice", packagePrice)
        }
    }
}