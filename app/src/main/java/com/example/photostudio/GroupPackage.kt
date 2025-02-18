package com.example.photostudio

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class GroupPackage : AppCompatActivity() {
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
    }

}