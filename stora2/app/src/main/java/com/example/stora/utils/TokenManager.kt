package com.example.stora.utils

import android.content.Context
import android.content.SharedPreferences

class TokenManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "stora_prefs"  // Match with repository
        private const val TOKEN_KEY = "auth_token"
        private const val USER_ID_KEY = "user_id"
        private const val USER_NAME_KEY = "user_name"
        private const val USER_EMAIL_KEY = "user_email"

        @Volatile
        private var instance: TokenManager? = null

        fun getInstance(context: Context): TokenManager {
            return instance ?: synchronized(this) {
                instance ?: TokenManager(context.applicationContext).also { instance = it }
            }
        }
    }

    fun saveToken(token: String) {
        prefs.edit().putString(TOKEN_KEY, token).apply()
    }

    fun getToken(): String? {
        return prefs.getString(TOKEN_KEY, null)
    }

    fun saveUserData(id: Int, name: String, email: String) {
        prefs.edit().apply {
            putInt(USER_ID_KEY, id)
            putString(USER_NAME_KEY, name)
            putString(USER_EMAIL_KEY, email)
            apply()
        }
    }

    fun getUserId(): Int {
        return prefs.getInt(USER_ID_KEY, -1)
    }

    fun getUserName(): String? {
        return prefs.getString(USER_NAME_KEY, null)
    }

    fun getUserEmail(): String? {
        return prefs.getString(USER_EMAIL_KEY, null)
    }

    fun isLoggedIn(): Boolean {
        return getToken() != null && getUserId() != -1
    }

    fun getAuthHeader(): String? {
        return getToken()?.let { "Bearer $it" }
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    fun hasValidToken(): Boolean {
        return !getToken().isNullOrEmpty()
    }
}
