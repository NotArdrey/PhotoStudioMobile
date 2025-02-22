package com.example.photostudio

import Booking
import BookingAdapter
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class ActiveBookingPage : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var bookingAdapter: BookingAdapter
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_active_booking_page)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        recyclerView = findViewById(R.id.recyclerViewBookings)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Initialize adapter with empty list
        bookingAdapter = BookingAdapter(mutableListOf())
        recyclerView.adapter = bookingAdapter

        // Fetch bookings from Firestore
        fetchBookings()
    }

    private fun fetchBookings() {
        db.collection("payments").get()
            .addOnSuccessListener { documents ->
                val bookingsList = mutableListOf<Booking>()
                for (document in documents) {
                    val description = document.getString("description") ?: "Unknown"
                    val appointmentDate = document.getLong("appointmentDate")?.toString() ?: "N/A"
                    val totalAmount = document.getLong("totalAmount")?.toString() ?: "0"

                    val booking = Booking(description, appointmentDate, totalAmount)
                    bookingsList.add(booking)
                }
                bookingAdapter.updateBookings(bookingsList)
            }
            .addOnFailureListener { exception ->
                Log.e("Firestore", "Error fetching bookings", exception)
            }
    }
}
