package com.example.stora.repository

import android.content.Context
import android.util.Log
import com.example.stora.data.*
import com.example.stora.network.ApiService
import com.example.stora.utils.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class NotificationRepository(
    private val reminderDao: ReminderDao,
    private val historyDao: NotificationHistoryDao,
    private val apiService: ApiService,
    private val tokenManager: TokenManager,
    private val context: Context
) {
    companion object {
        private const val TAG = "NotificationRepository"
    }

    
    private fun getUserId(): Int {
        return tokenManager.getUserId()
    }

    
    fun isOnline(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
            as? android.net.ConnectivityManager
        val activeNetwork = connectivityManager?.activeNetworkInfo
        return activeNetwork?.isConnectedOrConnecting == true
    }

    fun getAllReminders(): Flow<List<ReminderEntity>> {
        val userId = getUserId()
        return reminderDao.getAllReminders(userId)
    }
    
    fun getActiveReminders(): Flow<List<ReminderEntity>> {
        val userId = getUserId()
        return reminderDao.getActiveReminders(userId)
    }
    
    suspend fun getPeriodicReminder(): ReminderEntity? {
        return withContext(Dispatchers.IO) {
            val userId = getUserId()
            reminderDao.getPeriodicReminder(userId)
        }
    }
    
    fun getCustomReminders(): Flow<List<ReminderEntity>> {
        val userId = getUserId()
        return reminderDao.getCustomReminders(userId)
    }
    
    suspend fun saveReminder(reminder: ReminderEntity): Result<ReminderEntity> {
        return withContext(Dispatchers.IO) {
            try {
                val userId = getUserId()
                if (userId == -1) {
                    return@withContext Result.failure(Exception("User not logged in"))
                }

                val authHeader = tokenManager.getAuthHeader()
                val reminderWithUser = reminder.copy(userId = userId)
                

                if (authHeader != null && isOnline()) {
                    Log.d(TAG, "Online mode: trying to save reminder to server first")
                    
                    try {
                        val request = reminderWithUser.toApiRequest()
                        
                        val response = if (reminder.serverId != null) {
                            apiService.updateReminder(authHeader, reminder.serverId, request)
                        } else {
                            apiService.createReminder(authHeader, request)
                        }
                        
                        Log.d(TAG, "Server response code: ${response.code()}")
                        
                        if (response.isSuccessful && response.body()?.success == true) {
                            val serverData = response.body()?.data
                            val itemToSave = reminderWithUser.copy(
                                serverId = serverData?.idReminder ?: reminder.serverId,
                                needsSync = false,
                                isSynced = true,
                                lastModified = System.currentTimeMillis()
                            )
                            reminderDao.insertReminder(itemToSave)
                            Log.d(TAG, "✓ Reminder saved to server and locally: ${reminder.title}")
                            return@withContext Result.success(itemToSave)
                        } else {
                            val errorBody = response.errorBody()?.string()
                            Log.e(TAG, "Server rejected: $errorBody")
                            
                            val errorMessage = try {
                                val jsonError = org.json.JSONObject(errorBody ?: "{}")
                                jsonError.optString("message", "Gagal menyimpan reminder ke server")
                            } catch (e: Exception) {
                                "Gagal menyimpan reminder ke server (${response.code()})"
                            }
                            
                            return@withContext Result.failure(Exception(errorMessage))
                        }
                    } catch (networkError: Exception) {
                        Log.w(TAG, "Network error, will save locally for later sync: ${networkError.message}")
                    }
                }
                
                Log.d(TAG, "Saving reminder locally for later sync")
                val itemWithSyncFlag = reminderWithUser.copy(
                    needsSync = true,
                    isSynced = false,
                    lastModified = System.currentTimeMillis()
                )
                reminderDao.insertReminder(itemWithSyncFlag)
                Log.d(TAG, "Reminder saved locally (will sync later): ${reminder.title}")
                Result.success(itemWithSyncFlag)
            } catch (e: Exception) {
                Log.e(TAG, "Error saving reminder", e)
                Result.failure(e)
            }
        }
    }
    
    suspend fun deleteReminder(id: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val reminder = reminderDao.getReminderById(id)
                
                if (reminder?.serverId != null && isOnline()) {
                    val authHeader = tokenManager.getAuthHeader()
                    if (authHeader != null) {
                        try {
                            val response = apiService.deleteReminder(authHeader, reminder.serverId)
                            if (response.isSuccessful) {
                                reminderDao.deleteReminder(id)
                                Log.d(TAG, "✓ Reminder deleted from server and locally")
                                return@withContext Result.success(Unit)
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Network error deleting from server, soft delete locally")
                        }
                    }
                }
                
                reminderDao.softDeleteReminder(id)
                Log.d(TAG, "Reminder soft deleted locally")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting reminder", e)
                Result.failure(e)
            }
        }
    }
    
    suspend fun updateLastNotified(id: String) {
        withContext(Dispatchers.IO) {
            val timestamp = System.currentTimeMillis()
            reminderDao.updateLastNotified(id, timestamp)
        }
    }
    
    suspend fun getDueReminders(): List<ReminderEntity> {
        return withContext(Dispatchers.IO) {
            val userId = getUserId()
            if (userId == -1) return@withContext emptyList()
            
            val currentTime = System.currentTimeMillis()
            val allDue = reminderDao.getDueReminders(userId, currentTime)
            
            allDue.filter { reminder ->
                if (reminder.reminderType == "periodic") {
                    val months = reminder.periodicMonths ?: 3
                    val baseline = reminder.lastNotified ?: reminder.lastModified
                    val intervalMs = months * 30L * 24 * 60 * 60 * 1000
                    val isDue = (currentTime - baseline) >= intervalMs
                    
                    Log.d(TAG, "Periodic reminder ${reminder.title}: baseline=${baseline}, interval=${intervalMs}ms, isDue=$isDue")
                    isDue
                } else {
                    true
                }
            }
        }
    }

    fun getNotificationHistory(): Flow<List<NotificationHistoryEntity>> {
        val userId = getUserId()
        return historyDao.getAllHistory(userId)
    }
    
    suspend fun recordLocalNotification(
        title: String,
        message: String,
        timestamp: Long = System.currentTimeMillis(),
        relatedLoanId: Int? = null,
        relatedReminderId: String? = null,
        serverReminderId: Int? = null
    ): Result<NotificationHistoryEntity> {
        return withContext(Dispatchers.IO) {
            try {
                val userId = getUserId()
                if (userId == -1) {
                    return@withContext Result.failure(Exception("User not logged in"))
                }
                

                if (relatedReminderId != null) {
                    val calendar = java.util.Calendar.getInstance()
                    calendar.timeInMillis = timestamp
                    calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
                    calendar.set(java.util.Calendar.MINUTE, 0)
                    calendar.set(java.util.Calendar.SECOND, 0)
                    calendar.set(java.util.Calendar.MILLISECOND, 0)
                    val startOfDay = calendar.timeInMillis
                    
                    calendar.add(java.util.Calendar.DAY_OF_MONTH, 1)
                    val endOfDay = calendar.timeInMillis
                    
                    val existing = historyDao.getNotificationByReminderAndDate(
                        userId, relatedReminderId, startOfDay, endOfDay
                    )
                    
                    if (existing != null) {
                        Log.d(TAG, "⏭ Notification already exists for reminder $relatedReminderId, skipping")
                        return@withContext Result.success(existing)
                    }
                }
                
                val notification = NotificationHistoryEntity.createLocal(
                    userId = userId,
                    title = title,
                    message = message,
                    timestamp = timestamp,
                    relatedLoanId = relatedLoanId,
                    relatedReminderId = relatedReminderId,
                    serverReminderId = serverReminderId
                )
                
                historyDao.insertNotification(notification)
                Log.d(TAG, "✓ Local notification recorded: $title at ${java.util.Date(timestamp)}")
                Result.success(notification)
            } catch (e: Exception) {
                Log.e(TAG, "Error recording notification", e)
                Result.failure(e)
            }
        }
    }
    
    suspend fun markNotificationAsRead(id: String) {
        withContext(Dispatchers.IO) {
            historyDao.markAsRead(id)
        }
    }
    
    suspend fun markAllNotificationsAsRead() {
        withContext(Dispatchers.IO) {
            val userId = getUserId()
            if (userId != -1) {
                historyDao.markAllAsRead(userId)
            }
        }
    }
    
    suspend fun getUnreadCount(): Int {
        return withContext(Dispatchers.IO) {
            val userId = getUserId()
            if (userId != -1) {
                historyDao.getUnreadCount(userId)
            } else {
                0
            }
        }
    }

    suspend fun syncToServer(): Result<Int> {
        return withContext(Dispatchers.IO) {
            try {
                val authHeader = tokenManager.getAuthHeader()
                val userId = getUserId()
                
                if (authHeader == null || userId == -1) {
                    return@withContext Result.failure(Exception("Authentication required"))
                }
                
                if (!isOnline()) {
                    return@withContext Result.failure(Exception("No network connection"))
                }
                
                var syncedCount = 0
                var errorCount = 0
                
                val deletedReminders = reminderDao.getUnsyncedReminders(userId).filter { it.isDeleted }
                deletedReminders.forEach { reminder ->
                    if (reminder.serverId != null) {
                        try {
                            val response = apiService.deleteReminder(authHeader, reminder.serverId)
                            if (response.isSuccessful) {
                                reminderDao.deleteReminder(reminder.id)
                                syncedCount++
                            } else {
                                errorCount++
                            }
                        } catch (e: Exception) {
                            errorCount++
                        }
                    } else {
                        reminderDao.deleteReminder(reminder.id)
                    }
                }
                
                val unsyncedReminders = reminderDao.getUnsyncedReminders(userId).filter { !it.isDeleted }
                unsyncedReminders.forEach { reminder ->
                    try {
                        val request = reminder.toApiRequest()
                        
                        val response = if (reminder.serverId != null) {
                            apiService.updateReminder(authHeader, reminder.serverId, request)
                        } else {
                            apiService.createReminder(authHeader, request)
                        }
                        
                        if (response.isSuccessful && response.body()?.success == true) {
                            val serverData = response.body()?.data
                            if (serverData != null) {
                                reminderDao.updateServerId(reminder.id, serverData.idReminder)
                            } else {
                                reminderDao.markAsSynced(reminder.id)
                            }
                            syncedCount++
                        } else {
                            errorCount++
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error syncing reminder ${reminder.title}", e)
                        errorCount++
                    }
                }
                
                val unsyncedNotifications = historyDao.getUnsyncedNotifications(userId)
                unsyncedNotifications.forEach { notification ->
                    try {
                        val requestBody = mutableMapOf<String, Any>(
                            "Judul" to notification.title,
                            "Pesan" to notification.message,
                            "timestamp" to notification.timestamp.toString(),
                            "Status" to notification.status
                        )
                        
                        val reminderId = notification.serverReminderId 
                            ?: notification.relatedReminderId?.toIntOrNull()
                        if (reminderId != null) {
                            requestBody["ID_Reminder"] = reminderId
                        }
                        
                        val response = apiService.createNotificationHistory(
                            authHeader,
                            requestBody
                        )
                        
                        if (response.isSuccessful && response.body()?.success == true) {
                            val serverData = response.body()?.data
                            val isDuplicate = response.body()?.let { body ->
                                try {
                                    val json = org.json.JSONObject(response.raw().toString())
                                    json.optBoolean("isDuplicate", false)
                                } catch (e: Exception) { false }
                            } ?: false
                            
                            if (serverData != null && !isDuplicate) {
                                historyDao.updateServerId(notification.id, serverData.idNotifikasi)
                            } else {
                                historyDao.markAsSynced(notification.id)
                            }
                            syncedCount++
                        } else {
                            errorCount++
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error syncing notification ${notification.title}", e)
                        errorCount++
                    }
                }
                

                reminderDao.deleteSyncedDeletedReminders()
                
                Log.d(TAG, "Sync to server completed: $syncedCount items synced, $errorCount errors")
                
                if (errorCount > 0) {
                    Result.failure(Exception("Synced $syncedCount items, but $errorCount failed"))
                } else {
                    Result.success(syncedCount)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing to server", e)
                Result.failure(e)
            }
        }
    }
    
    suspend fun syncFromServer(): Result<Int> {
        return withContext(Dispatchers.IO) {
            try {
                val authHeader = tokenManager.getAuthHeader()
                val userId = getUserId()
                
                if (authHeader == null || userId == -1) {
                    return@withContext Result.failure(Exception("Authentication required"))
                }
                
                if (!isOnline()) {
                    return@withContext Result.failure(Exception("No network connection"))
                }
                
                var syncedCount = 0
                
                try {
                    val remindersResponse = apiService.getReminders(authHeader)
                    if (remindersResponse.isSuccessful && remindersResponse.body()?.success == true) {
                        val serverReminders = remindersResponse.body()?.data ?: emptyList()
                        val serverReminderIds = serverReminders.mapNotNull { it.idReminder }.toSet()
                        
                        val localSyncedReminders = reminderDao.getSyncedRemindersWithServerId(userId)
                        
                        localSyncedReminders.filter { it.serverId !in serverReminderIds }.forEach { local ->
                            if (!local.needsSync) {
                                reminderDao.deleteReminder(local.id)
                            }
                        }
                        
                        serverReminders.forEach { serverReminder ->
                            val existingLocal = reminderDao.getReminderByServerId(serverReminder.idReminder)
                            
                            if (existingLocal != null && existingLocal.needsSync) {
                                Log.d(TAG, "⏭ Skipping server update for reminder - local changes pending")
                                return@forEach
                            }
                            
                            val reminderEntity = ReminderEntity.fromApiModel(
                                serverReminder,
                                existingLocal,
                                userId
                            )
                            reminderDao.insertReminder(reminderEntity)
                            Log.d(TAG, "✓ Synced reminder from server: ${reminderEntity.title}, scheduledDatetime=${reminderEntity.scheduledDatetime}")
                            
                            if (reminderEntity.reminderType == "custom" && 
                                reminderEntity.isActive && 
                                !reminderEntity.isDeleted) {
                                val scheduledTime = reminderEntity.scheduledDatetime
                                if (scheduledTime != null && scheduledTime > System.currentTimeMillis()) {
                                    com.example.stora.notification.ReminderAlarmManager.scheduleExactAlarm(
                                        context = context,
                                        reminderId = reminderEntity.id,
                                        reminderTitle = reminderEntity.title ?: "Pengingat",
                                        reminderType = "custom",
                                        serverReminderId = reminderEntity.serverId,
                                        scheduledTimeMillis = scheduledTime
                                    )
                                    
                                    com.example.stora.notification.ReminderScheduler.scheduleCustomReminder(
                                        context,
                                        reminderEntity.id,
                                        scheduledTime
                                    )
                                    
                                    Log.d(TAG, "📅 Scheduled alarms for synced reminder: ${reminderEntity.title}")
                                }
                            }
                            
                            syncedCount++
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error syncing reminders from server", e)
                }
                
                try {
                    val historyResponse = apiService.getNotificationHistory(authHeader)
                    if (historyResponse.isSuccessful && historyResponse.body()?.success == true) {
                        val serverHistory = historyResponse.body()?.data ?: emptyList()
                        
                        val serverReminderIds = serverHistory.mapNotNull { it.idReminder }.toSet()
                        Log.d(TAG, "Server has notifications for reminder IDs: $serverReminderIds")
                        
                        serverReminderIds.forEach { reminderId ->
                            historyDao.deleteAllLocalNotificationsByServerReminderId(userId, reminderId)
                            Log.d(TAG, "🗑 Deleted all local notifications for serverReminderId=$reminderId")
                        }
                        
                        serverHistory.forEach { serverNotification ->
                            val existingByServerId = historyDao.getNotificationByServerId(serverNotification.idNotifikasi)
                            
                            if (existingByServerId == null) {
                                val serverTimestamp = serverNotification.tanggal?.let {
                                    try {
                                        val isoFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
                                        isoFormat.timeZone = java.util.TimeZone.getTimeZone("UTC")
                                        isoFormat.parse(it.replace(".000Z", ""))?.time
                                    } catch (e: Exception) {
                                        null
                                    }
                                } ?: System.currentTimeMillis()
                                
                                val calendar = java.util.Calendar.getInstance()
                                calendar.timeInMillis = serverTimestamp
                                calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
                                calendar.set(java.util.Calendar.MINUTE, 0)
                                calendar.set(java.util.Calendar.SECOND, 0)
                                calendar.set(java.util.Calendar.MILLISECOND, 0)
                                val startOfDay = calendar.timeInMillis
                                calendar.add(java.util.Calendar.DAY_OF_MONTH, 1)
                                val endOfDay = calendar.timeInMillis
                                
                                val title = serverNotification.judul
                                val message = serverNotification.pesan
                                
                                if (!title.isNullOrEmpty() && !message.isNullOrEmpty()) {
                                    val existingByTitleMessage = historyDao.getNotificationByTitleMessageAndDate(
                                        userId, title, message, startOfDay, endOfDay
                                    )
                                    if (existingByTitleMessage != null && existingByTitleMessage.isLocallyCreated) {
                                        historyDao.deleteNotification(existingByTitleMessage.id)
                                        Log.d(TAG, "🗑 Deleted local notification by title+message: '$title'")
                                    }
                                }
                                
                                if (!title.isNullOrEmpty()) {
                                    historyDao.deleteLocalNotificationByTitleAndDate(
                                        userId,
                                        title,
                                        startOfDay,
                                        endOfDay
                                    )
                                    Log.d(TAG, "🗑 Deleted local notification by title='$title' for date=$startOfDay")
                                }
                                
                                val historyEntity = NotificationHistoryEntity.fromApiModel(
                                    serverNotification,
                                    null,
                                    userId
                                )
                                historyDao.insertNotification(historyEntity)
                                syncedCount++
                                Log.d(TAG, "✓ Synced notification from server: ${serverNotification.judul}")
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error syncing notification history from server", e)
                }
                
                Log.d(TAG, "✓ Sync from server completed: $syncedCount items synced")
                Result.success(syncedCount)
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing from server", e)
                Result.failure(e)
            }
        }
    }
    
    suspend fun performFullSync(): Result<Pair<Int, Int>> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Starting full notification sync...")
                
                val toServerResult = syncToServer()
                val toServerCount = toServerResult.getOrDefault(0)
                
                val fromServerResult = syncFromServer()
                val fromServerCount = fromServerResult.getOrDefault(0)
                
                Log.d(TAG, "Full sync completed: to server=$toServerCount, from server=$fromServerCount")
                Result.success(Pair(toServerCount, fromServerCount))
            } catch (e: Exception) {
                Log.e(TAG, "Error during full sync", e)
                Result.failure(e)
            }
        }
    }
    
    suspend fun getUnsyncedReminderCount(): Int {
        return withContext(Dispatchers.IO) {
            val userId = getUserId()
            if (userId != -1) {
                reminderDao.getUnsyncedCount(userId)
            } else {
                0
            }
        }
    }
}
