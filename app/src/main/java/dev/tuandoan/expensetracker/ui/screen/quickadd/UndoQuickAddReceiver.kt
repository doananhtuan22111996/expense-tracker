package dev.tuandoan.expensetracker.ui.screen.quickadd

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.workDataOf
import dev.tuandoan.expensetracker.core.notification.NotificationHelper
import dev.tuandoan.expensetracker.data.worker.QuickAddNotifierWorker
import dev.tuandoan.expensetracker.domain.analytics.AnalyticsEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import kotlin.coroutines.cancellation.CancellationException

/**
 * Broadcast target of the v3.12.0 quick-add confirmation notification's
 * **Undo** action. Implements the full undo flow (T4.3).
 *
 * When the user taps Undo within the 10-second window, this receiver:
 * 1. Validates the `EXTRA_TRANSACTION_ID` extra (rejects null / 0 / negative —
 *    drops silently, no user-facing error).
 * 2. `WorkManager.cancelAllWorkByTag("quick-add-notify-{txId}")` — cancels
 *    the pending 10s expiry worker AND the 30s auto-dismiss worker in one
 *    call (per ADR-012's shared-tag scheme). Awaited so the cancel
 *    completes before we re-post the notification.
 * 3. `TransactionRepository.deleteTransaction(txId)` — the actual undo.
 * 4. Posts "Undone" notification update on the same notification id
 *    (OS update-in-place) via
 *    [NotificationHelper.updateQuickAddConfirmationToUndone].
 * 5. Schedules a fresh 3-second dismiss worker with a NEW tag
 *    (`quick-add-undone-{txId}`) — separate from step 2's cancelled tag
 *    so this dismiss isn't collateral-damage if step 2 were ever called
 *    twice.
 * 6. Fires `AnalyticsEvent.TransactionUndone` (parameterless, from PR #138).
 * 7. Kicks `WidgetUpdater.requestUpdate()` so the widget's today-total
 *    rolls back within ~1s.
 *
 * ### Why `goAsync` + our own CoroutineScope
 * `BroadcastReceiver.onReceive` runs on the main thread with a ~10-second
 * deadline before ANR. `goAsync()` lets the receiver run longer while
 * the async work proceeds off the main thread. The returned
 * `PendingResult.finish()` tells Android we're done.
 *
 * `CoroutineScope(SupervisorJob() + Dispatchers.IO)` is deliberately
 * unstructured — the receiver has no parent scope, and the work is
 * one-shot. `SupervisorJob` so a failure in one step doesn't cancel
 * the others we try to complete best-effort.
 *
 * ### Dependency injection via `@EntryPoint`
 * BroadcastReceivers can't use `@AndroidEntryPoint`, so we pull the 5
 * singletons we need directly off the application graph via
 * [UndoQuickAddEntryPoint]. Process-death-safe — works even if the user
 * taps Undo after Android memory-killed the app.
 *
 * ### Best-effort step ordering
 * Each numbered step above runs in its own try/catch. A failure in one
 * step logs via `CrashReporter` but does NOT abort the subsequent steps:
 * the user tapped Undo, so we honour the intent as completely as we can
 * even in the face of partial failure. Worst realistic case: DB delete
 * succeeds but widget refresh fails → transaction gone, widget stale for
 * up to 30 minutes until the periodic refresh backstop runs (PR #89).
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

        scope.launch {
            try {
                runUndoFlow(
                    appContext = context.applicationContext,
                    transactionId = transactionId,
                    entry = entry,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                entry.crashReporter().recordException(e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    /**
     * Executes the seven-step undo flow. Extracted from [onReceive] so the
     * step-ordering is readable top-to-bottom without the `goAsync`/scope
     * scaffolding muddling the sequence.
     */
    @Suppress("TooGenericExceptionCaught")
    private suspend fun runUndoFlow(
        appContext: Context,
        transactionId: Long,
        entry: UndoQuickAddEntryPoint,
    ) {
        val notificationId = NotificationHelper.quickAddNotificationId(transactionId)
        val workManager = WorkManager.getInstance(appContext)

        // 1. Cancel the pending expiry + dismiss workers from the original
        //    post. Awaited so the 10s expiry can't race with the "Undone"
        //    update we post in step 4. Bounded timeout: `ListenableFuture.get`
        //    with no timeout would block until `goAsync`'s 10s ANR window
        //    fires. 2s is well below that and leaves >7s for remaining steps.
        try {
            workManager
                .cancelAllWorkByTag(QuickAddNotifierWorker.tag(transactionId))
                .result
                .get(CANCEL_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Includes TimeoutException — in that case we proceed anyway:
            // the cancel's in-memory effect is immediate even if the DB
            // commit hasn't finished, so step 4's update will usually win.
            entry.crashReporter().recordException(e)
        }

        // 2. Delete the transaction. If this throws, we still continue —
        //    the user tapped Undo; posting "Undone" without actually
        //    deleting is confusing but at least the intent is visible.
        //    Worst case a second Undo attempt or a reopen of the Home
        //    screen will reflect the stale transaction; rare.
        try {
            entry.transactionRepository().deleteTransaction(transactionId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            entry.crashReporter().recordException(e)
        }

        // 3. Post the "Undone" notification (update-in-place on the same id).
        try {
            entry.notificationHelper().updateQuickAddConfirmationToUndone(notificationId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            entry.crashReporter().recordException(e)
        }

        // 4. Schedule the 3-second auto-dismiss for the "Undone" update.
        //    New tag (`quick-add-undone-{txId}`) so this isn't accidentally
        //    cancelled by a future cancelAllWorkByTag(quick-add-notify-...)
        //    call targeting the original tag.
        try {
            val dismissWork =
                OneTimeWorkRequestBuilder<QuickAddNotifierWorker>()
                    .setInitialDelay(UNDONE_DISMISS_SECONDS, TimeUnit.SECONDS)
                    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    .setInputData(
                        workDataOf(
                            QuickAddNotifierWorker.KEY_NOTIFICATION_ID to notificationId,
                            QuickAddNotifierWorker.KEY_MODE to
                                QuickAddNotifierWorker.Mode.DISMISS.name,
                        ),
                    ).addTag(undoneDismissTag(transactionId))
                    .build()
            workManager.enqueue(dismissWork)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            entry.crashReporter().recordException(e)
        }

        // 5. Fire the parameterless TransactionUndone analytics event (PR #138).
        //    NoOp in debug; in release, gated on the user's analytics-events
        //    consent per ADR-011.
        try {
            entry.analytics().logEvent(AnalyticsEvent.TransactionUndone)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            entry.crashReporter().recordException(e)
        }

        // 6. Kick the widget so the today-total rolls back. Runs last
        //    because it's the heaviest IPC (Glance re-render); failure here
        //    leaves a slightly stale widget until PR #89's 30-minute backstop.
        try {
            entry.widgetUpdater().requestUpdate()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            entry.crashReporter().recordException(e)
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
        private const val UNDONE_DISMISS_SECONDS: Long = 3L

        /**
         * Maximum time to wait for [WorkManager.cancelAllWorkByTag] to commit
         * its cancel to SQLite. Set well below `goAsync`'s 10-second ANR
         * budget. If the cancel times out, the in-memory scheduler cancel
         * still takes effect immediately — the persist is just eventually
         * consistent.
         */
        private const val CANCEL_TIMEOUT_MS: Long = 2_000L

        /**
         * Tag for the 3-second dismiss worker scheduled after the "Undone"
         * update. Separate from [QuickAddNotifierWorker.tag] so the original
         * post's shared tag cancel in step 1 doesn't collaterally kill this
         * dismiss.
         */
        internal fun undoneDismissTag(transactionId: Long): String = "quick-add-undone-$transactionId"
    }
}
