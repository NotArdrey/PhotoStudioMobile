package com.example.photostudio

import Booking
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth

class BookingHistoryPage : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var bookingAdapter: BookingHistoryAdapter
    private var bookingList = mutableListOf<Booking>()

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance() // Get current user

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
        val searchBar = findViewById<android.widget.EditText>(R.id.searchBar)
        searchBar.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                filterBookings(s.toString())
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun loadPastBookings() {
        val userId = auth.currentUser?.uid // Get current user ID

        if (userId != null) {
            firestore.collection("payments") // Fetch from 'payments' collection
                .whereEqualTo("uid", userId) // Get only bookings related to this user
                .get()
                .addOnSuccessListener { documents ->
                    bookingList.clear() // Clear existing data

                    for (document in documents) {
                        val packageName = document.getString("description") ?: "Unknown Package"
                        val date = document.getLong("appointmentDate")?.let { convertTimestampToDate(it) } ?: "No Date"
                        val price = document.getDouble("totalAmount")?.toString() ?: "0"

                        // Add to list
                        bookingList.add(Booking(packageName, date, price))
                    }
                    bookingAdapter.updateBookings(bookingList) // Update RecyclerView
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
        return sdf.format(java.util.Date(timestamp * 1000)) // Convert seconds to milliseconds
    }
}
