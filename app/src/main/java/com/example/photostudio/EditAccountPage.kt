package com.example.photostudio

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class EditAccountPage : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var databaseReference: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_account_page)

        auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        databaseReference = FirebaseDatabase.getInstance().getReference("Users")

        val usernameInput = findViewById<EditText>(R.id.usernameInput)
        val emailInput = findViewById<EditText>(R.id.emailInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)
        val confirmPasswordInput = findViewById<EditText>(R.id.confirmPasswordInput)
        val confirmButton = findViewById<Button>(R.id.confirmButton)
        val cancelButton = findViewById<Button>(R.id.cancelButton)

        currentUser?.let {
            databaseReference.child(it.uid).get().addOnSuccessListener { snapshot ->
                val userData = snapshot.getValue(User::class.java)
                usernameInput.setText(userData?.userName)
                emailInput.setText(userData?.email)
            }
        }

        confirmButton.setOnClickListener {
            val newUsername = usernameInput.text.toString().trim()
            val newEmail = emailInput.text.toString().trim()
            val newPassword = passwordInput.text.toString().trim()
            val confirmPassword = confirmPasswordInput.text.toString().trim()

            if (newUsername.isEmpty() || newEmail.isEmpty()) {
                Toast.makeText(this, "Username and email cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPassword.isNotEmpty() && newPassword != confirmPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            currentUser?.let { user ->
                val updates = mutableMapOf<String, Any>(
                    "userName" to newUsername,
                    "email" to newEmail
                )
                databaseReference.child(user.uid).updateChildren(updates).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        if (newPassword.isNotEmpty()) {
                            user.updatePassword(newPassword).addOnCompleteListener { passTask ->
                                if (passTask.isSuccessful) {
                                    Toast.makeText(this, "Account updated successfully", Toast.LENGTH_SHORT).show()
                                    finish()
                                } else {
                                    Toast.makeText(this, "Failed to update password", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            Toast.makeText(this, "Account updated successfully", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    } else {
                        Toast.makeText(this, "Failed to update account", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        cancelButton.setOnClickListener {
            finish()
        }
    }

    data class User(
        var userName: String? = null,
        var email: String? = null
    )
}
