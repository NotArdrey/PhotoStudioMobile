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
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.*
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.firestore.FirebaseFirestore
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.Base64

class PaymentPage : AppCompatActivity() {

    private lateinit var viewModel: PaymentViewModel
    private var pendingPaymentData: Map<String, Any>? = null
    private lateinit var firestore: FirebaseFirestore
    private lateinit var paymentWebView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("PaymentPage", "onCreate called with intent: ${intent.data}")
        setContentView(R.layout.activity_payment_page)

        firestore = FirebaseFirestore.getInstance()

        // Initialize the WebView overlay (declared in XML)
        paymentWebView = findViewById(R.id.paymentWebView)
        paymentWebView.settings.javaScriptEnabled = true
        paymentWebView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                Log.d("PaymentPage", "WebView shouldOverrideUrlLoading called with URL: $url")
                url?.let {
                    // Check if the URL is our deep link (either from our own domain or pm.link)
                    if (it.startsWith("https://com.example.photostudio/payment") ||
                        it.startsWith("https://pm.link/gcash/success") ||
                        it.startsWith("https://pm.link/gcash/failed")
                    ) {
                        val uri = Uri.parse(it)
                        Log.d("PaymentPage", "Deep link detected in WebView: $uri")
                        // Try to extract the status from the query parameters.
                        var status = uri.getQueryParameter("status")
                        // If status is missing, infer from the URL path.
                        if (status == null) {
                            if (it.contains("gcash/success")) {
                                status = "success"
                            } else if (it.contains("gcash/failed")) {
                                status = "failed"
                            }
                        }
                        Log.d("PaymentPage", "Status from deep link: $status")
                        if (status == "success") {
                            Toast.makeText(this@PaymentPage, "Payment successful", Toast.LENGTH_SHORT).show()
                            val paymentId = uri.getQueryParameter("payment_id")
                            Log.d("PaymentPage", "Payment ID from deep link: $paymentId")
                            if (paymentId != null) {
                                viewModel.getPaymentDetails(paymentId)
                            }
                        } else if (status == "failed") {
                            Toast.makeText(this@PaymentPage, "Payment failed", Toast.LENGTH_SHORT).show()
                            Log.d("PaymentPage", "Payment failed branch in WebView")
                        }
                        // Hide the WebView after processing the deep link
                        paymentWebView.visibility = View.GONE
                        Log.d("PaymentPage", "WebView visibility set to GONE")
                        // Clear focus and hide keyboard
                        Handler(Looper.getMainLooper()).post {
                            clearFocusAndHideKeyboard()
                        }
                        return true
                    }
                }
                return false
            }
        }

        // Retrieve intent extras (if any)
        val defaultPax = intent.getIntExtra("defaultPax", 3)
        val description = intent.getStringExtra("description")
        val packagePriceStr = intent.getStringExtra("packagePrice")

        // UI elements from your layout
        val extraPersonSection = findViewById<LinearLayout>(R.id.extraPersonSection)
        extraPersonSection.visibility = View.GONE

        val showExtraSection = intent.getBooleanExtra("showExtraSection", false)
        val extraPersonQtyInput = findViewById<EditText>(R.id.extraPersonQty)
        val softCopyQtyInput = findViewById<EditText>(R.id.softCopyQty)
        val calendarView = findViewById<CalendarView>(R.id.calendarView)
        val timePicker = findViewById<TimePicker>(R.id.timePicker)
        val defaultBackdropAutoComplete = findViewById<MaterialAutoCompleteTextView>(R.id.defaultBackdropSpinner)
        val extraBackdropAutoComplete = findViewById<MaterialAutoCompleteTextView>(R.id.optionalBackdropSpinner)
        val defaultBackdropLayout = findViewById<TextInputLayout>(R.id.defaultBackdropDropdownLayout)

        val paymentButton: Button = findViewById(R.id.downpayment_button)
        val fullPaymentButton: Button = findViewById(R.id.full_payment_button)

        // Retrieve and hide the bottom navigation view
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigationView.visibility = View.GONE

        extraPersonSection.visibility = if (showExtraSection) View.VISIBLE else View.GONE

        fun buildPaymentData(paymentType: String): Map<String, Any>? {
            val defaultBackdropValue = defaultBackdropAutoComplete.text.toString().trim()
            if (defaultBackdropValue.isEmpty() || defaultBackdropValue == "Select Backdrop") {
                defaultBackdropLayout.error = "Backdrop selection is required"
                Log.d("PaymentPage", "Default backdrop not selected")
                return null
            } else {
                defaultBackdropLayout.error = null
            }

            val extraPersonAmount = defaultPax * 200
            val extraBackdropCost = if (extraBackdropAutoComplete.text.toString().isNotEmpty() &&
                extraBackdropAutoComplete.text.toString() != "Select Backdrop") 200 else 0
            val packagePriceInt = packagePriceStr?.toIntOrNull() ?: 0

            val totalAmount = extraPersonAmount + extraBackdropCost + packagePriceInt
            Log.d("PaymentPage", "Total amount calculated: $totalAmount")

            val extraPersonQtyValue = extraPersonQtyInput.text.toString().toIntOrNull() ?: 0
            val softCopyQtyValue = softCopyQtyInput.text.toString().toIntOrNull() ?: 0

            val appointmentDate = calendarView.date

            val appointmentHour = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M)
                timePicker.hour else timePicker.currentHour
            val appointmentMinute = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M)
                timePicker.minute else timePicker.currentMinute
            val appointmentTime = "$appointmentHour:$appointmentMinute"

            val sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
            val uid = sharedPreferences.getString("uid", null)
            return if (uid != null) {
                Log.d("PaymentPage", "Building payment data with UID: $uid")
                mapOf(
                    "uid" to uid,
                    "totalAmount" to totalAmount,
                    "description" to "Captured By K Package: $description",
                    "paymentType" to paymentType,
                    "extraPersonQty" to extraPersonQtyValue,
                    "softCopyQty" to softCopyQtyValue,
                    "defaultBackdrop" to defaultBackdropValue,
                    "selectedExtraBackdrop" to extraBackdropAutoComplete.text.toString(),
                    "appointmentDate" to appointmentDate,
                    "appointmentTime" to appointmentTime
                )
            } else {
                Log.e("PaymentPage", "User UID not found in SharedPreferences")
                null
            }
        }

        paymentButton.setOnClickListener {
            Log.d("PaymentPage", "Downpayment button clicked")
            buildPaymentData("Downpayment")?.let { paymentData ->
                pendingPaymentData = paymentData
                val totalAmount = paymentData["totalAmount"] as Int
                Log.d("PaymentPage", "Creating payment link for Downpayment with amount: $totalAmount")
                viewModel.createPaymentLink(totalAmount, paymentData["description"].toString())
            }
        }

        fullPaymentButton.setOnClickListener {
            Log.d("PaymentPage", "Full Payment button clicked")
            buildPaymentData("Full Payment")?.let { paymentData ->
                pendingPaymentData = paymentData
                val totalAmount = paymentData["totalAmount"] as Int
                Log.d("PaymentPage", "Creating payment link for Full Payment with amount: $totalAmount")
                viewModel.createPaymentLink(totalAmount, paymentData["description"].toString())
            }
        }

        viewModel = ViewModelProvider(this, PaymentViewModelFactory())
            .get(PaymentViewModel::class.java)

        // Observe the redirect URL; when available, load it in the WebView
        viewModel.redirectUrl.observe(this) { url ->
            if (!url.isNullOrEmpty()) {
                Log.d("PaymentPage", "Loading checkout URL in WebView: $url")
                paymentWebView.visibility = View.VISIBLE
                paymentWebView.loadUrl(url)
            } else {
                Toast.makeText(this, "No redirection URL available.", Toast.LENGTH_LONG).show()
                Log.d("PaymentPage", "No redirect URL available")
            }
        }

        // Observe the payment details after calling getPaymentDetails
        viewModel.paymentDetails.observe(this) { json ->
            Log.d("PaymentPage", "Observed payment details: $json")
            if (json != null) {
                val status = json
                    .optJSONObject("data")
                    ?.optJSONObject("attributes")
                    ?.optString("status")
                Log.d("PaymentPage", "Payment details status: $status")
                if (status == "paid") {
                    Toast.makeText(this, "Payment completed successfully.", Toast.LENGTH_LONG).show()
                    Log.d("PaymentPage", "Payment completed successfully. Details: $json")
                    // Save the stored payment data to Firestore if available
                    pendingPaymentData?.let { paymentData ->
                        Log.d("PaymentPage", "Writing paymentData to Firestore: $paymentData")
                        firestore.collection("payments")
                            .add(paymentData)
                            .addOnSuccessListener { documentReference ->
                                Log.d("PaymentPage", "Payment data saved with ID: ${documentReference.id}")
                                // Finish the activity only after Firestore write is successful.
                                finish()
                            }
                            .addOnFailureListener { e ->
                                Log.e("PaymentPage", "Error saving payment data to Firestore", e)
                                // Optionally finish the activity even if there is an error.
                                finish()
                            }
                        pendingPaymentData = null
                    }
                } else {
                    Toast.makeText(this, "Payment not completed yet.", Toast.LENGTH_LONG).show()
                    Log.d("PaymentPage", "Payment details not paid: $json")
                }
            }
        }
    }

    // Handle deep links when the activity is already running
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.d("PaymentPage", "onNewIntent called with intent: ${intent.data}")
        intent.data?.let { uri ->
            Log.d("PaymentPage", "Received URI in onNewIntent: $uri")
            var status = uri.getQueryParameter("status")
            if (status == null) {
                val url = uri.toString()
                if (url.contains("gcash/success")) {
                    status = "success"
                } else if (url.contains("gcash/failed")) {
                    status = "failed"
                }
            }
            Log.d("PaymentPage", "Status in onNewIntent: $status")
            if (status != null) {
                if (status == "success") {
                    Toast.makeText(this, "Payment successful", Toast.LENGTH_SHORT).show()
                    val paymentId = uri.getQueryParameter("payment_id")
                    Log.d("PaymentPage", "Payment ID in onNewIntent: $paymentId")
                    if (paymentId != null) {
                        viewModel.getPaymentDetails(paymentId)
                    }
                } else if (status == "failed") {
                    Toast.makeText(this, "Payment failed", Toast.LENGTH_SHORT).show()
                    Log.d("PaymentPage", "Payment failed branch in onNewIntent")
                }
                Handler(Looper.getMainLooper()).post {
                    clearFocusAndHideKeyboard()
                }
            } else {
                val paymentId = uri.getQueryParameter("payment_id")
                if (paymentId != null) {
                    Log.d("PaymentPage", "No status, but found Payment ID: $paymentId")
                    viewModel.getPaymentDetails(paymentId)
                } else {
                    Log.d("PaymentPage", "No status and no payment ID in onNewIntent")
                }
            }
        } ?: Log.d("PaymentPage", "No URI data in onNewIntent")
    }

    // Helper function to clear focus and hide the keyboard
    private fun clearFocusAndHideKeyboard() {
        // Clear focus from the current view (if any) before hiding the keyboard.
        currentFocus?.clearFocus()
        currentFocus?.let { view ->
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
            Log.d("PaymentPage", "Keyboard hidden")
        }
    }

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

        private val _error = MutableLiveData<String>()
        val error: LiveData<String> get() = _error

        private val _paymentDetails = MutableLiveData<JSONObject?>()
        val paymentDetails: LiveData<JSONObject?> get() = _paymentDetails

        private val apiClient = createApiClient()

        fun createPaymentLink(amount: Int, description: String) {
            val convertedAmount = amount * 100

            // Prepare the JSON payload for creating the payment link.
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

            Log.d("PaymentViewModel", "Request Body: $requestBody")

            val request = Request.Builder()
                .url("https://api.paymongo.com/v1/links")
                .post(RequestBody.create("application/json".toMediaType(), requestBody.toString()))
                .addHeader("Authorization", "Basic ${encodeApiKey("sk_test_yESi8KQWKn2mCE4ZnvKksGVk")}")
                .addHeader("Content-Type", "application/json")
                .build()

            apiClient.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    val errorMessage = "Network request failed: ${e.localizedMessage ?: "Unknown error"}"
                    Log.e("PaymentViewModel", errorMessage, e)
                    _error.postValue(errorMessage)
                }

                override fun onResponse(call: Call, response: Response) {
                    val responseBody = response.body?.string() ?: run {
                        val errorMessage = "Empty response body from server."
                        Log.e("PaymentViewModel", errorMessage)
                        _error.postValue(errorMessage)
                        return
                    }
                    Log.d("PaymentViewModel", "Full API Response: $responseBody")
                    if (response.isSuccessful) {
                        try {
                            val jsonResponse = JSONObject(responseBody)
                            Log.d("PaymentViewModel", "Parsed JSON: $jsonResponse")
                            val checkoutUrl = jsonResponse
                                .optJSONObject("data")
                                ?.optJSONObject("attributes")
                                ?.optString("checkout_url")
                            if (!checkoutUrl.isNullOrEmpty()) {
                                _redirectUrl.postValue(checkoutUrl)
                            } else {
                                _error.postValue("Payment link created, but no checkout URL was returned.")
                            }
                        } catch (e: Exception) {
                            val errorMessage = "Error parsing response: ${e.localizedMessage}"
                            Log.e("PaymentViewModel", errorMessage, e)
                            _error.postValue(errorMessage)
                        }
                    } else {
                        val errorMessage = "API Error: HTTP ${response.code} - ${response.message}, Body: $responseBody"
                        Log.e("PaymentViewModel", errorMessage)
                        _error.postValue(errorMessage)
                    }
                }
            })
        }

        fun getPaymentDetails(paymentId: String) {
            Log.d("PaymentViewModel", "getPaymentDetails called with paymentId: $paymentId")
            // Simulate a paid response for testing if paymentId is "test123"
            if (paymentId == "test123") {
                val simulatedResponse = JSONObject("""{
                    "data": {
                        "attributes": {
                            "status": "paid"
                        }
                    }
                }""")
                Log.d("PaymentViewModel", "Simulated Payment Details Response: $simulatedResponse")
                _paymentDetails.postValue(simulatedResponse)
                return
            }

            val request = Request.Builder()
                .url("https://api.paymongo.com/v1/payments/$paymentId")
                .get()
                .addHeader("Authorization", "Basic ${encodeApiKey("sk_test_yESi8KQWKn2mCE4ZnvKksGVk")}")
                .build()

            apiClient.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    val errorMessage = "Network request failed: ${e.localizedMessage}"
                    Log.e("PaymentViewModel", errorMessage, e)
                    _error.postValue(errorMessage)
                }

                override fun onResponse(call: Call, response: Response) {
                    val responseBody = response.body?.string() ?: ""
                    if (response.isSuccessful) {
                        try {
                            val jsonResponse = JSONObject(responseBody)
                            Log.d("PaymentViewModel", "Payment Details Response: $jsonResponse")
                            _paymentDetails.postValue(jsonResponse)
                        } catch (e: Exception) {
                            val errorMessage = "Error parsing payment details: ${e.localizedMessage}"
                            Log.e("PaymentViewModel", errorMessage, e)
                            _error.postValue(errorMessage)
                        }
                    } else {
                        val errorMessage = "API Error: HTTP ${response.code} - ${response.message}"
                        Log.e("PaymentViewModel", errorMessage)
                        _error.postValue(errorMessage)
                    }
                }
            })
        }

        private fun encodeApiKey(apiKey: String): String {
            return Base64.getEncoder().encodeToString("$apiKey:".toByteArray())
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
