package com.example.stora.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Room entity to store notification history locally for offline access.
 * Records all notifications sent to the user (both from server and local).
 */
@Entity(tableName = "notification_history")
data class NotificationHistoryEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    
    /** Server ID (null if created locally/offline and not yet synced) */
    val serverId: Int? = null,
    
    /** User ID who received this notification */
    val userId: Int,
    
    /** Notification title */
    val title: String,
    
    /** Notification message/body */
    val message: String,
    
    /** When the notification was sent (timestamp in milliseconds) */
    val timestamp: Long = System.currentTimeMillis(),
    
    /** Status: "sent", "read", "pending" */
    val status: String = "sent",
    
    /** Related loan ID if this is a loan reminder notification */
    val relatedLoanId: Int? = null,
    
    /** Related reminder ID if this was triggered by a reminder */
    val relatedReminderId: String? = null,
    
    /** Whether this notification was created locally (offline) */
    val isLocallyCreated: Boolean = false,
    
    /** Whether this notification has local changes that need to sync to server */
    val needsSync: Boolean = false,
    
    /** Whether this notification is synced with server */
    val isSynced: Boolean = true,
    
    /** Last modification timestamp */
    val lastModified: Long = System.currentTimeMillis()
) {
    companion object {
        /**
         * Create from API response
         */
        fun fromApiModel(apiModel: NotificationHistoryApiModel, localId: String? = null, userId: Int): NotificationHistoryEntity {
            val timestamp = apiModel.tanggal?.let {
                try {
                    java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).parse(it)?.time
                        ?: System.currentTimeMillis()
                } catch (e: Exception) {
                    System.currentTimeMillis()
                }
            } ?: System.currentTimeMillis()
            
            return NotificationHistoryEntity(
                id = localId ?: UUID.randomUUID().toString(),
                serverId = apiModel.idNotifikasi,
                userId = userId,
                title = apiModel.judul ?: "Notifikasi",
                message = apiModel.pesan ?: "",
                timestamp = timestamp,
                status = apiModel.status ?: "sent",
                relatedLoanId = apiModel.idPeminjaman,
                isLocallyCreated = false,
                needsSync = false,
                isSynced = true,
                lastModified = System.currentTimeMillis()
            )
        }
        
        /**
         * Create a local notification record (for offline notifications)
         */
        fun createLocal(
            userId: Int,
            title: String,
            message: String,
            relatedLoanId: Int? = null,
            relatedReminderId: String? = null
        ): NotificationHistoryEntity {
            return NotificationHistoryEntity(
                id = UUID.randomUUID().toString(),
                serverId = null,
                userId = userId,
                title = title,
                message = message,
                timestamp = System.currentTimeMillis(),
                status = "sent",
                relatedLoanId = relatedLoanId,
                relatedReminderId = relatedReminderId,
                isLocallyCreated = true,
                needsSync = true,
                isSynced = false,
                lastModified = System.currentTimeMillis()
            )
        }
    }
}
