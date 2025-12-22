package com.example.stora.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    
    @Query("SELECT * FROM reminders WHERE userId = :userId AND isDeleted = 0 ORDER BY lastModified DESC")
    fun getAllReminders(userId: Int): Flow<List<ReminderEntity>>
    
    @Query("SELECT * FROM reminders WHERE userId = :userId AND isActive = 1 AND isDeleted = 0")
    fun getActiveReminders(userId: Int): Flow<List<ReminderEntity>>
    
    @Query("SELECT * FROM reminders WHERE userId = :userId AND reminderType = 'periodic' AND isDeleted = 0 LIMIT 1")
    suspend fun getPeriodicReminder(userId: Int): ReminderEntity?
    
    @Query("SELECT * FROM reminders WHERE userId = :userId AND reminderType = 'custom' AND isDeleted = 0 ORDER BY scheduledDatetime ASC")
    fun getCustomReminders(userId: Int): Flow<List<ReminderEntity>>
    
    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getReminderById(id: String): ReminderEntity?
    
    @Query("SELECT * FROM reminders WHERE serverId = :serverId")
    suspend fun getReminderByServerId(serverId: Int): ReminderEntity?
    
    @Query("SELECT * FROM reminders WHERE needsSync = 1 AND userId = :userId")
    suspend fun getUnsyncedReminders(userId: Int): List<ReminderEntity>
    
    @Query("SELECT COUNT(*) FROM reminders WHERE needsSync = 1 AND userId = :userId")
    suspend fun getUnsyncedCount(userId: Int): Int
    
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
    
    @Query("SELECT * FROM reminders WHERE userId = :userId AND serverId IS NOT NULL AND isDeleted = 0")
    suspend fun getSyncedRemindersWithServerId(userId: Int): List<ReminderEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminders(reminders: List<ReminderEntity>)
    
    @Update
    suspend fun updateReminder(reminder: ReminderEntity)
    
    @Query("UPDATE reminders SET needsSync = 0, isSynced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: String)
    
    @Query("UPDATE reminders SET serverId = :serverId, needsSync = 0, isSynced = 1 WHERE id = :id")
    suspend fun updateServerId(id: String, serverId: Int)
    
    @Query("UPDATE reminders SET lastNotified = :timestamp, lastModified = :timestamp WHERE id = :id")
    suspend fun updateLastNotified(id: String, timestamp: Long)
    
    @Query("UPDATE reminders SET isDeleted = 1, needsSync = 1, lastModified = :timestamp WHERE id = :id")
    suspend fun softDeleteReminder(id: String, timestamp: Long = System.currentTimeMillis())
    
    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteReminder(id: String)
    
    @Query("DELETE FROM reminders WHERE isDeleted = 1 AND isSynced = 1")
    suspend fun deleteSyncedDeletedReminders()
    
    @Query("DELETE FROM reminders WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: Int)
}
