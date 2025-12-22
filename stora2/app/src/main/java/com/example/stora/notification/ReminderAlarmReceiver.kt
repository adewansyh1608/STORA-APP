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
import com.example.stora.utils.TokenManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class ReminderAlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "ReminderAlarmReceiver"
        const val CHANNEL_ID = "stora_reminders"
        const val CHANNEL_NAME = "Pengingat Inventaris"
        
        const val EXTRA_REMINDER_ID = "reminder_id"
        const val EXTRA_REMINDER_TITLE = "reminder_title"
        const val EXTRA_REMINDER_TYPE = "reminder_type"
        const val EXTRA_SERVER_REMINDER_ID = "server_reminder_id"
        const val EXTRA_SCHEDULED_TIME = "scheduled_time"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "=== ReminderAlarmReceiver triggered ===")
        
        val reminderId = intent.getStringExtra(EXTRA_REMINDER_ID)
        if (reminderId == null) {
            Log.e(TAG, "No reminder ID in intent, aborting")
            return
        }
        
        val reminderTitle = intent.getStringExtra(EXTRA_REMINDER_TITLE) ?: "Pengingat"
        val reminderType = intent.getStringExtra(EXTRA_REMINDER_TYPE) ?: "custom"
        val serverReminderId = intent.getIntExtra(EXTRA_SERVER_REMINDER_ID, -1)
        val scheduledTime = intent.getLongExtra(EXTRA_SCHEDULED_TIME, System.currentTimeMillis())
        
        Log.d(TAG, "Reminder: id=$reminderId, title=$reminderTitle, type=$reminderType")
        Log.d(TAG, "Scheduled time: $scheduledTime (${java.util.Date(scheduledTime)})")
        
        showNotification(context, reminderId, reminderTitle, reminderType)
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = AppDatabase.getDatabase(context)
                val tokenManager = TokenManager(context)
                val userId = tokenManager.getUserId()
                
                if (userId == -1) {
                    Log.d(TAG, "No user logged in, notification shown but not saved")
                    return@launch
                }
                
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = scheduledTime
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startOfDay = calendar.timeInMillis
                
                calendar.add(Calendar.DAY_OF_MONTH, 1)
                val endOfDay = calendar.timeInMillis
                
                val existingNotification = database.notificationHistoryDao()
                    .getNotificationByReminderAndDate(userId, reminderId, startOfDay, endOfDay)
                
                if (existingNotification != null) {
                    Log.d(TAG, "⏭ History already exists for reminder $reminderId, not duplicating")
                    return@launch
                }
                
                val message = "Waktu pengingat: $reminderTitle"
                
                val notification = NotificationHistoryEntity.createLocal(
                    userId = userId,
                    title = reminderTitle,
                    message = message,
                    timestamp = System.currentTimeMillis(),
                    relatedReminderId = reminderId,
                    serverReminderId = if (serverReminderId != -1) serverReminderId else null
                )
                
                database.notificationHistoryDao().insertNotification(notification)
                Log.d(TAG, "✓ Notification history saved: $reminderTitle at ${java.util.Date(notification.timestamp)}")
                
                database.reminderDao().updateLastNotified(reminderId, System.currentTimeMillis())
                
                if (reminderType == "custom") {
                    database.reminderDao().deleteReminder(reminderId)
                    Log.d(TAG, "✓ Custom reminder deleted after notification")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error saving notification history", e)
            }
        }
    }
    
    private fun showNotification(context: Context, reminderId: String, title: String, type: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi pengingat untuk pengecekan inventaris"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
                enableLights(true)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
        
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("from_notification", true)
            putExtra("reminder_id", reminderId)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            reminderId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val message = "Waktu pengingat: $title"
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .build()
        
        val notificationId = reminderId.hashCode()
        notificationManager.notify(notificationId, notification)
        
        Log.d(TAG, "✓ Notification shown: $title (id=$notificationId)")
    }
}
