package com.example.photostudio

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputFilter
import android.text.Spanned
import android.text.InputType
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
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Base64
import java.util.Calendar
import java.util.Locale

// InputFilter that restricts a number input between min and max.
class InputFilterMinMax(private val min: Int, private val max: Int) : InputFilter {
    override fun filter(
        source: CharSequence, start: Int, end: Int,
        dest: Spanned, dstart: Int, dend: Int
    ): CharSequence? {
        try {
            val newVal = dest.subSequence(0, dstart).toString() +
                    source.subSequence(start, end) +
                    dest.subSequence(dend, dest.length)
            val input = newVal.toInt()
            if (input in min..max)
                return null
        } catch (e: NumberFormatException) { }
        return ""
    }
}

fun encodeApiKey(apiKey: String): String {
    return Base64.getEncoder().encodeToString("$apiKey:".toByteArray())
}

class PaymentPage : AppCompatActivity() {

    private lateinit var viewModel: PaymentViewModel
    private var pendingPaymentData: Map<String, Any>? = null
    private lateinit var firestore: FirebaseFirestore
    private lateinit var paymentWebView: WebView

    // Main storage as a string for selected date and time
    var selectedDate: Long? = null
    var selectedTime: String? = null

    // We'll store the date in dd-MM-yyyy format for Firestore queries and saving
    private var selectedDateString: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("PaymentPage", "onCreate called with intent: ${intent.data}")
        setContentView(R.layout.activity_payment_page)

        firestore = FirebaseFirestore.getInstance()

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
                            val expectedPackageType = pendingPaymentData?.get("packageType") as? String ?: ""
                            if (paymentLinkId != null) {
                                Log.d("PaymentPage", "Fetching payments for link ID: $paymentLinkId")
                                viewModel.getPaymentsForLink(paymentLinkId, expectedPackageType)
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

        // Use packageType instead of description
        val packageType = intent.getStringExtra("packageType")
        val packagePrice = intent.getIntExtra("packagePrice", 0)

        val extraPersonSection = findViewById<LinearLayout>(R.id.extraPersonSection)
        extraPersonSection.visibility = View.GONE

        val showExtraSection = intent.getBooleanExtra("showExtraSection", false)
        val extraPersonQtyInput = findViewById<EditText>(R.id.extraPersonQty)
        val softCopyQtyInput = findViewById<EditText>(R.id.softCopyQty)

        // Apply input filters so user cannot enter more than the allowed values.
        extraPersonQtyInput.filters = arrayOf(InputFilterMinMax(0, 5))
        softCopyQtyInput.filters = arrayOf(InputFilterMinMax(0, 20))

        val defaultBackdropAutoComplete = findViewById<MaterialAutoCompleteTextView>(R.id.defaultBackdropSpinner)
        defaultBackdropAutoComplete.inputType = InputType.TYPE_NULL
        defaultBackdropAutoComplete.setOnClickListener {
            defaultBackdropAutoComplete.showDropDown()
        }

        val extraBackdropAutoComplete = findViewById<MaterialAutoCompleteTextView>(R.id.optionalBackdropSpinner)
        extraBackdropAutoComplete.inputType = InputType.TYPE_NULL
        extraBackdropAutoComplete.setOnClickListener {
            extraBackdropAutoComplete.showDropDown()
        }

        val defaultBackdropLayout = findViewById<TextInputLayout>(R.id.defaultBackdropDropdownLayout)

        // Set adapters for backdrop dropdowns with color options.
        val colorOptions = listOf("Select Color", "Red", "Blue", "Green", "Yellow")
        var defaultBackdropAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, colorOptions)
        defaultBackdropAutoComplete.setAdapter(defaultBackdropAdapter)
        var extraBackdropAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, colorOptions)
        extraBackdropAutoComplete.setAdapter(extraBackdropAdapter)

        // Update backdrop adapters so that if one is selected, it is removed from the other.
        defaultBackdropAutoComplete.setOnItemClickListener { _, _, position, _ ->
            val selectedDefault = (defaultBackdropAdapter.getItem(position) ?: "Select Color")
            val extraOptions = if (selectedDefault != "Select Color")
                colorOptions.filter { it != selectedDefault }
            else
                colorOptions
            extraBackdropAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, extraOptions)
            extraBackdropAutoComplete.setAdapter(extraBackdropAdapter)
        }

        extraBackdropAutoComplete.setOnItemClickListener { _, _, position, _ ->
            val selectedExtra = (extraBackdropAdapter.getItem(position) ?: "Select Color")
            val defaultOptions = if (selectedExtra != "Select Color")
                colorOptions.filter { it != selectedExtra }
            else
                colorOptions
            defaultBackdropAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, defaultOptions)
            defaultBackdropAutoComplete.setAdapter(defaultBackdropAdapter)
        }

        // Retrieve and hide the bottom navigation view.
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigationView.visibility = View.GONE

        extraPersonSection.visibility = if (showExtraSection) View.VISIBLE else View.GONE

        // ----------------- PREPARE TIME DROPDOWN AND HELPER FUNCTION ------------------
        val timeDropdown = findViewById<MaterialAutoCompleteTextView>(R.id.timeDropdown)
        timeDropdown.inputType = InputType.TYPE_NULL
        timeDropdown.isEnabled = false
        timeDropdown.setOnClickListener {
            if (timeDropdown.isEnabled) {
                timeDropdown.showDropDown()
            } else {
                Toast.makeText(this, "Please select a date first.", Toast.LENGTH_SHORT).show()
            }
        }

        fun generateTimeSlots(startHour: Int, endHour: Int, intervalMinutes: Int): List<String> {
            val slots = mutableListOf<String>()
            var hour = startHour
            var minute = 0
            while (hour < endHour || (hour == endHour && minute == 0)) {
                val hour12 = if (hour % 12 == 0) 12 else hour % 12
                val amPm = if (hour < 12) "AM" else "PM"
                slots.add(String.format("%02d:%02d %s", hour12, minute, amPm))
                minute += intervalMinutes
                if (minute >= 60) {
                    minute %= 60
                    hour++
                }
            }
            return slots
        }

        val allFixedTimeSlots = generateTimeSlots(9, 18, 30)
        val timeAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, allFixedTimeSlots.toMutableList())
        timeDropdown.setAdapter(timeAdapter)

        timeDropdown.setOnItemClickListener { _, _, position, _ ->
            selectedTime = (timeDropdown.adapter as ArrayAdapter<String>).getItem(position)
            Log.d("PaymentPage", "Selected time: $selectedTime")
        }

        val dateEditText = findViewById<TextInputEditText>(R.id.dateEditText)
        val tomorrowCalendar = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }

        dateEditText.setOnClickListener {
            val dpd = DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    // Convert selected date to dd-MM-yyyy format and store it
                    val selectedCal = Calendar.getInstance().apply { set(year, month, dayOfMonth) }
                    selectedDate = selectedCal.timeInMillis
                    val dateFormatter = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                    val dateString = dateFormatter.format(selectedCal.time)

                    // Show dd-MM-yyyy in the EditText
                    dateEditText.setText(dateString)
                    // Store the string for Firestore usage
                    selectedDateString = dateString

                    // Reset the selected time when a new date is chosen
                    selectedTime = null
                    timeDropdown.setText("")

                    // Query Firestore by the string date
                    firestore.collection("payments")
                        .whereEqualTo("appointmentDate", dateString)
                        .addSnapshotListener { querySnapshot, e ->
                            if (e != null) {
                                Log.e("PaymentPage", "Listen failed: ${e.message}")
                                return@addSnapshotListener
                            }
                            val bookedTimes = mutableSetOf<String>()
                            querySnapshot?.documents?.forEach { document ->
                                document.getString("appointmentTime")?.let { bookedTime ->
                                    bookedTimes.add(bookedTime)
                                }
                            }
                            val availableTimes = allFixedTimeSlots.filter { it !in bookedTimes }

                            // If no available time slots, inform user and clear the date selection.
                            if (availableTimes.isEmpty()) {
                                Toast.makeText(
                                    this,
                                    "No available time slots for this date. Please choose another date.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                dateEditText.setText("")
                                selectedDateString = null
                                selectedDate = null
                                timeDropdown.isEnabled = false
                                (timeDropdown.adapter as ArrayAdapter<String>).clear()
                            } else {
                                (timeDropdown.adapter as ArrayAdapter<String>).clear()
                                (timeDropdown.adapter as ArrayAdapter<String>).addAll(availableTimes)
                                (timeDropdown.adapter as ArrayAdapter<String>).notifyDataSetChanged()
                                timeDropdown.isEnabled = true
                                // Ensure time is reset in case previously selected time is not available.
                                if (selectedTime != null && selectedTime !in availableTimes) {
                                    selectedTime = null
                                    timeDropdown.setText("")
                                }
                            }
                        }
                },
                tomorrowCalendar.get(Calendar.YEAR),
                tomorrowCalendar.get(Calendar.MONTH),
                tomorrowCalendar.get(Calendar.DAY_OF_MONTH)
            )
            dpd.datePicker.minDate = tomorrowCalendar.timeInMillis
            dpd.show()
        }

        // Build payment data using three calculated values: fullTotal, amountToCharge, and remainingAmount.
        fun buildPaymentData(paymentType: String): Map<String, Any>? {
            val packageTypeValue = if (!packageType.isNullOrBlank()) packageType else "Default Package"

            // Check and set default backdrop value
            var defaultBackdropValue = defaultBackdropAutoComplete.text.toString().trim()
            if (defaultBackdropValue.isEmpty() || defaultBackdropValue == "Select Color") {
                // For Birthday packages, default to "Default"
                // For Group Packages, also allow a default value without error
                if (packageTypeValue.contains("Years Old Package", ignoreCase = true) ||
                    packageTypeValue.equals("Group Package", ignoreCase = true)
                ) {
                    defaultBackdropValue = "Default"
                    defaultBackdropLayout.error = null
                } else {
                    defaultBackdropLayout.error = "Color selection is required"
                    Log.d("PaymentPage", "Default backdrop (color) not selected")
                    return null
                }
            } else {
                defaultBackdropLayout.error = null
            }

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

            val extraPersonCost = intent.getIntExtra("extraPersonCost", 300)
            val extraPersonQtyValue = extraPersonQtyInput.text.toString().toIntOrNull() ?: 0
            val includedPersons = 3
            val additionalPersons = if (extraPersonQtyValue > includedPersons) extraPersonQtyValue - includedPersons else 0
            val extraPersonAmount = extraPersonCost * additionalPersons

            val extraBackdropCost = if (extraBackdropValue.isNotEmpty() && extraBackdropValue != "Select Color") 200 else 0

            val originalTotal = extraPersonAmount + extraBackdropCost + packagePrice

            val fullTotal = originalTotal
            val (amountToCharge, remainingAmount) = if (paymentType.equals("Downpayment", ignoreCase = true)) {
                val charge = originalTotal / 2
                val remain = originalTotal - charge
                Pair(charge, remain)
            } else {
                Pair(originalTotal, 0)
            }

            val totalAmountForAPI = amountToCharge * 100

            val softCopyQtyValue = softCopyQtyInput.text.toString().toIntOrNull() ?: 0

            if (selectedDateString.isNullOrEmpty()) {
                Toast.makeText(this, "Please select a date.", Toast.LENGTH_SHORT).show()
                return null
            }

            if (selectedTime.isNullOrEmpty()) {
                Toast.makeText(this, "Please select a time.", Toast.LENGTH_SHORT).show()
                return null
            }

            val timeParts = selectedTime!!.split(" ")
            if (timeParts.size != 2) {
                Toast.makeText(this, "Invalid time format.", Toast.LENGTH_SHORT).show()
                return null
            }
            val hmParts = timeParts[0].split(":")
            if (hmParts.size != 2) {
                Toast.makeText(this, "Invalid time format.", Toast.LENGTH_SHORT).show()
                return null
            }
            var hour = hmParts[0].toIntOrNull() ?: 0
            val minute = hmParts[1].toIntOrNull() ?: 0
            val period = timeParts[1]
            if (period.equals("PM", ignoreCase = true) && hour != 12) {
                hour += 12
            }
            if (period.equals("AM", ignoreCase = true) && hour == 12) {
                hour = 0
            }
            val selectedTimeInMinutes = hour * 60 + minute

            val openingTime = 9 * 60
            val lastAllowedStartTime = 18 * 60 + 30
            if (selectedTimeInMinutes < openingTime || selectedTimeInMinutes > lastAllowedStartTime) {
                Toast.makeText(
                    this,
                    "Please select a time between 9:00 AM and 6:30 PM. Each session lasts 30 minutes.",
                    Toast.LENGTH_SHORT
                ).show()
                return null
            }

            val appointmentDate = selectedDateString!!
            val appointmentTime = selectedTime!!

            val sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
            val uid = sharedPreferences.getString("uid", null)
            return if (uid != null) {
                Log.d("PaymentPage", "Building payment data with UID: $uid")
                val transactionAmount = if (paymentType.equals("Downpayment", ignoreCase = true)) amountToCharge else fullTotal
                mapOf(
                    "uid" to uid,
                    "totalAmount" to transactionAmount,
                    "amountToCharge" to amountToCharge,
                    "remainingAmount" to remainingAmount,
                    "packageType" to packageTypeValue,
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

        val paymentButton: Button = findViewById(R.id.downpayment_button)
        val fullPaymentButton: Button = findViewById(R.id.full_payment_button)

        paymentButton.setOnClickListener {
            Log.d("PaymentPage", "Downpayment button clicked")
            buildPaymentData("Downpayment")?.let { paymentData ->
                pendingPaymentData = paymentData

                val chargeAmount = paymentData["amountToCharge"] as Int
                Log.d("PaymentPage", "Creating payment link for Downpayment with charge amount: $chargeAmount")
                viewModel.createPaymentLink(chargeAmount, paymentData["packageType"].toString())
            }
        }

        fullPaymentButton.setOnClickListener {
            Log.d("PaymentPage", "Full Payment button clicked")
            buildPaymentData("Full Payment")?.let { paymentData ->
                pendingPaymentData = paymentData
                // For full payment, charge the full total
                val fullTotal = paymentData["totalAmount"] as Int
                Log.d("PaymentPage", "Creating payment link for Full Payment with amount: $fullTotal")
                viewModel.createPaymentLink(fullTotal, paymentData["packageType"].toString())
            }
        }

        viewModel = ViewModelProvider(this, PaymentViewModelFactory())
            .get(PaymentViewModel::class.java)

        viewModel.paymentLinkId.observe(this) { linkId ->
            linkId?.let {
                pendingPaymentData = pendingPaymentData?.toMutableMap()?.apply {
                    put("paymentLinkId", it)
                } ?: mutableMapOf("paymentLinkId" to it)
                Log.d("PaymentPage", "Stored payment link ID: $it")
            }
        }

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

        viewModel.paymentDetails.observe(this) { json ->
            Log.d("PaymentPage", "Observed payment details: $json")
            if (json != null) {
                val attributes = json.optJSONObject("data")?.optJSONObject("attributes")
                val status = attributes?.optString("status")?.trim()?.toLowerCase()
                Log.d("PaymentPage", "Payment details status: $status")
                if (status == "paid" || status == "succeeded") {
                    Toast.makeText(this, "Payment completed successfully.", Toast.LENGTH_LONG).show()
                    Log.d("PaymentPage", "Payment successful. Preparing to write paymentData to Firestore.")
                    pendingPaymentData?.let { paymentData ->
                        val paymongoName = attributes?.optJSONObject("billing")?.optString("name") ?: ""
                        val updatedPaymentData = paymentData.toMutableMap().apply {
                            put("paymongoName", paymongoName)
                            put("archive", "no")
                            put("complete", "no")
                        }
                        Log.d("PaymentPage", "Payment data to be saved: $updatedPaymentData")
                        firestore.collection("payments")
                            .add(updatedPaymentData)
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
                    val expectedPackageType = pendingPaymentData?.get("packageType") as? String ?: ""
                    if (paymentLinkId != null) {
                        Log.d("PaymentPage", "Fetching payments for link ID: $paymentLinkId")
                        viewModel.getPaymentsForLink(paymentLinkId, expectedPackageType)
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

        private val _paymentLinkId = MutableLiveData<String?>()
        val paymentLinkId: LiveData<String?> get() = _paymentLinkId

        private val apiClient = createApiClient()

        fun createPaymentLink(amount: Int, packageType: String) {
            // Multiply by 100 because PayMongo API expects the smallest currency unit.
            val convertedAmount = amount * 100

            val requestBody = JSONObject().apply {
                put("data", JSONObject().apply {
                    put("attributes", JSONObject().apply {
                        put("amount", convertedAmount)
                        put("currency", "PHP")
                        // Use the packageType as description
                        put("description", packageType)
                        put("payment_method_types", JSONArray().apply {
                            put("gcash")
                            put("card")
                        })
                        // Include packageType in metadata for matching later.
                        put("metadata", JSONObject().apply {
                            put("packageType", packageType)
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

                            val data = jsonResponse.optJSONObject("data")
                            val id = data?.optString("id")
                            _paymentLinkId.postValue(id)

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
                        "status": "paid",
                        "payer_name": "Test User"
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

        fun getPaymentsForLink(linkId: String, expectedPackageType: String) {
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

                            val referenceNumber = linkData.getString("reference_number")
                            Log.d("PaymentViewModel", "Link reference number: $referenceNumber")
                            Log.d("PaymentViewModel", "Fetching payments using alternative matching logic.")
                            getPaymentsForReference(referenceNumber, expectedPackageType)
                        } catch (e: Exception) {
                            _error.postValue("Error parsing link details: ${e.message}")
                        }
                    } else {
                        _error.postValue("Failed to fetch link details: HTTP ${response.code}")
                    }
                }
            })
        }

        private fun getPaymentsForReference(referenceNumber: String, expectedPackageType: String) {
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

                                // Try to get package type from metadata first.
                                val metadata = attributes.optJSONObject("metadata")
                                val paymentPackageTypeFromMetadata = metadata?.optString("packageType", "") ?: ""

                                // Fallback to checking the description if metadata.packageType is empty.
                                val description = attributes.optString("description", "")

                                Log.d("PaymentViewModel", "Checking payment: status=$status, metadata.packageType=$paymentPackageTypeFromMetadata, description=$description")

                                if ((status == "paid" || status == "succeeded") &&
                                    (paymentPackageTypeFromMetadata.equals(expectedPackageType, ignoreCase = true) ||
                                            description.equals(expectedPackageType, ignoreCase = true))
                                ) {
                                    foundPaymentId = paymentObj.getString("id")
                                    break
                                }
                            }
                            if (foundPaymentId != null) {
                                Log.d("PaymentViewModel", "Found matching payment ID: $foundPaymentId")
                                getPaymentDetails(foundPaymentId)
                            } else {
                                _error.postValue("No payments found matching package type: $expectedPackageType")
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
