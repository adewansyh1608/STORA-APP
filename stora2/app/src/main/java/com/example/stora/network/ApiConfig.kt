package com.example.stora.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiConfig {
    // Base URL - ganti dengan URL API Anda
    // Untuk development local: "http://10.0.2.2:3000/api/" (Android Emulator)
    // Untuk device fisik: "http://YOUR_LOCAL_IP:3000/api/"
    // Jika error 404, coba ganti ke "http://10.0.2.2:3000/" tanpa /api/
    private const val BASE_URL = "http://10.0.2.2:3000/api/v1/"
    private const val TAG = "ApiConfig"

    private fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request()
                val url = request.url.toString()
                val method = request.method

                android.util.Log.d(TAG, "========================================")
                android.util.Log.d(TAG, "REQUEST: $method $url")
                android.util.Log.d(TAG, "Headers: ${request.headers}")

                val startTime = System.currentTimeMillis()
                val response = chain.proceed(request)
                val duration = System.currentTimeMillis() - startTime

                android.util.Log.d(TAG, "RESPONSE: ${response.code} (${duration}ms)")
                android.util.Log.d(TAG, "Response Headers: ${response.headers}")

                if (!response.isSuccessful) {
                    val errorBody = response.peekBody(Long.MAX_VALUE).string()
                    android.util.Log.e(TAG, "ERROR BODY: $errorBody")
                }

                android.util.Log.d(TAG, "========================================")
                response
            }
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private fun provideRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(provideOkHttpClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    fun provideApiService(): ApiService {
        return provideRetrofit().create(ApiService::class.java)
    }
}
