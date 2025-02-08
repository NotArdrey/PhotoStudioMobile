package com.example.photostudio

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.Base64

class PaymentPage : AppCompatActivity() {

    private lateinit var viewModel: PaymentViewModel
    private lateinit var backdropQty: EditText
    private lateinit var dynamicBackdropCard: CardView
    private lateinit var backdropChipGroup: ChipGroup
    private val backdropOptions = arrayOf("Red", "Yellow", "Peach", "Brown", "Backdrop E")
    private lateinit var extraPersonSection: android.widget.LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment_page)


        val defaultPax = intent.getIntExtra("defaultPax", 3)
        val description = intent.getStringExtra("description")
        val packagePriceStr = intent.getStringExtra("packagePrice")

        extraPersonSection = findViewById(R.id.extraPersonSection)
        extraPersonSection.visibility = android.view.View.GONE
        backdropQty = findViewById(R.id.backdropQty)
        dynamicBackdropCard = findViewById(R.id.dynamicBackdropCard)
        backdropChipGroup = findViewById(R.id.backdropChipGroup)
        dynamicBackdropCard.visibility = android.view.View.GONE

        val paymentButton: Button = findViewById(R.id.downpayment_button)

        paymentButton.setOnClickListener {

            val extraPersonAmount = defaultPax * 200

            val backdropAmount = (backdropQty.text.toString().toIntOrNull() ?: 0) * 200


            val packagePriceInt = packagePriceStr?.toIntOrNull() ?: 0

            // Calculate the total amount.
            val totalAmount = extraPersonAmount + backdropAmount + packagePriceInt

            // Use totalAmount as needed...
            Log.d("PaymentPage", "Total amount calculated: $totalAmount")
            viewModel.createPaymentLink(totalAmount, "Captured By K Package: $description")
        }

        // Initialize ViewModel.
        viewModel = ViewModelProvider(this, PaymentViewModelFactory())
            .get(PaymentViewModel::class.java)

        // Observe redirect URL.
        viewModel.redirectUrl.observe(this) { url ->
            if (!url.isNullOrEmpty()) {
                Log.d("PaymentPage", "Redirecting to: $url")
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            } else {
                Toast.makeText(this, "No redirection URL available.", Toast.LENGTH_LONG).show()
            }
        }

        // Observe errors.
        viewModel.error.observe(this) { errorMessage ->
            if (!errorMessage.isNullOrEmpty()) {
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                Log.e("PaymentPage", "Error: $errorMessage")
            }
        }

        // When backdropQty loses focus, update the extra backdrop selection.
        backdropQty.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                updateBackdropSelection()
            }
        }

        // Configure the default backdrop dropdown.
        val defaultBackdropAutoComplete = findViewById<MaterialAutoCompleteTextView>(R.id.defaultBackdropSpinner)
        val options = resources.getStringArray(R.array.Defaltbackdrop_options)
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, options)
        defaultBackdropAutoComplete.setAdapter(adapter)
        defaultBackdropAutoComplete.threshold = 0
        defaultBackdropAutoComplete.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                defaultBackdropAutoComplete.showDropDown()
            }
        }
    }

    /**
     * Updates the extra backdrop selection dynamically based on the quantity entered.
     * This implementation uses a ChipGroup inside a CardView to display available backdrop options.
     * Each Chip, when clicked, will show a dialog to let the user select a backdrop from backdropOptions.
     */
    private fun updateBackdropSelection() {
        val qtyStr = backdropQty.text.toString()
        if (qtyStr.isEmpty() || qtyStr.toInt() == 0) {
            dynamicBackdropCard.visibility = android.view.View.GONE
            backdropChipGroup.removeAllViews()
            return
        }

        val quantity = qtyStr.toInt()
        backdropChipGroup.removeAllViews()

        for (i in 1..quantity) {
            // Create a new Chip for the extra backdrop selection.
            val chip = Chip(this)
            // Set a default text for the chip.
            chip.text = "Select Backdrop"
            chip.isClickable = true
            chip.isCheckable = false

            // Set a click listener to allow the user to select a backdrop.
            chip.setOnClickListener {
                MaterialAlertDialogBuilder(this@PaymentPage)
                    .setTitle("Select Backdrop")
                    .setItems(backdropOptions) { dialog, which ->
                        chip.text = backdropOptions[which]
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }

            backdropChipGroup.addView(chip)
        }
        dynamicBackdropCard.visibility = android.view.View.VISIBLE
    }

    // ViewModel Factory
    class PaymentViewModelFactory : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(PaymentViewModel::class.java)) {
                return PaymentViewModel() as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

    // PaymentViewModel that handles the API call.
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
                .addHeader("Authorization", "Basic ${encodeApiKey("")}") // Insert your API key here.
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
