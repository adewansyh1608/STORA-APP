package com.example.stora.repository

import android.content.Context
import android.util.Log
import com.example.stora.data.*
import com.example.stora.network.ApiService
import com.example.stora.utils.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Repository for managing notification data (reminders and notification history)
 * with offline-first sync support.
 */
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

    // ============ User ID Helper ============
    
    private fun getUserId(): Int {
        return tokenManager.getUserId()
    }

    // ============ Network Check ============
    
    fun isOnline(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
            as? android.net.ConnectivityManager
        val activeNetwork = connectivityManager?.activeNetworkInfo
        return activeNetwork?.isConnectedOrConnecting == true
    }

    // ============ Reminder Operations ============
    
    /**
     * Get all reminders for current user
     */
    fun getAllReminders(): Flow<List<ReminderEntity>> {
        val userId = getUserId()
        return reminderDao.getAllReminders(userId)
    }
    
    /**
     * Get active reminders for current user
     */
    fun getActiveReminders(): Flow<List<ReminderEntity>> {
        val userId = getUserId()
        return reminderDao.getActiveReminders(userId)
    }
    
    /**
     * Get periodic reminder for current user
     */
    suspend fun getPeriodicReminder(): ReminderEntity? {
        return withContext(Dispatchers.IO) {
            val userId = getUserId()
            reminderDao.getPeriodicReminder(userId)
        }
    }
    
    /**
     * Get custom reminders for current user
     */
    fun getCustomReminders(): Flow<List<ReminderEntity>> {
        val userId = getUserId()
        return reminderDao.getCustomReminders(userId)
    }
    
    /**
     * Insert or update a reminder - server-first when online
     */
    suspend fun saveReminder(reminder: ReminderEntity): Result<ReminderEntity> {
        return withContext(Dispatchers.IO) {
            try {
                val userId = getUserId()
                if (userId == -1) {
                    return@withContext Result.failure(Exception("User not logged in"))
                }

                val authHeader = tokenManager.getAuthHeader()
                val reminderWithUser = reminder.copy(userId = userId)
                
                // If online, try to save to server first
                if (authHeader != null && isOnline()) {
                    Log.d(TAG, "Online mode: trying to save reminder to server first")
                    
                    try {
                        val request = reminderWithUser.toApiRequest()
                        
                        val response = if (reminder.serverId != null) {
                            // Update existing
                            apiService.updateReminder(authHeader, reminder.serverId, request)
                        } else {
                            // Create new
                            apiService.createReminder(authHeader, request)
                        }
                        
                        Log.d(TAG, "Server response code: ${response.code()}")
                        
                        if (response.isSuccessful && response.body()?.success == true) {
                            // Server accepted - save to Room with serverId
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
                            // Server rejected
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
                        // Fall through to offline save
                    }
                }
                
                // Offline mode or network error - save locally with needsSync flag
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
    
    /**
     * Delete a reminder
     */
    suspend fun deleteReminder(id: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val reminder = reminderDao.getReminderById(id)
                
                if (reminder?.serverId != null && isOnline()) {
                    // Has server ID and online - delete from server
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
                
                // Offline or no server ID - soft delete
                reminderDao.softDeleteReminder(id)
                Log.d(TAG, "Reminder soft deleted locally")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting reminder", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Update last notified timestamp for a reminder
     */
    suspend fun updateLastNotified(id: String) {
        withContext(Dispatchers.IO) {
            val timestamp = System.currentTimeMillis()
            reminderDao.updateLastNotified(id, timestamp)
        }
    }
    
    /**
     * Get due reminders that need to fire
     */
    suspend fun getDueReminders(): List<ReminderEntity> {
        return withContext(Dispatchers.IO) {
            val userId = getUserId()
            if (userId == -1) return@withContext emptyList()
            
            val currentTime = System.currentTimeMillis()
            val allDue = reminderDao.getDueReminders(userId, currentTime)
            
            // Filter periodic reminders that are actually due
            allDue.filter { reminder ->
                if (reminder.reminderType == "periodic") {
                    val months = reminder.periodicMonths ?: 3
                    // FIXED: Use lastNotified if exists, otherwise use lastModified (creation time)
                    // This prevents new reminders from immediately firing
                    val baseline = reminder.lastNotified ?: reminder.lastModified
                    val intervalMs = months * 30L * 24 * 60 * 60 * 1000 // Approximate
                    val isDue = (currentTime - baseline) >= intervalMs
                    
                    Log.d(TAG, "Periodic reminder ${reminder.title}: baseline=${baseline}, interval=${intervalMs}ms, isDue=$isDue")
                    isDue
                } else {
                    // Custom reminders - already filtered by SQL
                    true
                }
            }
        }
    }

    // ============ Notification History Operations ============
    
    /**
     * Get all notification history for current user
     */
    fun getNotificationHistory(): Flow<List<NotificationHistoryEntity>> {
        val userId = getUserId()
        return historyDao.getAllHistory(userId)
    }
    
    /**
     * Record a local notification (when notification fires offline)
     * Includes deduplication check to prevent duplicate notifications
     */
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
                
                // Check for existing notification (deduplication)
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
    
    /**
     * Mark notification as read
     */
    suspend fun markNotificationAsRead(id: String) {
        withContext(Dispatchers.IO) {
            historyDao.markAsRead(id)
        }
    }
    
    /**
     * Mark all notifications as read
     */
    suspend fun markAllNotificationsAsRead() {
        withContext(Dispatchers.IO) {
            val userId = getUserId()
            if (userId != -1) {
                historyDao.markAllAsRead(userId)
            }
        }
    }
    
    /**
     * Get unread notification count
     */
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

    // ============ Sync Operations ============
    
    /**
     * Sync reminders and notifications to server
     */
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
                
                // Sync deleted reminders first
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
                        // No server ID - just delete locally
                        reminderDao.deleteReminder(reminder.id)
                    }
                }
                
                // Sync new/updated reminders
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
                
                // Sync local notifications to server
                val unsyncedNotifications = historyDao.getUnsyncedNotifications(userId)
                unsyncedNotifications.forEach { notification ->
                    try {
                        // Create notification on server with full timestamp and ID_Reminder
                        val requestBody = mutableMapOf<String, Any>(
                            "Judul" to notification.title,
                            "Pesan" to notification.message,
                            "timestamp" to notification.timestamp.toString(),
                            "Status" to notification.status
                        )
                        
                        // Add ID_Reminder if available
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
                            // Check if it was a duplicate
                            val isDuplicate = response.body()?.let { body ->
                                // Check if response contains isDuplicate flag
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
                
                // Delete synced deleted reminders
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
    
    /**
     * Sync reminders and notifications from server
     */
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
                
                // Sync reminders from server
                try {
                    val remindersResponse = apiService.getReminders(authHeader)
                    if (remindersResponse.isSuccessful && remindersResponse.body()?.success == true) {
                        val serverReminders = remindersResponse.body()?.data ?: emptyList()
                        val serverReminderIds = serverReminders.mapNotNull { it.idReminder }.toSet()
                        
                        // Get local synced reminders
                        val localSyncedReminders = reminderDao.getSyncedRemindersWithServerId(userId)
                        
                        // Delete local reminders that no longer exist on server
                        localSyncedReminders.filter { it.serverId !in serverReminderIds }.forEach { local ->
                            if (!local.needsSync) {
                                reminderDao.deleteReminder(local.id)
                            }
                        }
                        
                        // Add/update reminders from server
                        serverReminders.forEach { serverReminder ->
                            val existingLocal = reminderDao.getReminderByServerId(serverReminder.idReminder)
                            
                            // Skip if local has pending changes
                            if (existingLocal != null && existingLocal.needsSync) {
                                Log.d(TAG, "⏭ Skipping server update for reminder - local changes pending")
                                return@forEach
                            }
                            
                            // Use new fromApiModel that preserves local data when server returns null
                            val reminderEntity = ReminderEntity.fromApiModel(
                                serverReminder,
                                existingLocal,  // Pass existing local entity to preserve datetime
                                userId
                            )
                            reminderDao.insertReminder(reminderEntity)
                            Log.d(TAG, "✓ Synced reminder from server: ${reminderEntity.title}, scheduledDatetime=${reminderEntity.scheduledDatetime}")
                            syncedCount++
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error syncing reminders from server", e)
                }
                
                // Sync notification history from server
                try {
                    val historyResponse = apiService.getNotificationHistory(authHeader)
                    if (historyResponse.isSuccessful && historyResponse.body()?.success == true) {
                        val serverHistory = historyResponse.body()?.data ?: emptyList()
                        
                        // First pass: collect all server notification reminder IDs
                        val serverReminderIds = serverHistory.mapNotNull { it.idReminder }.toSet()
                        Log.d(TAG, "Server has notifications for reminder IDs: $serverReminderIds")
                        
                        // Delete all local (offline) notifications that have matching serverReminderId
                        serverReminderIds.forEach { reminderId ->
                            historyDao.deleteAllLocalNotificationsByServerReminderId(userId, reminderId)
                            Log.d(TAG, "🗑 Deleted all local notifications for serverReminderId=$reminderId")
                        }
                        
                        // Second pass: insert server notifications and delete duplicates by title+timestamp
                        serverHistory.forEach { serverNotification ->
                            val existingByServerId = historyDao.getNotificationByServerId(serverNotification.idNotifikasi)
                            
                            if (existingByServerId == null) {
                                // IMPORTANT: Also delete by title and timestamp to handle mismatched reminder IDs
                                // This happens when reminder is created offline and gets new ID after sync
                                val title = serverNotification.judul
                                if (!title.isNullOrEmpty()) {
                                    // Calculate today's timestamp range
                                    val calendar = java.util.Calendar.getInstance()
                                    calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
                                    calendar.set(java.util.Calendar.MINUTE, 0)
                                    calendar.set(java.util.Calendar.SECOND, 0)
                                    calendar.set(java.util.Calendar.MILLISECOND, 0)
                                    val startOfDay = calendar.timeInMillis
                                    calendar.add(java.util.Calendar.DAY_OF_MONTH, 1)
                                    val endOfDay = calendar.timeInMillis
                                    
                                    // Delete local notification with same title on same day
                                    historyDao.deleteLocalNotificationByTitleAndDate(
                                        userId,
                                        title,
                                        startOfDay,
                                        endOfDay
                                    )
                                    Log.d(TAG, "🗑 Deleted local notification by title='$title'")
                                }
                                
                                // Insert server version
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
    
    /**
     * Perform full sync (to server first, then from server)
     */
    suspend fun performFullSync(): Result<Pair<Int, Int>> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Starting full notification sync...")
                
                // First, sync local changes to server
                val toServerResult = syncToServer()
                val toServerCount = toServerResult.getOrDefault(0)
                
                // Then, sync server data to local
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
    
    /**
     * Get unsynced count for reminders
     */
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
