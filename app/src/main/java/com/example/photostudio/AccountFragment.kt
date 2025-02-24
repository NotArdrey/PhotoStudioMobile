package com.example.photostudio

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.airbnb.lottie.LottieAnimationView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Properties
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.MessagingException
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

// A helper class to send email using JavaMail API
class MailSender(private val username: String, private val password: String) {

    private val session: Session

    init {
        val props = Properties().apply {
            put("mail.smtp.auth", "true")
            put("mail.smtp.starttls.enable", "true")
            put("mail.smtp.host", "smtp.gmail.com")
            put("mail.smtp.port", "587")
        }
        session = Session.getInstance(props, object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication(username, password)
            }
        })
    }

    @Throws(MessagingException::class)
    fun sendMail(subject: String, body: String, recipient: String) {
        val message = MimeMessage(session).apply {
            setFrom(InternetAddress(username))
            setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient))
            this.subject = subject
            setText(body)
        }
        Transport.send(message)
    }
}

class AccountFragment : Fragment() {

    private val TAG = "AccountFragment"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_account_page, container, false)

        val btnLogout = view.findViewById<Button>(R.id.btnLogout)

        btnLogout.setOnClickListener {
            showLogoutConfirmation()
        }

        val sharedPref = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val uid = sharedPref.getString("uid", null)
        val email = sharedPref.getString("email", null)

        if (uid == null || email.isNullOrEmpty()) {
            showCustomToast("Please log in to access your account", R.raw.warning)
            return view
        }

        // Get references for each clickable TextView
        val editAccount = view.findViewById<TextView>(R.id.editAccount)
        val activeBookings = view.findViewById<TextView>(R.id.activeBookings)
        val viewBookingHistory = view.findViewById<TextView>(R.id.viewBookingHistory)
        val terms = view.findViewById<TextView>(R.id.terms)
        val subtitleTextView = view.findViewById<TextView>(R.id.subtitleTextView)
        val displayName = sharedPref.getString("displayName", "User")
        subtitleTextView.text = "Welcome $displayName"

        Log.d(TAG, "Current UID: $uid")
        Log.d(TAG, "User email: $email")

        // Edit Account click listener (sends OTP)
        editAccount.setOnClickListener {
            if (email.isNotEmpty()) {
                // Show custom toast with mail animation before sending OTP
                showConfirmationToast("Sending OTP to $email", R.raw.mail)  // Change animation to mail.json
                sendOtpDirectly(email)
            } else {
                showCustomToast("No email associated with this account", R.raw.warning)
            }
        }

        // Active Bookings click listener
        activeBookings.setOnClickListener {
            // Replace ActiveBookingsActivity with your actual activity class for active bookings
            val intent = Intent(requireContext(), ActiveBookingPage::class.java)
            startActivity(intent)
        }

        // View Booking History click listener
        viewBookingHistory.setOnClickListener {
            // Replace BookingHistoryActivity with your actual activity class for booking history
            val intent = Intent(requireContext(), BookingHistoryPage::class.java)
            startActivity(intent)
        }

        // Terms and Conditions click listener
        terms.setOnClickListener {
            // Replace TermsAndConditionsActivity with your actual activity class for T&C
            val intent = Intent(requireContext(), TermsAndCondition::class.java)
            startActivity(intent)
        }

        return view
    }

    private fun generateDummyOtp(): String {
        return (100000..999999).random().toString()
    }

    private fun sendOtpDirectly(email: String) {
        val otp = generateDummyOtp()
        Log.d(TAG, "OTP generated: $otp")

        // Show confirmation toast before sending OTP
        showConfirmationToast("Sending OTP to $email", R.raw.mail) // Change animation to mail.json

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val senderEmail = "neilardrey14@gmail.com"
                val senderPassword = "rihz vsdd beov fpit"
                val mailSender = MailSender(senderEmail, senderPassword)
                val subject = "Your OTP Code"
                val body = "Your OTP is: $otp"

                mailSender.sendMail(subject, body, email)

                // Switch to Main dispatcher to update UI
                withContext(Dispatchers.Main) {
                    // Show confirmation that OTP has been sent
                    showConfirmationToast("OTP sent to $email", R.raw.mail)
                    navigateToCodeVerification(email, otp)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error sending email: ${e.message}")
                withContext(Dispatchers.Main) {
                    // Show cancellation toast if an error occurs
                    showCancelToast("Error sending OTP: ${e.message}")
                }
            }
        }
    }

    private fun logoutUser() {
        val sharedPref = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        sharedPref.edit().clear().apply() // Clear all saved user data

        showConfirmationToast("Logged out successfully", R.raw.mail)

        // Navigate to Login Page and clear the back stack
        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(requireContext()).apply {
            setTitle("Logout")
            setMessage("Are you sure you want to log out?")
            setPositiveButton("Yes") { _, _ ->
                logoutUser() // Proceed with logout
            }
            setNegativeButton("No") { dialog, _ ->
                dialog.dismiss() // Close dialog
                showCancelToast("Logout canceled")
            }
            setCancelable(false)
            show()
        }
    }

    private fun navigateToCodeVerification(email: String, otp: String) {
        Log.d(TAG, "Navigating to CodeVerificationPage with email: $email and OTP: $otp")
        val intent = Intent(requireContext(), CodeVerificationPage::class.java)
        intent.putExtra("email", email)
        intent.putExtra("otp", otp)
        startActivity(intent)
    }

    private fun showConfirmationToast(message: String, animationResource: Int) {
        showCustomToast(message, animationResource)
    }

    private fun showCancelToast(message: String) {
        showCustomToast(message, R.raw.success)
    }

    private fun showCustomToast(message: String, animationResource: Int) {
        val layoutInflater = LayoutInflater.from(requireContext())
        val view = layoutInflater.inflate(R.layout.custom_animated_toast, null)

        // Find views inside the custom toast
        val animationView: LottieAnimationView = view.findViewById(R.id.toastAnimation)
        val messageView: TextView = view.findViewById(R.id.toastMessage)

        // Set the message and animation
        messageView.text = message
        animationView.setAnimation(animationResource)
        animationView.playAnimation()

        // Create and show toast
        val toast = Toast(requireContext())
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
