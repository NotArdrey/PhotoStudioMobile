package com.example.photostudio

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.security.MessageDigest

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private val verificationCheckDelay: Long = 3000 // Delay in milliseconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registration)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val userNameInput = findViewById<EditText>(R.id.userNameInput)
        val emailInput = findViewById<EditText>(R.id.emailInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)
        val confirmPasswordInput = findViewById<EditText>(R.id.confirmPasswordInput)
        val registerButton = findViewById<Button>(R.id.registerButton)
        val loginLink = findViewById<TextView>(R.id.loginLink)

        // Set colored "Login here" link text
        val fullText = "Already have an account? Login here"
        val spannableString = SpannableString(fullText)
        val start = fullText.indexOf("Login here")
        val end = fullText.length
        val blueColor = Color.parseColor("#321890")
        spannableString.setSpan(
            ForegroundColorSpan(blueColor),
            start,
            end,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        loginLink.text = spannableString

        loginLink.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        registerButton.setOnClickListener {
            val userName = userNameInput.text.toString().trim()
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()
            val confirmPassword = confirmPasswordInput.text.toString().trim()

            if (userName.isNotEmpty() && email.isNotEmpty() &&
                password.isNotEmpty() && confirmPassword.isNotEmpty()
            ) {
                if (password == confirmPassword) {
                    if (isValidEmail(email)) {
                        registerUser(userName, email, password)
                    } else {
                        Toast.makeText(this, "Invalid email format", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun registerUser(userName: String, email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val firebaseUser = auth.currentUser
                    // Send built-in email verification
                    firebaseUser?.sendEmailVerification()?.addOnCompleteListener { emailTask ->
                        if (emailTask.isSuccessful) {
                            val userId = firebaseUser.uid
                            val hashedPassword = hashPassword(password)
                            // Create user object
                            val newUser = User(
                                uid = userId,
                                userName = userName,
                                email = email,
                                hashedPassword = hashedPassword
                            )
                            // Save user details in Firestore
                            firestore.collection("Users")
                                .document(userId)
                                .set(newUser)
                                .addOnSuccessListener {
                                    Toast.makeText(
                                        this,
                                        "Registration successful! A verification email has been sent. Please verify your email.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    // Start checking for email verification automatically
                                    startEmailVerificationCheck()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(
                                        this,
                                        "Registration failed: ${e.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        } else {
                            Toast.makeText(
                                this,
                                "Failed to send verification email. Please try again.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } else {
                    Toast.makeText(
                        this,
                        "Registration failed: ${task.exception?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }


    private fun startEmailVerificationCheck() {
        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed(object : Runnable {
            override fun run() {
                auth.currentUser?.reload()?.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        if (auth.currentUser?.isEmailVerified == true) {
                            // Update Firestore with the verified status
                            val userId = auth.currentUser?.uid
                            if (userId != null) {
                                firestore.collection("Users")
                                    .document(userId)
                                    .update("emailVerified", true)
                                    .addOnSuccessListener {
                                        Toast.makeText(this@RegisterActivity, "Email verified!", Toast.LENGTH_SHORT).show()
                                        startActivity(Intent(this@RegisterActivity, LoginActivity::class.java))
                                        finish()
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(this@RegisterActivity, "Firestore update failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                        // Optionally retry or handle the error as needed
                                    }
                            }
                        } else {
                            // Continue checking after the delay
                            handler.postDelayed(this, verificationCheckDelay)
                        }
                    } else {
                        Toast.makeText(this@RegisterActivity, "Error checking verification status.", Toast.LENGTH_SHORT).show()
                        handler.postDelayed(this, verificationCheckDelay)
                    }
                }
            }
        }, verificationCheckDelay)
    }

    private fun hashPassword(password: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(password.toByteArray())
        return digest.joinToString(separator = "") { byte -> "%02x".format(byte) }
    }

    data class User(
        val uid: String = "",
        val userName: String = "",
        val email: String = "",
        val hashedPassword: String = "",
        val lastLoginTimestamp: Long = 0L,
        val signInProvider: String = "",
        val isEmailVerified: Boolean = false,
        val createdAt: Long = 0L,
        val index: Int = 0
    )
}