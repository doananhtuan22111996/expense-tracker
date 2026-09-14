package dev.tuandoan.expensetracker.repository

import dev.tuandoan.expensetracker.core.util.RecurrenceScheduler
import dev.tuandoan.expensetracker.core.util.TimeProvider
import dev.tuandoan.expensetracker.data.database.TransactionRunner
import dev.tuandoan.expensetracker.data.database.dao.CategoryDao
import dev.tuandoan.expensetracker.data.database.dao.RecurringTransactionDao
import dev.tuandoan.expensetracker.data.database.dao.TransactionDao
import dev.tuandoan.expensetracker.domain.analytics.Analytics
import dev.tuandoan.expensetracker.domain.analytics.AnalyticsEvent
import dev.tuandoan.expensetracker.domain.analytics.TransactionKind
import dev.tuandoan.expensetracker.domain.analytics.TransactionSource
import dev.tuandoan.expensetracker.domain.model.TransactionType
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.time.ZoneId

/**
 * Pins the T5.4 invariant: `RecurringTransactionRepositoryImpl.processDueRecurring`
 * fires [AnalyticsEvent.TransactionAdded] with [TransactionSource.RECURRING]
 * for every transaction it generates, and fires nothing when no transactions
 * are due.
 */
class RecurringTransactionRepositoryImplAnalyticsTest {
    private val recurringDao: RecurringTransactionDao = mock()
    private val transactionDao: TransactionDao = mock()
    private val categoryDao: CategoryDao = mock()
    private val runner: TransactionRunner = mock()
    private val timeProvider: TimeProvider = mock()
    private val scheduler: RecurrenceScheduler = mock()

    private fun newRepo(analytics: Analytics) =
        RecurringTransactionRepositoryImpl(
            recurringDao = recurringDao,
            transactionDao = transactionDao,
            categoryDao = categoryDao,
            transactionRunner = runner,
            recurrenceScheduler = scheduler,
            timeProvider = timeProvider,
            zoneId = ZoneId.of("UTC"),
            analytics = analytics,
        )

    @Test
    fun processDueRecurring_emptyDueList_firesNoAnalyticsEvents() =
        runTest {
            val analytics: Analytics = mock()
            whenever(scheduler.processDueRecurring(any(), any(), any(), any()))
                .thenReturn(emptyList())

            newRepo(analytics).processDueRecurring()

            verifyNoInteractions(analytics)
        }

    @Test
    fun processDueRecurring_oneExpense_emitsOneTransactionAddedWithRecurringSourceAndExpenseKind() =
        runTest {
            val analytics = RecordingAnalytics()
            whenever(scheduler.processDueRecurring(any(), any(), any(), any()))
                .thenReturn(listOf(TransactionType.EXPENSE))

            newRepo(analytics).processDueRecurring()

            assertEquals(1, analytics.events.size)
            val event = analytics.events[0] as AnalyticsEvent.TransactionAdded
            assertEquals(TransactionKind.EXPENSE, event.type)
            assertEquals(TransactionSource.RECURRING, event.source)
            assertFalse(event.tripAttached)
        }

    @Test
    fun processDueRecurring_mixedTypes_emitsOneEventPerInsertionInOrder_allWithRecurringSource() =
        runTest {
            val analytics = RecordingAnalytics()
            whenever(scheduler.processDueRecurring(any(), any(), any(), any()))
                .thenReturn(
                    listOf(TransactionType.EXPENSE, TransactionType.INCOME, TransactionType.EXPENSE),
                )

            newRepo(analytics).processDueRecurring()

            assertEquals(3, analytics.events.size)
            val kinds =
                analytics.events.map { (it as AnalyticsEvent.TransactionAdded).type }
            assertEquals(
                listOf(TransactionKind.EXPENSE, TransactionKind.INCOME, TransactionKind.EXPENSE),
                kinds,
            )
            val sources =
                analytics.events.map { (it as AnalyticsEvent.TransactionAdded).source }
            assertTrue(sources.all { it == TransactionSource.RECURRING })
            val tripsAttached =
                analytics.events.map { (it as AnalyticsEvent.TransactionAdded).tripAttached }
            assertTrue(tripsAttached.all { !it })
        }

    @Test
    fun processDueRecurring_oneIncome_emitsIncomeKindEvent() =
        runTest {
            val analytics = RecordingAnalytics()
            whenever(scheduler.processDueRecurring(any(), any(), any(), any()))
                .thenReturn(listOf(TransactionType.INCOME))

            newRepo(analytics).processDueRecurring()

            assertEquals(1, analytics.events.size)
            val event = analytics.events[0] as AnalyticsEvent.TransactionAdded
            assertEquals(TransactionKind.INCOME, event.type)
            assertEquals(TransactionSource.RECURRING, event.source)
            assertFalse(event.tripAttached)
        }

    private class RecordingAnalytics : Analytics {
        val events = mutableListOf<AnalyticsEvent>()

        override fun logEvent(event: AnalyticsEvent) {
            events += event
        }

        override fun setCollectionEnabled(enabled: Boolean) = Unit
    }
}
