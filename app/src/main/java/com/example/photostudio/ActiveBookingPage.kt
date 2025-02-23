package com.example.photostudio

import Booking
import BookingAdapter
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class ActiveBookingPage : AppCompatActivity(), BookingAdapter.OnBookingClickListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var bookingAdapter: BookingAdapter
    private lateinit var bottomNav: BottomNavigationView
    private val db = FirebaseFirestore.getInstance()
    private val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_active_booking_page)

        // ✅ Back Button Fix
        val backButton: ImageView = findViewById(R.id.backButton)
        backButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed() // Use the correct method for back navigation
        }

        // ✅ RecyclerView Setup
        recyclerView = findViewById(R.id.recyclerViewBookings)
        recyclerView.layoutManager = LinearLayoutManager(this)

        bookingAdapter = BookingAdapter(mutableListOf(), this)
        recyclerView.adapter = bookingAdapter

        fetchBookings()

        // ✅ Bottom Navigation Setup
        bottomNav = findViewById(R.id.bottomNavigationView)

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> replaceFragment(LandingFragment())
                R.id.nav_booking -> replaceFragment(BookingFragment())
                R.id.nav_profile -> replaceFragment(AccountFragment())
            }
            true
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    private fun fetchBookings() {
        db.collection("payments")
            .whereEqualTo("complete", "no") // Retrieve only active bookings
            .get()
            .addOnSuccessListener { documents ->
                val bookingsList = mutableListOf<Booking>()
                for (document in documents) {
                    val id = document.id
                    val appointmentDate = document.getString("appointmentDate") ?: "N/A"
                    val appointmentTime = document.getString("appointmentTime") ?: "N/A"
                    val archive = document.getString("archive") ?: "no"
                    val complete = document.getString("complete") ?: "no"
                    val defaultBackdrop = document.getString("defaultBackdrop") ?: "N/A"
                    val description = document.getString("description") ?: "Unknown Package"
                    val extraPersonQty = document.getLong("extraPersonQty")?.toInt() ?: 0
                    val paymentLinkId = document.getString("paymentLinkId") ?: "N/A"
                    val paymentType = document.getString("paymentType") ?: "Unknown"
                    val paymongoName = document.getString("paymongoName") ?: "N/A"
                    val selectedExtraBackdrop = document.getString("selectedExtraBackdrop") ?: "N/A"
                    val softCopyQty = document.getLong("softCopyQty")?.toInt() ?: 0
                    val totalAmount = document.getDouble("totalAmount")?.toInt() ?: 0
                    val remainingAmount = document.getDouble("remainingAmount")?.toInt() ?: 0
                    val uid = document.getString("uid") ?: "N/A"

                    bookingsList.add(
                        Booking(
                            id = id,
                            appointmentDate = appointmentDate,
                            appointmentTime = appointmentTime,
                            archive = archive,
                            complete = complete,
                            defaultBackdrop = defaultBackdrop,
                            imageUrl = defaultBackdrop,
                            description = description,
                            extraPersonQty = extraPersonQty,
                            paymentLinkId = paymentLinkId,
                            paymentType = paymentType,
                            paymongoName = paymongoName,
                            selectedExtraBackdrop = selectedExtraBackdrop,
                            softCopyQty = softCopyQty,
                            totalAmount = totalAmount,
                            remainingAmount = remainingAmount,
                            uid = uid
                        )
                    )
                }

                // ✅ Sort by appointment date
                bookingsList.sortBy { booking ->
                    try {
                        dateFormat.parse(booking.appointmentDate)
                    } catch (e: Exception) {
                        null
                    }
                }

                // ✅ Update RecyclerView
                bookingAdapter.updateBookings(bookingsList)
            }
            .addOnFailureListener { exception ->
                Log.e("Firestore", "Error fetching bookings", exception)
            }
    }

    override fun onRebookClicked(booking: Booking) {
        val intent = Intent(this, RebookPage::class.java).apply {
            putExtra("appointmentDate", booking.appointmentDate)
            putExtra("appointmentTime", booking.appointmentTime)
            putExtra("defaultBackdrop", booking.defaultBackdrop)
            putExtra("description", booking.description)
            putExtra("extraPersonQty", booking.extraPersonQty)
            putExtra("paymentType", booking.paymentType)
            putExtra("paymongoName", booking.paymongoName)
            putExtra("selectedExtraBackdrop", booking.selectedExtraBackdrop)
            putExtra("softCopyQty", booking.softCopyQty)
            putExtra("totalAmount", booking.totalAmount)
            putExtra("remainingAmount", booking.remainingAmount)
            putExtra("uid", booking.uid)
        }
        startActivity(intent)
    }

    override fun onCancelClicked(booking: Booking) {
        db.collection("payments").document(booking.id)
            .update("complete", "yes")
            .addOnSuccessListener {
                Toast.makeText(this, "Booking Cancelled", Toast.LENGTH_SHORT).show()
                fetchBookings() // Refresh bookings list
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to cancel booking", Toast.LENGTH_SHORT).show()
                Log.e("Firestore", "Error updating booking status", e)
            }
    }
}
