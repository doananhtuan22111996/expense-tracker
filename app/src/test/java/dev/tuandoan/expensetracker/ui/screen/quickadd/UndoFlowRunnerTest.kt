package dev.tuandoan.expensetracker.ui.screen.quickadd

import dev.tuandoan.expensetracker.core.notification.NotificationHelper
import dev.tuandoan.expensetracker.domain.analytics.Analytics
import dev.tuandoan.expensetracker.domain.analytics.AnalyticsEvent
import dev.tuandoan.expensetracker.domain.crash.CrashReporter
import dev.tuandoan.expensetracker.domain.model.MonthlyBarPoint
import dev.tuandoan.expensetracker.domain.model.MonthlySummary
import dev.tuandoan.expensetracker.domain.model.Transaction
import dev.tuandoan.expensetracker.domain.model.TransactionType
import dev.tuandoan.expensetracker.domain.repository.TransactionRepository
import dev.tuandoan.expensetracker.domain.widget.WidgetUpdater
import dev.tuandoan.expensetracker.testutil.FakeQuickAddNotificationSurface
import dev.tuandoan.expensetracker.testutil.FakeQuickAddUndoWorkScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [UndoFlowRunner] — the pure-Kotlin 6-step orchestrator
 * extracted from [UndoQuickAddReceiver] so the step-ordering + best-
 * effort error-handling invariants are JVM-testable.
 *
 * The test file scopes coverage to what Notion T4.4 calls out:
 * - Undo deletes the correct transaction id.
 * - Idempotent (running twice with the same id doesn't break anything).
 * - Best-effort step ordering (failure in one step doesn't abort others).
 * - Every step fires in the correct order on the happy path.
 * - Analytics + widget refresh fire at the tail.
 */
class UndoFlowRunnerTest {
    private val notificationSurface = FakeQuickAddNotificationSurface()
    private val workScheduler = FakeQuickAddUndoWorkScheduler()
    private val transactionRepo = RecordingTransactionRepository()
    private val analytics = UndoRecordingAnalytics()
    private val widgetUpdater = RecordingWidgetUpdater()
    private val crashReporter = UndoRecordingCrashReporter()

    private val runner =
        UndoFlowRunner(
            notificationSurface = notificationSurface,
            workScheduler = workScheduler,
            transactionRepository = transactionRepo,
            analytics = analytics,
            widgetUpdater = widgetUpdater,
            crashReporter = crashReporter,
        )

    // --- Happy-path contract (Notion: "Undo deletes correct ID") ---

    @Test
    fun run_deletesCorrectTransactionId() =
        runTest {
            runner.run(transactionId = 42L)
            // This is the Notion-scope headline assertion: the Undo flow
            // must delete EXACTLY the transaction id it was passed.
            assertEquals(listOf(42L), transactionRepo.deletedIds)
        }

    @Test
    fun run_callsAllSixStepsInOrder() =
        runTest {
            runner.run(transactionId = 42L)

            // 1. cancelExpiryAndDismiss
            assertEquals(listOf(42L), workScheduler.cancelExpiryAndDismissCalls)
            // 2. deleteTransaction
            assertEquals(listOf(42L), transactionRepo.deletedIds)
            // 3. updateQuickAddConfirmationToUndone — uses the derived id.
            val expectedNotificationId = NotificationHelper.quickAddNotificationId(42L)
            assertEquals(listOf(expectedNotificationId), notificationSurface.updateToUndoneCalls)
            // 4. scheduleUndoneDismiss with the same notification id + tx id.
            val dismissCall = workScheduler.scheduleUndoneDismissCalls.single()
            assertEquals(42L, dismissCall.transactionId)
            assertEquals(expectedNotificationId, dismissCall.notificationId)
            // 5. TransactionUndone analytics fires.
            assertEquals(listOf(AnalyticsEvent.TransactionUndone), analytics.events)
            // 6. Widget refresh requested.
            assertEquals(1, widgetUpdater.refreshCount)
            // No errors should have been logged on the happy path.
            assertTrue(crashReporter.recordedExceptions.isEmpty())
        }

    @Test
    fun run_firesTransactionUndoneAnalytics() =
        runTest {
            runner.run(transactionId = 42L)
            // Separate from the ordering test so that the
            // TransactionUndone contract breaks with a specific, easy-to-
            // diagnose failure if analytics wiring regresses.
            assertEquals(1, analytics.events.size)
            assertEquals(AnalyticsEvent.TransactionUndone, analytics.events.single())
        }

    // --- Idempotency (Notion: "idempotent") ---

    @Test
    fun run_idempotent_repeatInvocationCompletesWithoutThrowing() =
        runTest {
            // Per Notion, the Undo flow must be idempotent: a second
            // invocation (e.g. system re-delivering the broadcast) must
            // not throw or leave partial state. The recording fakes let
            // us observe that all six steps ran twice without error.
            runner.run(transactionId = 42L)
            runner.run(transactionId = 42L)

            assertEquals(listOf(42L, 42L), transactionRepo.deletedIds)
            assertEquals(listOf(42L, 42L), workScheduler.cancelExpiryAndDismissCalls)
            assertEquals(2, widgetUpdater.refreshCount)
            assertTrue(crashReporter.recordedExceptions.isEmpty())
        }

    // --- Best-effort step ordering ---

    @Test
    fun run_cancelFailure_continuesRemainingSteps() =
        runTest {
            workScheduler.throwOnCancelExpiryAndDismiss = IllegalStateException("WorkManager down")

            runner.run(transactionId = 42L)

            // Despite cancel throwing, steps 2-6 should still run — the
            // user tapped Undo, honour their intent as completely as
            // possible. This is the best-effort step-ordering invariant.
            assertEquals(listOf(42L), transactionRepo.deletedIds)
            assertEquals(1, notificationSurface.updateToUndoneCalls.size)
            assertEquals(1, workScheduler.scheduleUndoneDismissCalls.size)
            assertEquals(1, analytics.events.size)
            assertEquals(1, widgetUpdater.refreshCount)
            // The exception was logged.
            assertEquals(1, crashReporter.recordedExceptions.size)
        }

    @Test
    fun run_deleteFailure_stillPostsUndoneNotification() =
        runTest {
            transactionRepo.throwOnDelete = RuntimeException("DB constraint")

            runner.run(transactionId = 42L)

            // "Undone" update MUST still post even if the delete fails —
            // the user pressed Undo, so the shade should reflect that
            // intent. Documented v1 tradeoff: the transaction may persist
            // if the delete threw.
            assertEquals(1, notificationSurface.updateToUndoneCalls.size)
            assertEquals(1, widgetUpdater.refreshCount)
            assertEquals(1, crashReporter.recordedExceptions.size)
        }

    @Test
    fun run_notifierFailure_stillSchedulesDismiss() =
        runTest {
            notificationSurface.throwOnUpdateToUndone = RuntimeException("notify IPC blew up")

            runner.run(transactionId = 42L)

            // scheduleUndoneDismiss is decoupled from the update-in-place
            // failure; it still runs so a zombie notification (if one
            // eventually shows) will at least auto-dismiss.
            assertEquals(1, workScheduler.scheduleUndoneDismissCalls.size)
            assertEquals(1, analytics.events.size)
            assertEquals(1, widgetUpdater.refreshCount)
            assertEquals(1, crashReporter.recordedExceptions.size)
        }

    @Test
    fun run_widgetRefreshFailure_logsToCrashReporter() =
        runTest {
            widgetUpdater.throwOnRefresh = RuntimeException("widget update failed")

            runner.run(transactionId = 42L)

            // Widget failure is the last step — all prior steps still
            // completed successfully. The user sees the undo reflected in
            // the app; the widget catches up on the next periodic refresh
            // backstop (PR #89).
            assertEquals(1, transactionRepo.deletedIds.size)
            assertEquals(1, analytics.events.size)
            assertEquals(1, crashReporter.recordedExceptions.size)
            assertTrue(
                crashReporter.recordedExceptions
                    .single()
                    .message!!
                    .contains("widget"),
            )
        }
}

// --- Local test fakes ---

private class RecordingTransactionRepository : TransactionRepository {
    val deletedIds: MutableList<Long> = mutableListOf()
    var throwOnDelete: Throwable? = null

    override suspend fun deleteTransaction(id: Long) {
        deletedIds += id
        throwOnDelete?.let { throw it }
    }

    // --- Unused for UndoFlowRunner tests; unsupported so accidental invocation fails loudly ---

    override fun observeTransactions(
        from: Long,
        to: Long,
        filterType: TransactionType?,
    ): Flow<List<Transaction>> = flow { emit(emptyList()) }

    override suspend fun addTransaction(
        type: TransactionType,
        amount: Long,
        categoryId: Long,
        note: String?,
        timestamp: Long,
        currencyCode: String,
    ): Long = throw UnsupportedOperationException()

    override suspend fun updateTransaction(transaction: Transaction) = throw UnsupportedOperationException()

    override suspend fun getTransaction(id: Long): Transaction? = throw UnsupportedOperationException()

    override fun observeMonthlySummary(
        from: Long,
        to: Long,
    ): Flow<MonthlySummary> = throw UnsupportedOperationException()

    override fun searchTransactions(
        from: Long,
        to: Long,
        query: String,
        filterType: TransactionType?,
    ): Flow<List<Transaction>> = throw UnsupportedOperationException()

    override fun searchTransactionsAdvanced(
        from: Long?,
        to: Long?,
        query: String,
        filterType: TransactionType?,
        categoryId: Long?,
    ): Flow<List<Transaction>> = throw UnsupportedOperationException()

    override suspend fun getMonthlyExpenseTotals(
        from: Long,
        to: Long,
        currencyCode: String,
    ): List<MonthlyBarPoint> = throw UnsupportedOperationException()
}

private class UndoRecordingAnalytics : Analytics {
    val events: MutableList<AnalyticsEvent> = mutableListOf()

    override fun logEvent(event: AnalyticsEvent) {
        events += event
    }

    override fun setCollectionEnabled(enabled: Boolean) = Unit
}

private class RecordingWidgetUpdater : WidgetUpdater {
    var refreshCount: Int = 0
    var throwOnRefresh: Throwable? = null

    override suspend fun requestUpdate() {
        refreshCount++
        throwOnRefresh?.let { throw it }
    }
}

private class UndoRecordingCrashReporter : CrashReporter {
    val recordedExceptions: MutableList<Exception> = mutableListOf()

    override fun recordException(e: Exception) {
        recordedExceptions += e
    }

    override fun setCollectionEnabled(enabled: Boolean) = Unit
}
