package dev.tuandoan.expensetracker.testutil

import dev.tuandoan.expensetracker.domain.notification.QuickAddNotificationSurface

/**
 * Recording fake for [QuickAddNotificationSurface]. Captures every call so
 * tests can assert on invocation order, argument correctness, and
 * per-method call count.
 *
 * Each method can optionally throw a pre-configured exception via
 * `throwOnXxx` — lets tests exercise best-effort error-handling in the
 * receiver and notifier orchestration logic.
 *
 * [nextNotificationId] controls the return value of
 * [showQuickAddConfirmation]; the default `17` is arbitrary but
 * non-zero so tests can verify it flows through to downstream calls.
 */
class FakeQuickAddNotificationSurface : QuickAddNotificationSurface {
    data class ShowCall(
        val transactionId: Long,
        val amountFormatted: String,
        val categoryName: String,
    )

    data class UpdateWithoutUndoCall(
        val notificationId: Int,
        val amountFormatted: String,
        val categoryName: String,
    )

    val showCalls: MutableList<ShowCall> = mutableListOf()
    val updateWithoutUndoCalls: MutableList<UpdateWithoutUndoCall> = mutableListOf()
    val updateToUndoneCalls: MutableList<Int> = mutableListOf()
    val cancelCalls: MutableList<Int> = mutableListOf()

    var nextNotificationId: Int = 17

    var throwOnShow: Throwable? = null
    var throwOnUpdateWithoutUndo: Throwable? = null
    var throwOnUpdateToUndone: Throwable? = null
    var throwOnCancel: Throwable? = null

    override fun showQuickAddConfirmation(
        transactionId: Long,
        amountFormatted: String,
        categoryName: String,
    ): Int {
        showCalls += ShowCall(transactionId, amountFormatted, categoryName)
        throwOnShow?.let { throw it }
        return nextNotificationId
    }

    override fun updateQuickAddConfirmationWithoutUndo(
        notificationId: Int,
        amountFormatted: String,
        categoryName: String,
    ) {
        updateWithoutUndoCalls += UpdateWithoutUndoCall(notificationId, amountFormatted, categoryName)
        throwOnUpdateWithoutUndo?.let { throw it }
    }

    override fun updateQuickAddConfirmationToUndone(notificationId: Int) {
        updateToUndoneCalls += notificationId
        throwOnUpdateToUndone?.let { throw it }
    }

    override fun cancelQuickAddConfirmation(notificationId: Int) {
        cancelCalls += notificationId
        throwOnCancel?.let { throw it }
    }
}
