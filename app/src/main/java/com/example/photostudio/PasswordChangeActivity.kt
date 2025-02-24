package com.example.photostudio

import android.app.AlertDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.LottieAnimationView
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth

class PasswordChangeActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var oldPasswordInput: EditText
    private lateinit var newPasswordInput: EditText
    private lateinit var confirmNewPasswordInput: EditText
    private lateinit var btnChangePassword: Button
    private lateinit var backButton: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_password)

        auth = FirebaseAuth.getInstance()

        oldPasswordInput = findViewById(R.id.oldPasswordInput)
        newPasswordInput = findViewById(R.id.newPasswordInput)
        confirmNewPasswordInput = findViewById(R.id.confirmNewPasswordInput)
        btnChangePassword = findViewById(R.id.btnChangePassword)
        backButton = findViewById(R.id.backButton) // Initialize back button

        backButton.setOnClickListener {
            finish() // Go back to previous screen
        }

        btnChangePassword.setOnClickListener {
            showConfirmationDialog()
        }
    }

    private fun showConfirmationDialog() {
        val oldPassword = oldPasswordInput.text.toString().trim()
        val newPassword = newPasswordInput.text.toString().trim()
        val confirmNewPassword = confirmNewPasswordInput.text.toString().trim()

        if (oldPassword.isEmpty() || newPassword.isEmpty() || confirmNewPassword.isEmpty()) {
            showCustomToast("All fields are required", R.raw.warning)
            return
        }

        if (newPassword != confirmNewPassword) {
            showCustomToast("New passwords do not match", R.raw.error)
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Confirm Password Change")
            .setMessage("Are you sure you want to change your password?")
            .setPositiveButton("Yes") { _, _ ->
                changePassword(oldPassword, newPassword)
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun changePassword(oldPassword: String, newPassword: String) {
        val user = auth.currentUser
        user?.let {
            val credential = EmailAuthProvider.getCredential(it.email!!, oldPassword)

            // Reauthenticate user with old password
            it.reauthenticate(credential)
                .addOnCompleteListener { authTask ->
                    if (authTask.isSuccessful) {
                        // Update password
                        user.updatePassword(newPassword)
                            .addOnCompleteListener { updateTask ->
                                if (updateTask.isSuccessful) {
                                    showCustomToast("Password changed successfully", R.raw.success)
                                    finish() // Close the activity
                                } else {
                                    showCustomToast("Failed to change password", R.raw.error)
                                }
                            }
                    } else {
                        showCustomToast("Old password is incorrect", R.raw.error)
                    }
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
