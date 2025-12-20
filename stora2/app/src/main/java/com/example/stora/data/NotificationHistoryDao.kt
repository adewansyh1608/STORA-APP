package com.example.stora.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for notification history.
 * Provides methods for CRUD operations and sync queries.
 */
@Dao
interface NotificationHistoryDao {
    
    // ============ Query Methods ============
    
    /**
     * Get all notification history for a user, ordered by most recent first
     */
    @Query("SELECT * FROM notification_history WHERE userId = :userId ORDER BY timestamp DESC")
    fun getAllHistory(userId: Int): Flow<List<NotificationHistoryEntity>>
    
    /**
     * Get notification history by ID
     */
    @Query("SELECT * FROM notification_history WHERE id = :id")
    suspend fun getNotificationById(id: String): NotificationHistoryEntity?
    
    /**
     * Get notification history by server ID
     */
    @Query("SELECT * FROM notification_history WHERE serverId = :serverId")
    suspend fun getNotificationByServerId(serverId: Int): NotificationHistoryEntity?
    
    /**
     * Get notifications that need to be synced to server
     */
    @Query("SELECT * FROM notification_history WHERE needsSync = 1 AND userId = :userId")
    suspend fun getUnsyncedNotifications(userId: Int): List<NotificationHistoryEntity>
    
    /**
     * Get unsynced count for a user
     */
    @Query("SELECT COUNT(*) FROM notification_history WHERE needsSync = 1 AND userId = :userId")
    suspend fun getUnsyncedCount(userId: Int): Int
    
    /**
     * Get total notification count for a user
     */
    @Query("SELECT COUNT(*) FROM notification_history WHERE userId = :userId")
    suspend fun getTotalCount(userId: Int): Int
    
    /**
     * Get unread notification count
     */
    @Query("SELECT COUNT(*) FROM notification_history WHERE userId = :userId AND status != 'read'")
    suspend fun getUnreadCount(userId: Int): Int
    
    /**
     * Get recent notifications (last N)
     */
    @Query("SELECT * FROM notification_history WHERE userId = :userId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentNotifications(userId: Int, limit: Int): List<NotificationHistoryEntity>
    
    /**
     * Get synced notifications with server ID
     */
    @Query("SELECT * FROM notification_history WHERE userId = :userId AND serverId IS NOT NULL")
    suspend fun getSyncedNotificationsWithServerId(userId: Int): List<NotificationHistoryEntity>
    
    // ============ Deduplication Queries ============
    
    /**
     * Check if notification already exists for a specific reminder on a specific date
     * Used to prevent duplicate notifications when switching between offline/online
     */
    @Query("""
        SELECT * FROM notification_history 
        WHERE userId = :userId 
        AND relatedReminderId = :reminderId 
        AND timestamp >= :startOfDay 
        AND timestamp < :endOfDay 
        LIMIT 1
    """)
    suspend fun getNotificationByReminderAndDate(
        userId: Int, 
        reminderId: String, 
        startOfDay: Long, 
        endOfDay: Long
    ): NotificationHistoryEntity?
    
    /**
     * Check if notification already exists by title, message, and date
     * Used for loan notification deduplication
     */
    @Query("""
        SELECT * FROM notification_history 
        WHERE userId = :userId 
        AND title = :title 
        AND message = :message 
        AND timestamp >= :startOfDay 
        AND timestamp < :endOfDay 
        LIMIT 1
    """)
    suspend fun getNotificationByTitleMessageAndDate(
        userId: Int, 
        title: String,
        message: String,
        startOfDay: Long, 
        endOfDay: Long
    ): NotificationHistoryEntity?
    
    /**
     * Check if notification exists by server reminder ID on a specific date
     */
    @Query("""
        SELECT * FROM notification_history 
        WHERE userId = :userId 
        AND serverReminderId = :serverReminderId 
        AND timestamp >= :startOfDay 
        AND timestamp < :endOfDay 
        LIMIT 1
    """)
    suspend fun getNotificationByServerReminderAndDate(
        userId: Int, 
        serverReminderId: Int, 
        startOfDay: Long, 
        endOfDay: Long
    ): NotificationHistoryEntity?
    
    /**
     * Delete local (offline) notification for a specific reminder on a specific date
     * Used when online notification arrives and we want to replace the offline one
     */
    @Query("""
        DELETE FROM notification_history 
        WHERE userId = :userId 
        AND relatedReminderId = :reminderId 
        AND timestamp >= :startOfDay 
        AND timestamp < :endOfDay 
        AND isLocallyCreated = 1
    """)
    suspend fun deleteLocalNotificationForReminder(
        userId: Int, 
        reminderId: String, 
        startOfDay: Long, 
        endOfDay: Long
    )
    
    /**
     * Delete local (offline) notification by server reminder ID on a specific date
     * Used when syncing and server notification has ID_Reminder
     */
    @Query("""
        DELETE FROM notification_history 
        WHERE userId = :userId 
        AND serverReminderId = :serverReminderId 
        AND timestamp >= :startOfDay 
        AND timestamp < :endOfDay 
        AND isLocallyCreated = 1
    """)
    suspend fun deleteLocalNotificationByServerReminderId(
        userId: Int, 
        serverReminderId: Int, 
        startOfDay: Long, 
        endOfDay: Long
    )
    
    /**
     * Delete ALL local (offline) notifications by server reminder ID
     * Used when syncing - no timestamp restriction to ensure cleanup
     */
    @Query("""
        DELETE FROM notification_history 
        WHERE userId = :userId 
        AND serverReminderId = :serverReminderId 
        AND isLocallyCreated = 1
    """)
    suspend fun deleteAllLocalNotificationsByServerReminderId(
        userId: Int, 
        serverReminderId: Int
    )
    
    /**
     * Delete local (offline) notification by title and date
     * Used as fallback when serverReminderId doesn't match (offline created reminder got new ID after sync)
     */
    @Query("""
        DELETE FROM notification_history 
        WHERE userId = :userId 
        AND title = :title 
        AND timestamp >= :startOfDay 
        AND timestamp < :endOfDay 
        AND isLocallyCreated = 1
    """)
    suspend fun deleteLocalNotificationByTitleAndDate(
        userId: Int, 
        title: String,
        startOfDay: Long, 
        endOfDay: Long
    )
    
    // ============ Insert/Update Methods ============
    
    /**
     * Insert or replace a notification
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationHistoryEntity)
    
    /**
     * Insert multiple notifications
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotifications(notifications: List<NotificationHistoryEntity>)
    
    /**
     * Update a notification
     */
    @Update
    suspend fun updateNotification(notification: NotificationHistoryEntity)
    
    /**
     * Mark notification as read
     */
    @Query("UPDATE notification_history SET status = 'read', lastModified = :timestamp WHERE id = :id")
    suspend fun markAsRead(id: String, timestamp: Long = System.currentTimeMillis())
    
    /**
     * Mark all notifications as read for a user
     */
    @Query("UPDATE notification_history SET status = 'read', lastModified = :timestamp WHERE userId = :userId AND status != 'read'")
    suspend fun markAllAsRead(userId: Int, timestamp: Long = System.currentTimeMillis())
    
    /**
     * Mark notification as synced
     */
    @Query("UPDATE notification_history SET needsSync = 0, isSynced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: String)
    
    /**
     * Update server ID after successful sync
     */
    @Query("UPDATE notification_history SET serverId = :serverId, needsSync = 0, isSynced = 1 WHERE id = :id")
    suspend fun updateServerId(id: String, serverId: Int)
    
    // ============ Delete Methods ============
    
    /**
     * Delete a notification
     */
    @Query("DELETE FROM notification_history WHERE id = :id")
    suspend fun deleteNotification(id: String)
    
    /**
     * Delete all notifications for a user (used on logout)
     */
    @Query("DELETE FROM notification_history WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: Int)
    
    /**
     * Delete old notifications (older than N days)
     */
    @Query("DELETE FROM notification_history WHERE userId = :userId AND timestamp < :cutoffTimestamp")
    suspend fun deleteOldNotifications(userId: Int, cutoffTimestamp: Long)
}
