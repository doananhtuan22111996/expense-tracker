package dev.tuandoan.expensetracker.testutil

import dev.tuandoan.expensetracker.domain.notification.QuickAddUndoWorkScheduler

/**
 * Recording fake for [QuickAddUndoWorkScheduler]. Captures every call so
 * tests can assert on tag naming, invocation order, and argument
 * correctness.
 *
 * `cancelExpiryAndDismissResult` controls the Boolean returned by
 * [cancelExpiryAndDismiss] — defaults to `true` (cancel succeeded
 * within the timeout). Setting `false` lets tests exercise the
 * receiver's "timeout, proceed anyway" path.
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

    var cancelExpiryAndDismissResult: Boolean = true

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

    override suspend fun cancelExpiryAndDismiss(transactionId: Long): Boolean {
        cancelExpiryAndDismissCalls += transactionId
        throwOnCancelExpiryAndDismiss?.let { throw it }
        return cancelExpiryAndDismissResult
    }

    override fun scheduleUndoneDismiss(
        transactionId: Long,
        notificationId: Int,
    ) {
        scheduleUndoneDismissCalls += ScheduleUndoneDismissCall(transactionId, notificationId)
        throwOnScheduleUndoneDismiss?.let { throw it }
    }
}
