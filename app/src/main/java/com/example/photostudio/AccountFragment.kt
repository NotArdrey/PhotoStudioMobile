package com.example.photostudio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
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

        val sharedPref = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val uid = sharedPref.getString("uid", null)
        val email = sharedPref.getString("userEmail", null)

        if (uid == null || email.isNullOrEmpty()) {
            showToast("Please log in to access your account")
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
                sendOtpDirectly(email)
            } else {
                showToast("No email associated with this account")
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
                    showToast("OTP sent to $email")
                    navigateToCodeVerification(email, otp)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error sending email: ${e.message}")
                withContext(Dispatchers.Main) {
                    showToast("Error sending email: ${e.message}")
                }
            }
        }
    }

    private fun navigateToCodeVerification(email: String, otp: String) {
        Log.d(TAG, "Navigating to CodeVerificationPage with email: $email and OTP: $otp")
        val intent = Intent(requireContext(), CodeVerificationPage::class.java)
        intent.putExtra("email", email)
        intent.putExtra("otp", otp)
        startActivity(intent)
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}
