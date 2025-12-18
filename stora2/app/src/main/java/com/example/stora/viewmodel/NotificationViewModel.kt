package com.example.stora.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.stora.data.*
import com.example.stora.network.ApiConfig
import com.example.stora.notification.ReminderScheduler
import com.example.stora.repository.NotificationRepository
import com.example.stora.utils.TokenManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * ViewModel for notification/reminder management with offline-first support.
 */
class NotificationViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "NotificationViewModel"
    }

    private val database = AppDatabase.getDatabase(application)
    private val tokenManager = TokenManager.getInstance(application)
    private val repository = NotificationRepository(
        database.reminderDao(),
        database.notificationHistoryDao(),
        ApiConfig.provideApiService(),
        tokenManager,
        application
    )

    // State flows
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _syncStatus = MutableStateFlow<String?>(null)
    val syncStatus: StateFlow<String?> = _syncStatus.asStateFlow()

    private val _isOnline = MutableStateFlow(repository.isOnline())
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    // Live data from Room - automatically updates when data changes
    val reminders: StateFlow<List<ReminderEntity>> = repository.getAllReminders()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val notificationHistory: StateFlow<List<NotificationHistoryEntity>> = repository.getNotificationHistory()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        // Sync on startup if online
        viewModelScope.launch {
            if (repository.isOnline()) {
                syncData()
            }
        }
    }

    /**
     * Create or update periodic reminder - works offline
     */
    fun savePeriodicReminder(months: Int, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val userId = tokenManager.getUserId()
                if (userId == -1) {
                    onError("Silakan login terlebih dahulu")
                    return@launch
                }

                // Check if periodic reminder already exists
                val existingPeriodic = repository.getPeriodicReminder()

                val reminder = if (existingPeriodic != null) {
                    // Update existing
                    existingPeriodic.copy(
                        periodicMonths = months,
                        lastNotified = System.currentTimeMillis(), // Reset countdown
                        needsSync = true,
                        isSynced = false,
                        lastModified = System.currentTimeMillis()
                    )
                } else {
                    // Create new
                    ReminderEntity(
                        id = UUID.randomUUID().toString(),
                        userId = userId,
                        reminderType = "periodic",
                        title = "Pengingat Pengecekan Inventory",
                        periodicMonths = months,
                        isActive = true,
                        needsSync = true,
                        isSynced = false,
                        lastModified = System.currentTimeMillis()
                    )
                }

                val result = repository.saveReminder(reminder)
                if (result.isSuccess) {
                    Log.d(TAG, "Periodic reminder saved successfully")
                    onSuccess()

                    // Schedule local notification check
                    ReminderScheduler.checkNow(getApplication())
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "Gagal menyimpan pengingat"
                    onError(errorMsg)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error saving periodic reminder", e)
                onError("Error: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Create custom reminder - works offline
     */
    fun saveCustomReminder(
        title: String,
        scheduledDatetime: Long,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val userId = tokenManager.getUserId()
                if (userId == -1) {
                    onError("Silakan login terlebih dahulu")
                    return@launch
                }

                val reminder = ReminderEntity(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    reminderType = "custom",
                    title = title.ifEmpty { "Pengingat Kustom" },
                    scheduledDatetime = scheduledDatetime,
                    isActive = true,
                    needsSync = true,
                    isSynced = false,
                    lastModified = System.currentTimeMillis()
                )

                val result = repository.saveReminder(reminder)
                if (result.isSuccess) {
                    Log.d(TAG, "Custom reminder saved successfully")

                    // Schedule specific notification for this reminder
                    ReminderScheduler.scheduleCustomReminder(
                        getApplication(),
                        reminder.id,
                        scheduledDatetime
                    )

                    onSuccess()
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "Gagal menyimpan pengingat"
                    onError(errorMsg)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error saving custom reminder", e)
                onError("Error: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Delete a reminder - works offline
     */
    fun deleteReminder(id: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val result = repository.deleteReminder(id)
                if (result.isSuccess) {
                    ReminderScheduler.cancelCustomReminder(getApplication(), id)
                    onSuccess()
                } else {
                    onError(result.exceptionOrNull()?.message ?: "Gagal menghapus pengingat")
                }
            } catch (e: Exception) {
                onError("Error: ${e.message}")
            }
        }
    }

    /**
     * Toggle reminder active state
     */
    fun toggleReminder(reminder: ReminderEntity, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val updatedReminder = reminder.copy(
                    isActive = !reminder.isActive,
                    needsSync = true,
                    isSynced = false,
                    lastModified = System.currentTimeMillis()
                )
                repository.saveReminder(updatedReminder)
            } catch (e: Exception) {
                onError("Error: ${e.message}")
            }
        }
    }

    /**
     * Sync data with server
     */
    fun syncData() {
        viewModelScope.launch {
            try {
                _syncStatus.value = "Menyinkronkan..."
                _isOnline.value = repository.isOnline()

                if (!repository.isOnline()) {
                    _syncStatus.value = "Offline - Data tersimpan lokal"
                    return@launch
                }

                val result = repository.performFullSync()
                if (result.isSuccess) {
                    val (toServer, fromServer) = result.getOrDefault(Pair(0, 0))
                    _syncStatus.value = "Sinkronisasi berhasil"
                    Log.d(TAG, "Sync completed: $toServer to server, $fromServer from server")
                } else {
                    _syncStatus.value = "Gagal sinkronisasi"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing", e)
                _syncStatus.value = "Error: ${e.message}"
            } finally {
                // Clear sync status after delay
                kotlinx.coroutines.delay(2000)
                _syncStatus.value = null
            }
        }
    }

    /**
     * Check network status
     */
    fun refreshNetworkStatus() {
        _isOnline.value = repository.isOnline()
    }

    /**
     * Get unread notification count
     */
    suspend fun getUnreadCount(): Int {
        return repository.getUnreadCount()
    }

    /**
     * Mark notification as read
     */
    fun markNotificationAsRead(id: String) {
        viewModelScope.launch {
            repository.markNotificationAsRead(id)
        }
    }

    /**
     * Mark all as read
     */
    fun markAllNotificationsAsRead() {
        viewModelScope.launch {
            repository.markAllNotificationsAsRead()
        }
    }
}
