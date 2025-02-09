package com.example.photostudio

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.functions.FirebaseFunctions

class CodeVerificationPage : AppCompatActivity() {

    private var receivedOtp: String? = null
    private lateinit var functions: FirebaseFunctions
    private lateinit var userEmail: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_code_verification_page)

        // Retrieve OTP and Email from Intent
        receivedOtp = intent.getStringExtra("otp")
        userEmail = intent.getStringExtra("email") ?: ""

        // Initialize Firebase Functions
        functions = FirebaseFunctions.getInstance()

        // Initialize Views
        val codeInput = findViewById<EditText>(R.id.codeInput)
        val continueButton = findViewById<Button>(R.id.continueButton)
        val resendCode = findViewById<TextView>(R.id.resendCode)
        val backButton = findViewById<ImageView>(R.id.BackImage)

        // Verify OTP
        continueButton.setOnClickListener {
            val enteredOtp = codeInput.text.toString().trim()

            if (enteredOtp == receivedOtp) {
                Toast.makeText(this, "OTP Verified!", Toast.LENGTH_SHORT).show()
                // Proceed to the next step, e.g., open EditAccountPage
                val intent = Intent(this, EditAccountPage::class.java)
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "Invalid OTP!", Toast.LENGTH_SHORT).show()
            }
        }

        // Resend OTP Click Listener
        resendCode.setOnClickListener {
            resendOtp()
        }

        // Back Button Click Listener
        backButton.setOnClickListener {
            finish() // Simply go back to the previous activity
        }
    }

    private fun resendOtp() {
        val data = hashMapOf("email" to userEmail)

        functions.getHttpsCallable("sendOTP")
            .call(data)
            .addOnSuccessListener { result ->
                receivedOtp = (result.data as HashMap<*, *>)?["otp"].toString()
                Toast.makeText(this, "New OTP sent to $userEmail", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to resend OTP: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
