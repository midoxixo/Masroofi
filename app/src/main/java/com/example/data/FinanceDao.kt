package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FinanceDao {
    // --- Month Budget Queries ---
    @Query("SELECT * FROM month_budgets ORDER BY monthId DESC")
    fun getAllMonthBudgets(): Flow<List<MonthBudget>>

    @Query("SELECT * FROM month_budgets WHERE monthId = :monthId LIMIT 1")
    suspend fun getMonthBudgetById(monthId: String): MonthBudget?

    @Query("SELECT * FROM month_budgets WHERE monthId = :monthId LIMIT 1")
    fun getMonthBudgetFlowById(monthId: String): Flow<MonthBudget?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMonthBudget(budget: MonthBudget)

    @Query("DELETE FROM month_budgets WHERE monthId = :monthId")
    suspend fun deleteMonthBudgetById(monthId: String)

    // --- Transaction Queries ---
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE monthId = :monthId ORDER BY timestamp DESC")
    fun getTransactionsForMonth(monthId: String): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE timestamp >= :startTimestamp AND timestamp <= :endTimestamp ORDER BY timestamp DESC")
    fun getTransactionsBetweenDates(startTimestamp: Long, endTimestamp: Long): Flow<List<Transaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction)

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

    @Query("DELETE FROM transactions WHERE monthId = :monthId")
    suspend fun deleteTransactionsByMonth(monthId: String)

    // --- Custom Category Queries ---
    @Query("SELECT * FROM custom_categories ORDER BY isSystem DESC, name ASC")
    fun getAllCategories(): Flow<List<CustomCategory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CustomCategory)

    @Delete
    suspend fun deleteCategory(category: CustomCategory)

    // --- Loan Queries ---
    @Query("SELECT * FROM loans ORDER BY timestamp DESC")
    fun getAllLoans(): Flow<List<Loan>>

    @Query("SELECT * FROM loans WHERE monthId = :monthId ORDER BY timestamp DESC")
    fun getLoansForMonth(monthId: String): Flow<List<Loan>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoan(loan: Loan)

    @Delete
    suspend fun deleteLoan(loan: Loan)
}
