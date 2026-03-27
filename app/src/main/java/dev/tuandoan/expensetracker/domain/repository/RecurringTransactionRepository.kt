package dev.tuandoan.expensetracker.domain.repository

import dev.tuandoan.expensetracker.domain.model.RecurringTransaction
import kotlinx.coroutines.flow.Flow

interface RecurringTransactionRepository {
    fun observeAll(): Flow<List<RecurringTransaction>>

    suspend fun getById(id: Long): RecurringTransaction?

    suspend fun create(recurring: RecurringTransaction): Long

    suspend fun update(recurring: RecurringTransaction)

    suspend fun delete(id: Long)

    suspend fun setActive(
        id: Long,
        active: Boolean,
    )

    suspend fun processDueRecurring()
}
