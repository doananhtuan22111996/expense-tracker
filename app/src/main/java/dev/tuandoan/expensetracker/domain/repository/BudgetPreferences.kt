package dev.tuandoan.expensetracker.domain.repository

import kotlinx.coroutines.flow.Flow

interface BudgetPreferences {
    fun getBudget(currencyCode: String): Flow<Long?>

    suspend fun setBudget(
        currencyCode: String,
        amount: Long,
    )

    suspend fun clearBudget(currencyCode: String)

    fun getAllBudgets(): Flow<Map<String, Long>>
}
