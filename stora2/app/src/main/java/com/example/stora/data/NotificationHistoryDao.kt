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
