package dev.tuandoan.expensetracker.repository

import dev.tuandoan.expensetracker.core.util.RecurrenceScheduler
import dev.tuandoan.expensetracker.core.util.TimeProvider
import dev.tuandoan.expensetracker.data.database.TransactionRunner
import dev.tuandoan.expensetracker.data.database.dao.CategoryDao
import dev.tuandoan.expensetracker.data.database.dao.RecurringTransactionDao
import dev.tuandoan.expensetracker.data.database.dao.TransactionDao
import dev.tuandoan.expensetracker.data.database.entity.RecurringTransactionEntity
import dev.tuandoan.expensetracker.domain.model.RecurrenceFrequency
import dev.tuandoan.expensetracker.domain.model.RecurringTransaction
import dev.tuandoan.expensetracker.domain.model.TransactionType
import dev.tuandoan.expensetracker.domain.repository.RecurringTransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecurringTransactionRepositoryImpl
    @Inject
    constructor(
        private val recurringDao: RecurringTransactionDao,
        private val transactionDao: TransactionDao,
        private val categoryDao: CategoryDao,
        private val transactionRunner: TransactionRunner,
        private val recurrenceScheduler: RecurrenceScheduler,
        private val timeProvider: TimeProvider,
        private val zoneId: ZoneId,
    ) : RecurringTransactionRepository {
        override fun observeAll(): Flow<List<RecurringTransaction>> =
            recurringDao.getAll().map { entities ->
                entities.map { entity -> entity.toDomain() }
            }

        override suspend fun create(recurring: RecurringTransaction): Long {
            val now = timeProvider.currentTimeMillis()
            val entity =
                RecurringTransactionEntity(
                    type = recurring.type.toInt(),
                    amount = recurring.amount,
                    currencyCode = recurring.currencyCode,
                    categoryId = recurring.categoryId,
                    note = recurring.note,
                    frequency = recurring.frequency.toInt(),
                    dayOfMonth = recurring.dayOfMonth,
                    dayOfWeek = recurring.dayOfWeek,
                    nextDueMillis = recurring.nextDueMillis,
                    isActive = recurring.isActive,
                    createdAt = now,
                    updatedAt = now,
                )
            return recurringDao.insert(entity)
        }

        override suspend fun update(recurring: RecurringTransaction) {
            val now = timeProvider.currentTimeMillis()
            val entity =
                RecurringTransactionEntity(
                    id = recurring.id,
                    type = recurring.type.toInt(),
                    amount = recurring.amount,
                    currencyCode = recurring.currencyCode,
                    categoryId = recurring.categoryId,
                    note = recurring.note,
                    frequency = recurring.frequency.toInt(),
                    dayOfMonth = recurring.dayOfMonth,
                    dayOfWeek = recurring.dayOfWeek,
                    nextDueMillis = recurring.nextDueMillis,
                    isActive = recurring.isActive,
                    createdAt = now,
                    updatedAt = now,
                )
            recurringDao.update(entity)
        }

        override suspend fun delete(id: Long) {
            recurringDao.deleteById(id)
        }

        override suspend fun setActive(
            id: Long,
            active: Boolean,
        ) {
            val now = timeProvider.currentTimeMillis()
            recurringDao.setActive(id, active, now)
        }

        override suspend fun processDueRecurring() {
            recurrenceScheduler.processDueRecurring(
                recurringDao = recurringDao,
                transactionDao = transactionDao,
                transactionRunner = transactionRunner,
                zoneId = zoneId,
            )
        }

        private suspend fun RecurringTransactionEntity.toDomain(): RecurringTransaction {
            val categoryName =
                categoryId?.let { categoryDao.getById(it)?.name } ?: "Unknown"
            return RecurringTransaction(
                id = id,
                type = TransactionType.fromInt(type),
                amount = amount,
                currencyCode = currencyCode,
                categoryId = categoryId,
                categoryName = categoryName,
                note = note,
                frequency = RecurrenceFrequency.fromInt(frequency),
                dayOfMonth = dayOfMonth,
                dayOfWeek = dayOfWeek,
                nextDueMillis = nextDueMillis,
                isActive = isActive,
            )
        }
    }
