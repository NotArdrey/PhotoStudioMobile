package com.example.photostudio

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.*
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.firebase.database.FirebaseDatabase
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
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
    private lateinit var extraPersonSection: LinearLayout

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

        val showExtraSection = intent.getBooleanExtra("showExtraSection", false)
        val extraPersonQtyInput = findViewById<EditText>(R.id.extraPersonQty)
        val softCopyQtyInput = findViewById<EditText>(R.id.softCopyQty)
        val calendarView = findViewById<CalendarView>(R.id.calendarView)
        val timePicker = findViewById<TimePicker>(R.id.timePicker)
        val defaultBackdropAutoComplete = findViewById<MaterialAutoCompleteTextView>(R.id.defaultBackdropSpinner)

        val paymentButton: Button = findViewById(R.id.downpayment_button)
        val fullPaymentButton: Button = findViewById(R.id.full_payment_button)

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigationView.setOnNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    true
                }
                R.id.nav_cart -> {
                    startActivity(Intent(this, bookingPage::class.java))
                    finish()
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, AccountPage::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }

        extraPersonSection.visibility = if (showExtraSection) View.VISIBLE else View.GONE


        paymentButton.setOnClickListener {

            val extraPersonAmount = defaultPax * 200
            val backdropAmount = (backdropQty.text.toString().toIntOrNull() ?: 0) * 200
            val packagePriceInt = packagePriceStr?.toIntOrNull() ?: 0


            val totalAmount = extraPersonAmount + backdropAmount + packagePriceInt
            Log.d("PaymentPage", "Total amount calculated: $totalAmount")


            val extraPersonQtyValue = extraPersonQtyInput.text.toString().toIntOrNull() ?: 0

            val softCopyQtyValue = softCopyQtyInput.text.toString().toIntOrNull() ?: 0

            val defaultBackdropValue = defaultBackdropAutoComplete.text.toString()

            val selectedExtraBackdropOptions = mutableListOf<String>()
            for (i in 0 until backdropChipGroup.childCount) {
                val chip = backdropChipGroup.getChildAt(i) as Chip
                if (chip.text.toString() != "Select Backdrop") {
                    selectedExtraBackdropOptions.add(chip.text.toString())
                }
            }
            val selectedExtraBackdrop = if (selectedExtraBackdropOptions.isNotEmpty())
                selectedExtraBackdropOptions.joinToString(", ") else ""

            val appointmentDate = calendarView.date

            val appointmentHour = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M)
                timePicker.hour else timePicker.currentHour
            val appointmentMinute = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M)
                timePicker.minute else timePicker.currentMinute
            val appointmentTime = "$appointmentHour:$appointmentMinute"


            val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
            val uid = sharedPreferences.getString("uid", null)
            if (uid != null) {
                val paymentType = "Downpayment"
                val paymentData = hashMapOf(
                    "uid" to uid,
                    "totalAmount" to totalAmount,
                    "description" to "Captured By K Package: $description",
                    "paymentType" to paymentType,
                    "extraPersonQty" to extraPersonQtyValue,
                    "softCopyQty" to softCopyQtyValue,
                    "defaultBackdrop" to defaultBackdropValue,
                    "selectedExtraBackdrop" to selectedExtraBackdrop,
                    "appointmentDate" to appointmentDate,
                    "appointmentTime" to appointmentTime
                )
                val databaseReference = FirebaseDatabase.getInstance().reference
                databaseReference.child("payments").push().setValue(paymentData)
                    .addOnSuccessListener {
                        Log.d("PaymentPage", "Payment data saved successfully in Realtime Database.")
                    }
                    .addOnFailureListener { e ->
                        Log.e("PaymentPage", "Error saving payment data to Realtime Database", e)
                    }
            } else {
                Log.e("PaymentPage", "User UID not found in SharedPreferences")
            }

            viewModel.createPaymentLink(totalAmount, "Captured By K Package: $description")
        }


        fullPaymentButton.setOnClickListener {
            val extraPersonAmount = defaultPax * 200
            val backdropAmount = (backdropQty.text.toString().toIntOrNull() ?: 0) * 200
            val packagePriceInt = packagePriceStr?.toIntOrNull() ?: 0


            val totalAmount = extraPersonAmount + backdropAmount + packagePriceInt
            Log.d("PaymentPage", "Total amount calculated for full payment: $totalAmount")


            val extraPersonQtyValue = extraPersonQtyInput.text.toString().toIntOrNull() ?: 0

            val softCopyQtyValue = softCopyQtyInput.text.toString().toIntOrNull() ?: 0

            val defaultBackdropValue = defaultBackdropAutoComplete.text.toString()

            val selectedExtraBackdropOptions = mutableListOf<String>()
            for (i in 0 until backdropChipGroup.childCount) {
                val chip = backdropChipGroup.getChildAt(i) as Chip
                if (chip.text.toString() != "Select Backdrop") {
                    selectedExtraBackdropOptions.add(chip.text.toString())
                }
            }
            val selectedExtraBackdrop = if (selectedExtraBackdropOptions.isNotEmpty())
                selectedExtraBackdropOptions.joinToString(", ") else ""

            val appointmentDate = calendarView.date

            val appointmentHour = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M)
                timePicker.hour else timePicker.currentHour
            val appointmentMinute = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M)
                timePicker.minute else timePicker.currentMinute
            val appointmentTime = "$appointmentHour:$appointmentMinute"


            val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
            val uid = sharedPreferences.getString("uid", null)
            if (uid != null) {
                val paymentType = "Full Payment"
                val paymentData = hashMapOf(
                    "uid" to uid,
                    "totalAmount" to totalAmount,
                    "description" to "Captured By K Package: $description",
                    "paymentType" to paymentType,

                    "extraPersonQty" to extraPersonQtyValue,
                    "softCopyQty" to softCopyQtyValue,
                    "defaultBackdrop" to defaultBackdropValue,
                    "selectedExtraBackdrop" to selectedExtraBackdrop,
                    "appointmentDate" to appointmentDate,
                    "appointmentTime" to appointmentTime
                )


                val databaseReference = FirebaseDatabase.getInstance().reference
                databaseReference.child("payments").push().setValue(paymentData)
                    .addOnSuccessListener {
                        Log.d("PaymentPage", "Full payment data saved successfully in Realtime Database.")
                    }
                    .addOnFailureListener { e ->
                        Log.e("PaymentPage", "Error saving full payment data to Realtime Database", e)
                    }
            } else {
                Log.e("PaymentPage", "User UID not found in SharedPreferences for full payment")
            }

            viewModel.createPaymentLink(totalAmount, "Captured By K Package: $description")
        }


        viewModel = ViewModelProvider(this, PaymentViewModelFactory())
            .get(PaymentViewModel::class.java)


        viewModel.redirectUrl.observe(this) { url ->
            if (!url.isNullOrEmpty()) {
                Log.d("PaymentPage", "Redirecting to: $url")
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
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


        backdropQty.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                updateBackdropSelection()
            }
        }


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

            val chip = Chip(this)

            chip.text = "Select Backdrop"
            chip.isClickable = true
            chip.isCheckable = false


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
                .addHeader("Authorization", "Basic ${encodeApiKey("pk_test_3uB5YGaGyt3GtosNJ4A9Hyo6")}") // Insert your API key here.
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