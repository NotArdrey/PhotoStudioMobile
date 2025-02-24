package com.example.photostudio

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class EditAccountPage : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private val TAG = "EditAccountPage"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_account_page)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val usernameInput = findViewById<EditText>(R.id.usernameInput)
        val emailInput = findViewById<EditText>(R.id.emailInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput) // Password confirmation
        val confirmButton = findViewById<Button>(R.id.confirmButton)
        val cancelButton = findViewById<Button>(R.id.cancelButton)
        val backButton = findViewById<ImageView>(R.id.backButton)
        val changePasswordButton = findViewById<Button>(R.id.btnChangePassword)

        emailInput.isEnabled = false
        usernameInput.isEnabled = false // Prevents editing username

        // Pre-fill the fields
        val passedUserName = intent.getStringExtra("userName")
        val passedEmail = intent.getStringExtra("email")
        Log.d(TAG, "Received userName: $passedUserName, email: $passedEmail")
        usernameInput.setText(passedUserName ?: "")
        emailInput.setText(passedEmail ?: "")

        changePasswordButton.setOnClickListener {
            val intent = Intent(this, PasswordChangeActivity::class.java)
            startActivity(intent)
        }

        confirmButton.setOnClickListener {
            val emailField = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (password.isEmpty()) {
                showToast("⚠️ Please enter your password!")
                return@setOnClickListener
            }

            // Re-authenticate user
            auth.signInWithEmailAndPassword(emailField, password)
                .addOnCompleteListener { authTask ->
                    if (authTask.isSuccessful) {
                        showToast("✅ Account confirmed!")
                        redirectToAccountActivity()
                    } else {
                        showToast("❌ Incorrect password. Try again.")
                    }
                }
        }

        cancelButton.setOnClickListener {
            finish()
        }

        backButton.setOnClickListener {
            finish()
        }
    }

    private fun redirectToAccountActivity() {
        val intent = Intent(this, AccountFragment::class.java)
        startActivity(intent)
        finish()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
