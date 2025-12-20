package com.example.stora.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.stora.MainActivity
import com.example.stora.R
import com.example.stora.data.AppDatabase
import com.example.stora.data.NotificationHistoryEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * BroadcastReceiver for handling loan deadline alarms when the app is closed.
 * Similar to ReminderAlarmReceiver but for loan notifications.
 */
class LoanAlarmReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "LoanAlarmReceiver"
        const val CHANNEL_ID = "loan_deadline_channel"
        const val CHANNEL_NAME = "Pengingat Peminjaman"
        
        // Intent extras
        const val EXTRA_LOAN_ID = "loan_id"
        const val EXTRA_LOAN_SERVER_ID = "loan_server_id"
        const val EXTRA_BORROWER_NAME = "borrower_name"
        const val EXTRA_NOTIFICATION_TYPE = "notification_type" // warning, deadline, overdue
        const val EXTRA_SCHEDULED_TIME = "scheduled_time"
        
        // Notification types
        const val TYPE_WARNING = "warning"   // 1 hour before
        const val TYPE_DEADLINE = "deadline" // at deadline
        const val TYPE_OVERDUE = "overdue"   // 1 hour after
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "🔔 Loan alarm received!")
        
        val loanId = intent.getStringExtra(EXTRA_LOAN_ID) ?: return
        val loanServerId = intent.getIntExtra(EXTRA_LOAN_SERVER_ID, -1)
        val borrowerName = intent.getStringExtra(EXTRA_BORROWER_NAME) ?: "Peminjam"
        val notificationType = intent.getStringExtra(EXTRA_NOTIFICATION_TYPE) ?: TYPE_DEADLINE
        val scheduledTime = intent.getLongExtra(EXTRA_SCHEDULED_TIME, System.currentTimeMillis())
        
        Log.d(TAG, "Loan ID: $loanId, Type: $notificationType, Borrower: $borrowerName")
        
        // Generate notification message based on type
        val (title, message) = when (notificationType) {
            TYPE_WARNING -> "Pengingat Peminjaman" to "Peminjaman \"$borrowerName\" akan deadline dalam 1 jam"
            TYPE_DEADLINE -> "Pengingat Peminjaman" to "Peminjaman \"$borrowerName\" sudah mencapai batas waktu pengembalian"
            TYPE_OVERDUE -> "Pengingat Peminjaman" to "Peminjaman \"$borrowerName\" sudah terlambat 1 jam dari deadline"
            else -> "Pengingat Peminjaman" to "Deadline peminjaman \"$borrowerName\""
        }
        
        // Always show notification when alarm fires (offline support)
        showNotification(context, loanId, title, message, notificationType)
        
        // Save to Room database
        saveNotificationToDatabase(context, loanId, loanServerId, title, message, notificationType)
    }
    
    private fun showNotification(
        context: Context,
        loanId: String,
        title: String,
        message: String,
        notificationType: String
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Create notification channel for Android O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi pengingat deadline peminjaman"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
        
        // Create intent to open app
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "loan_detail")
            putExtra("loan_id", loanId)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            loanId.hashCode() + notificationType.hashCode(),
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Build notification
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()
        
        // Use unique ID based on loan ID and notification type
        val notificationId = (loanId.hashCode() + notificationType.hashCode()) and 0x7FFFFFFF
        notificationManager.notify(notificationId, notification)
        
        Log.d(TAG, "✓ Loan notification shown: $title - $message")
    }
    
    private fun saveNotificationToDatabase(
        context: Context,
        loanId: String,
        loanServerId: Int,
        title: String,
        message: String,
        notificationType: String
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = AppDatabase.getDatabase(context)
                val historyDao = database.notificationHistoryDao()
                
                // Get user ID from shared preferences
                val sharedPrefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                val userId = sharedPrefs.getInt("user_id", -1)
                
                if (userId == -1) {
                    Log.w(TAG, "No user ID found, skipping database save")
                    return@launch
                }
                
                val currentTime = System.currentTimeMillis()
                
                // Check for existing notification to avoid duplicates
                // Use title + message + today as deduplication key
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startOfDay = calendar.timeInMillis
                calendar.add(Calendar.DAY_OF_MONTH, 1)
                val endOfDay = calendar.timeInMillis
                
                // Check if notification already exists for this loan + type + today
                val existing = historyDao.getNotificationByTitleMessageAndDate(
                    userId, title, message, startOfDay, endOfDay
                )
                
                if (existing != null) {
                    Log.d(TAG, "Notification already exists for this loan type today, skipping")
                    return@launch
                }
                
                // Create notification history entry
                val notification = NotificationHistoryEntity(
                    id = java.util.UUID.randomUUID().toString(),
                    title = title,
                    message = message,
                    timestamp = currentTime,
                    status = "Terkirim",
                    userId = userId,
                    serverId = null,
                    relatedReminderId = loanId, // Use loan ID as reference
                    serverReminderId = if (loanServerId != -1) loanServerId else null,
                    isLocallyCreated = true,
                    isSynced = false,
                    needsSync = true,
                    lastModified = currentTime
                )
                
                historyDao.insertNotification(notification)
                Log.d(TAG, "✓ Loan notification saved to database")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error saving loan notification to database", e)
            }
        }
    }
}
