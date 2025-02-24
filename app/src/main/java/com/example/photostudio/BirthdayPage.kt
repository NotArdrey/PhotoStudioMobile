package com.example.photostudio

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class BirthdayPage : AppCompatActivity() {
     lateinit var backButton: ImageView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_pre_birthday_page)

        backButton = findViewById(R.id.backButton)
        backButton.setOnClickListener {
            val intent = Intent(this, BottomNavActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            intent.putExtra("destination", "booking")
            startActivity(intent)
            finish()
        }
        val plusFirst: ImageView = findViewById(R.id.plusFirst)
        plusFirst.setOnClickListener {
            val intent = Intent(this, PaymentPage::class.java)
            intent.putExtra("defaultPax", 0)
            intent.putExtra("packageType", "1-2 Years Old Package")
            intent.putExtra("packagePrice", 800)
            startActivity(intent)
        }

        val plusSecond: ImageView = findViewById(R.id.plusSecond)
        plusSecond.setOnClickListener {
            val intent = Intent(this, PaymentPage::class.java)
            intent.putExtra("defaultPax", 0)
            intent.putExtra("packageType", "3-4 Years Old Package")
            intent.putExtra("packagePrice", 1000)
            startActivity(intent)
        }

        val plusThird: ImageView = findViewById(R.id.plusThird)
        plusThird.setOnClickListener {
            val intent = Intent(this, PaymentPage::class.java)
            intent.putExtra("defaultPax", 0)
            intent.putExtra("packageType", "5-9 Years Old Package")
            intent.putExtra("packagePrice", 1500)
            startActivity(intent)
        }



    }
}
