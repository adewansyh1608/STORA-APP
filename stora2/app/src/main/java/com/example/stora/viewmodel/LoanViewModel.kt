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

    private val _isServerAvailable = MutableStateFlow(true)
    val isServerAvailable: StateFlow<Boolean> = _isServerAvailable.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        val apiService = ApiConfig.provideApiService()
        loanRepository = LoanRepository(database.loanDao(), database.inventoryDao(), apiService, application)

        loadActiveLoans()
        loadLoanHistory()
        updateUnsyncedCount()
        
        startPeriodicServerCheck()
        
        if (isOnline()) {
            checkServerAndSync()
        } else {
            _isServerAvailable.value = false
        }
    }
    
    private fun startPeriodicServerCheck() {
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(10000)
                checkServerAvailability()
            }
        }
    }
    
    fun checkServerAvailability() {
        viewModelScope.launch {
            val wasOnline = isOnline()
            val wasServerAvailable = _isServerAvailable.value
            
            if (wasOnline) {
                try {
                    val result = loanRepository.checkServerHealth()
                    _isServerAvailable.value = result
                    
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
                updateInMemoryLoansData(loans, isHistory = false)
            }
        }
    }

    private fun loadLoanHistory() {
        viewModelScope.launch {
            loanRepository.getLoanHistory().collect { loans ->
                _loanHistory.value = loans
                updateInMemoryLoansData(loans, isHistory = true)
            }
        }
    }

    private fun updateInMemoryLoansData(loans: List<LoanWithItems>, isHistory: Boolean) {
        val targetList = if (isHistory) {
            com.example.stora.data.LoansData.loansHistory
        } else {
            com.example.stora.data.LoansData.loansOnLoan
        }
        
        targetList.removeAll { it.roomLoanId != null }
        
        val existingItemIds = targetList.map { it.roomItemId }.toSet()
        
        loans.forEach { loanWithItems ->
            val loan = loanWithItems.loan
            val items = loanWithItems.items
            
            items.forEach { item ->
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
                viewModelScope.launch {
                    kotlinx.coroutines.delay(3000)
                    _syncStatus.value = null
                }
            }
        }
    }

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

    fun syncLoans() {
        syncFromServer()
    }
    
    fun clearSyncStatus() {
        _syncStatus.value = null
    }

    fun clearError() {
        _error.value = null
    }

    fun isOnline(): Boolean {
        return loanRepository.isOnline()
    }

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
