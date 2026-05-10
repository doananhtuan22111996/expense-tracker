package dev.tuandoan.expensetracker.ui.screen.quickadd

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Broadcast target of the v3.12.0 quick-add confirmation notification's
 * **Undo** action. Stub in T4.2 — T4.3 fills in the body.
 *
 * ### T4.2 (this PR) — stub only
 * Receives the `EXTRA_TRANSACTION_ID` extra dispatched via the Undo
 * action's PendingIntent (`QuickAddConfirmationNotifier` builds it).
 * Currently a no-op so the PendingIntent has a valid target; the manifest
 * entry declares `exported="false"` so the receiver is reachable only
 * via our own in-process PendingIntent.
 *
 * ### T4.3 (next PR) — adds the full body
 * When the user taps Undo within 10s, the receiver will:
 * 1. Validate the `EXTRA_TRANSACTION_ID` extra (reject null / 0 / negative).
 * 2. `WorkManager.cancelAllWorkByTag("quick-add-notify-{txId}")` to cancel
 *    both the 10s expiry worker and the 30s dismiss worker in one call
 *    (per ADR-012's shared-tag scheme).
 * 3. `TransactionRepository.deleteTransaction(txId)` — the actual undo.
 * 4. Post an "Undone" notification update + 3s auto-dismiss worker.
 * 5. Fire `AnalyticsEvent.TransactionUndone` (parameterless, from PR #138).
 * 6. Kick `WidgetUpdater.requestUpdate()` so the widget's today-total
 *    rolls back within ~1s.
 *
 * Dependency injection for T4.3 is via `Hilt @EntryPoint` (BroadcastReceivers
 * aren't `@AndroidEntryPoint`-supported directly and the lifecycle is too
 * short for a regular Hilt scope — `@EntryPoint` gives us a process-death-
 * safe pull of the singletons we need).
 */
class UndoQuickAddReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        // Stub: accept the broadcast but take no action. T4.3 fills in the
        // Undo flow. Leaving an empty body here (rather than throwing)
        // means that if this stub accidentally ships to a release build,
        // the user sees a non-functional Undo button but no crash.
    }

    companion object {
        /**
         * Intent extra carrying the transaction id to undo. Validated
         * in `onReceive` (T4.3) — null / 0 / negative values are rejected
         * and the undo is dropped with no side effects.
         */
        const val EXTRA_TRANSACTION_ID: String = "transaction_id"

        /**
         * Unique intent action for the Undo button. Including an explicit
         * action (not just extras) keeps the PendingIntent distinguishable
         * from other broadcasts targeting the same receiver class in the
         * future.
         */
        const val ACTION_UNDO: String = "dev.tuandoan.expensetracker.action.UNDO_QUICK_ADD"
    }
}
