package com.example.photostudio

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
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
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class RegisterActivity : AppCompatActivity() {


    private lateinit var auth: FirebaseAuth

    private var databaseReference: DatabaseReference? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registration)


        auth = FirebaseAuth.getInstance()
        databaseReference = FirebaseDatabase.getInstance().getReference("Users")


        val userNameInput = findViewById<EditText>(R.id.userNameInput)
        val emailInput = findViewById<EditText>(R.id.emailInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)
        val confirmPasswordInput = findViewById<EditText>(R.id.confirmPasswordInput)
        val registerButton = findViewById<Button>(R.id.registerButton)
        val loginLink = findViewById<TextView>(R.id.loginLink)


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
                password.isNotEmpty() && confirmPassword.isNotEmpty()) {

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
                    firebaseUser?.sendEmailVerification()?.addOnCompleteListener { emailTask ->
                        if (emailTask.isSuccessful) {

                            val userId = firebaseUser.uid
                            val newUser = User(userName, email)
                            databaseReference?.child(userId)?.setValue(newUser)

                            Toast.makeText(
                                this,
                                "Registration successful! Please check your email to verify your account.",
                                Toast.LENGTH_LONG
                            ).show()


                            startActivity(Intent(this, LoginActivity::class.java))
                            finish()
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


    data class User(
        var userName: String? = null,
        var email: String? = null
    )
}
