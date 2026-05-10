package dev.tuandoan.expensetracker.ui.screen.quickadd

import dev.tuandoan.expensetracker.core.notification.NotificationHelper
import dev.tuandoan.expensetracker.domain.analytics.Analytics
import dev.tuandoan.expensetracker.domain.analytics.AnalyticsEvent
import dev.tuandoan.expensetracker.domain.crash.CrashReporter
import dev.tuandoan.expensetracker.domain.notification.QuickAddNotificationSurface
import dev.tuandoan.expensetracker.domain.notification.QuickAddUndoWorkScheduler
import dev.tuandoan.expensetracker.domain.repository.TransactionRepository
import dev.tuandoan.expensetracker.domain.widget.WidgetUpdater
import kotlin.coroutines.cancellation.CancellationException

/**
 * Pure-Kotlin orchestrator for the 6-step undo flow. Extracted from
 * [UndoQuickAddReceiver] during T4.4 so the orchestration logic is
 * JVM-unit-testable (see `UndoFlowRunnerTest`).
 *
 * Every step runs in its own try/catch with `CancellationException`
 * re-thrown — the 6 steps run **best-effort in sequence**, each
 * independent. A failure in one step is logged via [crashReporter] but
 * does NOT abort subsequent steps: the user explicitly tapped Undo, we
 * honour the intent as completely as possible even under partial
 * failure.
 *
 * See the full failure matrix in [UndoQuickAddReceiver]'s class KDoc.
 */
internal class UndoFlowRunner(
    private val notificationSurface: QuickAddNotificationSurface,
    private val workScheduler: QuickAddUndoWorkScheduler,
    private val transactionRepository: TransactionRepository,
    private val analytics: Analytics,
    private val widgetUpdater: WidgetUpdater,
    private val crashReporter: CrashReporter,
) {
    @Suppress("TooGenericExceptionCaught")
    suspend fun run(transactionId: Long) {
        val notificationId = NotificationHelper.quickAddNotificationId(transactionId)

        // 1. Cancel the pending expiry + dismiss workers from the original
        //    post. Bounded wait in the scheduler impl — if it times out,
        //    we proceed anyway (the in-memory cancel is immediate).
        try {
            workScheduler.cancelExpiryAndDismiss(transactionId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            crashReporter.recordException(e)
        }

        // 2. Delete the transaction. If this throws, we still continue —
        //    posting "Undone" without actually deleting is confusing but
        //    at least the user's intent is visible. Worst case a reopen
        //    reflects the stale transaction; rare.
        try {
            transactionRepository.deleteTransaction(transactionId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            crashReporter.recordException(e)
        }

        // 3. Post the "Undone" notification (update-in-place on the same id).
        try {
            notificationSurface.updateQuickAddConfirmationToUndone(notificationId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            crashReporter.recordException(e)
        }

        // 4. Schedule the 3-second auto-dismiss for the "Undone" update.
        //    New tag (`quick-add-undone-{txId}`) so this isn't collaterally
        //    cancelled by a future cancelExpiryAndDismiss call.
        try {
            workScheduler.scheduleUndoneDismiss(
                transactionId = transactionId,
                notificationId = notificationId,
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            crashReporter.recordException(e)
        }

        // 5. Fire the parameterless TransactionUndone analytics event
        //    (PR #138). NoOp in debug; release-gated on user consent.
        try {
            analytics.logEvent(AnalyticsEvent.TransactionUndone)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            crashReporter.recordException(e)
        }

        // 6. Kick the widget so the today-total rolls back. Runs last
        //    because it's the heaviest IPC (Glance re-render); failure
        //    here leaves a slightly stale widget until PR #89's
        //    30-minute backstop.
        try {
            widgetUpdater.requestUpdate()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            crashReporter.recordException(e)
        }
    }
}
