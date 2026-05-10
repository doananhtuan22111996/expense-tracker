package dev.tuandoan.expensetracker.data.notification

import android.content.Context
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.tuandoan.expensetracker.data.worker.QuickAddNotifierWorker
import dev.tuandoan.expensetracker.domain.notification.QuickAddUndoWorkScheduler
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Production implementation of [QuickAddUndoWorkScheduler]. Wraps
 * `WorkManager` + the `OneTimeWorkRequestBuilder` invocations that used
 * to live inline in `QuickAddConfirmationNotifierImpl` and
 * `UndoQuickAddReceiver`.
 *
 * Extracted during T4.4 so the orchestration logic in those callers
 * becomes unit-testable against a fake implementation. Nothing in this
 * class is interesting from a correctness standpoint — it's pure
 * plumbing. The interesting decisions (timeouts, expedited policy,
 * shared tag, delays) are documented in
 * [ADR-012](https://www.notion.so/35cb1772541381088642c3aea93f7fd7).
 */
@Singleton
class QuickAddUndoWorkSchedulerImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : QuickAddUndoWorkScheduler {
        private val workManager: WorkManager
            get() = WorkManager.getInstance(context)

        override fun scheduleExpiryAndDismiss(
            transactionId: Long,
            notificationId: Int,
            amountFormatted: String,
            categoryName: String,
        ) {
            val tag = QuickAddNotifierWorker.tag(transactionId)
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

        override suspend fun cancelExpiryAndDismiss(transactionId: Long) {
            try {
                workManager
                    .cancelAllWorkByTag(QuickAddNotifierWorker.tag(transactionId))
                    .result
                    .get(CANCEL_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            } catch (e: TimeoutException) {
                // Bounded-wait timeout — caller proceeds anyway (the in-memory
                // scheduler cancel takes effect even if the SQLite commit
                // hasn't finished). Silent swallow because "didn't commit in
                // time" isn't a bug — it's the documented fallback per the
                // method's best-effort contract.
            }
            // Other Future exceptions (InterruptedException, ExecutionException)
            // propagate up to the caller's per-step try/catch in UndoFlowRunner,
            // where they're logged via CrashReporter. CancellationException
            // propagates too, as required by structured concurrency.
        }

        override fun scheduleUndoneDismiss(
            transactionId: Long,
            notificationId: Int,
        ) {
            val dismissWork =
                OneTimeWorkRequestBuilder<QuickAddNotifierWorker>()
                    .setInitialDelay(UNDONE_DISMISS_SECONDS, TimeUnit.SECONDS)
                    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    .setInputData(
                        workDataOf(
                            QuickAddNotifierWorker.KEY_NOTIFICATION_ID to notificationId,
                            QuickAddNotifierWorker.KEY_MODE to QuickAddNotifierWorker.Mode.DISMISS.name,
                        ),
                    ).addTag(undoneDismissTag(transactionId))
                    .build()
            workManager.enqueue(dismissWork)
        }

        companion object {
            /**
             * Tag for the 3-second dismiss worker scheduled after the
             * "Undone" update. Separate from [QuickAddNotifierWorker.tag]
             * so the original post's shared tag cancel doesn't collateral-
             * kill this dismiss.
             */
            internal fun undoneDismissTag(transactionId: Long): String = "quick-add-undone-$transactionId"

            // Design-doc values. Changing these requires a privacy-policy
            // review: the "10-second Undo" is user-facing copy.
            private const val UNDO_WINDOW_SECONDS: Long = 10L
            private const val AUTO_DISMISS_SECONDS: Long = 30L
            private const val UNDONE_DISMISS_SECONDS: Long = 3L

            /**
             * Maximum time to wait for [WorkManager.cancelAllWorkByTag] to
             * commit its cancel to SQLite. Set well below `goAsync`'s 10-
             * second ANR budget on the receiver's hot path.
             */
            private const val CANCEL_TIMEOUT_MS: Long = 2_000L
        }
    }
