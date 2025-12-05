package com.example.stora.data

import androidx.compose.runtime.mutableStateListOf

data class LoanItem(
    val id: Int,
    val name: String,
    val code: String,
    val quantity: Int,
    val borrower: String? = null,
    val borrowDate: String? = null,
    val returnDate: String? = null,
    val actualReturnDate: String? = null,  // Tanggal benar-benar dikembalikan
    val isReturned: Boolean = false,
    val imageUri: String? = null,  // URI gambar saat barang dipinjam
    val returnImageUri: String? = null,  // URI gambar saat barang dikembalikan
    val groupId: Int = -1  // ID untuk mengelompokkan peminjaman dari peminjam yang sama
)

object LoansData {
    // Data untuk items on loan (kosong untuk testing)
    val loansOnLoan = mutableStateListOf<LoanItem>()

    // Data untuk loan history (kosong untuk testing)
    val loansHistory = mutableStateListOf<LoanItem>()
    
    private var nextId = 1
    private var nextGroupId = 1
    private var currentGroupId = -1
    private var lastBorrower = ""
    private var lastBorrowDate = ""
    
    // Menghitung jumlah yang sedang dipinjam untuk item tertentu
    fun getBorrowedQuantity(itemCode: String): Int {
        return loansOnLoan
            .filter { it.code == itemCode && !it.isReturned }
            .sumOf { it.quantity }
    }
    
    // Menghitung available quantity untuk item tertentu
    fun getAvailableQuantity(item: InventoryItem): Int {
        val borrowed = getBorrowedQuantity(item.noinv)
        return (item.quantity - borrowed).coerceAtLeast(0)
    }
    
    fun addLoan(
        name: String,
        code: String,
        quantity: Int,
        borrower: String,
        borrowDate: String,
        returnDate: String,
        imageUri: String? = null
    ) {
        // Check if this is a new group (different borrower or different borrow date)
        if (borrower != lastBorrower || borrowDate != lastBorrowDate) {
            currentGroupId = nextGroupId++
            lastBorrower = borrower
            lastBorrowDate = borrowDate
        }
        
        val newLoan = LoanItem(
            id = nextId++,
            name = name,
            code = code,
            quantity = quantity,
            borrower = borrower,
            borrowDate = borrowDate,
            returnDate = returnDate,
            isReturned = false,
            imageUri = imageUri,
            groupId = currentGroupId
        )
        loansOnLoan.add(0, newLoan) // Add to beginning of list
    }
    
    fun returnLoan(loanId: Int, returnImageUri: String? = null) {
        val loan = loansOnLoan.find { it.id == loanId }
        if (loan != null) {
            // Remove from active loans
            loansOnLoan.remove(loan)
            
            // Get current date for actual return date
            val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
            val currentDate = sdf.format(java.util.Date())
            
            // Add to history with isReturned = true and returnImageUri
            val returnedLoan = loan.copy(
                isReturned = true,
                returnImageUri = returnImageUri,
                actualReturnDate = currentDate
            )
            loansHistory.add(0, returnedLoan)
        }
    }
    
    fun deleteLoanHistory(loanId: Int) {
        val loan = loansHistory.find { it.id == loanId }
        if (loan != null) {
            loansHistory.remove(loan)
        }
    }
}
