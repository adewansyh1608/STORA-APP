package com.example.stora.data

import com.google.gson.annotations.SerializedName

// Response wrapper for notification API
data class NotificationApiResponse<T>(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("data")
    val data: T?,
    @SerializedName("message")
    val message: String?
)

// Reminder setting model from API
data class ReminderApiModel(
    @SerializedName("ID_Reminder")
    val idReminder: Int,
    @SerializedName("ID_User")
    val idUser: Int,
    @SerializedName("reminder_type")
    val reminderType: String, // "periodic" or "custom"
    @SerializedName("title")
    val title: String?,
    @SerializedName("periodic_months")
    val periodicMonths: Int?,
    @SerializedName("scheduled_datetime")
    val scheduledDatetime: String?,
    @SerializedName("fcm_token")
    val fcmToken: String?,
    @SerializedName("is_active")
    val isActive: Boolean,
    @SerializedName("last_notified")
    val lastNotified: String?,
    @SerializedName("created_at")
    val createdAt: String?,
    @SerializedName("updated_at")
    val updatedAt: String?
)

// Request to create/update reminder
data class ReminderRequest(
    @SerializedName("reminder_type")
    val reminderType: String,
    @SerializedName("title")
    val title: String? = null,
    @SerializedName("periodic_months")
    val periodicMonths: Int? = null,
    @SerializedName("scheduled_datetime")
    val scheduledDatetime: String? = null,
    @SerializedName("fcm_token")
    val fcmToken: String? = null,
    @SerializedName("is_active")
    val isActive: Boolean? = null
)

// Request to register FCM token
data class FcmTokenRequest(
    @SerializedName("fcm_token")
    val fcmToken: String
)

// Notification history model from database
data class NotificationHistoryApiModel(
    @SerializedName("ID_Notifikasi")
    val idNotifikasi: Int,
    @SerializedName("Judul")
    val judul: String?,
    @SerializedName("Pesan")
    val pesan: String?,
    @SerializedName("Tanggal")
    val tanggal: String?,
    @SerializedName("Status")
    val status: String?,
    @SerializedName("ID_User")
    val idUser: Int?,
    @SerializedName("ID_Peminjaman")
    val idPeminjaman: Int?,
    @SerializedName("isSynced")
    val isSynced: Boolean?
)
