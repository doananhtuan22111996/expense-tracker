package dev.tuandoan.expensetracker.data.worker

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for [RecurringTransactionWorker].
 *
 * The worker is a thin delegation layer to [RecurringTransactionRepository.processDueRecurring].
 * Full integration of the recurring processing logic is covered by [RecurrenceSchedulerTest].
 * These tests verify the worker's companion constants used for WorkManager enqueue calls.
 */
class RecurringTransactionWorkerTest {
    @Test
    fun workName_isStableConstant() {
        assertEquals(
            "recurring_transaction_worker",
            RecurringTransactionWorker.WORK_NAME,
        )
    }

    @Test
    fun periodicWorkName_isStableConstant() {
        assertEquals(
            "recurring_transaction_periodic",
            RecurringTransactionWorker.PERIODIC_WORK_NAME,
        )
    }
}
