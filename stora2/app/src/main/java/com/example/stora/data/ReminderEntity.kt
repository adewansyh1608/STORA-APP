package com.example.stora.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Room entity to store reminder settings locally for offline-first functionality.
 * Supports both periodic (e.g., every 3 months) and custom (specific datetime) reminders.
 */
@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    
    /** Server ID (null if created offline and not yet synced) */
    val serverId: Int? = null,
    
    /** User ID who owns this reminder */
    val userId: Int,
    
    /** Type of reminder: "periodic" or "custom" */
    val reminderType: String,
    
    /** Title/label for the reminder */
    val title: String? = "Pengingat Pengecekan Inventory",
    
    /** For periodic reminders: interval in months (1-12) */
    val periodicMonths: Int? = 3,
    
    /** For custom reminders: specific datetime as timestamp (milliseconds) */
    val scheduledDatetime: Long? = null,
    
    /** Whether the reminder is active */
    val isActive: Boolean = true,
    
    /** Last time this reminder triggered a notification (timestamp) */
    val lastNotified: Long? = null,
    
    /** Whether this reminder has local changes that need to sync to server */
    val needsSync: Boolean = true,
    
    /** Whether this reminder is synced with server */
    val isSynced: Boolean = false,
    
    /** Whether this reminder is marked for deletion (soft delete for sync) */
    val isDeleted: Boolean = false,
    
    /** Last modification timestamp for conflict resolution */
    val lastModified: Long = System.currentTimeMillis()
) {
    /**
     * Convert to API request format for server sync
     * Send timestamp as ISO string in LOCAL timezone (no UTC conversion)
     */
    fun toApiRequest(): ReminderRequest {
        return ReminderRequest(
            reminderType = reminderType,
            title = title,
            periodicMonths = periodicMonths,
            scheduledDatetime = scheduledDatetime?.let { timestamp ->
                // Send the timestamp directly as milliseconds string for precision
                // This avoids timezone conversion issues
                timestamp.toString()
            },
            isActive = isActive
        )
    }
    
    companion object {
        // Multiple datetime formats that server might return
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
        
        /**
         * Parse datetime string with multiple format support
         * Handles: numeric timestamp, UTC strings ('Z' suffix), and local time strings
         */
        fun parseDatetime(dateStr: String?): Long? {
            if (dateStr.isNullOrBlank()) return null
            
            // First, check if it's a numeric timestamp (milliseconds)
            val numericTimestamp = dateStr.toLongOrNull()
            if (numericTimestamp != null) {
                android.util.Log.d("ReminderEntity", "Parsed numeric timestamp: $numericTimestamp")
                return numericTimestamp
            }
            
            // Check if it's a very large number (seconds since epoch, then convert to ms)
            val possibleSeconds = dateStr.replace(".", "").take(10).toLongOrNull()
            if (possibleSeconds != null && possibleSeconds > 1000000000 && possibleSeconds < 10000000000) {
                val timestampMs = possibleSeconds * 1000
                android.util.Log.d("ReminderEntity", "Parsed seconds timestamp: $timestampMs")
                return timestampMs
            }
            
            // Try parsing as date string
            for (format in DATE_FORMATS) {
                try {
                    val sdf = java.text.SimpleDateFormat(format, java.util.Locale.US)
                    
                    // If format has 'Z' suffix, parse as UTC
                    // Otherwise, parse as local time
                    if (format.contains("'Z'") || dateStr.endsWith("Z")) {
                        sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                    } else if (format.contains("XXX") || format.contains("X")) {
                        // Format with timezone offset - let parser handle it
                    } else {
                        // No timezone info - assume local time
                        sdf.timeZone = java.util.TimeZone.getDefault()
                    }
                    
                    val date = sdf.parse(dateStr)
                    if (date != null) {
                        android.util.Log.d("ReminderEntity", "Parsed '$dateStr' with format '$format' (tz=${sdf.timeZone.id}) -> ${date.time}")
                        return date.time
                    }
                } catch (e: Exception) {
                    // Try next format
                }
            }
            
            // Try parsing as timestamp (milliseconds or seconds)
            try {
                val ts = dateStr.toLong()
                // If it's in seconds (less than year 3000 in seconds), convert to millis
                return if (ts < 100000000000L) ts * 1000 else ts
            } catch (e: Exception) {
                // Not a number
            }
            
            android.util.Log.w("ReminderEntity", "Failed to parse datetime: $dateStr")
            return null
        }
        
        /**
         * Create from API response with fallback to existing local data
         * This preserves local datetime when server returns null
         */
        fun fromApiModel(
            apiModel: ReminderApiModel, 
            existingLocal: ReminderEntity?,
            userId: Int
        ): ReminderEntity {
            val serverScheduledTimestamp = parseDatetime(apiModel.scheduledDatetime)
            val serverLastNotifiedTimestamp = parseDatetime(apiModel.lastNotified)
            
            // Use server value if available, otherwise preserve local value
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
        
        /**
         * Create from API response (legacy, no local fallback)
         */
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


