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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account_page)

        auth = FirebaseAuth.getInstance()
        functions = FirebaseFunctions.getInstance()

        val currentUser = auth.currentUser
        val editAccount = findViewById<TextView>(R.id.editAccount)

        val userEmail = currentUser?.email ?: ""
        findViewById<TextView>(R.id.subtitleTextView).text = "Welcome ${currentUser?.displayName ?: "User"}"

        editAccount.setOnClickListener {
            if (userEmail.isNotEmpty()) {
                sendOtp(userEmail)
            } else {
                Toast.makeText(this, "No email associated with this account", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendOtp(email: String) {
        val data = hashMapOf("email" to email)

        functions.getHttpsCallable("sendOTP")
            .call(data)
            .addOnSuccessListener { result ->
                val otp = (result.data as? HashMap<*, *>)?.get("otp")?.toString()
                if (otp != null) {
                    Toast.makeText(this, "OTP sent to $email", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, CodeVerificationPage::class.java)
                    intent.putExtra("otp", otp)
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "Failed to retrieve OTP", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to send OTP: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
