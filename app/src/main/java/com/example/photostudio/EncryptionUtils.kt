package com.example.photostudio.utils

import android.util.Base64

object EncryptionUtils {
    fun encodeApiKey(apiKey: String): String {
        return Base64.encodeToString("$apiKey:".toByteArray(), Base64.NO_WRAP)
    }
}