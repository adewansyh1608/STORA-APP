package com.example.stora.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "inventory_items")
data class InventoryItem(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val noinv: String,
    val quantity: Int,
    val category: String,
    val condition: String,
    val location: String,
    val description: String,
    val date: String,
    val photoUri: String? = null,
    val serverId: Int? = null, // ID dari backend server
    val userId: Int = -1, // ID user pemilik item (dari login)
    val isSynced: Boolean = false, // Status sinkronisasi
    val isDeleted: Boolean = false, // Soft delete flag
    val lastModified: Long = System.currentTimeMillis(), // Timestamp terakhir diubah
    val needsSync: Boolean = false // Flag untuk data yang perlu di-sync
)
