package com.example.stora.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Room Entity untuk menyimpan data peminjaman
 */
@Entity(tableName = "loans")
data class LoanEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val namaPeminjam: String,
    val noHpPeminjam: String = "",
    val tanggalPinjam: String,
    val tanggalKembali: String,
    val tanggalDikembalikan: String? = null,
    val status: String = "Dipinjam", // Menunggu, Dipinjam, Selesai, Terlambat, Ditolak
    val serverId: Int? = null,
    val userId: Int = -1,
    val isSynced: Boolean = false,
    val needsSync: Boolean = false,
    val isDeleted: Boolean = false, // For soft delete during offline mode
    val lastModified: Long = System.currentTimeMillis()
)

/**
 * Room Entity untuk menyimpan detail barang yang dipinjam
 * imageUri untuk foto saat dipinjam, returnImageUri untuk foto saat dikembalikan
 */
@Entity(tableName = "loan_items")
data class LoanItemEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val loanId: String,
    val inventarisId: Int? = null,
    val namaBarang: String,
    val kodeBarang: String,
    val jumlah: Int,
    val imageUri: String? = null,       // Foto kondisi barang saat dipinjam
    val returnImageUri: String? = null, // Foto kondisi barang saat dikembalikan
    val serverId: Int? = null
)

/**
 * Data class untuk menggabungkan Loan dengan item-itemnya
 */
data class LoanWithItems(
    val loan: LoanEntity,
    val items: List<LoanItemEntity>
)

/**
 * Data class untuk display di UI
 */
data class LoanDisplayItem(
    val id: String,
    val borrower: String,
    val borrowerPhone: String,
    val borrowDate: String,
    val returnDate: String,
    val actualReturnDate: String? = null,
    val status: String,
    val totalQuantity: Int,
    val items: List<LoanItemDisplay>,
    val isSynced: Boolean = false
)

data class LoanItemDisplay(
    val id: String,
    val name: String,
    val code: String,
    val quantity: Int,
    val imageUri: String? = null,
    val returnImageUri: String? = null
)

/**
 * Extension function untuk convert LoanWithItems ke LoanDisplayItem
 */
fun LoanWithItems.toDisplayItem(): LoanDisplayItem {
    return LoanDisplayItem(
        id = loan.id,
        borrower = loan.namaPeminjam,
        borrowerPhone = loan.noHpPeminjam,
        borrowDate = loan.tanggalPinjam,
        returnDate = loan.tanggalKembali,
        actualReturnDate = loan.tanggalDikembalikan,
        status = loan.status,
        totalQuantity = items.sumOf { it.jumlah },
        items = items.map { 
            LoanItemDisplay(
                id = it.id,
                name = it.namaBarang,
                code = it.kodeBarang,
                quantity = it.jumlah,
                imageUri = it.imageUri,
                returnImageUri = it.returnImageUri
            )
        },
        isSynced = loan.isSynced
    )
}
