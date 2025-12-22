package com.example.stora.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "notification_history")
data class NotificationHistoryEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val serverId: Int? = null,
    val serverReminderId: Int? = null,
    val userId: Int,
    val title: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "Terkirim",
    val relatedLoanId: Int? = null,
    val relatedReminderId: String? = null,
    val isLocallyCreated: Boolean = false,
    val needsSync: Boolean = false,
    val isSynced: Boolean = true,
    val lastModified: Long = System.currentTimeMillis()
) {
    companion object {
        fun fromApiModel(apiModel: NotificationHistoryApiModel, localId: String? = null, userId: Int): NotificationHistoryEntity {
            val timestamp = apiModel.tanggal?.let {
                try {
                    val isoFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
                    isoFormat.timeZone = java.util.TimeZone.getTimeZone("UTC")
                    isoFormat.parse(it.replace(".000Z", ""))?.time
                        ?: run {
                            java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).parse(it)?.time
                                ?: System.currentTimeMillis()
                        }
                } catch (e: Exception) {
                    try {
                        it.toLongOrNull() ?: System.currentTimeMillis()
                    } catch (e2: Exception) {
                        System.currentTimeMillis()
                    }
                }
            } ?: System.currentTimeMillis()
            
            return NotificationHistoryEntity(
                id = localId ?: UUID.randomUUID().toString(),
                serverId = apiModel.idNotifikasi,
                serverReminderId = apiModel.idReminder,
                userId = userId,
                title = apiModel.judul ?: "Notifikasi",
                message = apiModel.pesan ?: "",
                timestamp = timestamp,
                status = apiModel.status?.let { 
                    if (it.equals("sent", ignoreCase = true)) "Terkirim" else it 
                } ?: "Terkirim",
                relatedLoanId = apiModel.idPeminjaman,
                isLocallyCreated = false,
                needsSync = false,
                isSynced = true,
                lastModified = System.currentTimeMillis()
            )
        }
        
        fun createLocal(
            userId: Int,
            title: String,
            message: String,
            timestamp: Long = System.currentTimeMillis(),
            relatedLoanId: Int? = null,
            relatedReminderId: String? = null,
            serverReminderId: Int? = null
        ): NotificationHistoryEntity {
            return NotificationHistoryEntity(
                id = UUID.randomUUID().toString(),
                serverId = null,
                serverReminderId = serverReminderId,
                userId = userId,
                title = title,
                message = message,
                timestamp = timestamp,
                status = "Terkirim",
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
