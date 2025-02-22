package com.example.photostudio

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions

class AccountPage : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var functions: FirebaseFunctions
    val activeBookings = findViewById<TextView>(R.id.activeBookings)
    val viewBookingHistory = findViewById<TextView>(R.id.viewBookingHistory)
    val Terms = findViewById<TextView>(R.id.terms)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account_page)

        auth = FirebaseAuth.getInstance()
        functions = FirebaseFunctions.getInstance()

        val currentUser = auth.currentUser
        val editAccount = findViewById<TextView>(R.id.editAccount)
        val userEmail = currentUser?.email ?: ""

        findViewById<TextView>(R.id.subtitleTextView).text =
            "Welcome ${currentUser?.displayName ?: "User"}"

        editAccount.setOnClickListener {
            if (userEmail.isNotEmpty()) {
                sendOtp(userEmail)
            } else {
                showToast("No email associated with this account")
            }
        }
        // Navigate to Active Booking Page
        activeBookings.setOnClickListener {
            val intent = Intent(this, ActiveBookingPage::class.java)
            startActivity(intent)
        }

        // Navigate to Booking History Page
        viewBookingHistory.setOnClickListener {
            val intent = Intent(this, BookingHistoryPage::class.java)
            startActivity(intent)
        }
        // Navigate to Terms And Condition
        Terms.setOnClickListener {
            val intent = Intent(this, BookingHistoryPage::class.java)
            startActivity(intent)
        }
    }




    private fun sendOtp(email: String) {
        val data = hashMapOf("email" to email)

        functions.getHttpsCallable("sendOTP")
            .call(data)
            .addOnSuccessListener { result ->
                // Use getData() to retrieve data safely
                val resultData = result.getData() as? Map<*, *>
                val otp = resultData?.get("otp")?.toString()

                if (!otp.isNullOrEmpty()) {
                    showToast("OTP sent to $email")
                    navigateToCodeVerification(email, otp)
                } else {
                    showToast("Failed to retrieve OTP")
                }
            }
            .addOnFailureListener { e ->
                showToast("Failed to send OTP: ${e.message}")
            }
    }


    private fun navigateToCodeVerification(email: String, otp: String) {
        val intent = Intent(this, CodeVerificationPage::class.java)
        intent.putExtra("email", email)
        intent.putExtra("otp", otp)
        startActivity(intent)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
