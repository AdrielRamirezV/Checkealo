package com.checkealo.app.network

import android.content.Context
import android.util.Log
import com.checkealo.app.data.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class WhatsAppClientHelper(private val context: Context) {
    private val TAG = "WhatsAppClientHelper"
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun sendMessage(messageText: String): Result<Unit> = withContext(Dispatchers.IO) {
        val settings = AppSettings(context)
        if (!settings.whatsappEnabled) {
            return@withContext Result.failure(Exception("WhatsApp sending is disabled in settings"))
        }

        val apiUrl = settings.whatsappApiUrl
        val token = settings.whatsappToken
        val phone = settings.whatsappPhone

        if (apiUrl.isEmpty() || phone.isEmpty()) {
            return@withContext Result.failure(Exception("WhatsApp API URL or Phone is empty in settings"))
        }

        try {
            // Build the JSON request body. 
            // We use a generic format compatible with most WhatsApp gateways (e.g. evolution-api, baileys wrappers)
            val jsonBody = JSONObject().apply {
                put("phone", phone)
                put("message", messageText)
                // Add common variants just in case the gateway uses other keys
                put("number", phone)
                put("to", phone)
                put("text", messageText)
            }

            val requestBody = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            
            val requestBuilder = Request.Builder()
                .url(apiUrl)
                .post(requestBody)

            // Add authorization header if token is provided
            if (token.isNotEmpty()) {
                if (token.startsWith("Bearer ") || token.startsWith("apikey ")) {
                    requestBuilder.addHeader("Authorization", token)
                } else {
                    requestBuilder.addHeader("Authorization", "Bearer $token")
                    // Some gateways use 'apikey' header instead of Authorization
                    requestBuilder.addHeader("apikey", token)
                }
            }

            val request = requestBuilder.build()
            Log.d(TAG, "Sending request to WhatsApp Gateway: $apiUrl")
            
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: ""
                Log.d(TAG, "Response Code: ${response.code}, Body: $responseBody")
                
                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("HTTP Error ${response.code}: $responseBody"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending WhatsApp message", e)
            Result.failure(e)
        }
    }

    // Function to test message sending
    suspend fun testMessage(
        apiUrl: String,
        token: String,
        phone: String,
        messageText: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val jsonBody = JSONObject().apply {
                put("phone", phone)
                put("message", messageText)
                put("number", phone)
                put("to", phone)
                put("text", messageText)
            }
            val requestBody = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val requestBuilder = Request.Builder()
                .url(apiUrl)
                .post(requestBody)

            if (token.isNotEmpty()) {
                if (token.startsWith("Bearer ") || token.startsWith("apikey ")) {
                    requestBuilder.addHeader("Authorization", token)
                } else {
                    requestBuilder.addHeader("Authorization", "Bearer $token")
                    requestBuilder.addHeader("apikey", token)
                }
            }
            val request = requestBuilder.build()
            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("HTTP Error ${response.code}: $body"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
