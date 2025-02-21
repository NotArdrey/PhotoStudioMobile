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
import java.util.Calendar

// Top-level utility function for encoding API keys
fun encodeApiKey(apiKey: String): String {
    return Base64.getEncoder().encodeToString("$apiKey:".toByteArray())
}

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
                    if (it.startsWith("https://com.example.photostudio/payment") ||
                        it.startsWith("https://pm.link/gcash/success") ||
                        it.startsWith("https://pm.link/gcash/failed")
                    ) {
                        val uri = Uri.parse(it)
                        Log.d("PaymentPage", "Deep link detected in WebView: $uri")
                        var status = uri.getQueryParameter("status")
                        if (status == null) {
                            if (it.contains("gcash/success", ignoreCase = true)) {
                                status = "paid"
                            } else if (it.contains("gcash/failed", ignoreCase = true)) {
                                status = "failed"
                            }
                        }
                        Log.d("PaymentPage", "Status from deep link: $status")
                        if (status.equals("paid", ignoreCase = true) || status.equals("succeeded", ignoreCase = true)) {
                            Toast.makeText(this@PaymentPage, "Payment successful", Toast.LENGTH_SHORT).show()
                            val paymentLinkId = pendingPaymentData?.get("paymentLinkId") as? String
                            val expectedDesc = pendingPaymentData?.get("description") as? String ?: ""
                            if (paymentLinkId != null) {
                                Log.d("PaymentPage", "Fetching payments for link ID: $paymentLinkId")
                                viewModel.getPaymentsForLink(paymentLinkId, expectedDesc)
                            } else {
                                Log.e("PaymentPage", "Payment link ID not found")
                            }
                        } else if (status.equals("failed", ignoreCase = true)) {
                            Toast.makeText(this@PaymentPage, "Payment failed", Toast.LENGTH_SHORT).show()
                            Log.d("PaymentPage", "Payment failed branch in WebView")
                        }
                        paymentWebView.visibility = View.GONE
                        Log.d("PaymentPage", "WebView visibility set to GONE")
                        Handler(Looper.getMainLooper()).post { clearFocusAndHideKeyboard() }
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

        // Set adapters for backdrop dropdowns with color options.
        val colorOptions = listOf("Select Color", "Red", "Blue", "Green", "Yellow")
        val colorAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, colorOptions)
        defaultBackdropAutoComplete.setAdapter(colorAdapter)
        extraBackdropAutoComplete.setAdapter(colorAdapter)

        // Retrieve and hide the bottom navigation view.
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigationView.visibility = View.GONE

        extraPersonSection.visibility = if (showExtraSection) View.VISIBLE else View.GONE

        // Set minimum date for CalendarView to tomorrow (reservations must be made at least one day in advance).
        val tomorrowCalendar = Calendar.getInstance()
        tomorrowCalendar.add(Calendar.DAY_OF_YEAR, 1)
        calendarView.minDate = tomorrowCalendar.timeInMillis

        // Function to build payment data, including time validations.
        fun buildPaymentData(paymentType: String): Map<String, Any>? {
            val defaultBackdropValue = defaultBackdropAutoComplete.text.toString().trim()
            if (defaultBackdropValue.isEmpty() || defaultBackdropValue == "Select Color") {
                defaultBackdropLayout.error = "Color selection is required"
                Log.d("PaymentPage", "Default backdrop (color) not selected")
                return null
            } else {
                defaultBackdropLayout.error = null
            }

            // Retrieve extra backdrop value.
            val extraBackdropValue = extraBackdropAutoComplete.text.toString().trim()
            if (extraBackdropValue.isNotEmpty() && extraBackdropValue != "Select Color") {
                if (extraBackdropValue == defaultBackdropValue) {
                    Toast.makeText(
                        this,
                        "Extra backdrop color cannot be the same as the default color.",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.d("PaymentPage", "Extra backdrop color same as default")
                    return null
                }
            }

            val extraPersonAmount = defaultPax * 200
            val extraBackdropCost = if (extraBackdropValue.isNotEmpty() && extraBackdropValue != "Select Color") 200 else 0
            val packagePriceInt = packagePriceStr?.toIntOrNull() ?: 0
            val totalAmount = extraPersonAmount + extraBackdropCost + packagePriceInt
            Log.d("PaymentPage", "Total amount calculated: $totalAmount")

            val extraPersonQtyValue = extraPersonQtyInput.text.toString().toIntOrNull() ?: 0
            val softCopyQtyValue = softCopyQtyInput.text.toString().toIntOrNull() ?: 0

            val appointmentDate = calendarView.date
            // Validate that the selected date is not in the past.
            val currentCalendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val todayStart = currentCalendar.timeInMillis
            if (appointmentDate < todayStart) {
                Toast.makeText(this, "You cannot select a past date.", Toast.LENGTH_SHORT).show()
                return null
            }
            if (appointmentDate < todayStart + 24 * 60 * 60 * 1000) {
                Toast.makeText(this, "Reservations must be made at least one day in advance.", Toast.LENGTH_SHORT).show()
                return null
            }

            // Retrieve the time from the TimePicker.
            val appointmentHour = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M)
                timePicker.hour else timePicker.currentHour
            val appointmentMinute = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M)
                timePicker.minute else timePicker.currentMinute

            // Validate that the selected time is within studio hours.
            // Studio hours: 9:00 AM to 7:00 PM.
            // Since each session lasts 30 minutes, the latest allowed start time is 6:30 PM.
            val selectedTimeInMinutes = appointmentHour * 60 + appointmentMinute
            val openingTime = 9 * 60               // 9:00 AM = 540 minutes
            val lastAllowedStartTime = 18 * 60 + 30  // 6:30 PM = 1110 minutes

            if (selectedTimeInMinutes < openingTime || selectedTimeInMinutes > lastAllowedStartTime) {
                Toast.makeText(
                    this,
                    "Please select a time between 9:00 AM and 6:30 PM. Each session lasts 30 minutes.",
                    Toast.LENGTH_SHORT
                ).show()
                return null
            }

            val appointmentTime = "$appointmentHour:$appointmentMinute"

            // Check for scheduling conflicts (stubbed—replace with your actual logic).
            if (checkScheduleConflict(appointmentDate, appointmentTime)) {
                Toast.makeText(this, "There is a conflict in your schedule for the selected date/time.", Toast.LENGTH_SHORT).show()
                return null
            }

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
                    "selectedExtraBackdrop" to extraBackdropValue,
                    "appointmentDate" to appointmentDate,
                    "appointmentTime" to appointmentTime
                )
            } else {
                Log.e("PaymentPage", "User UID not found in SharedPreferences")
                null
            }
        }

        // Payment button click listeners.
        val paymentButton: Button = findViewById(R.id.downpayment_button)
        val fullPaymentButton: Button = findViewById(R.id.full_payment_button)

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

        // Observe Payment Link ID and store it in pendingPaymentData.
        viewModel.paymentLinkId.observe(this) { linkId ->
            linkId?.let {
                pendingPaymentData = pendingPaymentData?.toMutableMap()?.apply {
                    put("paymentLinkId", it)
                } ?: mutableMapOf("paymentLinkId" to it)
                Log.d("PaymentPage", "Stored payment link ID: $it")
            }
        }

        // Observe the redirect URL; when available, load it in the WebView.
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

        // Observe the payment details after calling getPaymentDetails or getPaymentsForLink.
        viewModel.paymentDetails.observe(this) { json ->
            Log.d("PaymentPage", "Observed payment details: $json")
            if (json != null) {
                val status = json
                    .optJSONObject("data")
                    ?.optJSONObject("attributes")
                    ?.optString("status")
                    ?.trim()
                    ?.toLowerCase()
                Log.d("PaymentPage", "Payment details status: $status")
                if (status == "paid" || status == "succeeded") {
                    Toast.makeText(this, "Payment completed successfully.", Toast.LENGTH_LONG).show()
                    Log.d("PaymentPage", "Payment successful. Preparing to write paymentData to Firestore.")
                    pendingPaymentData?.let { paymentData ->
                        Log.d("PaymentPage", "Payment data to be saved: $paymentData")
                        firestore.collection("payments")
                            .add(paymentData)
                            .addOnSuccessListener { documentReference ->
                                Log.d("PaymentPage", "Payment data saved with ID: ${documentReference.id}")
                                pendingPaymentData = null
                                finish()
                            }
                            .addOnFailureListener { e ->
                                Log.e("PaymentPage", "Error saving payment data to Firestore", e)
                                Toast.makeText(this, "Error saving payment data", Toast.LENGTH_SHORT).show()
                            }
                    } ?: run {
                        Log.e("PaymentPage", "No pendingPaymentData available. Cannot write to Firestore.")
                    }
                } else {
                    Toast.makeText(this, "Payment not completed yet. Status: $status", Toast.LENGTH_LONG).show()
                    Log.d("PaymentPage", "Payment details indicate that payment is not successful: $json")
                }
            } else {
                Log.e("PaymentPage", "Received null payment details JSON.")
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.d("PaymentPage", "onNewIntent called with intent: ${intent.data}")
        intent.data?.let { uri ->
            Log.d("PaymentPage", "Received URI in onNewIntent: $uri")
            var status = uri.getQueryParameter("status")
            if (status == null) {
                val url = uri.toString()
                if (url.contains("gcash/success", ignoreCase = true)) {
                    status = "paid"
                } else if (url.contains("gcash/failed", ignoreCase = true)) {
                    status = "failed"
                }
            }
            Log.d("PaymentPage", "Status in onNewIntent: $status")
            if (status != null) {
                if (status.equals("paid", ignoreCase = true) || status.equals("succeeded", ignoreCase = true)) {
                    Toast.makeText(this, "Payment successful", Toast.LENGTH_SHORT).show()
                    val paymentLinkId = pendingPaymentData?.get("paymentLinkId") as? String
                    val expectedDesc = pendingPaymentData?.get("description") as? String ?: ""
                    if (paymentLinkId != null) {
                        Log.d("PaymentPage", "Fetching payments for link ID: $paymentLinkId")
                        viewModel.getPaymentsForLink(paymentLinkId, expectedDesc)
                    } else {
                        Log.e("PaymentPage", "Payment link ID not found in onNewIntent")
                    }
                } else if (status.equals("failed", ignoreCase = true)) {
                    Toast.makeText(this, "Payment failed", Toast.LENGTH_SHORT).show()
                    Log.d("PaymentPage", "Payment failed branch in onNewIntent")
                }
                Handler(Looper.getMainLooper()).post { clearFocusAndHideKeyboard() }
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

    private fun clearFocusAndHideKeyboard() {
        currentFocus?.clearFocus()
        currentFocus?.let { view ->
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
            Log.d("PaymentPage", "Keyboard hidden")
        }
    }

    // Dummy function to simulate schedule conflict checking.
    private fun checkScheduleConflict(selectedDate: Long, appointmentTime: String): Boolean {
        return false
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

        // LiveData to capture the payment link ID.
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
                            // Capture the Payment Link ID.
                            val data = jsonResponse.optJSONObject("data")
                            val id = data?.optString("id")
                            _paymentLinkId.postValue(id)
                            // Retrieve and post the checkout URL.
                            val checkoutUrl = data
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

        // Updated getPaymentsForLink that now accepts an expectedDescription parameter.
        fun getPaymentsForLink(linkId: String, expectedDescription: String) {
            val request = Request.Builder()
                .url("https://api.paymongo.com/v1/links/$linkId")
                .get()
                .addHeader("Authorization", "Basic ${encodeApiKey("sk_test_yESi8KQWKn2mCE4ZnvKksGVk")}")
                .build()

            apiClient.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    _error.postValue("Failed to retrieve link details: ${e.message}")
                }

                override fun onResponse(call: Call, response: Response) {
                    val responseBody = response.body?.string()
                    if (response.isSuccessful && responseBody != null) {
                        try {
                            val json = JSONObject(responseBody)
                            val linkData = json.getJSONObject("data")
                                .getJSONObject("attributes")
                            // Retrieve the reference number from the link details.
                            val referenceNumber = linkData.getString("reference_number")
                            Log.d("PaymentViewModel", "Link reference number: $referenceNumber")
                            Log.d("PaymentViewModel", "Fetching payments using alternative matching logic.")
                            getPaymentsForReference(referenceNumber, expectedDescription)
                        } catch (e: Exception) {
                            _error.postValue("Error parsing link details: ${e.message}")
                        }
                    } else {
                        _error.postValue("Failed to fetch link details: HTTP ${response.code}")
                    }
                }
            })
        }

        // Modified filtering logic: match using the expected description passed in.
        private fun getPaymentsForReference(referenceNumber: String, expectedDescription: String) {
            val request = Request.Builder()
                .url("https://api.paymongo.com/v1/payments")
                .get()
                .addHeader("Authorization", "Basic ${encodeApiKey("sk_test_yESi8KQWKn2mCE4ZnvKksGVk")}")
                .build()

            apiClient.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    _error.postValue("Failed to retrieve payments: ${e.message}")
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
                                Log.d("PaymentViewModel", "Found matching payment ID: $foundPaymentId")
                                getPaymentDetails(foundPaymentId)
                            } else {
                                _error.postValue("No payments found matching description: $expectedDescription")
                            }
                        } catch (e: Exception) {
                            _error.postValue("Error parsing payments: ${e.message}")
                        }
                    } else {
                        _error.postValue("Failed to fetch payments: HTTP ${response.code}")
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
