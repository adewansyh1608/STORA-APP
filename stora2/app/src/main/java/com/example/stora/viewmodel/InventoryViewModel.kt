package com.example.stora.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.stora.data.AppDatabase
import com.example.stora.data.InventoryItem
import com.example.stora.network.ApiConfig
import com.example.stora.repository.InventoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class InventoryViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "InventoryViewModel"
    }

    private val database = AppDatabase.getDatabase(application)
    private val apiService = ApiConfig.provideApiService()
    private val repository = InventoryRepository(
        inventoryDao = database.inventoryDao(),
        apiService = apiService,
        context = application
    )

    private val _inventoryItems = MutableStateFlow<List<InventoryItem>>(emptyList())
    val inventoryItems: StateFlow<List<InventoryItem>> = _inventoryItems.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _syncStatus = MutableStateFlow<String?>(null)
    val syncStatus: StateFlow<String?> = _syncStatus.asStateFlow()

    private val _unsyncedCount = MutableStateFlow(0)
    val unsyncedCount: StateFlow<Int> = _unsyncedCount.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        loadInventoryItems()
        updateUnsyncedCount()
        // Auto sync on initialization if online
        if (repository.isOnline()) {
            syncData()
        }
    }

    private fun loadInventoryItems() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                repository.getAllInventoryItems().collectLatest { items ->
                    _inventoryItems.value = items
                    Log.d(TAG, "Loaded ${items.size} items from database")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading inventory items", e)
                _error.value = "Gagal memuat data: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun searchInventory(query: String) {
        _searchQuery.value = query
        viewModelScope.launch {
            try {
                if (query.isBlank()) {
                    repository.getAllInventoryItems().collectLatest { items ->
                        _inventoryItems.value = items
                    }
                } else {
                    repository.searchInventoryItems(query).collectLatest { items ->
                        _inventoryItems.value = items
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error searching inventory", e)
                _error.value = "Gagal mencari data: ${e.message}"
            }
        }
    }

    suspend fun getInventoryItemById(id: String): InventoryItem? {
        return try {
            repository.getInventoryItemById(id)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting item by ID", e)
            null
        }
    }

    fun addInventoryItem(item: InventoryItem, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val result = repository.insertInventoryItem(item)
                if (result.isSuccess) {
                    Log.d(TAG, "Item added successfully: ${item.name}")
                    updateUnsyncedCount()
                    // Try to sync if online
                    if (repository.isOnline()) {
                        syncToServer()
                    }
                    onSuccess()
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "Gagal menambahkan item"
                    _error.value = errorMsg
                    onError(errorMsg)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error adding item", e)
                val errorMsg = "Gagal menambahkan item: ${e.message}"
                _error.value = errorMsg
                onError(errorMsg)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateInventoryItem(item: InventoryItem, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val result = repository.updateInventoryItem(item)
                if (result.isSuccess) {
                    Log.d(TAG, "Item updated successfully: ${item.name}")
                    updateUnsyncedCount()
                    // Try to sync if online
                    if (repository.isOnline()) {
                        syncToServer()
                    }
                    onSuccess()
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "Gagal mengupdate item"
                    _error.value = errorMsg
                    onError(errorMsg)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating item", e)
                val errorMsg = "Gagal mengupdate item: ${e.message}"
                _error.value = errorMsg
                onError(errorMsg)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteInventoryItem(id: String, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val result = repository.deleteInventoryItem(id)
                if (result.isSuccess) {
                    Log.d(TAG, "Item deleted successfully: $id")
                    updateUnsyncedCount()
                    // Try to sync if online
                    if (repository.isOnline()) {
                        syncToServer()
                    }
                    onSuccess()
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "Gagal menghapus item"
                    _error.value = errorMsg
                    onError(errorMsg)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting item", e)
                val errorMsg = "Gagal menghapus item: ${e.message}"
                _error.value = errorMsg
                onError(errorMsg)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun syncData() {
        if (_isSyncing.value) {
            Log.d(TAG, "Sync already in progress")
            return
        }

        viewModelScope.launch {
            try {
                _isSyncing.value = true
                _syncStatus.value = "Sinkronisasi dimulai..."
                Log.d(TAG, "Starting full sync...")

                if (!repository.isOnline()) {
                    _syncStatus.value = "Offline - Data disimpan lokal"
                    Log.w(TAG, "Device is offline, skipping sync")
                    return@launch
                }

                val result = repository.performFullSync()
                if (result.isSuccess) {
                    val (toServer, fromServer) = result.getOrDefault(Pair(0, 0))
                    _syncStatus.value = "Sinkronisasi berhasil: $toServer ke server, $fromServer dari server"
                    Log.d(TAG, "Sync completed successfully")
                    updateUnsyncedCount()
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "Gagal sinkronisasi"
                    _syncStatus.value = "Gagal: $errorMsg"
                    _error.value = errorMsg
                    Log.e(TAG, "Sync failed: $errorMsg")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during sync", e)
                _syncStatus.value = "Error: ${e.message}"
                _error.value = "Gagal sinkronisasi: ${e.message}"
            } finally {
                _isSyncing.value = false
                // Clear sync status after 3 seconds
                viewModelScope.launch {
                    kotlinx.coroutines.delay(3000)
                    _syncStatus.value = null
                }
            }
        }
    }

    private fun syncToServer() {
        viewModelScope.launch {
            try {
                if (repository.isOnline()) {
                    repository.syncToServer()
                    updateUnsyncedCount()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing to server", e)
            }
        }
    }

    fun syncFromServer() {
        viewModelScope.launch {
            try {
                _isSyncing.value = true
                _syncStatus.value = "Mengambil data dari server..."

                if (!repository.isOnline()) {
                    _syncStatus.value = "Tidak ada koneksi internet"
                    return@launch
                }

                val result = repository.syncFromServer()
                if (result.isSuccess) {
                    val count = result.getOrDefault(0)
                    _syncStatus.value = "Berhasil mengambil $count item dari server"
                } else {
                    _syncStatus.value = "Gagal mengambil data dari server"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing from server", e)
                _syncStatus.value = "Error: ${e.message}"
            } finally {
                _isSyncing.value = false
                viewModelScope.launch {
                    kotlinx.coroutines.delay(3000)
                    _syncStatus.value = null
                }
            }
        }
    }

    private fun updateUnsyncedCount() {
        viewModelScope.launch {
            try {
                val count = repository.getUnsyncedCount()
                _unsyncedCount.value = count
                Log.d(TAG, "Unsynced items count: $count")
            } catch (e: Exception) {
                Log.e(TAG, "Error getting unsynced count", e)
            }
        }
    }

    suspend fun getTotalQuantity(): Int {
        return try {
            repository.getTotalQuantity()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting total quantity", e)
            0
        }
    }

    suspend fun isNoinvExists(noinv: String): Boolean {
        return try {
            repository.isNoinvExists(noinv)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking noinv", e)
            false
        }
    }

    fun filterByCategory(category: String) {
        viewModelScope.launch {
            try {
                repository.getInventoryByCategory(category).collectLatest { items ->
                    _inventoryItems.value = items
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error filtering by category", e)
                _error.value = "Gagal filter kategori: ${e.message}"
            }
        }
    }

    fun filterByCondition(condition: String) {
        viewModelScope.launch {
            try {
                repository.getInventoryByCondition(condition).collectLatest { items ->
                    _inventoryItems.value = items
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error filtering by condition", e)
                _error.value = "Gagal filter kondisi: ${e.message}"
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun clearSyncStatus() {
        _syncStatus.value = null
    }

    fun isOnline(): Boolean {
        return repository.isOnline()
    }

    fun refreshData() {
        loadInventoryItems()
        if (repository.isOnline()) {
            syncData()
        }
    }
}
