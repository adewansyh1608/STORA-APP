package com.example.stora.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.example.stora.MainActivity
import com.example.stora.R
import com.example.stora.data.AppDatabase
import com.example.stora.data.ReminderEntity
import com.example.stora.repository.NotificationRepository
import com.example.stora.network.ApiConfig
import com.example.stora.utils.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class ReminderWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "ReminderWorker"
        const val CHANNEL_ID = "stora_reminders"
        const val CHANNEL_NAME = "Pengingat Inventaris"
        private const val NOTIFICATION_GROUP = "stora_reminder_group"
    }

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "ReminderWorker started - checking for due reminders")
                
                val database = AppDatabase.getDatabase(context)
                val tokenManager = TokenManager(context)
                val repository = NotificationRepository(
                    database.reminderDao(),
                    database.notificationHistoryDao(),
                    ApiConfig.provideApiService(),
                    tokenManager,
                    context
                )
                
                val userId = tokenManager.getUserId()
                if (userId == -1) {
                    Log.d(TAG, "No user logged in, skipping reminder check")
                    return@withContext Result.success()
                }
                
                val isOnline = repository.isOnline()
                Log.d(TAG, "Network status: ${if (isOnline) "ONLINE" else "OFFLINE"}")
                
                if (isOnline) {
                    try {
                        repository.performFullSync()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error syncing on background", e)
                    }
                }
                
                val dueReminders = repository.getDueReminders()
                Log.d(TAG, "Found ${dueReminders.size} due reminders in Room database")
                
                dueReminders.forEach { reminder ->
                    val shouldFireLocally = !isOnline
                    
                    if (shouldFireLocally) {
                        val scheduledTime = reminder.scheduledDatetime ?: System.currentTimeMillis()
                        val calendar = java.util.Calendar.getInstance()
                        calendar.timeInMillis = scheduledTime
                        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
                        calendar.set(java.util.Calendar.MINUTE, 0)
                        calendar.set(java.util.Calendar.SECOND, 0)
                        calendar.set(java.util.Calendar.MILLISECOND, 0)
                        val startOfDay = calendar.timeInMillis
                        calendar.add(java.util.Calendar.DAY_OF_MONTH, 1)
                        val endOfDay = calendar.timeInMillis
                        
                        val existingNotification = database.notificationHistoryDao()
                            .getNotificationByReminderAndDate(userId, reminder.id, startOfDay, endOfDay)
                        
                        if (existingNotification != null) {
                            Log.d(TAG, "⏭ Notification already exists for ${reminder.title}, skipping")
                            return@forEach
                        }
                        
                        Log.d(TAG, "🔔 Firing local notification for: ${reminder.title} (${reminder.reminderType})")
                        showNotification(reminder)
                        
                        repository.updateLastNotified(reminder.id)
                        
                        val title = reminder.title ?: "Pengingat Pengecekan Inventory"
                        val message = "Waktu pengingat: $title"
                        
                        repository.recordLocalNotification(
                            title = title,
                            message = message,
                            timestamp = scheduledTime,
                            relatedReminderId = reminder.id,
                            serverReminderId = reminder.serverId
                        )
                        
                        Log.d(TAG, "✓ LOCAL notification fired for: ${reminder.title}")
                    } else {
                        Log.d(TAG, "⏭ Skipping local notification (online, Firebase will handle)")
                    }
                }
                
                Log.d(TAG, "ReminderWorker completed successfully")
                Result.success()
            } catch (e: Exception) {
                Log.e(TAG, "Error in ReminderWorker", e)
                Result.retry()
            }
        }
    }

    private fun showNotification(reminder: ReminderEntity) {
        Log.d(TAG, "=== showNotification called ===")
        Log.d(TAG, "Reminder: id=${reminder.id}, title=${reminder.title}, type=${reminder.reminderType}")
        
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
            Log.d(TAG, "Notification channel created with IMPORTANCE_HIGH")
        }
        
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("from_notification", true)
            putExtra("reminder_id", reminder.id)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            reminder.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val title = reminder.title ?: "Pengingat Pengecekan Inventory"
        val message = when (reminder.reminderType) {
            "periodic" -> "Sudah waktunya untuk melakukan pengecekan inventory Anda!"
            "custom" -> "Waktu pengingat telah tiba!"
            else -> "Anda memiliki pengingat yang perlu diperhatikan"
        }
        
        Log.d(TAG, "Building notification: title='$title', message='$message'")
        
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
        
        val notificationId = reminder.id.hashCode()
        Log.d(TAG, "Calling notificationManager.notify with id=$notificationId")
        
        notificationManager.notify(notificationId, notification)
        Log.d(TAG, "✓ Notification posted successfully!")
    }
}

object ReminderScheduler {
    private const val TAG = "ReminderScheduler"
    private const val PERIODIC_WORK_NAME = "stora_periodic_reminder_check"
    
    fun schedulePeriodicCheck(context: Context, intervalMinutes: Long = 15) {
        Log.d(TAG, "Scheduling periodic reminder check every $intervalMinutes minute(s)")
        
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(false)
            .build()
        
        val periodicWork = PeriodicWorkRequestBuilder<ReminderWorker>(
            intervalMinutes, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .addTag(PERIODIC_WORK_NAME)
            .build()
        
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            periodicWork
        )
        
        Log.d(TAG, "✓ Periodic reminder check scheduled every $intervalMinutes minutes")
    }
    
    fun checkNow(context: Context) {
        Log.d(TAG, "Scheduling immediate reminder check")
        
        val oneTimeWork = OneTimeWorkRequestBuilder<ReminderWorker>()
            .addTag("immediate_reminder_check")
            .build()
        
        WorkManager.getInstance(context).enqueue(oneTimeWork)
    }
    
    fun scheduleCustomReminder(context: Context, reminderId: String, scheduledTime: Long) {
        val delay = scheduledTime - System.currentTimeMillis()
        
        if (delay <= 0) {
            Log.d(TAG, "Reminder time already passed, checking now")
            checkNow(context)
            return
        }
        
        Log.d(TAG, "Scheduling custom reminder $reminderId for ${delay / 1000 / 60} minutes from now")
        
        val workData = workDataOf(
            "reminder_id" to reminderId
        )
        
        val oneTimeWork = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(workData)
            .addTag("custom_reminder_$reminderId")
            .build()
        
        WorkManager.getInstance(context).enqueueUniqueWork(
            "custom_reminder_$reminderId",
            ExistingWorkPolicy.REPLACE,
            oneTimeWork
        )
        
        Log.d(TAG, "✓ Custom reminder scheduled")
    }
    
    fun cancelCustomReminder(context: Context, reminderId: String) {
        Log.d(TAG, "Cancelling custom reminder: $reminderId")
        WorkManager.getInstance(context).cancelUniqueWork("custom_reminder_$reminderId")
    }
    
    fun cancelAll(context: Context) {
        Log.d(TAG, "Cancelling all reminder work")
        WorkManager.getInstance(context).cancelAllWorkByTag(PERIODIC_WORK_NAME)
    }
}
