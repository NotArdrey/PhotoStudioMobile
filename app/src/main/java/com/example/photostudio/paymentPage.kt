package com.example.photostudio

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
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
    private lateinit var backdropQty: EditText
    private lateinit var backdropSelectionLayout: LinearLayout
    private val backdropOptions = arrayOf("Backdrop A", "Backdrop B", "Backdrop C", "Backdrop D", "Backdrop E")
    private lateinit var extraPersonSection: LinearLayout



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment_page)

        extraPersonSection = findViewById(R.id.extraPersonSection)
        extraPersonSection.visibility = View.GONE
        val paymentButton: Button = findViewById(R.id.downpayment_button)
        backdropQty = findViewById(R.id.backdropQty)
        backdropSelectionLayout = findViewById(R.id.backdropSelectionLayout)

        viewModel = ViewModelProvider(this, PaymentViewModelFactory())
            .get(PaymentViewModel::class.java)

        viewModel.redirectUrl.observe(this) { url ->
            if (!url.isNullOrEmpty()) {
                Log.d("PaymentPage", "Redirecting to: $url")
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(intent)
            } else {
                Toast.makeText(this, "No redirection URL available.", Toast.LENGTH_LONG).show()
            }
        }

        viewModel.error.observe(this) { errorMessage ->
            if (!errorMessage.isNullOrEmpty()) {
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                Log.e("PaymentPage", "Error: $errorMessage")
            }
        }

        paymentButton.setOnClickListener {
            Log.d("PaymentPage", "Payment button clicked")
            viewModel.createPaymentLink(10000, "Photo Studio Downpayment")
        }

        // Monitor changes in the backdrop quantity field
        backdropQty.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) { // Only update when user leaves the field
                updateBackdropSelection()
            }
        }
    }


    private fun updateBackdropSelection() {
        val qtyStr = backdropQty.text.toString()
        if (qtyStr.isEmpty() || qtyStr.toInt() == 0) {
            backdropSelectionLayout.visibility = View.GONE
            backdropSelectionLayout.removeAllViews()
            return
        }

        val quantity = qtyStr.toInt()
        backdropSelectionLayout.removeAllViews() // Clear previous dropdowns

        for (i in 1..quantity) {
            val backdropSpinner = Spinner(this)
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, backdropOptions)
            backdropSpinner.adapter = adapter

            // Add spacing between spinners
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 10, 0, 10)
            backdropSpinner.layoutParams = params

            backdropSelectionLayout.addView(backdropSpinner)
        }

        // Show selection layout
        backdropSelectionLayout.visibility = View.VISIBLE
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

        private val apiClient = createApiClient()

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
                .addHeader("Authorization", "Basic ${encodeApiKey("")}") // Insert API key here
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
