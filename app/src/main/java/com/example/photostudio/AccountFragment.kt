package com.example.photostudio

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions

class AccountFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var functions: FirebaseFunctions

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_account_page, container, false)

        auth = FirebaseAuth.getInstance()
        functions = FirebaseFunctions.getInstance()

        val currentUser = auth.currentUser
        val editAccount = view.findViewById<TextView>(R.id.editAccount)
        val subtitleTextView = view.findViewById<TextView>(R.id.subtitleTextView)
        val userEmail = currentUser?.email ?: ""

        subtitleTextView.text = "Welcome ${currentUser?.displayName ?: "User"}"

        editAccount.setOnClickListener {
            if (userEmail.isNotEmpty()) {
                sendOtp(userEmail)
            } else {
                showToast("No email associated with this account")
            }
        }

        return view
    }

    private fun sendOtp(email: String) {
        val data = hashMapOf("email" to email)

        functions.getHttpsCallable("sendOTP")
            .call(data)
            .addOnSuccessListener { result ->
                val resultData = result.getData() as? Map<*, *> // Use getData()
                val otp = resultData?.get("otp")?.toString()

                if (!otp.isNullOrEmpty()) {
                    showToast("OTP sent to $email")
                    navigateToCodeVerification(email, otp)
                } else {
                    showToast("Failed to retrieve OTP")
                }
            }
            .addOnFailureListener { e ->
                showToast("Failed to send OTP: ${e.localizedMessage}")
            }
    }


    private fun navigateToCodeVerification(email: String, otp: String) {
        val intent = Intent(requireContext(), CodeVerificationPage::class.java)
        intent.putExtra("email", email)
        intent.putExtra("otp", otp)
        startActivity(intent)
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}
