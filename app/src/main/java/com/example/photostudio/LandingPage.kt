package com.example.photostudio

import ImageAdapter
import ImageItem
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView

class LandingPage : AppCompatActivity() {
//landing page
    private lateinit var recyclerView: RecyclerView
    private lateinit var imageAdapter: ImageAdapter
    private lateinit var imageItemList: List<ImageItem>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_landing_page)

        recyclerView = findViewById(R.id.imageRecyclerView)

        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        // list of ImageItems
        imageItemList = listOf(
            ImageItem(R.drawable.image1),
            ImageItem(R.drawable.image2),
            ImageItem(R.drawable.image3),
            ImageItem(R.drawable.image4),
            ImageItem(R.drawable.image5),
            ImageItem(R.drawable.group),
            ImageItem(R.drawable.fiveyrearsold),
            ImageItem(R.drawable.prebirthday)
        )

        imageAdapter = ImageAdapter(imageItemList)
        recyclerView.adapter = imageAdapter

        // Set up BottomNavigationView
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigationView.setOnNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    true
                }
                R.id.nav_cart -> {
                    startActivity(Intent(this, bookingPage::class.java))
                    finish()
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, AccountPage::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }
    }
}
