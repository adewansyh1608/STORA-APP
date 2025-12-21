package com.example.stora.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.stora.data.AppDatabase
import com.example.stora.data.LoanWithItems
import com.example.stora.network.ApiConfig
import com.example.stora.repository.LoanItemInfo
import com.example.stora.repository.LoanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LoanViewModel(application: Application) : AndroidViewModel(application) {
    
    companion object {
        private const val TAG = "LoanViewModel"
    }

    private val loanRepository: LoanRepository

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncStatus = MutableStateFlow<String?>(null)
    val syncStatus: StateFlow<String?> = _syncStatus.asStateFlow()

    private val _activeLoans = MutableStateFlow<List<LoanWithItems>>(emptyList())
    val activeLoans: StateFlow<List<LoanWithItems>> = _activeLoans.asStateFlow()

    private val _loanHistory = MutableStateFlow<List<LoanWithItems>>(emptyList())
    val loanHistory: StateFlow<List<LoanWithItems>> = _loanHistory.asStateFlow()

    private val _unsyncedCount = MutableStateFlow(0)
    val unsyncedCount: StateFlow<Int> = _unsyncedCount.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Server availability state - updated based on actual API calls
    private val _isServerAvailable = MutableStateFlow(true)
    val isServerAvailable: StateFlow<Boolean> = _isServerAvailable.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        val apiService = ApiConfig.provideApiService()
        loanRepository = LoanRepository(database.loanDao(), database.inventoryDao(), apiService, application)

        // Load loans from Room (local data first for fast loading)
        loadActiveLoans()
        loadLoanHistory()
        updateUnsyncedCount()
        
        // Start periodic server availability check
        startPeriodicServerCheck()
        
        // Initial sync from server if online
        if (isOnline()) {
            checkServerAndSync()
        } else {
            _isServerAvailable.value = false
        }
    }
    
    // Periodic server availability check
    private fun startPeriodicServerCheck() {
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(10000) // Check every 10 seconds
                checkServerAvailability()
            }
        }
    }
    
    // Public function to check server availability
    fun checkServerAvailability() {
        viewModelScope.launch {
            val wasOnline = isOnline()
            val wasServerAvailable = _isServerAvailable.value
            
            if (wasOnline) {
                try {
                    // Try a lightweight API call to check if server is reachable
                    val result = loanRepository.checkServerHealth()
                    _isServerAvailable.value = result
                    
                    // If we just came back online, trigger a sync
                    if (result && !wasServerAvailable) {
                        Log.d(TAG, "Server is back online, triggering sync")
                        syncFromServer()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Server health check failed: ${e.message}")
                    _isServerAvailable.value = false
                }
            } else {
                _isServerAvailable.value = false
            }
        }
    }

    // Check server availability and sync
    private fun checkServerAndSync() {
        viewModelScope.launch {
            try {
                syncFromServer()
                _isServerAvailable.value = true
            } catch (e: Exception) {
                Log.e(TAG, "Server not available: ${e.message}")
                _isServerAvailable.value = false
            }
        }
    }

    private fun loadActiveLoans() {
        viewModelScope.launch {
            loanRepository.getActiveLoans().collect { loans ->
                _activeLoans.value = loans
                // Also update in-memory LoansData for backward compatibility
                updateInMemoryLoansData(loans, isHistory = false)
            }
        }
    }

    private fun loadLoanHistory() {
        viewModelScope.launch {
            loanRepository.getLoanHistory().collect { loans ->
                _loanHistory.value = loans
                // Also update in-memory LoansData for backward compatibility
                updateInMemoryLoansData(loans, isHistory = true)
            }
        }
    }

    private fun updateInMemoryLoansData(loans: List<LoanWithItems>, isHistory: Boolean) {
        // Sync Room data to in-memory LoansData for backward compatibility with existing screens
        // Clear the target list and rebuild from Room data to prevent duplicates
        val targetList = if (isHistory) {
            com.example.stora.data.LoansData.loansHistory
        } else {
            com.example.stora.data.LoansData.loansOnLoan
        }
        
        // Remove items that have roomLoanId (Room-managed items) and rebuild
        targetList.removeAll { it.roomLoanId != null }
        
        // Build a set of existing item IDs to prevent any duplicates
        val existingItemIds = targetList.map { it.roomItemId }.toSet()
        
        // Add all items from Room
        loans.forEach { loanWithItems ->
            val loan = loanWithItems.loan
            val items = loanWithItems.items
            
            items.forEach { item ->
                // Skip if this item already exists (extra safety check)
                if (existingItemIds.contains(item.id)) return@forEach
                
                val loanItem = com.example.stora.data.LoanItem(
                    id = item.id.hashCode(),
                    groupId = loan.id.hashCode(),
                    name = item.namaBarang,
                    code = item.kodeBarang,
                    quantity = item.jumlah,
                    borrower = loan.namaPeminjam,
                    borrowerPhone = loan.noHpPeminjam,
                    borrowDate = loan.tanggalPinjam,
                    returnDate = loan.tanggalKembali,
                    actualReturnDate = loan.tanggalDikembalikan,
                    status = loan.status,
                    imageUri = item.imageUri,
                    returnImageUri = item.returnImageUri,
                    roomLoanId = loan.id,
                    roomItemId = item.id
                )
                
                targetList.add(loanItem)
            }
        }
    }

    // Sync data from server
    fun syncFromServer() {
        if (_isSyncing.value) {
            Log.d(TAG, "Sync already in progress")
            return
        }

        viewModelScope.launch {
            try {
                _isSyncing.value = true
                _syncStatus.value = "Sinkronisasi data peminjaman..."
                
                val result = loanRepository.performFullSync()
                result.fold(
                    onSuccess = { (toServer, fromServer) ->
                        _syncStatus.value = "Sinkronisasi berhasil: $toServer ke server, $fromServer dari server"
                        _isServerAvailable.value = true
                        Log.d(TAG, "Sync completed: $toServer to server, $fromServer from server")
                        
                        // Reload data from Room to update in-memory LoansData cache
                        loadActiveLoans()
                        loadLoanHistory()
                    },
                    onFailure = { error ->
                        _syncStatus.value = "Gagal sinkronisasi: ${error.message}"
                        _isServerAvailable.value = false
                        Log.e(TAG, "Sync failed", error)
                    }
                )
            } catch (e: Exception) {
                _syncStatus.value = "Error: ${e.message}"
                _isServerAvailable.value = false
                Log.e(TAG, "Sync exception", e)
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

    // Refresh data (reload from Room + sync from server)
    fun refreshData() {
        loadActiveLoans()
        loadLoanHistory()
        syncFromServer()
    }

    fun createLoan(
        namaPeminjam: String,
        noHpPeminjam: String,
        tanggalPinjam: String,
        tanggalKembali: String,
        items: List<LoanItemInfo>,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = loanRepository.createLoan(
                    namaPeminjam = namaPeminjam,
                    noHpPeminjam = noHpPeminjam,
                    tanggalPinjam = tanggalPinjam,
                    tanggalKembali = tanggalKembali,
                    items = items
                )
                result.fold(
                    onSuccess = {
                        Log.d(TAG, "Loan created successfully")
                        onSuccess()
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Error creating loan", error)
                        onError(error.message ?: "Unknown error")
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Exception creating loan", e)
                onError(e.message ?: "Unknown error")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun returnLoan(
        loanId: String,
        returnDateTime: String,
        itemReturnImages: Map<String, String?>,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = loanRepository.returnLoan(
                    loanId = loanId,
                    returnDateTime = returnDateTime,
                    itemReturnImages = itemReturnImages
                )
                result.fold(
                    onSuccess = {
                        Log.d(TAG, "Loan returned successfully")
                        onSuccess()
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Error returning loan", error)
                        onError(error.message ?: "Unknown error")
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Exception returning loan", e)
                onError(e.message ?: "Unknown error")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateLoan(
        loanId: String,
        newDeadline: String? = null,
        newItems: List<LoanItemInfo>? = null,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = loanRepository.updateLoan(
                    loanId = loanId,
                    newDeadline = newDeadline,
                    newItems = newItems
                )
                result.fold(
                    onSuccess = {
                        Log.d(TAG, "Loan updated successfully")
                        onSuccess()
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Error updating loan", error)
                        onError(error.message ?: "Unknown error")
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Exception updating loan", e)
                onError(e.message ?: "Unknown error")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteLoan(
        loanId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = loanRepository.deleteLoan(loanId)
                result.fold(
                    onSuccess = {
                        Log.d(TAG, "Loan deleted successfully")
                        onSuccess()
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Error deleting loan", error)
                        onError(error.message ?: "Unknown error")
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Exception deleting loan", e)
                onError(e.message ?: "Unknown error")
            } finally {
                _isLoading.value = false
            }
        }
    }

    suspend fun getLoanById(loanId: String): LoanWithItems? {
        return loanRepository.getLoanById(loanId)
    }

    // Alias for backward compatibility
    fun syncLoans() {
        syncFromServer()
    }
    
    // Clear sync status
    fun clearSyncStatus() {
        _syncStatus.value = null
    }

    // Clear error
    fun clearError() {
        _error.value = null
    }

    // Check if device is online
    fun isOnline(): Boolean {
        return loanRepository.isOnline()
    }

    // Update unsynced count
    private fun updateUnsyncedCount() {
        viewModelScope.launch {
            try {
                val count = loanRepository.getUnsyncedLoansCount()
                _unsyncedCount.value = count
                Log.d(TAG, "Unsynced loans count: $count")
            } catch (e: Exception) {
                Log.e(TAG, "Error getting unsynced count", e)
            }
        }
    }

    // Sync data (both directions)
    fun syncData() {
        if (_isSyncing.value) {
            Log.d(TAG, "Sync already in progress")
            return
        }

        viewModelScope.launch {
            try {
                _isSyncing.value = true
                _syncStatus.value = "Sinkronisasi dimulai..."

                if (!isOnline()) {
                    _syncStatus.value = "Offline - Data disimpan lokal"
                    return@launch
                }

                val result = loanRepository.performFullSync()
                result.fold(
                    onSuccess = { (toServer, fromServer) ->
                        _syncStatus.value = "Sinkronisasi berhasil: $toServer ke server, $fromServer dari server"
                        updateUnsyncedCount()
                        
                        // Reload data from Room to update in-memory LoansData cache
                        loadActiveLoans()
                        loadLoanHistory()
                    },
                    onFailure = { error ->
                        _syncStatus.value = "Gagal sinkronisasi: ${error.message}"
                        _error.value = error.message
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error during sync", e)
                _syncStatus.value = "Error: ${e.message}"
                _error.value = e.message
            } finally {
                _isSyncing.value = false
                viewModelScope.launch {
                    kotlinx.coroutines.delay(3000)
                    _syncStatus.value = null
                }
            }
        }
    }
}
