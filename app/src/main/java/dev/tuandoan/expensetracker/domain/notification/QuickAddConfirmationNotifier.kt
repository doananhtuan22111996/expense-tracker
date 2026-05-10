package dev.tuandoan.expensetracker.domain.notification

/**
 * Posts the v3.12.0 widget quick-add confirmation notification and schedules
 * its two follow-up updates per [ADR-012 — Undo Scheduling Mechanism]
 * (https://www.notion.so/35cb1772541381088642c3aea93f7fd7):
 *
 * 1. Immediate **post** of a `Notification` on `CHANNEL_QUICK_ADD_CONFIRMATION`
 *    (IMPORTANCE_LOW from PR #139) with an **Undo** action button.
 * 2. Scheduled **expiry** at 10s (WorkManager expedited): re-posts the same
 *    notification id without the Undo action. After this point the save is
 *    "committed" as far as the widget flow is concerned.
 * 3. Scheduled **dismiss** at 30s (WorkManager expedited): cancels the
 *    notification so the shade stays tidy.
 *
 * The Undo button's `PendingIntent` points at `UndoQuickAddReceiver`
 * (T4.3). When the user taps Undo within 10s, that receiver cancels the
 * pending expiry+dismiss work (via shared unique tag) and posts a separate
 * "Undone" update with its own 3s dismiss.
 *
 * ### Interface seam, not just a helper
 * Extracted as a domain interface (mirrors `WidgetUpdater` and
 * `BudgetAlertScheduler`) so `QuickAddViewModel` can be unit-tested with a
 * fake implementation. The concrete impl in `data/notification/` delegates
 * to `NotificationHelper` + `WorkManager` without leaking either type into
 * the VM.
 */
interface QuickAddConfirmationNotifier {
    /**
     * Post the confirmation notification for a just-saved transaction and
     * schedule the expiry + dismiss follow-ups.
     *
     * @param transactionId the auto-increment row id returned by
     *   `TransactionRepository.addTransaction`. Used as the key for
     *   per-notification identity (so rapid repeat saves don't collapse
     *   into a single shade entry) AND as the shared WorkManager unique
     *   tag so `UndoQuickAddReceiver` can cancel both expiry and dismiss
     *   in one call.
     * @param amountMinor the raw Long amount saved to the DB. Formatting
     *   into a display string happens at the notifier impl boundary — the
     *   ViewModel doesn't need to know the currency rendering rules.
     * @param currencyCode the ISO code of the currency used for the save
     *   (not necessarily the user's current default currency — a user who
     *   switched default mid-flow still sees the correct symbol here).
     * @param categoryName the display name of the category. Shown on the
     *   notification body; redacted on the lock screen via
     *   `VISIBILITY_PRIVATE`.
     */
    suspend fun post(
        transactionId: Long,
        amountMinor: Long,
        currencyCode: String,
        categoryName: String,
    )
}
