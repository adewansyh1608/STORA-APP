package com.example.stora.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.stora.data.*
import com.example.stora.network.ApiService
import com.example.stora.utils.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

class InventoryRepository(
    private val inventoryDao: InventoryDao,
    private val apiService: ApiService,
    private val context: Context
) {

    companion object {
        private const val TAG = "InventoryRepository"
    }

    private val tokenManager = TokenManager.getInstance(context)

   
    private fun getAuthToken(): String? {
        return tokenManager.getToken()
    }

    
    private fun getUserId(): Int {
        return tokenManager.getUserId()
    }

   
    private fun uriToFile(uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val file = File(context.cacheDir, "temp_photo_${System.currentTimeMillis()}.jpg")
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

    /**
     * Check if the photo URL is from the server (not a local file URI)
     * Server photos start with http:// or https:// or /uploads/
     */
    private fun isPhotoFromServer(photoUri: String?): Boolean {
        if (photoUri == null) return false
        return photoUri.startsWith("http://") || 
               photoUri.startsWith("https://") || 
               photoUri.startsWith("/uploads/")
    }

    
    private fun createPhotoPart(photoUri: String?): MultipartBody.Part? {
        if (photoUri == null) return null
        
        // Skip if photo is already from server (no need to re-upload)
        if (isPhotoFromServer(photoUri)) {
            Log.d(TAG, "Photo is from server, skipping upload: $photoUri")
            return null
        }
        
        return try {
            val uri = Uri.parse(photoUri)
            val file = uriToFile(uri) ?: return null
            
            // Determine MIME type based on file extension
            val extension = file.extension.lowercase()
            val mimeType = when (extension) {
                "jpg", "jpeg" -> "image/jpeg"
                "png" -> "image/png"
                "gif" -> "image/gif"
                "webp" -> "image/webp"
                else -> "image/jpeg" // Default to jpeg
            }
            
            Log.d(TAG, "Creating photo part: ${file.name}, MIME: $mimeType")
            
            val requestFile = file.asRequestBody(mimeType.toMediaTypeOrNull())
            MultipartBody.Part.createFormData("foto", file.name, requestFile)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating photo part", e)
            null
        }
    }


   
    fun getAllInventoryItems(): Flow<List<InventoryItem>> {
        val userId = getUserId()
        return if (userId != -1) {
            inventoryDao.getAllInventoryItems(userId)
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }

    
    suspend fun getInventoryItemById(id: String): InventoryItem? {
        return withContext(Dispatchers.IO) {
            inventoryDao.getInventoryItemById(id)
        }
    }

    
    fun searchInventoryItems(query: String): Flow<List<InventoryItem>> {
        val userId = getUserId()
        return if (userId != -1) {
            inventoryDao.searchInventoryItems(query, userId)
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }

    
    suspend fun insertInventoryItem(item: InventoryItem): Result<InventoryItem> {
        return withContext(Dispatchers.IO) {
            try {
                val userId = getUserId()
                if (userId == -1) {
                    return@withContext Result.failure(Exception("User not logged in"))
                }

                val authHeader = tokenManager.getAuthHeader()
                
                // If online and have auth, try to create on server first
                if (authHeader != null && isOnline()) {
                    Log.d(TAG, "Online mode: trying to create item on server first")
                    
                    val request = item.toApiRequest(userId)
                    
                    try {
                        val response = if (item.photoUri != null && !isPhotoFromServer(item.photoUri)) {
                            val fotoPart = createPhotoPart(item.photoUri)
                            apiService.createInventoryWithPhoto(
                                token = authHeader,
                                namaBarang = item.name.toRequestBody("text/plain".toMediaTypeOrNull()),
                                kodeBarang = item.noinv.toRequestBody("text/plain".toMediaTypeOrNull()),
                                jumlah = item.quantity.toString().toRequestBody("text/plain".toMediaTypeOrNull()),
                                kategori = item.category.toRequestBody("text/plain".toMediaTypeOrNull()),
                                lokasi = item.location.toRequestBody("text/plain".toMediaTypeOrNull()),
                                kondisi = item.condition.toRequestBody("text/plain".toMediaTypeOrNull()),
                                tanggalPengadaan = request.tanggalPengadaan.toRequestBody("text/plain".toMediaTypeOrNull()),
                                deskripsi = item.description?.toRequestBody("text/plain".toMediaTypeOrNull()),
                                foto = fotoPart
                            )
                        } else {
                            apiService.createInventory(
                                token = authHeader,
                                inventoryRequest = request
                            )
                        }
                        
                        Log.d(TAG, "Server response code: ${response.code()}")
                        
                        if (response.isSuccessful && response.body()?.success == true) {
                            // Server accepted - now save to Room with serverId
                            val serverId = response.body()?.data?.idInventaris
                            val serverPhotoUrl = response.body()?.data?.foto?.firstOrNull()?.foto
                            
                            val itemToSave = item.copy(
                                userId = userId,
                                serverId = serverId,
                                photoUri = serverPhotoUrl ?: item.photoUri,
                                needsSync = false,
                                isSynced = true,
                                lastModified = System.currentTimeMillis()
                            )
                            inventoryDao.insertInventoryItem(itemToSave)
                            Log.d(TAG, "✓ Item created on server and saved locally: ${item.name}")
                            return@withContext Result.success(itemToSave)
                        } else {
                            // Server rejected - return error with message
                            val errorBody = response.errorBody()?.string()
                            Log.e(TAG, "Server rejected: $errorBody")
                            
                            // Parse error message from server response
                            val errorMessage = try {
                                val jsonError = org.json.JSONObject(errorBody ?: "{}")
                                jsonError.optString("message", "Gagal menyimpan data ke server")
                            } catch (e: Exception) {
                                "Gagal menyimpan data ke server (${response.code()})"
                            }
                            
                            return@withContext Result.failure(Exception(errorMessage))
                        }
                    } catch (networkError: Exception) {
                        Log.w(TAG, "Network error, will save locally for later sync: ${networkError.message}")
                        // Network error - fall through to save locally
                    }
                }
                
                // Offline mode or network error - save locally with needsSync flag
                Log.d(TAG, "Saving item locally for later sync")
                val itemWithSyncFlag = item.copy(
                    userId = userId,
                    needsSync = true,
                    isSynced = false,
                    lastModified = System.currentTimeMillis()
                )
                inventoryDao.insertInventoryItem(itemWithSyncFlag)
                Log.d(TAG, "Item inserted locally (will sync later): ${item.name}")
                Result.success(itemWithSyncFlag)
            } catch (e: Exception) {
                Log.e(TAG, "Error inserting item locally", e)
                Result.failure(e)
            }
        }
    }

    
    suspend fun updateInventoryItem(item: InventoryItem): Result<InventoryItem> {
        return withContext(Dispatchers.IO) {
            try {
                val userId = getUserId()
                if (userId == -1) {
                    return@withContext Result.failure(Exception("User not logged in"))
                }

                val itemWithSyncFlag = item.copy(
                    userId = userId,
                    needsSync = true,
                    isSynced = false,
                    lastModified = System.currentTimeMillis()
                )
                inventoryDao.updateInventoryItem(itemWithSyncFlag)
                Log.d(TAG, "Item updated locally: ${item.name}")
                Result.success(itemWithSyncFlag)
            } catch (e: Exception) {
                Log.e(TAG, "Error updating item locally", e)
                Result.failure(e)
            }
        }
    }

    
    suspend fun deleteInventoryItem(id: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                inventoryDao.softDeleteInventoryItem(id, System.currentTimeMillis())
                Log.d(TAG, "Item soft deleted locally: $id")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting item locally", e)
                Result.failure(e)
            }
        }
    }

   
    suspend fun getUnsyncedCount(): Int {
        return withContext(Dispatchers.IO) {
            val userId = getUserId()
            if (userId != -1) {
                inventoryDao.getUnsyncedCount(userId)
            } else {
                0
            }
        }
    }

    
    suspend fun syncFromServer(): Result<Int> {
        return withContext(Dispatchers.IO) {
            try {
                val authHeader = tokenManager.getAuthHeader()
                val userId = getUserId()

                Log.d(TAG, "Auth header: ${authHeader?.take(30)}...")
                Log.d(TAG, "User ID: $userId")

                if (authHeader == null || userId == -1) {
                    Log.e(TAG, "No auth token or user ID, user must login first")
                    return@withContext Result.failure(Exception("Authentication required. Please login."))
                }

                Log.d(TAG, "Starting sync from server with authenticated user ID: $userId")

                val response = apiService.getAllInventory(
                    token = authHeader,
                    page = 1,
                    limit = 1000
                )

                Log.d(TAG, "Sync from server response code: ${response.code()}")

                if (response.isSuccessful) {
                    val responseBody = response.body()
                    Log.d(TAG, "Response body success: ${responseBody?.success}")

                    if (responseBody?.success == true) {
                        val serverItems = responseBody.data ?: emptyList()
                        Log.d(TAG, "Received ${serverItems.size} items from server")

                        // Get all server IDs from response
                        val serverItemIds = serverItems.mapNotNull { it.idInventaris }.toSet()
                        Log.d(TAG, "Server item IDs: $serverItemIds")

                        // Get all local items that have been synced (have serverId)
                        val localSyncedItems = inventoryDao.getSyncedItemsWithServerId(userId)
                        Log.d(TAG, "Local synced items count: ${localSyncedItems.size}")

                        // Find items that exist locally but NOT on server (deleted on server)
                        val itemsToDelete = localSyncedItems.filter { localItem ->
                            localItem.serverId != null && localItem.serverId !in serverItemIds
                        }

                        // Delete local items that were deleted on server
                        var deletedCount = 0
                        itemsToDelete.forEach { item ->
                            try {
                                inventoryDao.deleteInventoryItem(item)
                                deletedCount++
                                Log.d(TAG, "✓ Deleted local item (removed from server): ${item.name}")
                            } catch (e: Exception) {
                                Log.e(TAG, "Error deleting local item ${item.name}", e)
                            }
                        }
                        Log.d(TAG, "Deleted $deletedCount items that were removed from server")

                        // Now add/update items from server
                        var syncedCount = 0
                        serverItems.forEach { apiModel ->
                            try {
                                
                                val existingItem = apiModel.idInventaris?.let {
                                    inventoryDao.getInventoryItemByServerId(it)
                                }

                                val localItem = if (existingItem != null) {
                                   
                                    apiModel.toInventoryItem(existingItem.id, userId)
                                } else {
                                    
                                    apiModel.toInventoryItem(userId = userId)
                                }

                                inventoryDao.insertInventoryItem(localItem)
                                syncedCount++
                            } catch (e: Exception) {
                                Log.e(TAG, "Error syncing item ${apiModel.namaBarang}", e)
                            }
                        }

                        Log.d(TAG, "✓ Sync from server completed: $syncedCount items synced, $deletedCount items deleted")
                        Result.success(syncedCount)
                    } else {
                        val errorMsg = responseBody?.message ?: "Sync failed with success=false"
                        Log.e(TAG, "✗ Sync from server failed: $errorMsg")
                        Result.failure(Exception(errorMsg))
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "✗ Sync from server failed with code ${response.code()}: $errorBody")
                    Result.failure(Exception("HTTP ${response.code()}: $errorBody"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing from server: ${e.message}", e)
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }

    
    suspend fun syncToServer(): Result<Int> {
        return withContext(Dispatchers.IO) {
            try {
                val authHeader = tokenManager.getAuthHeader()
                val userId = getUserId()

                Log.d(TAG, "Auth header: ${authHeader?.take(30)}...")
                Log.d(TAG, "User ID: $userId")

                if (authHeader == null || userId == -1) {
                    Log.e(TAG, "No auth token or user ID, user must login first")
                    return@withContext Result.failure(Exception("Authentication required. Please login."))
                }

                val unsyncedItems = inventoryDao.getUnsyncedItems(userId)
                val deletedItems = inventoryDao.getDeletedUnsyncedItems(userId)

                Log.d(TAG, "Starting sync to server: ${unsyncedItems.size} items, ${deletedItems.size} deleted")
                Log.d(TAG, "Authenticated as user ID: $userId")

                var syncedCount = 0
                var errorCount = 0

                
                deletedItems.forEach { item ->
                    try {
                        if (item.serverId != null) {
                            Log.d(TAG, "Deleting item on server: ${item.name}, serverId: ${item.serverId}")
                            val response = apiService.deleteInventory(
                                token = authHeader,
                                id = item.serverId
                            )
                            Log.d(TAG, "Delete response code: ${response.code()}")
                            if (response.isSuccessful) {
                                inventoryDao.deleteInventoryItem(item)
                                syncedCount++
                                Log.d(TAG, "✓ Deleted item synced: ${item.name}")
                            } else {
                                Log.e(TAG, "✗ Delete failed: ${response.errorBody()?.string()}")
                                errorCount++
                            }
                        } else {
                            
                            inventoryDao.deleteInventoryItem(item)
                            Log.d(TAG, "Local-only item deleted: ${item.name}")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error syncing deleted item ${item.name}", e)
                        errorCount++
                    }
                }

                
                unsyncedItems.filter { !it.isDeleted }.forEach { item ->
                    try {
                        val request = item.toApiRequest(userId)
                        Log.d(TAG, "Preparing to sync item: ${item.name}, hasServerId: ${item.serverId != null}")
                        Log.d(TAG, "Request data: $request")

                        if (item.serverId != null) {
                            
                            Log.d(TAG, "Updating item on server: ${item.name}, serverId: ${item.serverId}")
                            val response = apiService.updateInventory(
                                token = authHeader,
                                id = item.serverId,
                                inventoryRequest = request
                            )
                            Log.d(TAG, "Update response code: ${response.code()}")

                            if (response.isSuccessful && response.body()?.success == true) {
                                inventoryDao.markAsSynced(item.id)
                                syncedCount++
                                Log.d(TAG, "✓ Item updated on server: ${item.name}")
                            } else {
                                val errorBody = response.errorBody()?.string()
                                Log.e(TAG, "✗ Update failed: $errorBody")
                                errorCount++
                            }
                        } else {
                            
                            Log.d(TAG, "Creating new item on server: ${item.name}")
                            Log.d(TAG, "Has photo: ${item.photoUri != null}")
                            Log.d(TAG, "Auth header being sent: ${authHeader.take(30)}...")
                            
                            val response = if (item.photoUri != null) {
                               
                                val fotoPart = createPhotoPart(item.photoUri)
                                
                                apiService.createInventoryWithPhoto(
                                    token = authHeader,
                                    namaBarang = item.name.toRequestBody("text/plain".toMediaTypeOrNull()),
                                    kodeBarang = item.noinv.toRequestBody("text/plain".toMediaTypeOrNull()),
                                    jumlah = item.quantity.toString().toRequestBody("text/plain".toMediaTypeOrNull()),
                                    kategori = item.category.toRequestBody("text/plain".toMediaTypeOrNull()),
                                    lokasi = item.location.toRequestBody("text/plain".toMediaTypeOrNull()),
                                    kondisi = item.condition.toRequestBody("text/plain".toMediaTypeOrNull()),
                                    tanggalPengadaan = request.tanggalPengadaan.toRequestBody("text/plain".toMediaTypeOrNull()),
                                    deskripsi = item.description.toRequestBody("text/plain".toMediaTypeOrNull()),
                                    foto = fotoPart
                                )
                            } else {
                               
                                apiService.createInventory(
                                    token = authHeader,
                                    inventoryRequest = request
                                )
                            }
                            
                            Log.d(TAG, "Create response code: ${response.code()}")

                            if (response.isSuccessful) {
                                val responseBody = response.body()
                                Log.d(TAG, "Create response body: $responseBody")

                                if (responseBody?.success == true) {
                                    val serverId = responseBody.data?.idInventaris
                                    if (serverId != null) {
                                        inventoryDao.updateServerId(item.id, serverId)
                                        syncedCount++
                                        Log.d(TAG, "✓ Item created on server: ${item.name}, serverId: $serverId")
                                    } else {
                                        Log.e(TAG, "✗ Server didn't return ID for: ${item.name}")
                                        errorCount++
                                    }
                                } else {
                                    Log.e(TAG, "✗ Create failed with success=false: ${responseBody?.message}")
                                    errorCount++
                                }
                            } else {
                                val errorBody = response.errorBody()?.string()
                                Log.e(TAG, "✗ Create failed with code ${response.code()}: $errorBody")
                                errorCount++
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error syncing item ${item.name}: ${e.message}", e)
                        e.printStackTrace()
                        errorCount++
                    }
                }

              
                inventoryDao.deleteSyncedDeletedItems()

                Log.d(TAG, "Sync to server completed: $syncedCount items synced, $errorCount errors")

                if (errorCount > 0) {
                    Result.failure(Exception("Synced $syncedCount items, but $errorCount failed"))
                } else {
                    Result.success(syncedCount)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing to server: ${e.message}", e)
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }

    
    suspend fun performFullSync(): Result<Pair<Int, Int>> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Starting full sync...")

                // First, sync local changes to server
                val toServerResult = syncToServer()
                val toServerCount = toServerResult.getOrDefault(0)

                // Then, sync server data to local
                val fromServerResult = syncFromServer()
                val fromServerCount = fromServerResult.getOrDefault(0)

                Log.d(TAG, "Full sync completed: to server=$toServerCount, from server=$fromServerCount")
                Result.success(Pair(toServerCount, fromServerCount))
            } catch (e: Exception) {
                Log.e(TAG, "Error during full sync", e)
                Result.failure(e)
            }
        }
    }

    fun isOnline(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
            as? android.net.ConnectivityManager
        val activeNetwork = connectivityManager?.activeNetworkInfo
        return activeNetwork?.isConnectedOrConnecting == true
    }

   
    suspend fun getTotalQuantity(): Int {
        return withContext(Dispatchers.IO) {
            val userId = getUserId()
            if (userId != -1) {
                inventoryDao.getTotalQuantity(userId) ?: 0
            } else {
                0
            }
        }
    }

   
    fun getInventoryByCategory(category: String): Flow<List<InventoryItem>> {
        val userId = getUserId()
        return if (userId != -1) {
            inventoryDao.getInventoryByCategory(category, userId)
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }

   
    fun getInventoryByCondition(condition: String): Flow<List<InventoryItem>> {
        val userId = getUserId()
        return if (userId != -1) {
            inventoryDao.getInventoryByCondition(condition, userId)
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }

    
    suspend fun isNoinvExists(noinv: String): Boolean {
        return withContext(Dispatchers.IO) {
            inventoryDao.isNoinvExists(noinv) > 0
        }
    }
}
