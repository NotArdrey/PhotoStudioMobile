package com.example.photostudio

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class RegisterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registration)

        val userNameInput = findViewById<EditText>(R.id.userNameInput)
        val emailInput = findViewById<EditText>(R.id.emailInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)
        val confirmPasswordInput = findViewById<EditText>(R.id.confirmPasswordInput)
        val registerButton = findViewById<Button>(R.id.registerButton)
        val googleSignInButton = findViewById<Button>(R.id.googleSignInButton)
        val loginLink = findViewById<TextView>(R.id.loginLink)

        registerButton.setOnClickListener {
            val userName = userNameInput.text.toString()
            val email = emailInput.text.toString()
            val password = passwordInput.text.toString()
            val confirmPassword = confirmPasswordInput.text.toString()
            val fullText = "Already have an account? Login here"

            // Create the SpannableString
            val spannableString = SpannableString(fullText)

            // Define the start and end positions of the "Register here" part
            val start = fullText.indexOf("Login here")
            val end = fullText.length

            // Set the color for the "Register here" part to blue
            val blueColor = Color.parseColor("#321890")  // Blue color
            spannableString.setSpan(ForegroundColorSpan(blueColor), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

            // Set the styled spannable text to the TextView
            loginLink.text = spannableString

            if (userName.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty() && confirmPassword.isNotEmpty()) {
                if (password == confirmPassword) {
                    // Add registration logic here
                    Toast.makeText(this, "Registration Successful", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            }
        }

        googleSignInButton.setOnClickListener {
            // Implement Google Sign-In logic
            Toast.makeText(this, "Google Sign-In Clicked", Toast.LENGTH_SHORT).show()
        }

        loginLink.setOnClickListener {
            // Redirect to Login Activity
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
    }
}
