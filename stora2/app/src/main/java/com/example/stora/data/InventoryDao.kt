package com.example.stora.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryDao {

    // Get all inventory items for specific user
    @Query("SELECT * FROM inventory_items WHERE isDeleted = 0 AND userId = :userId ORDER BY lastModified DESC")
    fun getAllInventoryItems(userId: Int): Flow<List<InventoryItem>>

    // Get all items as list (for one-time fetch) for specific user
    @Query("SELECT * FROM inventory_items WHERE isDeleted = 0 AND userId = :userId ORDER BY lastModified DESC")
    suspend fun getAllInventoryItemsList(userId: Int): List<InventoryItem>

    // Get inventory item by ID
    @Query("SELECT * FROM inventory_items WHERE id = :id AND isDeleted = 0")
    suspend fun getInventoryItemById(id: String): InventoryItem?

    // Get inventory item by server ID
    @Query("SELECT * FROM inventory_items WHERE serverId = :serverId AND isDeleted = 0")
    suspend fun getInventoryItemByServerId(serverId: Int): InventoryItem?

    // Search inventory items for specific user
    @Query("SELECT * FROM inventory_items WHERE isDeleted = 0 AND userId = :userId AND (name LIKE '%' || :query || '%' OR noinv LIKE '%' || :query || '%') ORDER BY lastModified DESC")
    fun searchInventoryItems(query: String, userId: Int): Flow<List<InventoryItem>>

    // Get items that need to be synced for specific user
    @Query("SELECT * FROM inventory_items WHERE userId = :userId AND (needsSync = 1 OR isSynced = 0)")
    suspend fun getUnsyncedItems(userId: Int): List<InventoryItem>

    // Get items that are deleted but not synced for specific user
    @Query("SELECT * FROM inventory_items WHERE userId = :userId AND isDeleted = 1 AND needsSync = 1")
    suspend fun getDeletedUnsyncedItems(userId: Int): List<InventoryItem>

    // Insert inventory item
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInventoryItem(item: InventoryItem): Long

    // Insert multiple inventory items
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInventoryItems(items: List<InventoryItem>)

    // Update inventory item
    @Update
    suspend fun updateInventoryItem(item: InventoryItem)

    // Delete inventory item (hard delete)
    @Delete
    suspend fun deleteInventoryItem(item: InventoryItem)

    // Soft delete inventory item
    @Query("UPDATE inventory_items SET isDeleted = 1, needsSync = 1, lastModified = :timestamp WHERE id = :id")
    suspend fun softDeleteInventoryItem(id: String, timestamp: Long = System.currentTimeMillis())

    // Mark item as synced
    @Query("UPDATE inventory_items SET isSynced = 1, needsSync = 0 WHERE id = :id")
    suspend fun markAsSynced(id: String)

    // Mark item as needs sync
    @Query("UPDATE inventory_items SET needsSync = 1, isSynced = 0 WHERE id = :id")
    suspend fun markAsNeedsSync(id: String)

    // Update server ID after sync
    @Query("UPDATE inventory_items SET serverId = :serverId, isSynced = 1, needsSync = 0 WHERE id = :id")
    suspend fun updateServerId(id: String, serverId: Int)

    // Delete all synced items that are marked as deleted
    @Query("DELETE FROM inventory_items WHERE isDeleted = 1 AND isSynced = 1")
    suspend fun deleteSyncedDeletedItems()

    // Get count of unsynced items for specific user
    @Query("SELECT COUNT(*) FROM inventory_items WHERE userId = :userId AND (needsSync = 1 OR isSynced = 0)")
    suspend fun getUnsyncedCount(userId: Int): Int

    // Clear all inventory items (for testing or reset)
    @Query("DELETE FROM inventory_items")
    suspend fun clearAllInventoryItems()

    // Get inventory by category for specific user
    @Query("SELECT * FROM inventory_items WHERE userId = :userId AND category = :category AND isDeleted = 0 ORDER BY lastModified DESC")
    fun getInventoryByCategory(category: String, userId: Int): Flow<List<InventoryItem>>

    // Get inventory by condition for specific user
    @Query("SELECT * FROM inventory_items WHERE userId = :userId AND condition = :condition AND isDeleted = 0 ORDER BY lastModified DESC")
    fun getInventoryByCondition(condition: String, userId: Int): Flow<List<InventoryItem>>

    // Get total quantity for specific user
    @Query("SELECT SUM(quantity) FROM inventory_items WHERE userId = :userId AND isDeleted = 0")
    suspend fun getTotalQuantity(userId: Int): Int?

    // Check if item exists by noinv for a specific user
    @Query("SELECT COUNT(*) FROM inventory_items WHERE noinv = :noinv AND userId = :userId AND isDeleted = 0")
    suspend fun isNoinvExists(noinv: String, userId: Int): Int
    
    // Check if item exists by noinv for a specific user, excluding a specific item (for updates)
    @Query("SELECT COUNT(*) FROM inventory_items WHERE noinv = :noinv AND userId = :userId AND id != :excludeId AND isDeleted = 0")
    suspend fun isNoinvExistsExcluding(noinv: String, userId: Int, excludeId: String): Int

    // Get inventory item by code (noinv) - synchronous for sync operations
    @Query("SELECT * FROM inventory_items WHERE noinv = :code AND isDeleted = 0 LIMIT 1")
    suspend fun getInventoryItemByCodeSync(code: String): InventoryItem?

    // Delete all items for a specific user (for fresh sync)
    @Query("DELETE FROM inventory_items WHERE userId = :userId")
    suspend fun deleteAllItemsForUser(userId: Int)

    // Get all items including deleted for sync operations
    @Query("SELECT * FROM inventory_items WHERE userId = :userId ORDER BY lastModified DESC")
    suspend fun getAllItemsIncludingDeleted(userId: Int): List<InventoryItem>

    // Get items that are not soft-deleted and have serverId (confirmed on server)
    @Query("SELECT * FROM inventory_items WHERE userId = :userId AND isDeleted = 0 AND serverId IS NOT NULL")
    suspend fun getSyncedItemsWithServerId(userId: Int): List<InventoryItem>
}
