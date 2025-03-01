package com.example.photostudio

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class TermsAndCondition : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_terms_and_condition)



        // Find the Accept button
        val acceptButton = findViewById<Button>(R.id.acceptButton)

        // Set click listener to navigate to AccountPage
        acceptButton.setOnClickListener {
            val intent = Intent(this, AccountFragment::class.java)
            startActivity(intent)
            finish() // Close the TermsAndCondition activity
        }
    }
}
