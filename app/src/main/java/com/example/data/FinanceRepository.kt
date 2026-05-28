package com.example.data

import kotlinx.coroutines.flow.Flow

class FinanceRepository(private val financeDao: FinanceDao) {

    val allMonthBudgets: Flow<List<MonthBudget>> = financeDao.getAllMonthBudgets()
    val allTransactions: Flow<List<Transaction>> = financeDao.getAllTransactions()

    fun getMonthBudgetFlow(monthId: String): Flow<MonthBudget?> {
        return financeDao.getMonthBudgetFlowById(monthId)
    }

    suspend fun getMonthBudget(monthId: String): MonthBudget? {
        return financeDao.getMonthBudgetById(monthId)
    }

    suspend fun insertMonthBudget(budget: MonthBudget) {
        financeDao.insertMonthBudget(budget)
    }

    suspend fun deleteMonthBudget(monthId: String) {
        financeDao.deleteMonthBudgetById(monthId)
        financeDao.deleteTransactionsByMonth(monthId)
    }

    suspend fun deleteAllData() {
        financeDao.deleteAllMonthBudgets()
        financeDao.deleteAllTransactions()
        financeDao.deleteAllLoans()
    }

    fun getTransactionsForMonth(monthId: String): Flow<List<Transaction>> {
        return financeDao.getTransactionsForMonth(monthId)
    }

    fun getTransactionsBetweenDates(start: Long, end: Long): Flow<List<Transaction>> {
        return financeDao.getTransactionsBetweenDates(start, end)
    }

    suspend fun insertTransaction(transaction: Transaction) {
        financeDao.insertTransaction(transaction)
    }

    suspend fun deleteTransaction(transaction: Transaction) {
        financeDao.deleteTransaction(transaction)
    }

    // --- Custom Category Repository Methods ---
    val allCategories: Flow<List<CustomCategory>> = financeDao.getAllCategories()

    suspend fun insertCategory(category: CustomCategory) {
        financeDao.insertCategory(category)
    }

    suspend fun deleteCategory(category: CustomCategory) {
        financeDao.deleteCategory(category)
    }

    // --- Loan Repository Methods ---
    val allLoans: Flow<List<Loan>> = financeDao.getAllLoans()

    fun getLoansForMonth(monthId: String): Flow<List<Loan>> {
        return financeDao.getLoansForMonth(monthId)
    }

    suspend fun insertLoan(loan: Loan) {
        financeDao.insertLoan(loan)
    }

    suspend fun deleteLoan(loan: Loan) {
        financeDao.deleteLoan(loan)
    }
}
