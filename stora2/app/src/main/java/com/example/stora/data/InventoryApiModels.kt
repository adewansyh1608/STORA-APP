package com.example.stora.data

import com.google.gson.annotations.SerializedName

// Response wrapper dari backend
data class InventoryApiResponse<T>(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("data")
    val data: T?,
    @SerializedName("message")
    val message: String?,
    @SerializedName("pagination")
    val pagination: PaginationData?
)

// Data pagination
data class PaginationData(
    @SerializedName("currentPage")
    val currentPage: Int,
    @SerializedName("totalPages")
    val totalPages: Int,
    @SerializedName("totalItems")
    val totalItems: Int,
    @SerializedName("hasNext")
    val hasNext: Boolean,
    @SerializedName("hasPrev")
    val hasPrev: Boolean
)

// Model inventory dari API (sesuai dengan backend)
data class InventoryApiModel(
    @SerializedName("ID_Inventaris")
    val idInventaris: Int?,
    @SerializedName("Nama_Barang")
    val namaBarang: String,
    @SerializedName("Kode_Barang")
    val kodeBarang: String,
    @SerializedName("Jumlah")
    val jumlah: Int,
    @SerializedName("Kategori")
    val kategori: String,
    @SerializedName("Lokasi")
    val lokasi: String,
    @SerializedName("Kondisi")
    val kondisi: String,
    @SerializedName("Tanggal_Pengadaan")
    val tanggalPengadaan: String,
    @SerializedName("Deskripsi")
    val deskripsi: String?,
    @SerializedName("ID_User")
    val idUser: Int?,
    @SerializedName("isSynced")
    val isSynced: Boolean?,
    @SerializedName("createdAt")
    val createdAt: String?,
    @SerializedName("updatedAt")
    val updatedAt: String?,
    @SerializedName("user")
    val user: InventoryUserData?,
    @SerializedName("foto")
    val foto: List<FotoData>?
)

// User data dari API
data class InventoryUserData(
    @SerializedName("ID_User")
    val idUser: Int,
    @SerializedName("Nama_User")
    val namaUser: String,
    @SerializedName("Email")
    val email: String?
)

// Foto data dari API
data class FotoData(
    @SerializedName("ID_Foto_Inventaris")
    val idFoto: Int,
    @SerializedName("Foto")
    val foto: String
)

// Request untuk create/update inventory
data class InventoryRequest(
    @SerializedName("Nama_Barang")
    val namaBarang: String,
    @SerializedName("Kode_Barang")
    val kodeBarang: String,
    @SerializedName("Jumlah")
    val jumlah: Int,
    @SerializedName("Kategori")
    val kategori: String,
    @SerializedName("Lokasi")
    val lokasi: String,
    @SerializedName("Kondisi")
    val kondisi: String,
    @SerializedName("Tanggal_Pengadaan")
    val tanggalPengadaan: String,
    @SerializedName("Deskripsi")
    val deskripsi: String?,
    @SerializedName("ID_User")
    val idUser: Int? = null
)

// Extension functions untuk konversi antara InventoryItem dan InventoryApiModel
fun InventoryItem.toApiRequest(userId: Int): InventoryRequest {
    // Convert date from "dd/MM/yyyy" to "yyyy-MM-dd" format
    val apiDate = try {
        val parts = this.date.split("/")
        if (parts.size == 3) {
            "${parts[2]}-${parts[1].padStart(2, '0')}-${parts[0].padStart(2, '0')}"
        } else {
            this.date
        }
    } catch (e: Exception) {
        this.date
    }

    return InventoryRequest(
        namaBarang = this.name,
        kodeBarang = this.noinv,
        jumlah = this.quantity,
        kategori = this.category,
        lokasi = this.location,
        kondisi = this.condition,
        tanggalPengadaan = apiDate,
        deskripsi = this.description,
        idUser = userId // Use real user ID from token
    )
}

fun InventoryApiModel.toInventoryItem(localId: String? = null, userId: Int): InventoryItem {
    // Construct full photo URL if the path is relative (from server)
    val fullPhotoUrl = this.foto?.firstOrNull()?.foto?.let { photoPath ->
        if (photoPath.startsWith("/uploads/")) {
            "${com.example.stora.network.ApiConfig.SERVER_URL}$photoPath"
        } else {
            photoPath
        }
    }
    
    return InventoryItem(
        id = localId ?: java.util.UUID.randomUUID().toString(),
        name = this.namaBarang,
        noinv = this.kodeBarang,
        quantity = this.jumlah,
        category = this.kategori,
        condition = this.kondisi,
        location = this.lokasi,
        description = this.deskripsi ?: "", // Get description from backend
        date = this.tanggalPengadaan,
        photoUri = fullPhotoUrl,
        serverId = this.idInventaris,
        userId = userId,
        isSynced = true,
        isDeleted = false,
        lastModified = System.currentTimeMillis(),
        needsSync = false
    )
}

// Batch sync request
data class BatchSyncRequest(
    @SerializedName("items")
    val items: List<InventoryRequest>
)

// Batch sync response
data class BatchSyncResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("synced")
    val synced: Int,
    @SerializedName("failed")
    val failed: Int,
    @SerializedName("results")
    val results: List<SyncResult>
)

data class SyncResult(
    @SerializedName("localId")
    val localId: String?,
    @SerializedName("serverId")
    val serverId: Int?,
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("message")
    val message: String?
)
