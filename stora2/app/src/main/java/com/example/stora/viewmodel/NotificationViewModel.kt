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

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _syncStatus = MutableStateFlow<String?>(null)
    val syncStatus: StateFlow<String?> = _syncStatus.asStateFlow()

    private val _isOnline = MutableStateFlow(repository.isOnline())
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    val reminders: StateFlow<List<ReminderEntity>> = repository.getAllReminders()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val notificationHistory: StateFlow<List<NotificationHistoryEntity>> = repository.getNotificationHistory()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        viewModelScope.launch {
            if (repository.isOnline()) {
                syncData()
            }
        }
    }

    fun savePeriodicReminder(months: Int, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val userId = tokenManager.getUserId()
                if (userId == -1) {
                    onError("Silakan login terlebih dahulu")
                    return@launch
                }

                val existingPeriodic = repository.getPeriodicReminder()

                val reminder = if (existingPeriodic != null) {
                    existingPeriodic.copy(
                        periodicMonths = months,
                        lastNotified = System.currentTimeMillis(),
                        needsSync = true,
                        isSynced = false,
                        lastModified = System.currentTimeMillis()
                    )
                } else {
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
                    val savedReminder = result.getOrNull() ?: reminder
                    Log.d(TAG, "Custom reminder saved successfully")

                    com.example.stora.notification.ReminderAlarmManager.scheduleExactAlarm(
                        context = getApplication(),
                        reminderId = savedReminder.id,
                        reminderTitle = savedReminder.title ?: "Pengingat",
                        reminderType = "custom",
                        serverReminderId = savedReminder.serverId,
                        scheduledTimeMillis = scheduledDatetime
                    )

                    ReminderScheduler.scheduleCustomReminder(
                        getApplication(),
                        savedReminder.id,
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

    fun deleteReminder(id: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val result = repository.deleteReminder(id)
                if (result.isSuccess) {
                    com.example.stora.notification.ReminderAlarmManager.cancelAlarm(getApplication(), id)
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
                kotlinx.coroutines.delay(2000)
                _syncStatus.value = null
            }
        }
    }

    fun refreshNetworkStatus() {
        _isOnline.value = repository.isOnline()
    }

    suspend fun getUnreadCount(): Int {
        return repository.getUnreadCount()
    }

    fun markNotificationAsRead(id: String) {
        viewModelScope.launch {
            repository.markNotificationAsRead(id)
        }
    }

    fun markAllNotificationsAsRead() {
        viewModelScope.launch {
            repository.markAllNotificationsAsRead()
        }
    }
}
