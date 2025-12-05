package com.example.stora.repository

import android.content.Context
import android.util.Log
import com.example.stora.data.*
import com.example.stora.network.ApiService
import com.example.stora.utils.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class LoanRepository(
    private val loanDao: LoanDao,
    private val apiService: ApiService,
    private val context: Context
) {
    companion object {
        private const val TAG = "LoanRepository"
    }

    private val tokenManager = TokenManager.getInstance(context)

    private fun getAuthToken(): String? = tokenManager.getAuthHeader()
    private fun getUserId(): Int = tokenManager.getUserId()

    // ==================== LOCAL OPERATIONS ====================

    fun getActiveLoans(): Flow<List<LoanWithItems>> {
        val userId = getUserId()
        return if (userId != -1) {
            loanDao.getActiveLoans(userId).map { loans ->
                loans.map { loan ->
                    val items = loanDao.getLoanItems(loan.id)
                    LoanWithItems(loan, items)
                }
            }
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }

    fun getLoanHistory(): Flow<List<LoanWithItems>> {
        val userId = getUserId()
        return if (userId != -1) {
            loanDao.getLoanHistory(userId).map { loans ->
                loans.map { loan ->
                    val items = loanDao.getLoanItems(loan.id)
                    LoanWithItems(loan, items)
                }
            }
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }

    suspend fun getLoanById(loanId: String): LoanWithItems? {
        return withContext(Dispatchers.IO) {
            val loan = loanDao.getLoanById(loanId)
            loan?.let {
                val items = loanDao.getLoanItems(loanId)
                LoanWithItems(it, items)
            }
        }
    }

    suspend fun getBorrowedQuantity(itemCode: String): Int {
        return withContext(Dispatchers.IO) {
            loanDao.getBorrowedQuantity(itemCode)
        }
    }

    suspend fun createLoan(
        namaPeminjam: String,
        noHpPeminjam: String,
        tanggalPinjam: String,
        tanggalKembali: String,
        items: List<LoanItemInfo>,
    ): Result<LoanWithItems> {
        return withContext(Dispatchers.IO) {
            try {
                val userId = getUserId()
                if (userId == -1) {
                    return@withContext Result.failure(Exception("User not logged in"))
                }

                val loan = LoanEntity(
                    namaPeminjam = namaPeminjam,
                    noHpPeminjam = noHpPeminjam,
                    tanggalPinjam = tanggalPinjam,
                    tanggalKembali = tanggalKembali,
                    status = "Dipinjam",
                    userId = userId,
                    needsSync = true,
                    isSynced = false
                )

                val loanItems = items.map { item ->
                    LoanItemEntity(
                        loanId = loan.id,
                        inventarisId = item.inventarisId,
                        namaBarang = item.namaBarang,
                        kodeBarang = item.kodeBarang,
                        jumlah = item.jumlah,
                        imageUri = item.imageUri
                    )
                }

                loanDao.insertLoanWithItems(loan, loanItems)
                Log.d(TAG, "Loan saved to Room: ${loan.id}")

                // Try to sync to server
                syncLoanToServer(loan, loanItems)

                Result.success(LoanWithItems(loan, loanItems))
            } catch (e: Exception) {
                Log.e(TAG, "Error creating loan", e)
                Result.failure(e)
            }
        }
    }

    suspend fun returnLoan(loanId: String, itemReturnImages: Map<String, String?>): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val loan = loanDao.getLoanById(loanId)
                    ?: return@withContext Result.failure(Exception("Loan not found"))

                val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                val currentDate = sdf.format(java.util.Date())
                
                // Determine if late based on due date
                val status = try {
                    val dueDateParsed = sdf.parse(loan.tanggalKembali)
                    if (dueDateParsed != null && java.util.Date().after(dueDateParsed)) {
                        "Terlambat"
                    } else {
                        "Selesai"
                    }
                } catch (e: Exception) {
                    "Selesai" // Default to Selesai if date parsing fails
                }

                // Update loan status in Room
                loanDao.updateLoanStatus(
                    loanId = loanId,
                    status = status,
                    returnDate = currentDate,
                    lastModified = System.currentTimeMillis()
                )

                // Update each item's return image
                itemReturnImages.forEach { (itemId, returnImageUri) ->
                    loanDao.updateLoanItemReturnImage(itemId, returnImageUri)
                }

                Log.d(TAG, "Loan returned in Room: $loanId with status: $status")

                // Try to sync to server
                loan.serverId?.let { serverId ->
                    try {
                        val authHeader = getAuthToken()
                        if (authHeader != null) {
                            // Convert date format for API (dd/MM/yyyy to yyyy-MM-dd)
                            val apiDateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                            val parsedDate = sdf.parse(currentDate)
                            val apiFormattedDate = parsedDate?.let { apiDateFormat.format(it) } ?: currentDate
                            
                            val response = apiService.updatePeminjamanStatus(
                                token = authHeader,
                                id = serverId,
                                request = LoanStatusUpdateRequest(
                                    status = status,
                                    tanggalDikembalikan = apiFormattedDate
                                )
                            )
                            if (response.isSuccessful) {
                                loanDao.markLoanAsSynced(loanId)
                                Log.d(TAG, "Loan status synced to server with date: $apiFormattedDate")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to sync loan status to server", e)
                    }
                }

                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Error returning loan", e)
                Result.failure(e)
            }
        }
    }

    suspend fun deleteLoanHistory(loanId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                loanDao.deleteLoanWithItems(loanId)
                Log.d(TAG, "Loan deleted from Room: $loanId")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting loan", e)
                Result.failure(e)
            }
        }
    }

    // ==================== SYNC OPERATIONS ====================

    private suspend fun syncLoanToServer(loan: LoanEntity, items: List<LoanItemEntity>) {
        try {
            val authHeader = getAuthToken() ?: return

            val validItems = items.filter { it.inventarisId != null && it.inventarisId > 0 }
            if (validItems.isEmpty()) {
                Log.w(TAG, "No valid items to sync (no inventory server IDs)")
                return
            }

            val request = loan.toApiRequest(validItems)
            Log.d(TAG, "Syncing loan to server: $request")

            val response = apiService.createPeminjaman(
                token = authHeader,
                request = request
            )

            if (response.isSuccessful && response.body()?.success == true) {
                val serverId = response.body()?.data?.idPeminjaman
                if (serverId != null) {
                    loanDao.updateLoanServerId(loan.id, serverId)
                    Log.d(TAG, "Loan synced to server with ID: $serverId")
                }
            } else {
                Log.e(TAG, "Failed to sync loan: ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing loan to server", e)
        }
    }

    suspend fun syncUnsyncedLoans(): Result<Int> {
        return withContext(Dispatchers.IO) {
            try {
                val userId = getUserId()
                if (userId == -1) {
                    return@withContext Result.failure(Exception("User not logged in"))
                }

                val unsyncedLoans = loanDao.getUnsyncedLoans(userId)
                var syncedCount = 0

                unsyncedLoans.forEach { loan ->
                    try {
                        val items = loanDao.getLoanItems(loan.id)
                        syncLoanToServer(loan, items)
                        syncedCount++
                    } catch (e: Exception) {
                        Log.e(TAG, "Error syncing loan ${loan.id}", e)
                    }
                }

                Log.d(TAG, "Synced $syncedCount loans to server")
                Result.success(syncedCount)
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing loans", e)
                Result.failure(e)
            }
        }
    }

    suspend fun syncFromServer(): Result<Int> {
        return withContext(Dispatchers.IO) {
            try {
                val authHeader = getAuthToken()
                val userId = getUserId()

                if (authHeader == null || userId == -1) {
                    return@withContext Result.failure(Exception("Authentication required"))
                }

                val response = apiService.getAllPeminjaman(
                    token = authHeader,
                    page = 1,
                    limit = 1000
                )

                if (response.isSuccessful && response.body()?.success == true) {
                    val serverLoans = response.body()?.data ?: emptyList()
                    var syncedCount = 0

                    serverLoans.forEach { apiModel ->
                        try {
                            val existingLoan = apiModel.idPeminjaman?.let {
                                loanDao.getLoanByServerId(it)
                            }

                            val loan = apiModel.toLoanEntity(existingLoan?.id, userId)
                            loanDao.insertLoan(loan)

                            apiModel.barang?.forEach { barang ->
                                val item = barang.toLoanItemEntity(loan.id)
                                loanDao.insertLoanItem(item)
                            }

                            syncedCount++
                        } catch (e: Exception) {
                            Log.e(TAG, "Error syncing loan from server", e)
                        }
                    }

                    Log.d(TAG, "Synced $syncedCount loans from server")
                    Result.success(syncedCount)
                } else {
                    Result.failure(Exception("Failed to fetch loans: ${response.code()}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing from server", e)
                Result.failure(e)
            }
        }
    }

    suspend fun performFullSync(): Result<Pair<Int, Int>> {
        return withContext(Dispatchers.IO) {
            try {
                val toServerResult = syncUnsyncedLoans()
                val toServerCount = toServerResult.getOrDefault(0)

                val fromServerResult = syncFromServer()
                val fromServerCount = fromServerResult.getOrDefault(0)

                Log.d(TAG, "Full sync: to=$toServerCount, from=$fromServerCount")
                Result.success(Pair(toServerCount, fromServerCount))
            } catch (e: Exception) {
                Log.e(TAG, "Error during full sync", e)
                Result.failure(e)
            }
        }
    }
}

/**
 * Data class for loan item info (used when creating loans)
 */
data class LoanItemInfo(
    val inventarisId: Int,
    val namaBarang: String,
    val kodeBarang: String,
    val jumlah: Int,
    val imageUri: String? = null
)
