package com.example.stora.data

import com.google.gson.annotations.SerializedName

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

data class InventoryUserData(
    @SerializedName("ID_User")
    val idUser: Int,
    @SerializedName("Nama_User")
    val namaUser: String,
    @SerializedName("Email")
    val email: String?
)

data class FotoData(
    @SerializedName("ID_Foto_Inventaris")
    val idFoto: Int,
    @SerializedName("Foto")
    val foto: String
)

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

fun InventoryItem.toApiRequest(userId: Int): InventoryRequest {
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
        idUser = userId
    )
}

fun InventoryApiModel.toInventoryItem(localId: String? = null, userId: Int): InventoryItem {
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
        description = this.deskripsi ?: "",
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

data class BatchSyncRequest(
    @SerializedName("items")
    val items: List<InventoryRequest>
)

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
