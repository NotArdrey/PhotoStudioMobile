package com.example.photostudio

import Booking
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.EditText
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class BookingHistoryPage : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var bookingAdapter: BookingHistoryAdapter
    private lateinit var bottomNav: BottomNavigationView
    private var bookingList = mutableListOf<Booking>()

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_booking_history_page)

        recyclerView = findViewById(R.id.bookingRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        bookingAdapter = BookingHistoryAdapter(bookingList)
        recyclerView.adapter = bookingAdapter

        // Load past bookings from Firestore
        loadPastBookings()

        // Set up search filter
        val searchBar = findViewById<EditText>(R.id.searchBar)
        searchBar.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                filterBookings(s.toString())
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Back Button Functionality
        val backButton = findViewById<ImageView>(R.id.backButton)
        backButton.setOnClickListener {
            finish()
        }

        // Bottom Navigation Setup
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

    private fun loadPastBookings() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            firestore.collection("payments")
                .whereEqualTo("uid", userId)
                .get()
                .addOnSuccessListener { documents ->
                    bookingList.clear()
                    for (document in documents) {
                        val packageName = document.getString("description") ?: "Unknown Package"
                        val date = document.getLong("appointmentDate")?.let { convertTimestampToDate(it) } ?: "No Date"
                        val price = document.getDouble("totalAmount")?.toString() ?: "0"
                        bookingList.add(Booking(packageName, date, price))
                    }
                    bookingAdapter.updateBookings(bookingList)
                }
                .addOnFailureListener { exception ->
                    Log.e("BookingHistory", "Error fetching bookings: ", exception)
                }
        }
    }

    private fun filterBookings(query: String) {
        val filteredList = bookingList.filter {
            it.description.contains(query, ignoreCase = true) || it.appointmentDate.contains(query, ignoreCase = true)
        }
        bookingAdapter.updateBookings(filteredList)
    }

    private fun convertTimestampToDate(timestamp: Long): String {
        val sdf = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(timestamp * 1000))
    }
}
