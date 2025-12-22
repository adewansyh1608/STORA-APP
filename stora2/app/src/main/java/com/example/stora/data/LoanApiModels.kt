package com.example.stora.data

import com.google.gson.annotations.SerializedName

data class LoanApiResponse<T>(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("message")
    val message: String? = null,
    @SerializedName("data")
    val data: T? = null,
    @SerializedName("pagination")
    val pagination: LoanPagination? = null
)

data class LoanPagination(
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

data class LoanApiModel(
    @SerializedName("ID_Peminjaman")
    val idPeminjaman: Int?,
    @SerializedName("Nama_Peminjam")
    val namaPeminjam: String?,
    @SerializedName("NoHP_Peminjam")
    val noHpPeminjam: String?,
    @SerializedName("Tanggal_Pinjam")
    val tanggalPinjam: String?,
    @SerializedName("Tanggal_Kembali")
    val tanggalKembali: String?,
    @SerializedName("Status")
    val status: String?,
    @SerializedName("ID_User")
    val idUser: Int?,
    @SerializedName("isSynced")
    val isSynced: Boolean?,
    @SerializedName("barang")
    val barang: List<LoanBarangApiModel>? = null,
    @SerializedName("user")
    val user: LoanUserApiModel? = null
)

data class LoanBarangApiModel(
    @SerializedName("ID_Peminjaman_Barang")
    val idPeminjamanBarang: Int?,
    @SerializedName("ID_Peminjaman")
    val idPeminjaman: Int?,
    @SerializedName("ID_Inventaris")
    val idInventaris: Int?,
    @SerializedName("Jumlah")
    val jumlah: Int?,
    @SerializedName("inventaris")
    val inventaris: LoanInventarisApiModel? = null,
    @SerializedName("foto")
    val foto: List<FotoApiModel>? = null
)

data class FotoApiModel(
    @SerializedName("ID_Foto_Peminjaman")
    val idFotoPeminjaman: Int?,
    @SerializedName("ID_Peminjaman_Barang")
    val idPeminjamanBarang: Int?,
    @SerializedName("Foto_Peminjaman")
    val fotoPeminjaman: String?,
    @SerializedName("Foto_Pengembalian")
    val fotoPengembalian: String?
)

data class LoanInventarisApiModel(
    @SerializedName("ID_Inventaris")
    val idInventaris: Int?,
    @SerializedName("Nama_Barang")
    val namaBarang: String?,
    @SerializedName("Kode_Barang")
    val kodeBarang: String?,
    @SerializedName("Kondisi")
    val kondisi: String?
)

data class LoanUserApiModel(
    @SerializedName("ID_User")
    val idUser: Int?,
    @SerializedName("Nama_User")
    val namaUser: String?,
    @SerializedName("Email")
    val email: String?
)

data class LoanCreateRequest(
    @SerializedName("Nama_Peminjam")
    val namaPeminjam: String,
    @SerializedName("NoHP_Peminjam")
    val noHpPeminjam: String,
    @SerializedName("Tanggal_Pinjam")
    val tanggalPinjam: String,
    @SerializedName("Tanggal_Kembali")
    val tanggalKembali: String,
    @SerializedName("ID_User")
    val idUser: Int,
    @SerializedName("barangList")
    val barangList: List<LoanBarangRequest>
)

data class LoanBarangRequest(
    @SerializedName("ID_Inventaris")
    val idInventaris: Int,
    @SerializedName("Jumlah")
    val jumlah: Int
)

data class LoanStatusUpdateRequest(
    @SerializedName("Status")
    val status: String,
    @SerializedName("Tanggal_Dikembalikan")
    val tanggalDikembalikan: String? = null,
    @SerializedName("Tanggal_Kembali")
    val tanggalKembali: String? = null
)

data class LoanUpdateRequest(
    @SerializedName("Tanggal_Kembali")
    val tanggalKembali: String? = null,
    @SerializedName("barangList")
    val barangList: List<LoanBarangRequest>? = null
)

fun LoanApiModel.toLoanEntity(existingId: String? = null, userId: Int): LoanEntity {
    return LoanEntity(
        id = existingId ?: java.util.UUID.randomUUID().toString(),
        namaPeminjam = namaPeminjam ?: "",
        noHpPeminjam = noHpPeminjam ?: "",
        tanggalPinjam = tanggalPinjam ?: "",
        tanggalKembali = tanggalKembali ?: "",
        status = status ?: "Dipinjam",
        serverId = idPeminjaman,
        userId = userId,
        isSynced = true,
        needsSync = false,
        lastModified = System.currentTimeMillis()
    )
}

fun LoanBarangApiModel.toLoanItemEntity(loanId: String, baseUrl: String = ""): LoanItemEntity {
    val itemId = idPeminjamanBarang?.let { "server_item_$it" } 
        ?: java.util.UUID.randomUUID().toString()
    
    val fotoItem = foto?.firstOrNull()
    
    val borrowPhotoUrl = fotoItem?.fotoPeminjaman?.let { 
        if (it.startsWith("http")) it else "$baseUrl$it"
    }
    val returnPhotoUrl = fotoItem?.fotoPengembalian?.let {
        if (it.startsWith("http")) it else "$baseUrl$it"
    }
    
    return LoanItemEntity(
        id = itemId,
        loanId = loanId,
        inventarisId = idInventaris,
        namaBarang = inventaris?.namaBarang ?: "",
        kodeBarang = inventaris?.kodeBarang ?: "",
        jumlah = jumlah ?: 0,
        serverId = idPeminjamanBarang,
        imageUri = borrowPhotoUrl,
        returnImageUri = returnPhotoUrl
    )
}

fun LoanEntity.toApiRequest(items: List<LoanItemEntity>): LoanCreateRequest {
    return LoanCreateRequest(
        namaPeminjam = namaPeminjam,
        noHpPeminjam = noHpPeminjam,
        tanggalPinjam = convertDateFormat(tanggalPinjam),
        tanggalKembali = convertDateFormat(tanggalKembali),
        idUser = userId,
        barangList = items.mapNotNull { item ->
            item.inventarisId?.let { invId ->
                LoanBarangRequest(
                    idInventaris = invId,
                    jumlah = item.jumlah
                )
            }
        }
    )
}

private fun convertDateFormat(date: String): String {
    return try {
        if (date.contains(":")) {
            val inputFormat = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
            val outputFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
            val parsedDate = inputFormat.parse(date)
            parsedDate?.let { outputFormat.format(it) } ?: date
        } else {
            val inputFormat = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
            val outputFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val parsedDate = inputFormat.parse(date)
            parsedDate?.let { outputFormat.format(it) } ?: date
        }
    } catch (e: Exception) {
        date
    }
}
