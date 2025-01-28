package com.example.photostudio

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.*

class paymentPage : AppCompatActivity() {

    private lateinit var viewModel: PaymentViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment_page)

        val paymentButton: Button = findViewById(R.id.downpayment_button)

        // Initialize ViewModel
        viewModel = ViewModelProvider(this, PaymentViewModelFactory())
            .get(PaymentViewModel::class.java)

        // Observe redirect URL
        viewModel.redirectUrl.observe(this) { url ->
            if (!url.isNullOrEmpty()) {
                Log.d("PaymentPage", "Redirecting to: $url")
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(intent)
            } else {
                Toast.makeText(this, "No redirection URL available.", Toast.LENGTH_LONG).show()
            }
        }

        // Observe errors
        viewModel.error.observe(this) { errorMessage ->
            if (!errorMessage.isNullOrEmpty()) {
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                Log.e("PaymentPage", "Error: $errorMessage")
            }
        }

        // Set button click listener
        paymentButton.setOnClickListener {
            Log.d("PaymentPage", "Payment button clicked")
            viewModel.createPaymentLink(10000, "Photo Studio Downpayment")
        }
    }

    // ViewModelFactory for creating the ViewModel
    class PaymentViewModelFactory : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(PaymentViewModel::class.java)) {
                return PaymentViewModel() as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

    // ViewModel for handling API logic
    class PaymentViewModel : ViewModel() {

        private val _redirectUrl = MutableLiveData<String?>()
        val redirectUrl: LiveData<String?> get() = _redirectUrl

        private val _error = MutableLiveData<String>()
        val error: LiveData<String> get() = _error

        private val apiClient = createApiClient()

        /**
         * Creates a payment link using PayMongo Links API.
         * @param amount Amount in cents (e.g., PHP 100.00 = 10000).
         * @param description Description of the payment link.
         */
        fun createPaymentLink(amount: Int, description: String) {
            val requestBody = JSONObject().apply {
                put("data", JSONObject().apply {
                    put("attributes", JSONObject().apply {
                        put("amount", amount)
                        put("currency", "PHP")
                        put("description", description)
                        put("payment_method_types", JSONArray().apply {
                            put("gcash")
                            put("card")
                        })
                    })
                })
            }

            Log.d("PaymentViewModel", "Request Body: $requestBody")

            val request = Request.Builder()
                .url("https://api.paymongo.com/v1/links")
                .post(RequestBody.create("application/json".toMediaType(), requestBody.toString()))
                .addHeader("Authorization", "Basic ${encodeApiKey("sk_test_RXWak95DFfcXnHg3KVBktkq6")}")
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
