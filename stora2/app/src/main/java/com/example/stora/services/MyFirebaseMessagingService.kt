package com.example.stora.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.stora.MainActivity
import com.example.stora.R
import com.example.stora.data.AppDatabase
import com.example.stora.data.NotificationHistoryEntity
import com.example.stora.utils.TokenManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FCMService"
        private const val CHANNEL_ID = "inventory_reminders"
        private const val CHANNEL_NAME = "Pengingat Inventory"
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.d(TAG, "=== FCM Message Received ===")
        Log.d(TAG, "From: ${remoteMessage.from}")
        Log.d(TAG, "Data: ${remoteMessage.data}")

        val title = remoteMessage.notification?.title 
            ?: remoteMessage.data["title"] 
            ?: "STORA"
        val body = remoteMessage.notification?.body 
            ?: remoteMessage.data["body"] 
            ?: ""
        val type = remoteMessage.data["type"] ?: "unknown"
        val reminderId = remoteMessage.data["reminder_id"]
        val reminderTimestamp = remoteMessage.data["reminder_timestamp"]?.toLongOrNull()
        
        Log.d(TAG, "Title: $title, Body: $body, Type: $type, ReminderId: $reminderId")
        
        // Handle reminder notifications with deduplication
        if (type == "custom_reminder" && reminderId != null) {
            handleReminderNotification(title, body, reminderId, reminderTimestamp)
        } else if (type in listOf("loan_deadline_warning", "loan_deadline", "loan_overdue")) {
            // Handle loan notifications
            val loanId = remoteMessage.data["loan_id"]
            val loanTimestamp = remoteMessage.data["loan_timestamp"]?.toLongOrNull()
            handleLoanNotification(title, body, type, loanId, loanTimestamp)
        } else {
            // Non-reminder notification - show directly
            showNotification(title, body, type)
        }
    }
    
    /**
     * Handle loan notification with deduplication check.
     * If offline notification already shown for this loan type, skip showing again.
     */
    private fun handleLoanNotification(
        title: String,
        body: String,
        type: String,
        loanId: String?,
        loanTimestamp: Long?
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = AppDatabase.getDatabase(applicationContext)
                val tokenManager = TokenManager(applicationContext)
                val userId = tokenManager.getUserId()
                
                if (userId == -1) {
                    Log.d(TAG, "No user logged in, showing loan notification without dedup check")
                    showNotification(title, body, type)
                    return@launch
                }
                
                // Calculate start and end of today for deduplication
                val timestamp = loanTimestamp ?: System.currentTimeMillis()
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = timestamp
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startOfDay = calendar.timeInMillis
                
                calendar.add(Calendar.DAY_OF_MONTH, 1)
                val endOfDay = calendar.timeInMillis
                
                // Check if notification already exists by title+message today (handles loan notifications)
                val existingNotification = database.notificationHistoryDao()
                    .getNotificationByTitleMessageAndDate(userId, title, body, startOfDay, endOfDay)
                
                if (existingNotification != null) {
                    Log.d(TAG, "⏭ Loan notification already exists (offline was shown)")
                    
                    // If existing notification was local (offline), update it to mark as synced
                    if (existingNotification.isLocallyCreated) {
                        val updated = existingNotification.copy(
                            needsSync = false,
                            isSynced = true,
                            lastModified = System.currentTimeMillis()
                        )
                        database.notificationHistoryDao().updateNotification(updated)
                        Log.d(TAG, "✓ Offline loan notification marked as synced")
                    }
                    
                    // DO NOT show notification again - offline version was already shown
                    return@launch
                }
                
                // No existing notification - show it
                Log.d(TAG, "🔔 First loan notification for type $type - showing")
                showNotification(title, body, type)
                
                // Save to Room database
                val notification = NotificationHistoryEntity(
                    id = java.util.UUID.randomUUID().toString(),
                    title = title,
                    message = body,
                    timestamp = timestamp,
                    status = "Terkirim",
                    userId = userId,
                    serverId = null,
                    relatedReminderId = loanId,
                    serverReminderId = loanId?.toIntOrNull(),
                    isLocallyCreated = false, // Came from FCM
                    isSynced = true,
                    needsSync = false,
                    lastModified = System.currentTimeMillis()
                )
                
                database.notificationHistoryDao().insertNotification(notification)
                Log.d(TAG, "✓ Loan FCM notification saved to Room: $title")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error handling loan notification", e)
                // Fallback: show notification anyway
                showNotification(title, body, type)
            }
        }
    }
    
    /**
     * Handle reminder notification with deduplication check.
     * If offline notification already shown for this reminder, skip showing again.
     */
    private fun handleReminderNotification(
        title: String, 
        body: String, 
        reminderId: String,
        reminderTimestamp: Long?
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = AppDatabase.getDatabase(applicationContext)
                val tokenManager = TokenManager(applicationContext)
                val userId = tokenManager.getUserId()
                
                if (userId == -1) {
                    Log.d(TAG, "No user logged in, showing notification without dedup check")
                    showNotification(title, body, "custom_reminder")
                    return@launch
                }
                
                // Calculate start and end of today for deduplication
                val timestamp = reminderTimestamp ?: System.currentTimeMillis()
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = timestamp
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startOfDay = calendar.timeInMillis
                
                calendar.add(Calendar.DAY_OF_MONTH, 1)
                val endOfDay = calendar.timeInMillis
                
                // Check if notification already exists for this reminder today
                val existingNotification = database.notificationHistoryDao()
                    .getNotificationByReminderAndDate(userId, reminderId, startOfDay, endOfDay)
                
                if (existingNotification != null) {
                    Log.d(TAG, "⏭ Notification already exists for reminder $reminderId (offline notification was shown)")
                    
                    // If existing notification was local (offline), update it to mark as synced
                    if (existingNotification.isLocallyCreated) {
                        val updated = existingNotification.copy(
                            needsSync = false,
                            isSynced = true,
                            lastModified = System.currentTimeMillis()
                        )
                        database.notificationHistoryDao().updateNotification(updated)
                        Log.d(TAG, "✓ Offline notification marked as synced")
                    }
                    
                    // DO NOT show notification again - offline version was already shown
                    return@launch
                }
                
                // No existing notification - this is the first notification for this reminder
                Log.d(TAG, "🔔 First notification for reminder $reminderId - showing")
                
                // Show the notification
                showNotification(title, body, "custom_reminder")
                
                // Save to Room database
                val serverReminderId = reminderId.toIntOrNull()
                val notification = NotificationHistoryEntity.createLocal(
                    userId = userId,
                    title = title,
                    message = body,
                    timestamp = timestamp,
                    relatedReminderId = reminderId,
                    serverReminderId = serverReminderId
                ).copy(
                    isLocallyCreated = false, // Not locally created - came from server
                    needsSync = false,
                    isSynced = true
                )
                
                database.notificationHistoryDao().insertNotification(notification)
                Log.d(TAG, "✓ FCM notification saved to Room: $title")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error handling reminder notification", e)
                // Fallback: show notification anyway
                showNotification(title, body, "custom_reminder")
            }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed FCM token: $token")
        
        // Save the token to SharedPreferences for later use
        getSharedPreferences("fcm_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("fcm_token", token)
            .apply()
    }

    private fun showNotification(title: String, body: String, type: String = "reminder") {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi pengingat pengecekan inventory"
                enableVibration(true)
                enableLights(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Create intent to open app when notification is tapped
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("notification_type", type)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Build notification
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .build()

        // Show notification with unique ID based on current time
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
        Log.d(TAG, "✓ Notification shown: $title")
    }
}
