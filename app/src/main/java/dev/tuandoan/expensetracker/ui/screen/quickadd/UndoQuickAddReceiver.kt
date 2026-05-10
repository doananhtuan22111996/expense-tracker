package dev.tuandoan.expensetracker.ui.screen.quickadd

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

/**
 * Broadcast target of the v3.12.0 quick-add confirmation notification's
 * **Undo** action. Thin wrapper around [UndoFlowRunner] — all 6-step
 * orchestration logic lives there so it's JVM-unit-testable (T4.4).
 *
 * Receiver-layer responsibilities (non-testable without Robolectric):
 * 1. Validate `EXTRA_TRANSACTION_ID` extra (rejects null / 0 / negative).
 * 2. Call `goAsync()` to extend past the 10-second onReceive deadline.
 * 3. Spin up a one-shot `CoroutineScope(SupervisorJob() + Dispatchers.IO)`.
 * 4. Pull dependencies via [UndoQuickAddEntryPoint] (Hilt `@EntryPoint`
 *    for process-death-safe singleton access).
 * 5. Run the 6-step flow via `UndoFlowRunner.run(transactionId)`.
 * 6. `pendingResult.finish()` in `finally` regardless of outcome.
 *
 * Manifest declares `exported="false"` so this receiver is reachable only
 * via our own in-process PendingIntent (built by
 * `NotificationHelper.showQuickAddConfirmation`).
 */
class UndoQuickAddReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val transactionId = intent.getLongExtra(EXTRA_TRANSACTION_ID, INVALID_TX_ID)
        if (transactionId <= 0L) return

        val pendingResult = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val entry = UndoQuickAddEntryPoint.get(context.applicationContext)

        val runner =
            UndoFlowRunner(
                notificationSurface = entry.quickAddNotificationSurface(),
                workScheduler = entry.quickAddUndoWorkScheduler(),
                transactionRepository = entry.transactionRepository(),
                analytics = entry.analytics(),
                widgetUpdater = entry.widgetUpdater(),
                crashReporter = entry.crashReporter(),
            )

        scope.launch {
            @Suppress("TooGenericExceptionCaught")
            try {
                runner.run(transactionId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                entry.crashReporter().recordException(e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        /**
         * Intent extra carrying the transaction id to undo. Validated in
         * `onReceive`: null / 0 / negative values are rejected and the
         * undo is dropped with no side effects.
         */
        const val EXTRA_TRANSACTION_ID: String = "transaction_id"

        /**
         * Unique intent action for the Undo button. Including an explicit
         * action (not just extras) keeps the PendingIntent distinguishable
         * from other broadcasts targeting the same receiver class.
         */
        const val ACTION_UNDO: String = "dev.tuandoan.expensetracker.action.UNDO_QUICK_ADD"

        private const val INVALID_TX_ID = -1L
    }
}
