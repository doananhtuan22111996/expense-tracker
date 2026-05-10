package dev.tuandoan.expensetracker.testutil

import dev.tuandoan.expensetracker.domain.notification.QuickAddUndoWorkScheduler

/**
 * Recording fake for [QuickAddUndoWorkScheduler]. Captures every call so
 * tests can assert on tag naming, invocation order, and argument
 * correctness.
 *
 * `throwOnCancelExpiryAndDismiss` lets tests exercise the
 * `UndoFlowRunner`'s best-effort per-step catch — a cancel failure
 * should log via `CrashReporter` but not abort the remaining 5 steps.
 */
class FakeQuickAddUndoWorkScheduler : QuickAddUndoWorkScheduler {
    data class ScheduleExpiryAndDismissCall(
        val transactionId: Long,
        val notificationId: Int,
        val amountFormatted: String,
        val categoryName: String,
    )

    data class ScheduleUndoneDismissCall(
        val transactionId: Long,
        val notificationId: Int,
    )

    val scheduleExpiryAndDismissCalls: MutableList<ScheduleExpiryAndDismissCall> = mutableListOf()
    val cancelExpiryAndDismissCalls: MutableList<Long> = mutableListOf()
    val scheduleUndoneDismissCalls: MutableList<ScheduleUndoneDismissCall> = mutableListOf()

    var throwOnScheduleExpiryAndDismiss: Throwable? = null
    var throwOnCancelExpiryAndDismiss: Throwable? = null
    var throwOnScheduleUndoneDismiss: Throwable? = null

    override fun scheduleExpiryAndDismiss(
        transactionId: Long,
        notificationId: Int,
        amountFormatted: String,
        categoryName: String,
    ) {
        scheduleExpiryAndDismissCalls +=
            ScheduleExpiryAndDismissCall(transactionId, notificationId, amountFormatted, categoryName)
        throwOnScheduleExpiryAndDismiss?.let { throw it }
    }

    override suspend fun cancelExpiryAndDismiss(transactionId: Long) {
        cancelExpiryAndDismissCalls += transactionId
        throwOnCancelExpiryAndDismiss?.let { throw it }
    }

    override fun scheduleUndoneDismiss(
        transactionId: Long,
        notificationId: Int,
    ) {
        scheduleUndoneDismissCalls += ScheduleUndoneDismissCall(transactionId, notificationId)
        throwOnScheduleUndoneDismiss?.let { throw it }
    }
}
