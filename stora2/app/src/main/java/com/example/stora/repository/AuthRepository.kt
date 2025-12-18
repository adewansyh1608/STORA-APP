package com.example.stora.repository

import com.example.stora.data.AuthResponse
import com.example.stora.data.ErrorResponse
import com.example.stora.data.LoginRequest
import com.example.stora.data.SignupRequest
import com.example.stora.data.ResetPasswordRequest
import com.example.stora.data.UpdateProfileRequest
import com.example.stora.network.ApiConfig
import com.example.stora.network.ApiService
import com.google.gson.Gson
import retrofit2.Response

class AuthRepository {
    private val apiService: ApiService = ApiConfig.provideApiService()
    
    suspend fun login(email: String, password: String): Result<AuthResponse> {
        return try {
            val loginRequest = LoginRequest(email, password)
            val response = apiService.login(loginRequest)
            
            if (response.isSuccessful) {
                response.body()?.let { authResponse ->
                    Result.success(authResponse)
                } ?: Result.failure(Exception("Response body is null"))
            } else {
                val errorBody = response.errorBody()?.string()
                val errorResponse = try {
                    Gson().fromJson(errorBody, ErrorResponse::class.java)
                } catch (e: Exception) {
                    ErrorResponse(false, "Unknown error occurred", null)
                }
                Result.failure(Exception(errorResponse.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun signup(
        name: String, 
        email: String, 
        password: String, 
        passwordConfirmation: String
    ): Result<AuthResponse> {
        return try {
            val signupRequest = SignupRequest(name, email, password, passwordConfirmation)
            val response = apiService.signup(signupRequest)
            
            if (response.isSuccessful) {
                response.body()?.let { authResponse ->
                    Result.success(authResponse)
                } ?: Result.failure(Exception("Response body is null"))
            } else {
                val errorBody = response.errorBody()?.string()
                val errorResponse = try {
                    Gson().fromJson(errorBody, ErrorResponse::class.java)
                } catch (e: Exception) {
                    ErrorResponse(false, "Unknown error occurred", null)
                }
                Result.failure(Exception(errorResponse.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun logout(token: String): Result<AuthResponse> {
        return try {
            val response = apiService.logout("Bearer $token")
            
            if (response.isSuccessful) {
                response.body()?.let { authResponse ->
                    Result.success(authResponse)
                } ?: Result.failure(Exception("Response body is null"))
            } else {
                val errorBody = response.errorBody()?.string()
                val errorResponse = try {
                    Gson().fromJson(errorBody, ErrorResponse::class.java)
                } catch (e: Exception) {
                    ErrorResponse(false, "Unknown error occurred", null)
                }
                Result.failure(Exception(errorResponse.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resetPassword(
        email: String,
        newPassword: String,
        confirmPassword: String
    ): Result<AuthResponse> {
        return try {
            val request = ResetPasswordRequest(email, newPassword, confirmPassword)
            val response = apiService.resetPassword(request)
            
            if (response.isSuccessful) {
                response.body()?.let { authResponse ->
                    Result.success(authResponse)
                } ?: Result.failure(Exception("Response body is null"))
            } else {
                val errorBody = response.errorBody()?.string()
                val errorResponse = try {
                    Gson().fromJson(errorBody, ErrorResponse::class.java)
                } catch (e: Exception) {
                    ErrorResponse(false, "Unknown error occurred", null)
                }
                Result.failure(Exception(errorResponse.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getProfile(token: String): Result<AuthResponse> {
        return try {
            val response = apiService.getProfile("Bearer $token")
            
            if (response.isSuccessful) {
                response.body()?.let { authResponse ->
                    Result.success(authResponse)
                } ?: Result.failure(Exception("Response body is null"))
            } else {
                val errorBody = response.errorBody()?.string()
                val errorResponse = try {
                    Gson().fromJson(errorBody, ErrorResponse::class.java)
                } catch (e: Exception) {
                    ErrorResponse(false, "Unknown error occurred", null)
                }
                Result.failure(Exception(errorResponse.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateProfile(token: String, name: String?, email: String?, fotoProfile: String? = null): Result<AuthResponse> {
        return try {
            val updateRequest = UpdateProfileRequest(name, email, fotoProfile)
            val response = apiService.updateProfile("Bearer $token", updateRequest)
            
            if (response.isSuccessful) {
                response.body()?.let { authResponse ->
                    Result.success(authResponse)
                } ?: Result.failure(Exception("Response body is null"))
            } else {
                val errorBody = response.errorBody()?.string()
                val errorResponse = try {
                    Gson().fromJson(errorBody, ErrorResponse::class.java)
                } catch (e: Exception) {
                    ErrorResponse(false, "Unknown error occurred", null)
                }
                Result.failure(Exception(errorResponse.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadProfilePhoto(token: String, photoPart: okhttp3.MultipartBody.Part): Result<AuthResponse> {
        return try {
            val response = apiService.uploadProfilePhoto("Bearer $token", photoPart)
            
            if (response.isSuccessful) {
                response.body()?.let { authResponse ->
                    Result.success(authResponse)
                } ?: Result.failure(Exception("Response body is null"))
            } else {
                val errorBody = response.errorBody()?.string()
                val errorResponse = try {
                    Gson().fromJson(errorBody, ErrorResponse::class.java)
                } catch (e: Exception) {
                    ErrorResponse(false, "Unknown error occurred", null)
                }
                Result.failure(Exception(errorResponse.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

