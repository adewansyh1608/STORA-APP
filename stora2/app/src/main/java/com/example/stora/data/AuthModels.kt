package com.example.stora.data

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    @SerializedName("email")
    val email: String,
    @SerializedName("password")
    val password: String
)

data class SignupRequest(
    @SerializedName("name")
    val name: String,
    @SerializedName("email")
    val email: String,
    @SerializedName("password")
    val password: String,
    @SerializedName("password_confirmation")
    val passwordConfirmation: String
)

data class UpdateProfileRequest(
    @SerializedName("name")
    val name: String?,
    @SerializedName("email")
    val email: String?,
    @SerializedName("foto_profile")
    val fotoProfile: String?
)

data class ResetPasswordRequest(
    @SerializedName("email")
    val email: String,
    @SerializedName("new_password")
    val newPassword: String,
    @SerializedName("confirm_password")
    val confirmPassword: String
)

data class AuthResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("message")
    val message: String,
    @SerializedName("data")
    val data: UserData?,
    @SerializedName("token")
    val token: String?
)

data class UserData(
    @SerializedName("id")
    val id: Int,
    @SerializedName("name")
    val name: String,
    @SerializedName("email")
    val email: String,
    @SerializedName("foto_profile")
    val fotoProfile: String?,
    @SerializedName("email_verified_at")
    val emailVerifiedAt: String?,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String
)

data class ErrorResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("message")
    val message: String,
    @SerializedName("errors")
    val errors: Map<String, List<String>>?
)
