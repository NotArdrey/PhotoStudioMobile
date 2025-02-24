package com.example.photostudio

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.LottieAnimationView
import com.google.firebase.firestore.FirebaseFirestore
import java.security.MessageDigest
import java.util.Date

class LoginActivity : AppCompatActivity() {

    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var loginButton: Button
    private lateinit var registerLink: TextView
    private lateinit var forgotPasswordLink: TextView
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
        forgotPasswordLink = findViewById(R.id.forgotPasswordLink)
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
        forgotPasswordLink.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }
    }

    private fun handleDeepLink(data: Uri?) {
        data?.let { uri ->
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
                showCustomToast("Email verified successfully!", R.raw.success)
            }
            .addOnFailureListener { e ->
                showCustomToast("Email verification failed: ${e.message}", R.raw.error)
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
                                showCustomToast("Email is not verified.", R.raw.warning)
                                showLoading(false)
                                return@addOnSuccessListener
                            }
                            saveUserToLocalStorage(user)

                            // ✅ Show Lottie animation on successful login
                            showCustomToast("Login successful!", R.raw.success)

                            startActivity(Intent(this, SplashScreenActivity::class.java))
                            finish()
                            return@addOnSuccessListener
                        }
                    }
                    showCustomToast("Invalid email or password.", R.raw.error)
                } else {
                    showCustomToast("User does not exist.", R.raw.error)
                }
                showLoading(false)
            }
            .addOnFailureListener { e ->
                showCustomToast("Error: ${e.message}", R.raw.error)
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

    private fun showCustomToast(message: String, animationResource: Int) {
        val layoutInflater = LayoutInflater.from(this)
        val view = layoutInflater.inflate(R.layout.custom_animated_toast, null)

        // Find views inside the custom toast
        val animationView: LottieAnimationView = view.findViewById(R.id.toastAnimation)
        val messageView: TextView = view.findViewById(R.id.toastMessage)

        // Set the message and animation
        messageView.text = message
        animationView.setAnimation(animationResource)
        animationView.playAnimation()

        // Create and show toast
        val toast = Toast(this)
        toast.duration = Toast.LENGTH_SHORT  // Shorter duration
        toast.setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, 250) // Lower center
        toast.view = view
        toast.show()

        // Auto dismiss after 1.5 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            toast.cancel()
        }, 1500)
    }

}
