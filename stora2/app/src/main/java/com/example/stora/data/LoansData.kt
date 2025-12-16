package com.example.stora.data

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList

/**
 * Data class for loan items used in the in-memory loan management
 */
data class LoanItem(
    val id: Int,
    val groupId: Int,
    val name: String,
    val code: String,
    val quantity: Int,
    val borrower: String? = null,
    val borrowerPhone: String? = null,
    val borrowDate: String? = null,
    val returnDate: String? = null,
    val actualReturnDate: String? = null,
    val status: String? = null,  // Dipinjam, Selesai, Terlambat
    val imageUri: String? = null,
    val returnImageUri: String? = null,
    val roomLoanId: String? = null,
    val roomItemId: String? = null
)

/**
 * In-memory loan data management object
 * Used for backward compatibility with existing UI screens
 */
object LoansData {
    private var nextId = 1
    private var nextGroupId = 1

    // Lists untuk menyimpan data peminjaman
    val loansOnLoan: SnapshotStateList<LoanItem> = mutableStateListOf()
    val loansHistory: SnapshotStateList<LoanItem> = mutableStateListOf()

    /**
     * Add a new loan item
     */
    fun addLoan(
        name: String,
        code: String,
        quantity: Int,
        borrower: String,
        borrowerPhone: String = "",
        borrowDate: String,
        returnDate: String,
        imageUri: String? = null,
        roomLoanId: String? = null,
        roomItemId: String? = null
    ): LoanItem {
        val loanItem = LoanItem(
            id = nextId++,
            groupId = nextGroupId,
            name = name,
            code = code,
            quantity = quantity,
            borrower = borrower,
            borrowerPhone = borrowerPhone,
            borrowDate = borrowDate,
            returnDate = returnDate,
            imageUri = imageUri,
            roomLoanId = roomLoanId,
            roomItemId = roomItemId
        )
        loansOnLoan.add(loanItem)
        return loanItem
    }

    /**
     * Start a new loan group (call before adding multiple items for same borrower)
     */
    fun startNewLoanGroup() {
        nextGroupId++
    }

    /**
     * Return a loan - moves from loansOnLoan to loansHistory
     */
    fun returnLoan(loanId: Int, returnImageUri: String? = null) {
        val loan = loansOnLoan.find { it.id == loanId } ?: return
        
        // Get current date as actual return date
        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
        val currentDate = sdf.format(java.util.Date())
        
        // Create history item with actual return date and return image
        val historyItem = loan.copy(
            actualReturnDate = currentDate,
            returnImageUri = returnImageUri ?: loan.returnImageUri
        )
        
        // Remove from on-loan and add to history
        loansOnLoan.remove(loan)
        loansHistory.add(historyItem)
    }

    /**
     * Delete a loan from history
     */
    fun deleteLoanHistory(loanId: Int) {
        loansHistory.removeAll { it.id == loanId }
    }

    /**
     * Get available quantity for an inventory item
     * (total quantity - currently on loan)
     */
    fun getAvailableQuantity(item: InventoryItem): Int {
        val borrowedQuantity = loansOnLoan
            .filter { it.code == item.noinv }
            .sumOf { it.quantity }
        return item.quantity - borrowedQuantity
    }

    /**
     * Get total borrowed quantity for all loans
     */
    fun getTotalBorrowedQuantity(): Int {
        return loansOnLoan.sumOf { it.quantity }
    }

    /**
     * Clear all loan data (for testing/debugging)
     */
    fun clearAll() {
        loansOnLoan.clear()
        loansHistory.clear()
        nextId = 1
        nextGroupId = 1
    }
}
