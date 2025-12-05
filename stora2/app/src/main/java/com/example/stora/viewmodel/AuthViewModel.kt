package com.example.stora.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.stora.data.AppDatabase
import com.example.stora.data.AuthResponse
import com.example.stora.repository.AuthRepository
import com.example.stora.utils.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null,
    val authResponse: AuthResponse? = null,
    val token: String? = null,
    val isLoggedIn: Boolean = false
)

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val authRepository = AuthRepository()
    private val tokenManager = TokenManager.getInstance(application)
    private val database = AppDatabase.getDatabase(application)

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        // Check if user already logged in
        checkLoginStatus()
    }

    private fun checkLoginStatus() {
        if (tokenManager.isLoggedIn()) {
            _uiState.value = _uiState.value.copy(
                isLoggedIn = true,
                token = tokenManager.getToken()
            )
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            authRepository.login(email, password)
                .onSuccess { response ->
                    // Save token and user data
                    response.token?.let { tokenManager.saveToken(it) }
                    response.data?.let { userData ->
                        tokenManager.saveUserData(
                            id = userData.id,
                            name = userData.name,
                            email = userData.email
                        )
                    }

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isSuccess = true,
                        authResponse = response,
                        token = response.token,
                        isLoggedIn = true
                    )
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isSuccess = false,
                        errorMessage = exception.message
                    )
                }
        }
    }

    fun signup(name: String, email: String, password: String, passwordConfirmation: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            authRepository.signup(name, email, password, passwordConfirmation)
                .onSuccess { response ->
                    // Save token and user data
                    response.token?.let { tokenManager.saveToken(it) }
                    response.data?.let { userData ->
                        tokenManager.saveUserData(
                            id = userData.id,
                            name = userData.name,
                            email = userData.email
                        )
                    }

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isSuccess = true,
                        authResponse = response,
                        token = response.token,
                        isLoggedIn = true
                    )
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isSuccess = false,
                        errorMessage = exception.message
                    )
                }
        }
    }

    fun logout() {
        viewModelScope.launch {
            val currentToken = tokenManager.getToken()
            if (currentToken != null) {
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

                authRepository.logout(currentToken)
                    .onSuccess { response ->
                        // Clear all saved data
                        tokenManager.clearAll()

                        // Clear Room database
                        viewModelScope.launch {
                            try {
                                database.inventoryDao().clearAllInventoryItems()
                                android.util.Log.d("AuthViewModel", "Room database cleared on logout")
                            } catch (e: Exception) {
                                android.util.Log.e("AuthViewModel", "Error clearing Room database", e)
                            }
                        }

                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isSuccess = true,
                            authResponse = response,
                            token = null,
                            isLoggedIn = false
                        )
                    }
                    .onFailure { exception ->
                        // Clear data even if logout API fails
                        tokenManager.clearAll()

                        // Clear Room database
                        viewModelScope.launch {
                            try {
                                database.inventoryDao().clearAllInventoryItems()
                                android.util.Log.d("AuthViewModel", "Room database cleared on logout (with error)")
                            } catch (e: Exception) {
                                android.util.Log.e("AuthViewModel", "Error clearing Room database", e)
                            }
                        }

                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isSuccess = false,
                            errorMessage = exception.message,
                            token = null,
                            isLoggedIn = false
                        )
                    }
            } else {
                // No token, just clear state
                tokenManager.clearAll()

                // Clear Room database
                viewModelScope.launch {
                    try {
                        database.inventoryDao().clearAllInventoryItems()
                        android.util.Log.d("AuthViewModel", "Room database cleared (no token)")
                    } catch (e: Exception) {
                        android.util.Log.e("AuthViewModel", "Error clearing Room database", e)
                    }
                }

                _uiState.value = _uiState.value.copy(
                    token = null,
                    isLoggedIn = false,
                    authResponse = null
                )
            }
        }
    }

    fun getProfile() {
        viewModelScope.launch {
            val currentToken = tokenManager.getToken()
            if (currentToken != null) {
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

                authRepository.getProfile(currentToken)
                    .onSuccess { response ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isSuccess = true,
                            authResponse = response
                        )
                    }
                    .onFailure { exception ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isSuccess = false,
                            errorMessage = exception.message
                        )
                    }
            } else {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "No authentication token found"
                )
            }
        }
    }

    fun updateProfile(name: String?, email: String?) {
        viewModelScope.launch {
            val currentToken = tokenManager.getToken()
            if (currentToken != null) {
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

                authRepository.updateProfile(currentToken, name, email)
                    .onSuccess { response ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isSuccess = true,
                            authResponse = response
                        )
                    }
                    .onFailure { exception ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isSuccess = false,
                            errorMessage = exception.message
                        )
                    }
            } else {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "No authentication token found"
                )
            }
        }
    }

    fun clearState() {
        _uiState.value = AuthUiState()
    }

    fun getUserId(): Int {
        return tokenManager.getUserId()
    }

    fun getUserName(): String? {
        return tokenManager.getUserName()
    }

    fun getUserEmail(): String? {
        return tokenManager.getUserEmail()
    }

    fun isUserLoggedIn(): Boolean {
        return tokenManager.isLoggedIn()
    }
}
