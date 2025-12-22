package com.example.stora.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val serverId: Int? = null,
    val userId: Int,
    val reminderType: String,
    val title: String? = "Pengingat Pengecekan Inventory",
    val periodicMonths: Int? = 3,
    val scheduledDatetime: Long? = null,
    val isActive: Boolean = true,
    val lastNotified: Long? = null,
    val needsSync: Boolean = true,
    val isSynced: Boolean = false,
    val isDeleted: Boolean = false,
    val lastModified: Long = System.currentTimeMillis()
) {
    fun toApiRequest(): ReminderRequest {
        return ReminderRequest(
            reminderType = reminderType,
            title = title,
            periodicMonths = periodicMonths,
            scheduledDatetime = scheduledDatetime?.let { timestamp ->
                timestamp.toString()
            },
            isActive = isActive
        )
    }
    
    companion object {
        private val DATE_FORMATS = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSSX",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSS",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss.SSS",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd"
        )
        
        fun parseDatetime(dateStr: String?): Long? {
            if (dateStr.isNullOrBlank()) return null
            
            val numericTimestamp = dateStr.toLongOrNull()
            if (numericTimestamp != null) {
                android.util.Log.d("ReminderEntity", "Parsed numeric timestamp: $numericTimestamp")
                return numericTimestamp
            }
            
            val possibleSeconds = dateStr.replace(".", "").take(10).toLongOrNull()
            if (possibleSeconds != null && possibleSeconds > 1000000000 && possibleSeconds < 10000000000) {
                val timestampMs = possibleSeconds * 1000
                android.util.Log.d("ReminderEntity", "Parsed seconds timestamp: $timestampMs")
                return timestampMs
            }
            
            for (format in DATE_FORMATS) {
                try {
                    val sdf = java.text.SimpleDateFormat(format, java.util.Locale.US)
                    
                    if (format.contains("'Z'") || dateStr.endsWith("Z")) {
                        sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                    } else if (format.contains("XXX") || format.contains("X")) {
                    } else {
                        sdf.timeZone = java.util.TimeZone.getDefault()
                    }
                    
                    val date = sdf.parse(dateStr)
                    if (date != null) {
                        android.util.Log.d("ReminderEntity", "Parsed '$dateStr' with format '$format' (tz=${sdf.timeZone.id}) -> ${date.time}")
                        return date.time
                    }
                } catch (e: Exception) {
                }
            }
            
            try {
                val ts = dateStr.toLong()
                return if (ts < 100000000000L) ts * 1000 else ts
            } catch (e: Exception) {
            }
            
            android.util.Log.w("ReminderEntity", "Failed to parse datetime: $dateStr")
            return null
        }
        
        fun fromApiModel(
            apiModel: ReminderApiModel, 
            existingLocal: ReminderEntity?,
            userId: Int
        ): ReminderEntity {
            val serverScheduledTimestamp = parseDatetime(apiModel.scheduledDatetime)
            val serverLastNotifiedTimestamp = parseDatetime(apiModel.lastNotified)
            
            val finalScheduledDatetime = serverScheduledTimestamp ?: existingLocal?.scheduledDatetime
            val finalLastNotified = serverLastNotifiedTimestamp ?: existingLocal?.lastNotified
            
            android.util.Log.d("ReminderEntity", "fromApiModel: " +
                "server_scheduled='${apiModel.scheduledDatetime}' -> $serverScheduledTimestamp, " +
                "local_scheduled=${existingLocal?.scheduledDatetime}, " +
                "final=$finalScheduledDatetime")
            
            return ReminderEntity(
                id = existingLocal?.id ?: UUID.randomUUID().toString(),
                serverId = apiModel.idReminder,
                userId = userId,
                reminderType = apiModel.reminderType,
                title = apiModel.title ?: existingLocal?.title,
                periodicMonths = apiModel.periodicMonths ?: existingLocal?.periodicMonths,
                scheduledDatetime = finalScheduledDatetime,
                isActive = apiModel.isActive,
                lastNotified = finalLastNotified,
                needsSync = false,
                isSynced = true,
                isDeleted = false,
                lastModified = System.currentTimeMillis()
            )
        }
        
        fun fromApiModel(apiModel: ReminderApiModel, localId: String? = null, userId: Int): ReminderEntity {
            val scheduledTimestamp = parseDatetime(apiModel.scheduledDatetime)
            val lastNotifiedTimestamp = parseDatetime(apiModel.lastNotified)
            
            android.util.Log.d("ReminderEntity", "fromApiModel: scheduledDatetime='${apiModel.scheduledDatetime}' -> $scheduledTimestamp")
            
            return ReminderEntity(
                id = localId ?: UUID.randomUUID().toString(),
                serverId = apiModel.idReminder,
                userId = userId,
                reminderType = apiModel.reminderType,
                title = apiModel.title,
                periodicMonths = apiModel.periodicMonths,
                scheduledDatetime = scheduledTimestamp,
                isActive = apiModel.isActive,
                lastNotified = lastNotifiedTimestamp,
                needsSync = false,
                isSynced = true,
                isDeleted = false,
                lastModified = System.currentTimeMillis()
            )
        }
    }
}
