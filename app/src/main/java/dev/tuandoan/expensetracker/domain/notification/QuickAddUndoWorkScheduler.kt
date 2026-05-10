package dev.tuandoan.expensetracker.domain.notification

/**
 * Narrow seam over the WorkManager scheduling paths the quick-add flow
 * uses. Extracted from `QuickAddConfirmationNotifierImpl` and
 * `UndoQuickAddReceiver` during T4.4 to enable unit-testing the
 * orchestration logic without instantiating `WorkManager`.
 *
 * Production implementation is `QuickAddUndoWorkSchedulerImpl` in
 * `data/notification/` — wraps `WorkManager.getInstance` + the
 * `OneTimeWorkRequestBuilder` invocations + the bounded
 * `ListenableFuture.get(timeout)` on cancel. Test implementation is
 * `FakeQuickAddUndoWorkScheduler` in `testutil/`.
 *
 * Per [ADR-012](https://www.notion.so/35cb1772541381088642c3aea93f7fd7):
 * both scheduling paths use expedited `OneTimeWorkRequest` with
 * `OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST` fallback.
 */
interface QuickAddUndoWorkScheduler {
    /**
     * Schedules the 10-second expiry (re-post without Undo) + 30-second
     * auto-dismiss workers for a freshly-posted confirmation notification.
     * Both share the tag `quick-add-notify-{transactionId}` so the undo
     * receiver can cancel them with a single `cancelExpiryAndDismiss` call.
     */
    fun scheduleExpiryAndDismiss(
        transactionId: Long,
        notificationId: Int,
        amountFormatted: String,
        categoryName: String,
    )

    /**
     * Cancels the expiry + auto-dismiss workers scheduled by
     * [scheduleExpiryAndDismiss] for this transaction id. Bounded wait
     * (~2s) to avoid blocking through `goAsync`'s 10-second ANR window
     * on the undo receiver's hot path.
     *
     * @return `true` if the cancel completed within the timeout, `false`
     * on timeout (caller proceeds anyway — the in-memory scheduler cancel
     * takes effect immediately even if the SQLite commit hasn't finished).
     */
    suspend fun cancelExpiryAndDismiss(transactionId: Long): Boolean

    /**
     * Schedules the 3-second auto-dismiss for an "Undone" notification
     * posted by [UndoQuickAddReceiver]. Uses a NEW tag
     * `quick-add-undone-{transactionId}` (not the one cancelled in step 1
     * of the undo flow) so a future
     * `cancelExpiryAndDismiss` call can't collaterally kill this dismiss.
     */
    fun scheduleUndoneDismiss(
        transactionId: Long,
        notificationId: Int,
    )
}
