package com.example.stora.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LoanDao {
    
    // ==================== LOAN QUERIES ====================
    
    @Query("SELECT * FROM loans WHERE userId = :userId AND status NOT IN ('Selesai', 'Terlambat') AND isDeleted = 0 ORDER BY lastModified DESC")
    fun getActiveLoans(userId: Int): Flow<List<LoanEntity>>
    
    @Query("SELECT * FROM loans WHERE userId = :userId AND status IN ('Selesai', 'Terlambat') AND isDeleted = 0 ORDER BY lastModified DESC")
    fun getLoanHistory(userId: Int): Flow<List<LoanEntity>>
    
    @Query("SELECT * FROM loans WHERE userId = :userId ORDER BY lastModified DESC")
    fun getAllLoans(userId: Int): Flow<List<LoanEntity>>
    
    @Query("SELECT * FROM loans WHERE id = :loanId")
    suspend fun getLoanById(loanId: String): LoanEntity?
    
    @Query("SELECT * FROM loans WHERE serverId = :serverId")
    suspend fun getLoanByServerId(serverId: Int): LoanEntity?

    @Query("SELECT * FROM loans WHERE userId = :userId AND needsSync = 1")
    suspend fun getUnsyncedLoans(userId: Int): List<LoanEntity>

    @Query("SELECT COUNT(*) FROM loans WHERE userId = :userId AND needsSync = 1")
    suspend fun getUnsyncedLoanCount(userId: Int): Int

    // Get deleted loans that need to be synced to server
    @Query("SELECT * FROM loans WHERE userId = :userId AND isDeleted = 1 AND serverId IS NOT NULL")
    suspend fun getDeletedLoansToSync(userId: Int): List<LoanEntity>
    
    // Soft delete a loan (mark as deleted for sync later)
    @Query("UPDATE loans SET isDeleted = 1, needsSync = 1, lastModified = :lastModified WHERE id = :loanId")
    suspend fun softDeleteLoan(loanId: String, lastModified: Long)
    
    // Update loan deadline and mark for sync
    @Query("UPDATE loans SET tanggalKembali = :deadline, needsSync = 1, lastModified = :lastModified WHERE id = :loanId")
    suspend fun updateLoanDeadline(loanId: String, deadline: String, lastModified: Long)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoan(loan: LoanEntity)
    
    @Update
    suspend fun updateLoan(loan: LoanEntity)
    
    @Delete
    suspend fun deleteLoan(loan: LoanEntity)

    @Query("UPDATE loans SET isSynced = 1, needsSync = 0 WHERE id = :loanId")
    suspend fun markLoanAsSynced(loanId: String)

    @Query("UPDATE loans SET serverId = :serverId, isSynced = 1, needsSync = 0 WHERE id = :loanId")
    suspend fun updateLoanServerId(loanId: String, serverId: Int)

    @Query("UPDATE loans SET status = :status, tanggalDikembalikan = :returnDate, needsSync = 1, lastModified = :lastModified WHERE id = :loanId")
    suspend fun updateLoanStatus(loanId: String, status: String, returnDate: String?, lastModified: Long)

    // ==================== LOAN ITEM QUERIES ====================
    
    @Query("SELECT * FROM loan_items WHERE loanId = :loanId")
    suspend fun getLoanItems(loanId: String): List<LoanItemEntity>
    
    @Query("SELECT * FROM loan_items WHERE loanId = :loanId")
    fun getLoanItemsFlow(loanId: String): Flow<List<LoanItemEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoanItem(item: LoanItemEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoanItems(items: List<LoanItemEntity>)
    
    @Update
    suspend fun updateLoanItem(item: LoanItemEntity)
    
    @Delete
    suspend fun deleteLoanItem(item: LoanItemEntity)
    
    @Query("DELETE FROM loan_items WHERE loanId = :loanId")
    suspend fun deleteLoanItemsByLoanId(loanId: String)

    @Query("UPDATE loan_items SET returnImageUri = :returnImageUri WHERE id = :itemId")
    suspend fun updateLoanItemReturnImage(itemId: String, returnImageUri: String?)

    @Query("UPDATE loan_items SET serverId = :serverId WHERE id = :itemId")
    suspend fun updateLoanItemServerId(itemId: String, serverId: Int)

    // ==================== COMBINED QUERIES ====================
    
    @Transaction
    suspend fun insertLoanWithItems(loan: LoanEntity, items: List<LoanItemEntity>) {
        insertLoan(loan)
        insertLoanItems(items)
    }

    @Transaction
    suspend fun deleteLoanWithItems(loanId: String) {
        deleteLoanItemsByLoanId(loanId)
        getLoanById(loanId)?.let { deleteLoan(it) }
    }

    // Get borrowed quantity for an item (active loans only)
    @Query("""
        SELECT COALESCE(SUM(li.jumlah), 0) 
        FROM loan_items li 
        INNER JOIN loans l ON li.loanId = l.id 
        WHERE li.kodeBarang = :itemCode 
        AND l.status IN ('Dipinjam', 'Menunggu', 'Terlambat')
    """)
    suspend fun getBorrowedQuantity(itemCode: String): Int

    // ==================== CLEAR ALL DATA ====================
    
    @Query("DELETE FROM loans")
    suspend fun clearAllLoans()
    
    @Query("DELETE FROM loan_items")
    suspend fun clearAllLoanItems()
    
    @Transaction
    suspend fun clearAllLoanData() {
        clearAllLoanItems()
        clearAllLoans()
    }
}
