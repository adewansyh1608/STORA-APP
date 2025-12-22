package com.example.stora.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryDao {

    @Query("SELECT * FROM inventory_items WHERE isDeleted = 0 AND userId = :userId ORDER BY lastModified DESC")
    fun getAllInventoryItems(userId: Int): Flow<List<InventoryItem>>

    @Query("SELECT * FROM inventory_items WHERE isDeleted = 0 AND userId = :userId ORDER BY lastModified DESC")
    suspend fun getAllInventoryItemsList(userId: Int): List<InventoryItem>

    @Query("SELECT * FROM inventory_items WHERE id = :id AND isDeleted = 0")
    suspend fun getInventoryItemById(id: String): InventoryItem?

    @Query("SELECT * FROM inventory_items WHERE serverId = :serverId AND isDeleted = 0")
    suspend fun getInventoryItemByServerId(serverId: Int): InventoryItem?

    @Query("SELECT * FROM inventory_items WHERE isDeleted = 0 AND userId = :userId AND (name LIKE '%' || :query || '%' OR noinv LIKE '%' || :query || '%') ORDER BY lastModified DESC")
    fun searchInventoryItems(query: String, userId: Int): Flow<List<InventoryItem>>

    @Query("SELECT * FROM inventory_items WHERE userId = :userId AND (needsSync = 1 OR isSynced = 0)")
    suspend fun getUnsyncedItems(userId: Int): List<InventoryItem>

    @Query("SELECT * FROM inventory_items WHERE userId = :userId AND isDeleted = 1 AND needsSync = 1")
    suspend fun getDeletedUnsyncedItems(userId: Int): List<InventoryItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInventoryItem(item: InventoryItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInventoryItems(items: List<InventoryItem>)

    @Update
    suspend fun updateInventoryItem(item: InventoryItem)

    @Delete
    suspend fun deleteInventoryItem(item: InventoryItem)

    @Query("UPDATE inventory_items SET isDeleted = 1, needsSync = 1, lastModified = :timestamp WHERE id = :id")
    suspend fun softDeleteInventoryItem(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE inventory_items SET isSynced = 1, needsSync = 0 WHERE id = :id")
    suspend fun markAsSynced(id: String)

    @Query("UPDATE inventory_items SET needsSync = 1, isSynced = 0 WHERE id = :id")
    suspend fun markAsNeedsSync(id: String)

    @Query("UPDATE inventory_items SET serverId = :serverId, isSynced = 1, needsSync = 0 WHERE id = :id")
    suspend fun updateServerId(id: String, serverId: Int)

    @Query("DELETE FROM inventory_items WHERE isDeleted = 1 AND isSynced = 1")
    suspend fun deleteSyncedDeletedItems()

    @Query("SELECT COUNT(*) FROM inventory_items WHERE userId = :userId AND (needsSync = 1 OR isSynced = 0)")
    suspend fun getUnsyncedCount(userId: Int): Int

    @Query("DELETE FROM inventory_items")
    suspend fun clearAllInventoryItems()

    @Query("SELECT * FROM inventory_items WHERE userId = :userId AND category = :category AND isDeleted = 0 ORDER BY lastModified DESC")
    fun getInventoryByCategory(category: String, userId: Int): Flow<List<InventoryItem>>

    @Query("SELECT * FROM inventory_items WHERE userId = :userId AND condition = :condition AND isDeleted = 0 ORDER BY lastModified DESC")
    fun getInventoryByCondition(condition: String, userId: Int): Flow<List<InventoryItem>>

    @Query("SELECT SUM(quantity) FROM inventory_items WHERE userId = :userId AND isDeleted = 0")
    suspend fun getTotalQuantity(userId: Int): Int?

    @Query("SELECT COUNT(*) FROM inventory_items WHERE noinv = :noinv AND userId = :userId AND isDeleted = 0")
    suspend fun isNoinvExists(noinv: String, userId: Int): Int
    
    @Query("SELECT COUNT(*) FROM inventory_items WHERE noinv = :noinv AND userId = :userId AND id != :excludeId AND isDeleted = 0")
    suspend fun isNoinvExistsExcluding(noinv: String, userId: Int, excludeId: String): Int

    @Query("SELECT * FROM inventory_items WHERE noinv = :code AND isDeleted = 0 LIMIT 1")
    suspend fun getInventoryItemByCodeSync(code: String): InventoryItem?

    @Query("DELETE FROM inventory_items WHERE userId = :userId")
    suspend fun deleteAllItemsForUser(userId: Int)

    @Query("SELECT * FROM inventory_items WHERE userId = :userId ORDER BY lastModified DESC")
    suspend fun getAllItemsIncludingDeleted(userId: Int): List<InventoryItem>

    @Query("SELECT * FROM inventory_items WHERE userId = :userId AND isDeleted = 0 AND serverId IS NOT NULL")
    suspend fun getSyncedItemsWithServerId(userId: Int): List<InventoryItem>
}
