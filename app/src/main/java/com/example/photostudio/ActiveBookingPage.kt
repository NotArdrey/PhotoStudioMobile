package com.example.photostudio

import Booking
import BookingAdapter
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.photostudio.R
import com.google.firebase.firestore.FirebaseFirestore
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class ActiveBookingPage : AppCompatActivity(), BookingAdapter.OnBookingClickListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var bookingAdapter: BookingAdapter
    // Existing code remains unchanged
    private val db = FirebaseFirestore.getInstance()
    private val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())

    // Added: WebView for payment redirection (used only for Paymongo payment)
    private lateinit var paymentWebView: WebView

    // Added: PaymentViewModel to manage payment API calls
    private lateinit var viewModel: PaymentViewModel

    // Added: To store the booking for which the remaining payment is being processed.
    private var currentBookingForPayment: Booking? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Original layout remains
        setContentView(R.layout.activity_active_booking_page)

        recyclerView = findViewById(R.id.recyclerViewBookings)
        recyclerView.layoutManager = LinearLayoutManager(this)

        bookingAdapter = BookingAdapter(mutableListOf(), this)
        recyclerView.adapter = bookingAdapter

        // Initialize the WebView for payment redirection
        paymentWebView = findViewById(R.id.paymentWebView)
        paymentWebView.settings.javaScriptEnabled = true
        paymentWebView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                url?.let {
                    if (it.startsWith("https://com.example.photostudio/payment") ||
                        it.startsWith("https://pm.link/gcash/success") ||
                        it.startsWith("https://pm.link/gcash/failed")
                    ) {
                        val uri = android.net.Uri.parse(it)
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
                            Toast.makeText(this@ActiveBookingPage, "Payment successful", Toast.LENGTH_SHORT).show()
                        } else if (status.equals("failed", ignoreCase = true)) {
                            Toast.makeText(this@ActiveBookingPage, "Payment failed", Toast.LENGTH_SHORT).show()
                        }
                        paymentWebView.visibility = View.GONE
                        Handler(Looper.getMainLooper()).post { }
                        return true
                    }
                }
                return false
            }
        }

        // Initialize PaymentViewModel (new code, original remains intact)
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
            json?.let {
                val attributes = it.optJSONObject("data")?.optJSONObject("attributes")
                val status = attributes?.optString("status")?.trim()?.toLowerCase()
                if (status == "paid" || status == "succeeded") {
                    Toast.makeText(this, "Payment completed successfully.", Toast.LENGTH_LONG).show()
                    // Update the Firestore document in the "payments" collection for the current booking
                    currentBookingForPayment?.let { booking ->
                        db.collection("payments").document(booking.id)
                            .update(mapOf(
                                "remainingAmount" to 0,
                                "paymentType" to "Full Payment"
                            ))
                            .addOnSuccessListener {
                                fetchBookings()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Error updating payment data", Toast.LENGTH_SHORT).show()
                            }
                    }
                } else {
                    Toast.makeText(this, "Payment not completed yet. Status: $status", Toast.LENGTH_LONG).show()
                }
            } ?: Log.e("ActiveBookingPage", "Received null payment details JSON.")
        }

        // Original fetchBookings() call
        fetchBookings()
    }

    private fun fetchBookings() {
        db.collection("payments")
            .whereEqualTo("complete", "no")
            .get()
            .addOnSuccessListener { documents ->
                val bookingsList = mutableListOf<Booking>()
                for (document in documents) {
                    val id = document.id

                    val appointmentDate = document.getString("appointmentDate") ?: "N/A"
                    val appointmentTime = document.getString("appointmentTime") ?: "N/A"
                    val archive = document.getString("archive") ?: "no"
                    val complete = document.getString("complete") ?: "no"
                    val defaultBackdrop = document.getString("defaultBackdrop") ?: "N/A"
                    val packageType = document.getString("packageType") ?: "Unknown Package"
                    val extraPersonQty = document.getLong("extraPersonQty")?.toInt() ?: 0
                    val paymentLinkId = document.getString("paymentLinkId") ?: "N/A"
                    val paymentType = document.getString("paymentType") ?: "Unknown"
                    val paymongoName = document.getString("paymongoName") ?: "N/A"
                    val selectedExtraBackdrop = document.getString("selectedExtraBackdrop") ?: "N/A"
                    val softCopyQty = document.getLong("softCopyQty")?.toInt() ?: 0
                    val totalAmount = document.getDouble("totalAmount")?.toInt() ?: 0
                    val remainingAmount = document.getDouble("remainingAmount")?.toInt() ?: 0
                    val uid = document.getString("uid") ?: "N/A"

                    bookingsList.add(
                        Booking(
                            id = id,
                            appointmentDate = appointmentDate,
                            appointmentTime = appointmentTime,
                            archive = archive,
                            complete = complete,
                            defaultBackdrop = defaultBackdrop,
                            imageUrl = defaultBackdrop,
                            packageType = packageType,
                            extraPersonQty = extraPersonQty,
                            paymentLinkId = paymentLinkId,
                            paymentType = paymentType,
                            paymongoName = paymongoName,
                            selectedExtraBackdrop = selectedExtraBackdrop,
                            softCopyQty = softCopyQty,
                            totalAmount = totalAmount,
                            remainingAmount = remainingAmount,
                            uid = uid
                        )
                    )
                }

                bookingsList.sortBy {
                    try {
                        dateFormat.parse(it.appointmentDate)
                    } catch (e: Exception) {
                        null
                    }
                }

                bookingAdapter.updateBookings(bookingsList)
            }
            .addOnFailureListener { exception ->
                Log.e("Firestore", "Error fetching bookings", exception)
            }
    }

    // Existing "Rebook" click handler remains unchanged
    override fun onRebookClicked(booking: Booking) {
        val intent = Intent(this, RebookPage::class.java).apply {
            putExtra("appointmentDate", booking.appointmentDate)
            putExtra("appointmentTime", booking.appointmentTime)
            putExtra("defaultBackdrop", booking.defaultBackdrop)
            putExtra("packageType", booking.packageType)
            putExtra("extraPersonQty", booking.extraPersonQty)
            putExtra("paymentType", booking.paymentType)
            putExtra("paymongoName", booking.paymongoName)
            putExtra("selectedExtraBackdrop", booking.selectedExtraBackdrop)
            putExtra("softCopyQty", booking.softCopyQty)
            putExtra("totalAmount", booking.totalAmount)
            putExtra("remainingAmount", booking.remainingAmount)
            putExtra("uid", booking.uid)
        }
        startActivity(intent)
    }

    // Existing "Cancel" click handler remains unchanged
    override fun onCancelClicked(booking: Booking) {
        db.collection("payments").document(booking.id)
            .update("complete", "yes")
            .addOnSuccessListener {
                Toast.makeText(this, "Booking Cancelled", Toast.LENGTH_SHORT).show()
                fetchBookings()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to cancel booking", Toast.LENGTH_SHORT).show()
                Log.e("Firestore", "Error updating booking status", e)
            }
    }

    // New "Pay Remaining" click handler added without removing any old code
    override fun onPayRemainingClicked(booking: Booking) {
        currentBookingForPayment = booking
        viewModel.createPaymentLink(booking.remainingAmount, booking.packageType ?: "Remaining Payment")
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
                        viewModel.getPaymentsForLink(paymentLinkId, currentBookingForPayment?.packageType ?: "")
                    }else{}
                } else if (status.equals("failed", ignoreCase = true)) {
                    Toast.makeText(this, "Payment failed", Toast.LENGTH_SHORT).show()
                }else{}
            } else {
                uri.getQueryParameter("payment_id")?.let { paymentId ->
                    viewModel.getPaymentDetails(paymentId)
                }
            }
        }
    }

    // PaymentViewModel and its Factory remain unchanged from your RebookPage implementation

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

        fun createPaymentLink(amount: Int, packageType: String) {
            val convertedAmount = amount * 100
            val requestBody = JSONObject().apply {
                put("data", JSONObject().apply {
                    put("attributes", JSONObject().apply {
                        put("amount", convertedAmount)
                        put("currency", "PHP")
                        put("description", packageType)
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
                .addHeader("Authorization", "Basic ${com.example.photostudio.utils.EncryptionUtils.encodeApiKey("sk_test_yESi8KQWKn2mCE4ZnvKksGVk")}")
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
                .addHeader("Authorization", "Basic ${com.example.photostudio.utils.EncryptionUtils.encodeApiKey("sk_test_yESi8KQWKn2mCE4ZnvKksGVk")}")
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

        fun getPaymentsForLink(linkId: String, expectedPackageType: String) {
            val request = Request.Builder()
                .url("https://api.paymongo.com/v1/links/$linkId")
                .get()
                .addHeader("Authorization", "Basic ${com.example.photostudio.utils.EncryptionUtils.encodeApiKey("sk_test_yESi8KQWKn2mCE4ZnvKksGVk")}")
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
                            getPaymentsForReference(referenceNumber, expectedPackageType)
                        } catch (e: Exception) {
                            Log.e("PaymentViewModel", "Error parsing link details: ${e.localizedMessage}")
                        }
                    } else {
                        Log.e("PaymentViewModel", "Failed to fetch link details: HTTP ${response.code}")
                    }
                }
            })
        }

        private fun getPaymentsForReference(referenceNumber: String, expectedPackageType: String) {
            val request = Request.Builder()
                .url("https://api.paymongo.com/v1/payments")
                .get()
                .addHeader("Authorization", "Basic ${com.example.photostudio.utils.EncryptionUtils.encodeApiKey("sk_test_yESi8KQWKn2mCE4ZnvKksGVk")}")
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
                                    paymentDescription.contains(expectedPackageType, ignoreCase = true)
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
