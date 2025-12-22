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
    val serverId: Int? = null,
    val userId: Int = -1,
    val isSynced: Boolean = false,
    val isDeleted: Boolean = false,
    val lastModified: Long = System.currentTimeMillis(),
    val needsSync: Boolean = false
)
