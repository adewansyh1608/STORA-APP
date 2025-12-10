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

    init {
        val database = AppDatabase.getDatabase(application)
        val apiService = ApiConfig.provideApiService()
        loanRepository = LoanRepository(database.loanDao(), apiService, application)

        // Load loans from Room (local data first for fast loading)
        loadActiveLoans()
        loadLoanHistory()
        
        // Sync from server on initialization (app start/resume)
        syncFromServer()
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
        
        // Add all items from Room
        loans.forEach { loanWithItems ->
            val loan = loanWithItems.loan
            val items = loanWithItems.items
            
            items.forEach { item ->
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
                        Log.d(TAG, "Sync completed: $toServer to server, $fromServer from server")
                    },
                    onFailure = { error ->
                        _syncStatus.value = "Gagal sinkronisasi: ${error.message}"
                        Log.e(TAG, "Sync failed", error)
                    }
                )
            } catch (e: Exception) {
                _syncStatus.value = "Error: ${e.message}"
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
        itemReturnImages: Map<String, String?>,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = loanRepository.returnLoan(
                    loanId = loanId,
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

    fun deleteLoanHistory(
        loanId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = loanRepository.deleteLoanHistory(loanId)
                result.fold(
                    onSuccess = {
                        Log.d(TAG, "Loan history deleted successfully")
                        onSuccess()
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Error deleting loan history", error)
                        onError(error.message ?: "Unknown error")
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Exception deleting loan history", e)
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
}
