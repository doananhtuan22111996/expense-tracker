package dev.tuandoan.expensetracker.domain.notification

/**
 * Narrow seam over the four `NotificationHelper` methods the v3.12.0
 * quick-add flow actually calls. Existed prior to T4.4 but lived as
 * direct method calls on the concrete [NotificationHelper] — extracting
 * the interface lets unit tests substitute a recording fake without
 * pulling in the whole Android notification API.
 *
 * Production implementation is [NotificationHelper] itself (implements
 * this interface directly — no separate adapter). Test implementation
 * is `FakeQuickAddNotificationSurface` in `testutil/`.
 */
interface QuickAddNotificationSurface {
    /**
     * Posts the initial "Expense added" confirmation notification with an
     * Undo action. Returns the notification id so the caller can schedule
     * expiry + dismiss workers against the same id.
     */
    fun showQuickAddConfirmation(
        transactionId: Long,
        amountFormatted: String,
        categoryName: String,
    ): Int

    /**
     * Replaces the confirmation with a no-Undo version on the same id.
     * Called by the T+10s expiry worker.
     */
    fun updateQuickAddConfirmationWithoutUndo(
        notificationId: Int,
        amountFormatted: String,
        categoryName: String,
    )

    /**
     * Replaces the confirmation with a static "Undone" body on the same id.
     * Called by [UndoQuickAddReceiver] after a successful undo.
     */
    fun updateQuickAddConfirmationToUndone(notificationId: Int)

    /**
     * Cancels the notification entirely. Called by the T+30s auto-dismiss
     * worker, the post-undo 3s dismiss worker, and the undo-receiver on
     * the cleanup path.
     */
    fun cancelQuickAddConfirmation(notificationId: Int)
}
