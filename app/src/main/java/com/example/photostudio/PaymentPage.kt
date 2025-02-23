package com.example.photostudio

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.example.photostudio.utils.EncryptionUtils
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class PaymentPage : AppCompatActivity() {

    private lateinit var viewModel: PaymentViewModel
    private var pendingPaymentData: Map<String, Any>? = null
    private lateinit var firestore: FirebaseFirestore
    private lateinit var paymentWebView: WebView


    lateinit var allFixedTimeSlots: List<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment_page)

        firestore = FirebaseFirestore.getInstance()

        paymentWebView = findViewById(R.id.paymentWebView)
        paymentWebView.settings.javaScriptEnabled = true
        paymentWebView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                Log.d("PaymentPage", "WebView shouldOverrideUrlLoading: $url")
                url?.let {
                    if (it.startsWith("https://com.example.photostudio/payment") ||
                        it.startsWith("https://pm.link/gcash/success") ||
                        it.startsWith("https://pm.link/gcash/failed")
                    ) {
                        val uri = Uri.parse(it)
                        Log.d("PaymentPage", "Deep link detected: $uri")
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
                        } else if (status.equals("failed", ignoreCase = true)) {
                            Toast.makeText(this@PaymentPage, "Payment failed", Toast.LENGTH_SHORT).show()
                        }
                        paymentWebView.visibility = View.GONE
                        Handler(Looper.getMainLooper()).post { clearFocusAndHideKeyboard() }
                        return true
                    }
                }
                return false
            }
        }

        // (Omitted: Code for initializing date/time pickers, spinners, extra person sections, etc.)

        viewModel = ViewModelProvider(this, PaymentViewModelFactory())
            .get(PaymentViewModel::class.java)

        viewModel.redirectUrl.observe(this) { url ->
            if (!url.isNullOrEmpty()) {
                paymentWebView.visibility = View.VISIBLE
                paymentWebView.loadUrl(url)
            } else {
                Toast.makeText(this, "No redirection URL available.", Toast.LENGTH_LONG).show()
            }
        }

        viewModel.paymentDetails.observe(this) { json ->
            Log.d("PaymentPage", "Observed payment details: $json")
            if (json != null) {
                val attributes = json.optJSONObject("data")?.optJSONObject("attributes")
                val status = attributes?.optString("status")?.trim()?.toLowerCase()
                if (status == "paid" || status == "succeeded") {
                    Toast.makeText(this, "Payment completed successfully.", Toast.LENGTH_LONG).show()
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
                                Log.e("PaymentPage", "Error saving payment data", e)
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
            Log.d("PaymentPage", "Received URI: $uri")
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

            Log.d("PaymentViewModel", "Request Body: $requestBody")

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
                    val responseBody = response.body?.string() ?: run {
                        Log.e("PaymentViewModel", "Empty response body from server.")
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
                                Log.e("PaymentViewModel", "Payment link created, but no checkout URL was returned.")
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
            Log.d("PaymentViewModel", "getPaymentDetails called with paymentId: $paymentId")

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
                            Log.d("PaymentViewModel", "Payment Details Response: $jsonResponse")
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
                            val linkData = json.getJSONObject("data")
                                .getJSONObject("attributes")
                            val referenceNumber = linkData.getString("reference_number")
                            Log.d("PaymentViewModel", "Link reference number: $referenceNumber")
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
                                Log.d("PaymentViewModel", "Found matching payment ID: $foundPaymentId")
                                getPaymentDetails(foundPaymentId)
                            } else {
                                Log.e("PaymentViewModel", "No payments found matching description: $expectedDescription")
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
