package dev.tuandoan.expensetracker.data.notification

import android.content.Context
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.tuandoan.expensetracker.core.formatter.CurrencyFormatter
import dev.tuandoan.expensetracker.core.notification.NotificationHelper
import dev.tuandoan.expensetracker.data.worker.QuickAddNotifierWorker
import dev.tuandoan.expensetracker.domain.notification.QuickAddConfirmationNotifier
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Production implementation of [QuickAddConfirmationNotifier].
 *
 * 1. Delegates notification construction to [NotificationHelper] (which owns
 *    the per-channel building, permission check, lock-screen visibility,
 *    and Undo PendingIntent wiring).
 * 2. Schedules two expedited `OneTimeWorkRequest`s per ADR-012:
 *    - T+10s with `Mode.EXPIRE` → re-posts without the Undo action
 *    - T+30s with `Mode.DISMISS` → cancels the notification
 *    Both work items share the unique tag `quick-add-notify-{transactionId}`
 *    so T4.3's Undo receiver can cancel both in one
 *    `workManager.cancelAllWorkByTag(tag)` call.
 *
 * `OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST` is the safe fallback:
 * if the expedited-work daily quota is exhausted, the work degrades to
 * regular WorkManager scheduling rather than failing. Worst-case the Undo
 * expiry arrives a few seconds late — acceptable per ADR-012's risk
 * analysis.
 */
@Singleton
class QuickAddConfirmationNotifierImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val notificationHelper: NotificationHelper,
        private val currencyFormatter: CurrencyFormatter,
    ) : QuickAddConfirmationNotifier {
        override suspend fun post(
            transactionId: Long,
            amountMinor: Long,
            currencyCode: String,
            categoryName: String,
        ) {
            val amountFormatted = currencyFormatter.format(amountMinor, currencyCode)
            val notificationId =
                notificationHelper.showQuickAddConfirmation(
                    transactionId = transactionId,
                    amountFormatted = amountFormatted,
                    categoryName = categoryName,
                )

            val tag = QuickAddNotifierWorker.tag(transactionId)
            val workManager = WorkManager.getInstance(context)

            val expireWork =
                OneTimeWorkRequestBuilder<QuickAddNotifierWorker>()
                    .setInitialDelay(UNDO_WINDOW_SECONDS, TimeUnit.SECONDS)
                    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    .setInputData(
                        workDataOf(
                            QuickAddNotifierWorker.KEY_NOTIFICATION_ID to notificationId,
                            QuickAddNotifierWorker.KEY_MODE to QuickAddNotifierWorker.Mode.EXPIRE.name,
                            QuickAddNotifierWorker.KEY_AMOUNT_FORMATTED to amountFormatted,
                            QuickAddNotifierWorker.KEY_CATEGORY_NAME to categoryName,
                        ),
                    ).addTag(tag)
                    .build()

            val dismissWork =
                OneTimeWorkRequestBuilder<QuickAddNotifierWorker>()
                    .setInitialDelay(AUTO_DISMISS_SECONDS, TimeUnit.SECONDS)
                    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    .setInputData(
                        workDataOf(
                            QuickAddNotifierWorker.KEY_NOTIFICATION_ID to notificationId,
                            QuickAddNotifierWorker.KEY_MODE to QuickAddNotifierWorker.Mode.DISMISS.name,
                        ),
                    ).addTag(tag)
                    .build()

            workManager.enqueue(listOf(expireWork, dismissWork))
        }

        companion object {
            // Design-doc values. Changing these requires a privacy-policy
            // review: the "10-second Undo" is a user-facing promise in the
            // widget flow description.
            private const val UNDO_WINDOW_SECONDS: Long = 10L
            private const val AUTO_DISMISS_SECONDS: Long = 30L
        }
    }
