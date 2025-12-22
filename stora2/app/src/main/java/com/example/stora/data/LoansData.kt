package com.example.stora.data

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList

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
    val status: String? = null,
    val imageUri: String? = null,
    val returnImageUri: String? = null,
    val roomLoanId: String? = null,
    val roomItemId: String? = null
)

object LoansData {
    private var nextId = 1
    private var nextGroupId = 1

    val loansOnLoan: SnapshotStateList<LoanItem> = mutableStateListOf()
    val loansHistory: SnapshotStateList<LoanItem> = mutableStateListOf()

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

    fun startNewLoanGroup() {
        nextGroupId++
    }

    fun returnLoan(loanId: Int, returnImageUri: String? = null) {
        val loan = loansOnLoan.find { it.id == loanId } ?: return
        
        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
        val currentDate = sdf.format(java.util.Date())
        
        val historyItem = loan.copy(
            actualReturnDate = currentDate,
            returnImageUri = returnImageUri ?: loan.returnImageUri
        )
        
        loansOnLoan.remove(loan)
        loansHistory.add(historyItem)
    }

    fun deleteLoanHistory(loanId: Int) {
        loansHistory.removeAll { it.id == loanId }
    }

    fun getBorrowedQuantity(item: InventoryItem): Int {
        return loansOnLoan
            .filter { it.code == item.noinv }
            .sumOf { it.quantity }
    }

    fun getBorrowedQuantityByCode(itemCode: String): Int {
        return loansOnLoan
            .filter { it.code == itemCode }
            .sumOf { it.quantity }
    }

    fun getAvailableQuantity(item: InventoryItem): Int {
        val borrowedQuantity = loansOnLoan
            .filter { it.code == item.noinv }
            .sumOf { it.quantity }
        return item.quantity - borrowedQuantity
    }

    fun getTotalBorrowedQuantity(): Int {
        return loansOnLoan.sumOf { it.quantity }
    }

    fun clearAll() {
        loansOnLoan.clear()
        loansHistory.clear()
        nextId = 1
        nextGroupId = 1
    }
}
