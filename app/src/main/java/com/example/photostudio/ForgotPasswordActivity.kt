package com.example.photostudio

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.LottieAnimationView
import com.google.firebase.auth.FirebaseAuth

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var emailInput: EditText
    private lateinit var resetButton: Button
    private lateinit var backButton: ImageView
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        emailInput = findViewById(R.id.emailInput)
        resetButton = findViewById(R.id.resetButton)
        backButton = findViewById(R.id.backButton) // Initialize back button
        auth = FirebaseAuth.getInstance()

        // Back button click event to close the activity
        backButton.setOnClickListener {
            finish()
        }

        resetButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            if (email.isEmpty()) {
                emailInput.error = "Email is required"
                showCustomToast("Please enter your email", R.raw.warning)
                return@setOnClickListener
            }

            showCustomToast("Sending reset link...", R.raw.mail)

            auth.sendPasswordResetEmail(email)
                .addOnSuccessListener {
                    showCustomToast("Reset link sent to your email", R.raw.success)
                    finish() // Close activity after sending email
                }
                .addOnFailureListener { e ->
                    showCustomToast("Error: ${e.message}", R.raw.error)
                }
        }
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
