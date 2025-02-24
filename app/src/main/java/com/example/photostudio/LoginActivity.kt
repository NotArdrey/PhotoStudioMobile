package com.example.photostudio

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import java.security.MessageDigest
import java.util.Date

class LoginActivity : AppCompatActivity() {

    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var loginButton: Button
    private lateinit var registerLink: TextView
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var firestore: FirebaseFirestore

    data class User(
        val uid: String = "",
        val email: String = "",
        val hashedPassword: String = "",
        val lastLoginTimestamp: Long = Date().time,
        val signInProvider: String = "",
        val emailVerified: Boolean = false,
        val createdAt: Long = Date().time,
        val index: Int = 0
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        initializeViews()
        initializeFirestore()
        handleDeepLink(intent?.data)
        setupClickListeners()
    }

    private fun initializeViews() {
        emailInput = findViewById(R.id.emailInput)
        passwordInput = findViewById(R.id.passwordInput)
        loginButton = findViewById(R.id.loginButton)
        registerLink = findViewById(R.id.registerLink)
        sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
    }

    private fun initializeFirestore() {
        firestore = FirebaseFirestore.getInstance()
    }

    private fun setupClickListeners() {
        loginButton.setOnClickListener {
            handleUserLogin()
        }
        registerLink.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun handleDeepLink(data: Uri?) {
        data?.let { uri ->
            // Expecting a link like: https://www.yourapp.com/verify?uid=USER_ID
            val uid = uri.getQueryParameter("uid")
            if (uid != null) {
                verifyEmail(uid)
            }
        }
    }

    private fun verifyEmail(uid: String) {
        firestore.collection("Users")
            .document(uid)
            .update("emailVerified", true)
            .addOnSuccessListener {
                showToast("Email verified successfully!")
            }
            .addOnFailureListener { e ->
                showError("Email verification failed: ${e.message}")
            }
    }

    private fun handleUserLogin() {
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()

        when {
            email.isEmpty() -> {
                emailInput.error = "Email is required"
                return
            }
            password.isEmpty() -> {
                passwordInput.error = "Password is required"
                return
            }
        }

        // Hardcoded testing credentials: bypass Firestore if email and password are test values
        if (email == "test@example.com" && password == "test") {
            val dummyUser = User(
                uid = "test_uid",
                email = "test@example.com",
                hashedPassword = hashPassword(password),
                lastLoginTimestamp = Date().time,
                signInProvider = "hardcoded",
                emailVerified = true,
                createdAt = Date().time,
                index = 0
            )
            saveUserToLocalStorage(dummyUser)
            showToast("Login successful (hardcoded test user)!")
            startActivity(Intent(this, SplashScreenActivity::class.java))
            finish()
            return
        }

        showLoading(true)
        val hashedPassword = hashPassword(password)

        firestore.collection("Users")
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty) {
                    for (doc in snapshot.documents) {
                        val user = doc.toObject(User::class.java)
                        if (user != null && user.hashedPassword == hashedPassword) {
                            if (!user.emailVerified) {
                                showError("Email is not verified. Please verify your email before logging in.")
                                showLoading(false)
                                return@addOnSuccessListener
                            }
                            saveUserToLocalStorage(user)
                            showToast("Login successful!")
                            startActivity(Intent(this, SplashScreenActivity::class.java))
                            finish()
                            return@addOnSuccessListener
                        }
                    }
                    showError("Invalid email or password.")
                } else {
                    showError("User does not exist.")
                }
                showLoading(false)
            }
            .addOnFailureListener { e ->
                showError("Error: ${e.message}")
                showLoading(false)
            }
    }

    private fun hashPassword(password: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(password.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun saveUserToLocalStorage(user: User) {
        sharedPreferences.edit().apply {
            putString("uid", user.uid)
            putString("email", user.email)
            putBoolean("emailVerified", user.emailVerified)
            apply()
        }
    }

    private fun showLoading(show: Boolean) {
        loginButton.isEnabled = !show
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
