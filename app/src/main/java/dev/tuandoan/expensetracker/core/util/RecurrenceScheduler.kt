package dev.tuandoan.expensetracker.core.util

import dev.tuandoan.expensetracker.data.database.TransactionRunner
import dev.tuandoan.expensetracker.data.database.dao.RecurringTransactionDao
import dev.tuandoan.expensetracker.data.database.dao.TransactionDao
import dev.tuandoan.expensetracker.data.database.entity.TransactionEntity
import dev.tuandoan.expensetracker.domain.model.RecurrenceFrequency
import dev.tuandoan.expensetracker.domain.model.TransactionType
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecurrenceScheduler
    @Inject
    constructor(
        private val timeProvider: TimeProvider,
    ) {
        fun calculateNextDue(
            frequency: RecurrenceFrequency,
            currentDueMillis: Long,
            zoneId: ZoneId,
        ): Long {
            val current = Instant.ofEpochMilli(currentDueMillis).atZone(zoneId)
            val next =
                when (frequency) {
                    RecurrenceFrequency.DAILY -> current.plusDays(1)
                    RecurrenceFrequency.WEEKLY -> current.plusWeeks(1)
                    RecurrenceFrequency.MONTHLY -> {
                        val nextMonth = current.plusMonths(1)
                        val maxDay = nextMonth.toLocalDate().lengthOfMonth()
                        if (nextMonth.dayOfMonth > maxDay) {
                            nextMonth.withDayOfMonth(maxDay)
                        } else {
                            nextMonth
                        }
                    }
                    RecurrenceFrequency.YEARLY -> current.plusYears(1)
                }
            return next.toInstant().toEpochMilli()
        }

        /**
         * Materializes every due recurring item into a real transaction row and
         * advances its `nextDueMillis`. Returns the [TransactionType] of each
         * inserted transaction, in insertion order — callers (e.g. the repository
         * impl) use this to emit one analytics `transaction_added` event per
         * materialized transaction with `source = RECURRING` (T5.4). Orphaned
         * items (categoryId == null) are skipped — their types do NOT appear in
         * the returned list because no transaction is inserted.
         */
        suspend fun processDueRecurring(
            recurringDao: RecurringTransactionDao,
            transactionDao: TransactionDao,
            transactionRunner: TransactionRunner,
            zoneId: ZoneId,
        ): List<TransactionType> {
            val now = timeProvider.currentTimeMillis()
            val dueItems = recurringDao.getDue(now)
            if (dueItems.isEmpty()) return emptyList()

            val insertedTypes = mutableListOf<TransactionType>()
            transactionRunner.runInTransaction {
                for (item in dueItems) {
                    val frequency = RecurrenceFrequency.fromInt(item.frequency)
                    // Only create transaction if category exists; skip orphaned items
                    val categoryId = item.categoryId
                    if (categoryId != null) {
                        transactionDao.insert(
                            TransactionEntity(
                                type = item.type,
                                amount = item.amount,
                                currencyCode = item.currencyCode,
                                categoryId = categoryId,
                                note = item.note,
                                timestamp = item.nextDueMillis,
                                createdAt = now,
                                updatedAt = now,
                            ),
                        )
                        insertedTypes.add(TransactionType.fromInt(item.type))
                    }
                    // Always advance next due date to prevent infinite re-processing
                    val nextDue = calculateNextDue(frequency, item.nextDueMillis, zoneId)
                    recurringDao.updateNextDue(item.id, nextDue, now)
                }
            }
            return insertedTypes
        }
    }
