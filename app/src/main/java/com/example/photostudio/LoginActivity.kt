package com.example.photostudio

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*
import java.util.Date

class LoginActivity : AppCompatActivity() {

    private lateinit var userNameInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var loginButton: Button
    private lateinit var registerLink: TextView

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var database: DatabaseReference

    data class User(
        val uid: String = "",
        val userName: String = "",
        val password: String = "",
        val lastLoginTimestamp: Long = Date().time,
        val signInProvider: String = "",
        val isEmailVerified: Boolean = false,
        val createdAt: Long = Date().time,
        val index: Int = 0
    )

    companion object {
        private const val TAG = "LoginActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        initializeViews()
        initializeFirebase()
        setupClickListeners()
    }

    private fun initializeViews() {
        userNameInput = findViewById(R.id.userNameInput)
        passwordInput = findViewById(R.id.passwordInput)
        loginButton = findViewById(R.id.loginButton)
        registerLink = findViewById(R.id.registerLink)
        sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
    }

    private fun initializeFirebase() {
        database = FirebaseDatabase.getInstance().reference.child("users")
    }

    private fun setupClickListeners() {
        loginButton.setOnClickListener {
            if (::userNameInput.isInitialized && ::passwordInput.isInitialized) {
                handleUserLogin()
            } else {
                showError("Error: Views not initialized properly")
            }
        }
        registerLink.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }


    private fun handleUserLogin() {
        val userName = userNameInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()

        when {
            userName.isEmpty() -> {
                userNameInput.error = "Username is required"
                return
            }
            password.isEmpty() -> {
                passwordInput.error = "Password is required"
                return
            }
        }


        showLoading(true)

        database.orderByChild("userName").equalTo(userName).get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    for (childSnapshot in snapshot.children) {
                        val user = childSnapshot.getValue(User::class.java)
                        if (user != null && user.password == password) {
                            saveUserToLocalStorage(user)
                            showToast("Login successful!")
                            startActivity(Intent(this, LandingPage::class.java))
                            finish()
                            return@addOnSuccessListener
                        }
                    }

                    showError("Invalid username or password.")
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

    private fun saveUserToDatabase(user: User) {
        val userId = database.push().key ?: return
        val newUser = user.copy(uid = userId)
        database.child(userId).setValue(newUser)
            .addOnSuccessListener {
                showToast("User saved successfully")
                saveUserToLocalStorage(newUser)
                startActivity(Intent(this, LandingPage::class.java))
                finish()
            }
            .addOnFailureListener { e ->
                showError("Failed to save user: ${e.message}")
            }
    }

    private fun saveUserToLocalStorage(user: User) {
        sharedPreferences.edit().apply {
            putString("uid", user.uid)
            putString("userName", user.userName)
            putBoolean("isEmailVerified", user.isEmailVerified)
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
