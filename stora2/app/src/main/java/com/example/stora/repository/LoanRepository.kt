package com.example.stora.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.stora.data.*
import com.example.stora.network.ApiService
import com.example.stora.utils.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

class LoanRepository(
    private val loanDao: LoanDao,
    private val inventoryDao: InventoryDao,
    private val apiService: ApiService,
    private val context: Context
) {
    companion object {
        private const val TAG = "LoanRepository"
    }

    private val tokenManager = TokenManager.getInstance(context)

    private fun getAuthToken(): String? = tokenManager.getAuthHeader()
    private fun getUserId(): Int = tokenManager.getUserId()

    // Check if device is online
    fun isOnline(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    // Get count of unsynced loans
    suspend fun getUnsyncedLoansCount(): Int {
        return withContext(Dispatchers.IO) {
            val userId = getUserId()
            if (userId == -1) return@withContext 0
            loanDao.getUnsyncedLoans(userId).size
        }
    }

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

                // Create loan items with proper inventarisId lookup
                val loanItems = items.map { item ->
                    // If inventarisId is 0 or invalid, try to look it up from inventory by kodeBarang
                    val actualInventarisId = if (item.inventarisId > 0) {
                        item.inventarisId
                    } else {
                        // Look up serverId from inventory table by kodeBarang
                        val inventory = inventoryDao.getInventoryItemByCodeSync(item.kodeBarang)
                        inventory?.serverId ?: 0
                    }
                    
                    Log.d(TAG, "Creating LoanItemEntity: name=${item.namaBarang}, inventarisId=${item.inventarisId} -> actualId=$actualInventarisId, kodeBarang=${item.kodeBarang}")
                    
                    LoanItemEntity(
                        loanId = loan.id,
                        inventarisId = if (actualInventarisId > 0) actualInventarisId else null,
                        namaBarang = item.namaBarang,
                        kodeBarang = item.kodeBarang,
                        jumlah = item.jumlah,
                        imageUri = item.imageUri
                    )
                }

                loanDao.insertLoanWithItems(loan, loanItems)
                Log.d(TAG, "Loan saved to Room: ${loan.id} with ${loanItems.size} items")

                // Try to sync to server
                Log.d(TAG, "Attempting to sync loan to server...")
                syncLoanToServer(loan, loanItems)

                Result.success(LoanWithItems(loan, loanItems))
            } catch (e: Exception) {
                Log.e(TAG, "Error creating loan", e)
                Result.failure(e)
            }
        }
    }

    suspend fun returnLoan(loanId: String, returnDateTime: String, itemReturnImages: Map<String, String?>): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val loan = loanDao.getLoanById(loanId)
                    ?: return@withContext Result.failure(Exception("Loan not found"))

                // Use the provided return date/time
                val returnDate = returnDateTime
                
                // Parse date for comparison (support both "dd/MM/yyyy" and "dd/MM/yyyy HH:mm" formats)
                val dateOnlyFormat = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                val dateTimeFormat = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
                
                // Determine if late based on due date
                // Selesai = returned on or before deadline
                // Terlambat = returned after deadline
                val status = try {
                    val returnCal = java.util.Calendar.getInstance()
                    val returnParsed = try {
                        dateTimeFormat.parse(returnDateTime)
                    } catch (e: Exception) {
                        dateOnlyFormat.parse(returnDateTime)
                    }
                    if (returnParsed != null) {
                        returnCal.time = returnParsed
                    }
                    
                    val dueDate = java.util.Calendar.getInstance()
                    val dueDateParsed = try {
                        dateTimeFormat.parse(loan.tanggalKembali)
                    } catch (e: Exception) {
                        dateOnlyFormat.parse(loan.tanggalKembali)
                    }
                    if (dueDateParsed != null) {
                        dueDate.time = dueDateParsed
                        // If due date has no time component, set it to end of day
                        if (!loan.tanggalKembali.contains(":")) {
                            dueDate.set(java.util.Calendar.HOUR_OF_DAY, 23)
                            dueDate.set(java.util.Calendar.MINUTE, 59)
                            dueDate.set(java.util.Calendar.SECOND, 59)
                        }
                    }
                    
                    if (dueDateParsed != null && returnCal.after(dueDate)) {
                        Log.d(TAG, "Return is LATE: return=$returnDateTime, deadline=${loan.tanggalKembali}")
                        "Terlambat"
                    } else {
                        Log.d(TAG, "Return is ON TIME: return=$returnDateTime, deadline=${loan.tanggalKembali}")
                        "Selesai"
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing date, defaulting to Selesai", e)
                    "Selesai" // Default to Selesai if date parsing fails
                }

                // Update loan status in Room
                loanDao.updateLoanStatus(
                    loanId = loanId,
                    status = status,
                    returnDate = returnDate,
                    lastModified = System.currentTimeMillis()
                )

                // Update each item's return image in Room
                itemReturnImages.forEach { (itemId, returnImageUri) ->
                    if (returnImageUri != null) {
                        loanDao.updateLoanItemReturnImage(itemId, returnImageUri)
                        Log.d(TAG, "Updated return image for item $itemId: $returnImageUri")
                    }
                }

                Log.d(TAG, "Loan returned in Room: $loanId with status: $status")

                // Try to sync to server
                loan.serverId?.let { serverId ->
                    try {
                        val authHeader = getAuthToken()
                        if (authHeader != null) {
                            // Convert date format for API (dd/MM/yyyy HH:mm to yyyy-MM-dd HH:mm:ss)
                            val apiDateTimeFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                            val apiDateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                            val parsedDate = try {
                                dateTimeFormat.parse(returnDate)
                            } catch (e: Exception) {
                                dateOnlyFormat.parse(returnDate)
                            }
                            val apiFormattedDate = parsedDate?.let { 
                                if (returnDate.contains(":")) {
                                    apiDateTimeFormat.format(it)
                                } else {
                                    apiDateFormat.format(it)
                                }
                            } ?: returnDate
                            
                            // Update peminjaman status
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
                            
                            // Upload return photos to server
                            val loanItems = loanDao.getLoanItems(loanId)
                            val photosToUpload = mutableListOf<MultipartBody.Part>()
                            val photoMappingList = mutableListOf<Map<String, Any?>>()
                            
                            loanItems.forEachIndexed { index, item ->
                                val returnUri = itemReturnImages[item.id]
                                if (returnUri != null) {
                                    val photoPart = createPhotoPart(returnUri, index)
                                    if (photoPart != null) {
                                        photosToUpload.add(photoPart)
                                        photoMappingList.add(mapOf(
                                            "ID_Peminjaman_Barang" to item.serverId,
                                            "index" to index
                                        ))
                                    }
                                }
                            }
                            
                            if (photosToUpload.isNotEmpty()) {
                                val photoMappingJson = com.google.gson.Gson().toJson(photoMappingList)
                                val photoMappingBody = photoMappingJson.toRequestBody("text/plain".toMediaTypeOrNull())
                                
                                val photoResponse = apiService.uploadReturnPhotos(
                                    token = authHeader,
                                    id = serverId,
                                    photoMapping = photoMappingBody,
                                    photos = photosToUpload
                                )
                                
                                if (photoResponse.isSuccessful) {
                                    Log.d(TAG, "Return photos uploaded to server: ${photosToUpload.size} photos")
                                } else {
                                    Log.e(TAG, "Failed to upload return photos: ${photoResponse.errorBody()?.string()}")
                                }
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

    suspend fun deleteLoanHistory(loanId: String, serverId: Int? = null): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // Get serverId from loan if not provided
                val actualServerId = serverId ?: loanDao.getLoanById(loanId)?.serverId
                
                // Try to delete from server first
                actualServerId?.let { sid ->
                    try {
                        val authHeader = getAuthToken()
                        if (authHeader != null) {
                            val response = apiService.deletePeminjaman(
                                token = authHeader,
                                id = sid
                            )
                            if (!response.isSuccessful) {
                                if (response.code() == 404) {
                                    Log.d(TAG, "Loan already deleted from server (404)")
                                } else {
                                    val errorMsg = response.errorBody()?.string() ?: response.message()
                                    Log.e(TAG, "Failed to delete from server: $errorMsg")
                                    throw Exception("Gagal menghapus dari server: $errorMsg")
                                }
                            } else {
                                Log.d(TAG, "Loan deleted from server: $sid")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error deleting from server", e)
                        throw e
                    }
                }
                
                // Delete from Room
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

    // Helper: Convert URI to File
    private fun uriToFile(uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val file = File(context.cacheDir, "temp_loan_photo_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(file)
            
            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            
            file
        } catch (e: Exception) {
            Log.e(TAG, "Error converting URI to File", e)
            null
        }
    }

    // Helper: Create photo MultipartBody.Part
    private fun createPhotoPart(photoUri: String?, index: Int): MultipartBody.Part? {
        if (photoUri == null) return null
        
        return try {
            val uri = Uri.parse(photoUri)
            val file = uriToFile(uri) ?: return null
            
            val mimeType = "image/jpeg"
            val requestFile = file.asRequestBody(mimeType.toMediaTypeOrNull())
            MultipartBody.Part.createFormData("photos", "photo_$index.jpg", requestFile)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating photo part", e)
            null
        }
    }

    // Helper: Sync inventory item to server and return serverId
    private suspend fun syncInventoryItemToServer(kodeBarang: String, authHeader: String): Int? {
        try {
            // Get inventory item from local database
            val inventoryItem = inventoryDao.getInventoryItemByCodeSync(kodeBarang) ?: return null
            
            // If already has serverId, return it
            if (inventoryItem.serverId != null && inventoryItem.serverId > 0) {
                Log.d(TAG, "Inventory item $kodeBarang already has serverId: ${inventoryItem.serverId}")
                return inventoryItem.serverId
            }
            
            // Need to sync this inventory item first
            Log.d(TAG, "Syncing inventory item $kodeBarang to server...")
            
            val userId = getUserId()
            val request = inventoryItem.toApiRequest(userId)
            
            val response = apiService.createInventory(
                token = authHeader,
                inventoryRequest = request
            )
            
            if (response.isSuccessful && response.body()?.success == true) {
                val serverId = response.body()?.data?.idInventaris
                if (serverId != null) {
                    // Update local inventory with serverId
                    inventoryDao.updateServerId(inventoryItem.id, serverId)
                    inventoryDao.markAsSynced(inventoryItem.id)
                    Log.d(TAG, "Inventory item $kodeBarang synced with serverId: $serverId")
                    return serverId
                }
            } else {
                Log.e(TAG, "Failed to sync inventory item $kodeBarang: ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing inventory item $kodeBarang", e)
        }
        return null
    }

    private suspend fun syncLoanToServer(loan: LoanEntity, items: List<LoanItemEntity>) {
        try {
            Log.d(TAG, "=== SYNC LOAN TO SERVER ===")
            Log.d(TAG, "Loan ID: ${loan.id}, Borrower: ${loan.namaPeminjam}")
            Log.d(TAG, "Items count: ${items.size}")
            items.forEach { item ->
                Log.d(TAG, "  - Item: ${item.namaBarang}, inventarisId: ${item.inventarisId}, kodeBarang: ${item.kodeBarang}, imageUri: ${item.imageUri}")
            }
            
            val authHeader = getAuthToken()
            if (authHeader == null) {
                Log.e(TAG, "No auth token available - cannot sync")
                return
            }
            Log.d(TAG, "Auth token present: ${authHeader.take(20)}...")

            // Try to get serverId from inventory for items that don't have inventarisId
            // If inventory doesn't have serverId, try to sync it first
            val itemsWithServerId = items.map { item ->
                if (item.inventarisId != null && item.inventarisId > 0) {
                    item
                } else {
                    // First check if inventory item has serverId locally
                    val inventory = inventoryDao.getInventoryItemByCodeSync(item.kodeBarang)
                    if (inventory?.serverId != null && inventory.serverId > 0) {
                        Log.d(TAG, "Found serverId ${inventory.serverId} for item ${item.kodeBarang} from inventory")
                        item.copy(inventarisId = inventory.serverId)
                    } else {
                        // Try to sync this inventory item to server first
                        Log.d(TAG, "Attempting to sync inventory item ${item.kodeBarang} to server first...")
                        val syncedServerId = syncInventoryItemToServer(item.kodeBarang, authHeader)
                        if (syncedServerId != null) {
                            Log.d(TAG, "Successfully synced inventory item ${item.kodeBarang}, got serverId: $syncedServerId")
                            item.copy(inventarisId = syncedServerId)
                        } else {
                            Log.w(TAG, "Failed to get serverId for item ${item.kodeBarang}")
                            item
                        }
                    }
                }
            }

            val validItems = itemsWithServerId.filter { it.inventarisId != null && it.inventarisId > 0 }
            Log.d(TAG, "Valid items (with inventarisId > 0): ${validItems.size}")
            
            if (validItems.isEmpty()) {
                Log.e(TAG, "No valid items to sync - all inventory items failed to sync")
                Log.e(TAG, "HINT: Check if inventory items exist in Room database and have correct data")
                return
            }

            // Check if any items have photos
            val itemsWithPhotos = validItems.filter { it.imageUri != null }
            
            if (itemsWithPhotos.isNotEmpty()) {
                // Use multipart endpoint with photos
                Log.d(TAG, "Syncing loan with ${itemsWithPhotos.size} photos")
                
                val dateOnlyFormat = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                val dateTimeFormat = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
                val apiDateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                val apiDateTimeFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                
                val tanggalPinjamApi = try {
                    val parsed = if (loan.tanggalPinjam.contains(":")) {
                        dateTimeFormat.parse(loan.tanggalPinjam)
                    } else {
                        dateOnlyFormat.parse(loan.tanggalPinjam)
                    }
                    parsed?.let { 
                        if (loan.tanggalPinjam.contains(":")) apiDateTimeFormat.format(it) else apiDateFormat.format(it)
                    } ?: loan.tanggalPinjam
                } catch (e: Exception) { loan.tanggalPinjam }
                
                val tanggalKembaliApi = try {
                    val parsed = if (loan.tanggalKembali.contains(":")) {
                        dateTimeFormat.parse(loan.tanggalKembali)
                    } else {
                        dateOnlyFormat.parse(loan.tanggalKembali)
                    }
                    parsed?.let { 
                        if (loan.tanggalKembali.contains(":")) apiDateTimeFormat.format(it) else apiDateFormat.format(it)
                    } ?: loan.tanggalKembali
                } catch (e: Exception) { loan.tanggalKembali }
                
                // Build barangList JSON
                val barangListJson = com.google.gson.Gson().toJson(
                    validItems.map { item ->
                        mapOf(
                            "ID_Inventaris" to item.inventarisId,
                            "Jumlah" to item.jumlah
                        )
                    }
                )
                
                // Create photo parts
                val photoParts = validItems.mapIndexedNotNull { index, item ->
                    createPhotoPart(item.imageUri, index)
                }
                
                val response = apiService.createPeminjamanWithPhotos(
                    token = authHeader,
                    namaPeminjam = loan.namaPeminjam.toRequestBody("text/plain".toMediaTypeOrNull()),
                    noHpPeminjam = loan.noHpPeminjam.toRequestBody("text/plain".toMediaTypeOrNull()),
                    tanggalPinjam = tanggalPinjamApi.toRequestBody("text/plain".toMediaTypeOrNull()),
                    tanggalKembali = tanggalKembaliApi.toRequestBody("text/plain".toMediaTypeOrNull()),
                    idUser = loan.userId.toString().toRequestBody("text/plain".toMediaTypeOrNull()),
                    barangList = barangListJson.toRequestBody("text/plain".toMediaTypeOrNull()),
                    photos = photoParts.ifEmpty { null }
                )
                
                if (response.isSuccessful && response.body()?.success == true) {
                    val serverData = response.body()?.data
                    val serverId = serverData?.idPeminjaman
                    if (serverId != null) {
                        loanDao.updateLoanServerId(loan.id, serverId)
                        Log.d(TAG, "Loan with photos synced to server with ID: $serverId")
                        
                        // Update item serverIds from response
                        serverData.barang?.forEachIndexed { index, barang ->
                            if (index < items.size && barang.idPeminjamanBarang != null) {
                                loanDao.updateLoanItemServerId(items[index].id, barang.idPeminjamanBarang)
                                Log.d(TAG, "Updated item ${items[index].id} with serverId: ${barang.idPeminjamanBarang}")
                            }
                        }
                    }
                } else {
                    Log.e(TAG, "Failed to sync loan with photos: ${response.errorBody()?.string()}")
                }
            } else {
                // Use JSON endpoint without photos
                val request = loan.toApiRequest(validItems)
                Log.d(TAG, "Syncing loan to server (no photos): $request")

                val response = apiService.createPeminjaman(
                    token = authHeader,
                    request = request
                )

                if (response.isSuccessful && response.body()?.success == true) {
                    val serverData = response.body()?.data
                    val serverId = serverData?.idPeminjaman
                    if (serverId != null) {
                        loanDao.updateLoanServerId(loan.id, serverId)
                        Log.d(TAG, "Loan synced to server with ID: $serverId")
                        
                        // Update item serverIds from response
                        serverData.barang?.forEachIndexed { index, barang ->
                            if (index < items.size && barang.idPeminjamanBarang != null) {
                                loanDao.updateLoanItemServerId(items[index].id, barang.idPeminjamanBarang)
                                Log.d(TAG, "Updated item ${items[index].id} with serverId: ${barang.idPeminjamanBarang}")
                            }
                        }
                    }
                } else {
                    Log.e(TAG, "Failed to sync loan: ${response.errorBody()?.string()}")
                }
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

                            // Get existing loan items to preserve local image URIs
                            val existingItems = loanDao.getLoanItems(loan.id)
                            val existingItemsByServerId = existingItems.associateBy { it.serverId }
                            val existingItemsByInventarisId = existingItems.associateBy { it.inventarisId }

                            // Delete existing loan items for this loan before inserting new ones
                            // This prevents duplication when syncing from server
                            loanDao.deleteLoanItemsByLoanId(loan.id)

                            apiModel.barang?.forEach { barang ->
                                val item = barang.toLoanItemEntity(loan.id, com.example.stora.network.ApiConfig.SERVER_URL)
                                
                                // Preserve local image URIs from existing items if server doesn't have them
                                // First try matching by serverId, then by inventarisId
                                val existingItem = existingItemsByServerId[barang.idPeminjamanBarang]
                                    ?: existingItemsByInventarisId[barang.idInventaris]
                                
                                val itemWithImages = if (existingItem != null) {
                                    item.copy(
                                        imageUri = item.imageUri ?: existingItem.imageUri,
                                        returnImageUri = item.returnImageUri ?: existingItem.returnImageUri
                                    )
                                } else {
                                    item
                                }
                                
                                loanDao.insertLoanItem(itemWithImages)
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
