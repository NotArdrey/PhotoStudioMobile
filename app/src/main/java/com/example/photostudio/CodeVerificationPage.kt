package com.example.photostudio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CodeVerificationPage : AppCompatActivity() {

    private var receivedOtp: String? = null
    private lateinit var userEmail: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_code_verification_page)

        // Retrieve OTP and Email from Intent
        receivedOtp = intent.getStringExtra("otp")
        userEmail = intent.getStringExtra("email") ?: ""

        // Initialize Views
        val codeInput = findViewById<EditText>(R.id.codeInput)
        val continueButton = findViewById<Button>(R.id.continueButton)
        val resendCode = findViewById<TextView>(R.id.resendCode)
        val backButton = findViewById<ImageView>(R.id.BackImage)

        // Verify OTP
        continueButton.setOnClickListener {
            val enteredOtp = codeInput.text.toString().trim()
            if (enteredOtp == receivedOtp) {
                showToast("OTP Verified!")
                fetchUserDataAndNavigate()
            } else {
                showToast("Invalid OTP!")
            }
        }

        // Resend OTP Click Listener
        resendCode.setOnClickListener { resendOtp() }
        // Back Button Click Listener
        backButton.setOnClickListener { finish() }
    }

    // Fetch user data using Firestore and then navigate to EditAccountPage.
    private fun fetchUserDataAndNavigate() {
        val sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val uid = sharedPref.getString("uid", null)
        Log.d("CodeVerificationPage", "Retrieved UID: $uid")
        if (uid.isNullOrEmpty()) {
            showToast("User ID not found in saved preferences.")
            // Fallback: Navigate with a default name and the provided email
            navigateToEditAccount("No Name", userEmail)
            return
        }

        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("Users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val userName = document.getString("userName")
                    val emailToPass = document.getString("email") ?: userEmail
                    val userNameToPass = if (userName.isNullOrEmpty()) "No Name" else userName
                    Log.d("CodeVerificationPage", "Fetched userName: $userNameToPass, email: $emailToPass")
                    navigateToEditAccount(userNameToPass, emailToPass)
                } else {
                    showToast("No data found for this user, proceeding with fallback.")
                    navigateToEditAccount("No Name", userEmail)
                }
            }
            .addOnFailureListener { e ->
                showToast("Failed to fetch user data: ${e.message}, proceeding with fallback.")
                navigateToEditAccount("No Name", userEmail)
            }
    }

    private fun navigateToEditAccount(userName: String, email: String) {
        val intent = Intent(this, EditAccountPage::class.java).apply {
            putExtra("userName", userName)
            putExtra("email", email)
        }
        Log.d("CodeVerificationPage", "Launching EditAccountPage with userName: $userName, email: $email")
        startActivity(intent)
        finish()
    }

    // Resend OTP using the MailSender class
    private fun resendOtp() {
        val otp = generateDummyOtp()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Replace these with your actual email credentials.
                val senderEmail = "neilardrey14@gmail.com"
                val senderPassword = "rihz vsdd beov fpit"
                val mailSender = MailSender(senderEmail, senderPassword)
                val subject = "Your New OTP Code"
                val body = "Your new OTP is: $otp"

                mailSender.sendMail(subject, body, userEmail)

                withContext(Dispatchers.Main) {
                    receivedOtp = otp
                    showToast("New OTP sent to $userEmail")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showToast("Failed to resend OTP: ${e.message}")
                }
            }
        }
    }

    private fun generateDummyOtp(): String {
        return (100000..999999).random().toString()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
