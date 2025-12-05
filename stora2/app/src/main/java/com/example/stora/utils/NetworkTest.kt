package com.example.stora.utils

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL

object NetworkTest {
    private const val TAG = "NetworkTest"
    
    fun testBackendConnection() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Test koneksi ke backend
                val url = URL("http://10.0.2.2:3000/api/login")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                
                val responseCode = connection.responseCode
                Log.d(TAG, "Backend connection test - Response code: $responseCode")
                
                if (responseCode in 200..299 || responseCode == 404 || responseCode == 405) {
                    Log.d(TAG, "✅ Backend is reachable!")
                } else {
                    Log.e(TAG, "❌ Backend connection failed with code: $responseCode")
                }
                
                connection.disconnect()
            } catch (e: Exception) {
                Log.e(TAG, "❌ Backend connection error: ${e.message}")
                Log.e(TAG, "Possible issues:")
                Log.e(TAG, "1. Backend not running on port 3000")
                Log.e(TAG, "2. Firewall blocking connection")
                Log.e(TAG, "3. Wrong IP address (try your computer's IP for physical device)")
            }
        }
    }
}
