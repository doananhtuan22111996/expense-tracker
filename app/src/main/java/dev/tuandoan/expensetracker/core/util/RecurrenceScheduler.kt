package dev.tuandoan.expensetracker.core.util

import dev.tuandoan.expensetracker.data.database.TransactionRunner
import dev.tuandoan.expensetracker.data.database.dao.RecurringTransactionDao
import dev.tuandoan.expensetracker.data.database.dao.TransactionDao
import dev.tuandoan.expensetracker.data.database.entity.TransactionEntity
import dev.tuandoan.expensetracker.domain.model.RecurrenceFrequency
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

        suspend fun processDueRecurring(
            recurringDao: RecurringTransactionDao,
            transactionDao: TransactionDao,
            transactionRunner: TransactionRunner,
            zoneId: ZoneId,
        ) {
            val now = timeProvider.currentTimeMillis()
            val dueItems = recurringDao.getDue(now)
            if (dueItems.isEmpty()) return

            transactionRunner.runInTransaction {
                for (item in dueItems) {
                    val frequency = RecurrenceFrequency.fromInt(item.frequency)
                    // Create the actual transaction
                    transactionDao.insert(
                        TransactionEntity(
                            type = item.type,
                            amount = item.amount,
                            currencyCode = item.currencyCode,
                            categoryId = item.categoryId,
                            note = item.note,
                            timestamp = item.nextDueMillis,
                            createdAt = now,
                            updatedAt = now,
                        ),
                    )
                    // Advance to next due date
                    val nextDue = calculateNextDue(frequency, item.nextDueMillis, zoneId)
                    recurringDao.updateNextDue(item.id, nextDue, now)
                }
            }
        }
    }
