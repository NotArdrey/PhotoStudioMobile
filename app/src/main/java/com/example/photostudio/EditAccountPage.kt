package com.example.photostudio

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.security.MessageDigest

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
        val currentPasswordInput = findViewById<EditText>(R.id.passwordInput)
        val newPasswordInput = findViewById<EditText>(R.id.newPasswordInput)
        val confirmNewPasswordInput = findViewById<EditText>(R.id.confirmNewPasswordInput)
        val confirmButton = findViewById<Button>(R.id.confirmButton)
        val cancelButton = findViewById<Button>(R.id.cancelButton)
        val backButton = findViewById<ImageView>(R.id.backButton) // Back button

        emailInput.isEnabled = false

        currentPasswordInput.hint = "Enter current password"
        newPasswordInput.hint = "Enter new password"
        confirmNewPasswordInput.hint = "Re-enter new password"

        // Pre-fill the fields with data passed via the Intent.
        val passedUserName = intent.getStringExtra("userName")
        val passedEmail = intent.getStringExtra("email")
        Log.d(TAG, "Received userName: $passedUserName, email: $passedEmail")
        usernameInput.setText(passedUserName ?: "")
        emailInput.setText(passedEmail ?: "")

        confirmButton.setOnClickListener {
            val newUsername = usernameInput.text.toString().trim()
            val emailField = emailInput.text.toString().trim()
            val currentPassword = currentPasswordInput.text.toString().trim()
            val newPassword = newPasswordInput.text.toString().trim()
            val confirmPassword = confirmNewPasswordInput.text.toString().trim()

            if (newUsername.isEmpty()) {
                Toast.makeText(this, "Username cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // If a new password is provided, ensure current and confirmation are valid.
            if (newPassword.isNotEmpty()) {
                if (currentPassword.isEmpty()) {
                    Toast.makeText(this, "Please enter your current password", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (confirmPassword.isEmpty()) {
                    Toast.makeText(this, "Please confirm your new password", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (newPassword != confirmPassword) {
                    Toast.makeText(this, "New passwords do not match", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            // Query Firestore to find a user document where the email matches the input.
            firestore.collection("Users")
                .whereEqualTo("email", emailField)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    if (querySnapshot.isEmpty) {
                        Toast.makeText(this, "No account found with this email", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }
                    // Assuming emails are unique, take the first matching document.
                    val userDocRef = querySnapshot.documents[0].reference

                    // Update the username field.
                    val userUpdates = hashMapOf("userName" to newUsername)
                    userDocRef.set(userUpdates, SetOptions.merge())
                        .addOnCompleteListener { usernameTask ->
                            if (usernameTask.isSuccessful) {
                                // If there is no password change, finish here.
                                if (newPassword.isEmpty()) {
                                    Toast.makeText(this, "Account updated successfully", Toast.LENGTH_SHORT).show()
                                    redirectToAccountActivity()
                                } else {
                                    // For a password change, reauthenticate the user.
                                    val currentUser = auth.currentUser
                                    if (currentUser == null) {
                                        Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
                                        return@addOnCompleteListener
                                    }
                                    val credential = EmailAuthProvider.getCredential(currentUser.email!!, currentPassword)
                                    currentUser.reauthenticate(credential)
                                        .addOnCompleteListener { reAuthTask ->
                                            if (reAuthTask.isSuccessful) {
                                                // Update FirebaseAuth password.
                                                currentUser.updatePassword(newPassword)
                                                    .addOnCompleteListener { passTask ->
                                                        if (passTask.isSuccessful) {
                                                            // Update the stored password hash in Firestore.
                                                            val newHashedPassword = hashPassword(newPassword)
                                                            userDocRef.update("hashedPassword", newHashedPassword)
                                                                .addOnCompleteListener { hashTask ->
                                                                    if (hashTask.isSuccessful) {
                                                                        Toast.makeText(this, "Account updated successfully", Toast.LENGTH_SHORT).show()
                                                                        redirectToAccountActivity()
                                                                    } else {
                                                                        Toast.makeText(this, "Password updated but failed to update hash", Toast.LENGTH_SHORT).show()
                                                                        Log.e(TAG, "Hash update error: ${hashTask.exception?.message}")
                                                                    }
                                                                }
                                                        } else {
                                                            Toast.makeText(this, "Failed to update password", Toast.LENGTH_SHORT).show()
                                                            Log.e(TAG, "Password update error: ${passTask.exception?.message}")
                                                        }
                                                    }
                                            } else {
                                                Toast.makeText(this, "Current password is incorrect", Toast.LENGTH_SHORT).show()
                                                Log.e(TAG, "Re-auth error: ${reAuthTask.exception?.message}")
                                            }
                                        }
                                }
                            } else {
                                Toast.makeText(this, "Failed to update account", Toast.LENGTH_SHORT).show()
                                Log.e(TAG, "Username update error: ${usernameTask.exception?.message}")
                            }
                        }
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(this, "Error querying account: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        }

        cancelButton.setOnClickListener {
            finish()
        }

        // Back Button Functionality
        backButton.setOnClickListener {
            finish() // Closes the activity and returns to the previous one
        }
    }

    // Helper function to hash passwords using SHA-256.
    private fun hashPassword(password: String): String {
        val bytes = password.toByteArray(Charsets.UTF_8)
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.joinToString(separator = "") { "%02x".format(it) }
    }

    // Redirect to the account screen (replace AccountFragment with your actual destination Activity or Fragment).
    private fun redirectToAccountActivity() {
        val intent = Intent(this, AccountFragment::class.java) // Changed to AccountActivity
        startActivity(intent)
        finish()
    }
}
