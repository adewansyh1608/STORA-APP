package com.example.stora.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationHistoryDao {
    
    @Query("SELECT * FROM notification_history WHERE userId = :userId ORDER BY timestamp DESC")
    fun getAllHistory(userId: Int): Flow<List<NotificationHistoryEntity>>
    
    @Query("SELECT * FROM notification_history WHERE id = :id")
    suspend fun getNotificationById(id: String): NotificationHistoryEntity?
    
    @Query("SELECT * FROM notification_history WHERE serverId = :serverId")
    suspend fun getNotificationByServerId(serverId: Int): NotificationHistoryEntity?
    
    @Query("SELECT * FROM notification_history WHERE needsSync = 1 AND userId = :userId")
    suspend fun getUnsyncedNotifications(userId: Int): List<NotificationHistoryEntity>
    
    @Query("SELECT COUNT(*) FROM notification_history WHERE needsSync = 1 AND userId = :userId")
    suspend fun getUnsyncedCount(userId: Int): Int
    
    @Query("SELECT COUNT(*) FROM notification_history WHERE userId = :userId")
    suspend fun getTotalCount(userId: Int): Int
    
    @Query("SELECT COUNT(*) FROM notification_history WHERE userId = :userId AND status != 'read'")
    suspend fun getUnreadCount(userId: Int): Int
    
    @Query("SELECT * FROM notification_history WHERE userId = :userId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentNotifications(userId: Int, limit: Int): List<NotificationHistoryEntity>
    
    @Query("SELECT * FROM notification_history WHERE userId = :userId AND serverId IS NOT NULL")
    suspend fun getSyncedNotificationsWithServerId(userId: Int): List<NotificationHistoryEntity>
    
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
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationHistoryEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotifications(notifications: List<NotificationHistoryEntity>)
    
    @Update
    suspend fun updateNotification(notification: NotificationHistoryEntity)
    
    @Query("UPDATE notification_history SET status = 'read', lastModified = :timestamp WHERE id = :id")
    suspend fun markAsRead(id: String, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE notification_history SET status = 'read', lastModified = :timestamp WHERE userId = :userId AND status != 'read'")
    suspend fun markAllAsRead(userId: Int, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE notification_history SET needsSync = 0, isSynced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: String)
    
    @Query("UPDATE notification_history SET serverId = :serverId, needsSync = 0, isSynced = 1 WHERE id = :id")
    suspend fun updateServerId(id: String, serverId: Int)
    
    @Query("DELETE FROM notification_history WHERE id = :id")
    suspend fun deleteNotification(id: String)
    
    @Query("DELETE FROM notification_history WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: Int)
    
    @Query("DELETE FROM notification_history WHERE userId = :userId AND timestamp < :cutoffTimestamp")
    suspend fun deleteOldNotifications(userId: Int, cutoffTimestamp: Long)
}
