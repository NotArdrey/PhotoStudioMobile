package com.example.photostudio

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.*
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore
import com.example.photostudio.utils.EncryptionUtils
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class RebookPage : AppCompatActivity() {

    // UI elements
    private lateinit var dateEditText: TextInputEditText
    private lateinit var timeDropdown: MaterialAutoCompleteTextView
    private lateinit var defaultBackdropSpinner: MaterialAutoCompleteTextView
    private lateinit var optionalBackdropSpinner: MaterialAutoCompleteTextView
    private lateinit var extraPersonQtyEditText: EditText
    private lateinit var softCopyQtyEditText: EditText
    private lateinit var rebookPaymentButton: MaterialButton
    private lateinit var paymentWebView: WebView

    // Firestore & Payment
    private lateinit var firestore: FirebaseFirestore
    private lateinit var viewModel: PaymentViewModel

    // Booking details received from ActiveBookingPage
    private var bookingId: String? = null  // Booking document ID to update
    private var appointmentDate: String? = null
    private var appointmentTime: String? = null
    private var defaultBackdrop: String? = null
    private var selectedExtraBackdrop: String? = null
    private var extraPersonQty: Int = 0
    private var softCopyQty: Int = 0
    private var description: String? = null
    private var paymentType: String? = null
    private var paymongoName: String? = null
    private var totalAmount: Int = 0
    private var uid: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rebook_page)

        firestore = FirebaseFirestore.getInstance()

        // Initialize UI components
        dateEditText = findViewById(R.id.dateEditText)
        timeDropdown = findViewById(R.id.timeDropdown)
        defaultBackdropSpinner = findViewById(R.id.defaultBackdropSpinner)
        optionalBackdropSpinner = findViewById(R.id.optionalBackdropSpinner)
        extraPersonQtyEditText = findViewById(R.id.extraPersonQty)
        softCopyQtyEditText = findViewById(R.id.softCopyQty)
        rebookPaymentButton = findViewById(R.id.rebook_payment_button)
        paymentWebView = findViewById(R.id.paymentWebView)

        // Retrieve booking details from intent extras
        bookingId = intent.getStringExtra("id")
        appointmentDate = intent.getStringExtra("appointmentDate")
        appointmentTime = intent.getStringExtra("appointmentTime")
        defaultBackdrop = intent.getStringExtra("defaultBackdrop")
        description = intent.getStringExtra("description")
        extraPersonQty = intent.getIntExtra("extraPersonQty", 0)
        paymentType = intent.getStringExtra("paymentType")
        paymongoName = intent.getStringExtra("paymongoName")
        selectedExtraBackdrop = intent.getStringExtra("selectedExtraBackdrop")
        softCopyQty = intent.getIntExtra("softCopyQty", 0)
        totalAmount = intent.getIntExtra("totalAmount", 0)
        uid = intent.getStringExtra("uid")

        // Pre-fill the UI fields with booking details
        dateEditText.setText(appointmentDate ?: "")
        timeDropdown.setText(appointmentTime ?: "")
        defaultBackdropSpinner.setText(defaultBackdrop ?: "")
        optionalBackdropSpinner.setText(selectedExtraBackdrop ?: "")
        extraPersonQtyEditText.setText(extraPersonQty.toString())
        softCopyQtyEditText.setText(softCopyQty.toString())

        // Initialize the PaymentViewModel
        viewModel = ViewModelProvider(this, PaymentViewModelFactory())
            .get(PaymentViewModel::class.java)

        // Configure the payment WebView to handle redirection URLs (payment success/failure)
        paymentWebView.settings.javaScriptEnabled = true
        paymentWebView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                url?.let {
                    if (it.startsWith("https://com.example.photostudio/payment") ||
                        it.startsWith("https://pm.link/gcash/success") ||
                        it.startsWith("https://pm.link/gcash/failed")
                    ) {
                        val uri = Uri.parse(it)
                        var status = uri.getQueryParameter("status")
                        if (status == null) {
                            status = when {
                                it.contains("gcash/success", ignoreCase = true) -> "paid"
                                it.contains("gcash/failed", ignoreCase = true) -> "failed"
                                else -> null
                            }
                        }
                        if (status.equals("paid", ignoreCase = true) ||
                            status.equals("succeeded", ignoreCase = true)
                        ) {
                            Toast.makeText(this@RebookPage, "Payment successful", Toast.LENGTH_SHORT).show()
                        } else if (status.equals("failed", ignoreCase = true)) {
                            Toast.makeText(this@RebookPage, "Payment failed", Toast.LENGTH_SHORT).show()
                        }
                        paymentWebView.visibility = View.GONE
                        Handler(Looper.getMainLooper()).post { clearFocusAndHideKeyboard() }
                        return true
                    }
                }
                return false
            }
        }

        // Observe changes to the PaymentViewModel LiveData objects
        viewModel.redirectUrl.observe(this) { url ->
            if (!url.isNullOrEmpty()) {
                paymentWebView.visibility = View.VISIBLE
                paymentWebView.loadUrl(url)
            } else {
                Toast.makeText(this, "No redirection URL available.", Toast.LENGTH_LONG).show()
            }
        }

        viewModel.paymentDetails.observe(this) { json ->
            json?.let {
                val attributes = it.optJSONObject("data")?.optJSONObject("attributes")
                val status = attributes?.optString("status")?.trim()?.toLowerCase()
                if (status == "paid" || status == "succeeded") {
                    Toast.makeText(this, "Payment completed successfully.", Toast.LENGTH_LONG).show()
                    // Get additional details from the API response
                    val paymongoNameFromResponse = attributes?.optJSONObject("billing")?.optString("name") ?: ""
                    // Build updated payment data using the booking details from intent extras
                    val updatedPaymentData = mapOf(
                        "appointmentDate" to (appointmentDate ?: ""),
                        "appointmentTime" to (appointmentTime ?: ""),
                        "defaultBackdrop" to (defaultBackdrop ?: ""),
                        "description" to (description ?: ""),
                        "extraPersonQty" to extraPersonQty,
                        "paymentType" to (paymentType ?: ""),
                        "paymongoName" to paymongoNameFromResponse,
                        "selectedExtraBackdrop" to (selectedExtraBackdrop ?: ""),
                        "softCopyQty" to softCopyQty,
                        "totalAmount" to totalAmount,
                        "uid" to (uid ?: ""),
                        "archive" to "no",
                        "complete" to "no"
                    )
                    // Update the existing payment document identified by bookingId
                    if (bookingId != null) {
                        firestore.collection("payments")
                            .document(bookingId!!)
                            .update(updatedPaymentData)
                            .addOnSuccessListener {
                                finish()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Error updating payment data", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        Log.e("RebookPage", "Booking ID is null; cannot update booking.")
                    }
                } else {
                    Toast.makeText(this, "Payment not completed yet. Status: $status", Toast.LENGTH_LONG).show()
                }
            } ?: Log.e("RebookPage", "Received null payment details JSON.")
        }

        // Set up the rebook payment button click to initiate the payment process.
        rebookPaymentButton.setOnClickListener {
            viewModel.createPaymentLink(totalAmount, description ?: "Rebook Payment")
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.data?.let { uri ->
            var status = uri.getQueryParameter("status")
            if (status == null) {
                status = when {
                    uri.toString().contains("gcash/success", ignoreCase = true) -> "paid"
                    uri.toString().contains("gcash/failed", ignoreCase = true) -> "failed"
                    else -> null
                }
            }
            if (status != null) {
                if (status.equals("paid", ignoreCase = true) || status.equals("succeeded", ignoreCase = true)) {
                    Toast.makeText(this, "Payment successful", Toast.LENGTH_SHORT).show()
                    val paymentLinkId = uri.getQueryParameter("paymentLinkId")
                    if (paymentLinkId != null) {
                        viewModel.getPaymentsForLink(paymentLinkId, description ?: "")
                    }
                } else if (status.equals("failed", ignoreCase = true)) {
                    Toast.makeText(this, "Payment failed", Toast.LENGTH_SHORT).show()
                }
                Handler(Looper.getMainLooper()).post { clearFocusAndHideKeyboard() }
            } else {
                uri.getQueryParameter("payment_id")?.let { paymentId ->
                    viewModel.getPaymentDetails(paymentId)
                }
            }
        }
    }

    private fun clearFocusAndHideKeyboard() {
        currentFocus?.let { view ->
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    // PaymentViewModel and its Factory remain largely unchanged.
    class PaymentViewModelFactory : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(PaymentViewModel::class.java)) {
                return PaymentViewModel() as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

    class PaymentViewModel : ViewModel() {

        private val _redirectUrl = MutableLiveData<String?>()
        val redirectUrl: LiveData<String?> get() = _redirectUrl

        private val _paymentDetails = MutableLiveData<JSONObject?>()
        val paymentDetails: LiveData<JSONObject?> get() = _paymentDetails

        private val _paymentLinkId = MutableLiveData<String?>()
        val paymentLinkId: LiveData<String?> get() = _paymentLinkId

        private val apiClient = createApiClient()

        fun createPaymentLink(amount: Int, description: String) {
            val convertedAmount = amount * 100
            val requestBody = JSONObject().apply {
                put("data", JSONObject().apply {
                    put("attributes", JSONObject().apply {
                        put("amount", convertedAmount)
                        put("currency", "PHP")
                        put("description", description)
                        put("payment_method_types", JSONArray().apply {
                            put("gcash")
                            put("card")
                        })
                        put("redirect", JSONObject().apply {
                            put("success", "https://com.example.photostudio/payment?status=success")
                            put("failed", "https://com.example.photostudio/payment?status=failed")
                        })
                    })
                })
            }

            val request = Request.Builder()
                .url("https://api.paymongo.com/v1/links")
                .post(RequestBody.create("application/json".toMediaType(), requestBody.toString()))
                .addHeader("Authorization", "Basic ${EncryptionUtils.encodeApiKey("sk_test_yESi8KQWKn2mCE4ZnvKksGVk")}")
                .addHeader("Content-Type", "application/json")
                .build()

            apiClient.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e("PaymentViewModel", "Network request failed: ${e.localizedMessage}", e)
                }

                override fun onResponse(call: Call, response: Response) {
                    val responseBody = response.body?.string() ?: return
                    if (response.isSuccessful) {
                        try {
                            val jsonResponse = JSONObject(responseBody)
                            val data = jsonResponse.optJSONObject("data")
                            val id = data?.optString("id")
                            _paymentLinkId.postValue(id)
                            val checkoutUrl = data?.optJSONObject("attributes")?.optString("checkout_url")
                            if (!checkoutUrl.isNullOrEmpty()) {
                                _redirectUrl.postValue(checkoutUrl)
                            }
                        } catch (e: Exception) {
                            Log.e("PaymentViewModel", "Error parsing response: ${e.localizedMessage}", e)
                        }
                    } else {
                        Log.e("PaymentViewModel", "API Error: HTTP ${response.code} - ${response.message}")
                    }
                }
            })
        }

        fun getPaymentDetails(paymentId: String) {
            val request = Request.Builder()
                .url("https://api.paymongo.com/v1/payments/$paymentId")
                .get()
                .addHeader("Authorization", "Basic ${EncryptionUtils.encodeApiKey("sk_test_yESi8KQWKn2mCE4ZnvKksGVk")}")
                .build()

            apiClient.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e("PaymentViewModel", "Network request failed: ${e.localizedMessage}", e)
                }

                override fun onResponse(call: Call, response: Response) {
                    val responseBody = response.body?.string() ?: ""
                    if (response.isSuccessful) {
                        try {
                            val jsonResponse = JSONObject(responseBody)
                            _paymentDetails.postValue(jsonResponse)
                        } catch (e: Exception) {
                            Log.e("PaymentViewModel", "Error parsing payment details: ${e.localizedMessage}", e)
                        }
                    } else {
                        Log.e("PaymentViewModel", "API Error: HTTP ${response.code} - ${response.message}")
                    }
                }
            })
        }

        fun getPaymentsForLink(linkId: String, expectedDescription: String) {
            val request = Request.Builder()
                .url("https://api.paymongo.com/v1/links/$linkId")
                .get()
                .addHeader("Authorization", "Basic ${EncryptionUtils.encodeApiKey("sk_test_yESi8KQWKn2mCE4ZnvKksGVk")}")
                .build()

            apiClient.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e("PaymentViewModel", "Failed to retrieve link details: ${e.localizedMessage}")
                }

                override fun onResponse(call: Call, response: Response) {
                    val responseBody = response.body?.string()
                    if (response.isSuccessful && responseBody != null) {
                        try {
                            val json = JSONObject(responseBody)
                            val linkData = json.getJSONObject("data").getJSONObject("attributes")
                            val referenceNumber = linkData.getString("reference_number")
                            getPaymentsForReference(referenceNumber, expectedDescription)
                        } catch (e: Exception) {
                            Log.e("PaymentViewModel", "Error parsing link details: ${e.localizedMessage}")
                        }
                    } else {
                        Log.e("PaymentViewModel", "Failed to fetch link details: HTTP ${response.code}")
                    }
                }
            })
        }

        private fun getPaymentsForReference(referenceNumber: String, expectedDescription: String) {
            val request = Request.Builder()
                .url("https://api.paymongo.com/v1/payments")
                .get()
                .addHeader("Authorization", "Basic ${EncryptionUtils.encodeApiKey("sk_test_yESi8KQWKn2mCE4ZnvKksGVk")}")
                .build()

            apiClient.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e("PaymentViewModel", "Failed to retrieve payments: ${e.localizedMessage}")
                }

                override fun onResponse(call: Call, response: Response) {
                    val responseBody = response.body?.string()
                    if (response.isSuccessful && responseBody != null) {
                        try {
                            val json = JSONObject(responseBody)
                            val payments = json.getJSONArray("data")
                            var foundPaymentId: String? = null
                            for (i in 0 until payments.length()) {
                                val paymentObj = payments.getJSONObject(i)
                                val attributes = paymentObj.getJSONObject("attributes")
                                val status = attributes.optString("status", "").trim().toLowerCase()
                                val paymentDescription = attributes.optString("description", "")
                                if ((status == "paid" || status == "succeeded") &&
                                    paymentDescription.contains(expectedDescription, ignoreCase = true)
                                ) {
                                    foundPaymentId = paymentObj.getString("id")
                                    break
                                }
                            }
                            if (foundPaymentId != null) {
                                getPaymentDetails(foundPaymentId)
                            }
                        } catch (e: Exception) {
                            Log.e("PaymentViewModel", "Error parsing payments: ${e.localizedMessage}")
                        }
                    } else {
                        Log.e("PaymentViewModel", "Failed to fetch payments: HTTP ${response.code}")
                    }
                }
            })
        }

        private fun createApiClient(): OkHttpClient {
            val logging = HttpLoggingInterceptor()
            logging.level = HttpLoggingInterceptor.Level.BODY
            return OkHttpClient.Builder()
                .addInterceptor(logging)
                .build()
        }
    }
}
