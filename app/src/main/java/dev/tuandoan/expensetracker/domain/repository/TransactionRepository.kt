package dev.tuandoan.expensetracker.domain.repository

import dev.tuandoan.expensetracker.domain.model.MonthlySummary
import dev.tuandoan.expensetracker.domain.model.Transaction
import dev.tuandoan.expensetracker.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun observeTransactions(
        from: Long,
        to: Long,
        filterType: TransactionType? = null,
    ): Flow<List<Transaction>>

    suspend fun addTransaction(
        type: TransactionType,
        amount: Long,
        categoryId: Long,
        note: String?,
        timestamp: Long,
        currencyCode: String = "VND",
    ): Long

    suspend fun updateTransaction(transaction: Transaction)

    suspend fun deleteTransaction(id: Long)

    suspend fun getTransaction(id: Long): Transaction?

    fun observeMonthlySummary(
        from: Long,
        to: Long,
    ): Flow<MonthlySummary>
}
