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

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val emailInput = findViewById<EditText>(R.id.emailInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)
        val loginButton = findViewById<Button>(R.id.loginButton)
        val googleSignInButton = findViewById<Button>(R.id.googleSignInButton)
        val registerLink = findViewById<TextView>(R.id.registerLink)

        // Ensure the full text is set
        val fullText = "Don't have an account? Register here"

        // Create the SpannableString
        val spannableString = SpannableString(fullText)

        // Define the start and end positions of the "Register here" part
        val start = fullText.indexOf("Register here")
        val end = fullText.length

        // Set the color for the "Register here" part to blue
        val blueColor = Color.parseColor("#321890")  // Blue color
        spannableString.setSpan(ForegroundColorSpan(blueColor), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        // Set the styled spannable text to the TextView
        registerLink.text = spannableString

        loginButton.setOnClickListener {
            val email = emailInput.text.toString()
            val password = passwordInput.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                // Add authentication logic here
                Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            }
        }

        googleSignInButton.setOnClickListener {
            // Implement Google Sign-In logic
            Toast.makeText(this, "Google Sign-In Clicked", Toast.LENGTH_SHORT).show()
        }

        registerLink.setOnClickListener {
            // Redirect to Register Activity
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }
}

