package com.example.stora.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for reminder settings.
 * Provides methods for CRUD operations and sync queries.
 */
@Dao
interface ReminderDao {
    
    // ============ Query Methods ============
    
    /**
     * Get all active reminders for a user (non-deleted)
     */
    @Query("SELECT * FROM reminders WHERE userId = :userId AND isDeleted = 0 ORDER BY lastModified DESC")
    fun getAllReminders(userId: Int): Flow<List<ReminderEntity>>
    
    /**
     * Get only active reminders for a user (isActive = true, non-deleted)
     */
    @Query("SELECT * FROM reminders WHERE userId = :userId AND isActive = 1 AND isDeleted = 0")
    fun getActiveReminders(userId: Int): Flow<List<ReminderEntity>>
    
    /**
     * Get periodic reminder for a user (there should only be one)
     */
    @Query("SELECT * FROM reminders WHERE userId = :userId AND reminderType = 'periodic' AND isDeleted = 0 LIMIT 1")
    suspend fun getPeriodicReminder(userId: Int): ReminderEntity?
    
    /**
     * Get all custom reminders for a user
     */
    @Query("SELECT * FROM reminders WHERE userId = :userId AND reminderType = 'custom' AND isDeleted = 0 ORDER BY scheduledDatetime ASC")
    fun getCustomReminders(userId: Int): Flow<List<ReminderEntity>>
    
    /**
     * Get reminder by ID
     */
    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getReminderById(id: String): ReminderEntity?
    
    /**
     * Get reminder by server ID
     */
    @Query("SELECT * FROM reminders WHERE serverId = :serverId")
    suspend fun getReminderByServerId(serverId: Int): ReminderEntity?
    
    /**
     * Get reminders that need to be synced to server
     */
    @Query("SELECT * FROM reminders WHERE needsSync = 1 AND userId = :userId")
    suspend fun getUnsyncedReminders(userId: Int): List<ReminderEntity>
    
    /**
     * Get unsynced count for a user
     */
    @Query("SELECT COUNT(*) FROM reminders WHERE needsSync = 1 AND userId = :userId")
    suspend fun getUnsyncedCount(userId: Int): Int
    
    /**
     * Get reminders that are due to fire (for periodic: check lastNotified + months)
     * For custom: scheduledDatetime <= current time and not yet notified
     */
    @Query("""
        SELECT * FROM reminders 
        WHERE userId = :userId 
        AND isActive = 1 
        AND isDeleted = 0
        AND (
            (reminderType = 'custom' AND scheduledDatetime <= :currentTime AND (lastNotified IS NULL OR lastNotified < scheduledDatetime))
            OR
            (reminderType = 'periodic')
        )
    """)
    suspend fun getDueReminders(userId: Int, currentTime: Long): List<ReminderEntity>
    
    /**
     * Get synced reminders with server ID (for deletion detection)
     */
    @Query("SELECT * FROM reminders WHERE userId = :userId AND serverId IS NOT NULL AND isDeleted = 0")
    suspend fun getSyncedRemindersWithServerId(userId: Int): List<ReminderEntity>
    
    // ============ Insert/Update Methods ============
    
    /**
     * Insert or replace a reminder
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderEntity)
    
    /**
     * Insert multiple reminders
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminders(reminders: List<ReminderEntity>)
    
    /**
     * Update a reminder
     */
    @Update
    suspend fun updateReminder(reminder: ReminderEntity)
    
    /**
     * Mark reminder as synced
     */
    @Query("UPDATE reminders SET needsSync = 0, isSynced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: String)
    
    /**
     * Update server ID after successful sync
     */
    @Query("UPDATE reminders SET serverId = :serverId, needsSync = 0, isSynced = 1 WHERE id = :id")
    suspend fun updateServerId(id: String, serverId: Int)
    
    /**
     * Update last notified timestamp
     */
    @Query("UPDATE reminders SET lastNotified = :timestamp, lastModified = :timestamp WHERE id = :id")
    suspend fun updateLastNotified(id: String, timestamp: Long)
    
    // ============ Delete Methods ============
    
    /**
     * Soft delete a reminder (mark as deleted for sync)
     */
    @Query("UPDATE reminders SET isDeleted = 1, needsSync = 1, lastModified = :timestamp WHERE id = :id")
    suspend fun softDeleteReminder(id: String, timestamp: Long = System.currentTimeMillis())
    
    /**
     * Hard delete a reminder
     */
    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteReminder(id: String)
    
    /**
     * Delete reminders that are synced and marked as deleted
     */
    @Query("DELETE FROM reminders WHERE isDeleted = 1 AND isSynced = 1")
    suspend fun deleteSyncedDeletedReminders()
    
    /**
     * Delete all reminders for a user (used on logout)
     */
    @Query("DELETE FROM reminders WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: Int)
}
