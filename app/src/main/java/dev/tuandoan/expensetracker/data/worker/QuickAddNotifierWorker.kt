package dev.tuandoan.expensetracker.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dev.tuandoan.expensetracker.core.notification.NotificationHelper
import dev.tuandoan.expensetracker.domain.crash.CrashReporter
import kotlin.coroutines.cancellation.CancellationException

/**
 * WorkManager worker that applies scheduled updates to the quick-add
 * confirmation notification. Two modes:
 *
 * - [Mode.EXPIRE] — at 10s: re-post the notification without the Undo
 *   action button. The save is "committed" from the widget flow's point
 *   of view; there is no further one-tap recovery path.
 * - [Mode.DISMISS] — at 30s: cancel the notification entirely so the
 *   shade stays tidy.
 *
 * Both modes are scheduled at post-time by `QuickAddConfirmationNotifier`
 * as expedited `OneTimeWorkRequest`s sharing a unique tag
 * `quick-add-notify-{transactionId}`. T4.3's `UndoQuickAddReceiver`
 * cancels both with a single `workManager.cancelAllWorkByTag(tag)` when
 * the user taps Undo before 10s elapses.
 *
 * Per [ADR-012](https://www.notion.so/35cb1772541381088642c3aea93f7fd7):
 * WorkManager chosen over `Handler.postDelayed` (dies on process death,
 * fails the 10s correctness bar) and over `AlarmManager.setExact`
 * (overshoots precision need, requires `SCHEDULE_EXACT_ALARM` permission).
 */
@HiltWorker
class QuickAddNotifierWorker
    @AssistedInject
    constructor(
        @Assisted appContext: Context,
        @Assisted workerParams: WorkerParameters,
        private val notificationHelper: NotificationHelper,
        private val crashReporter: CrashReporter,
    ) : CoroutineWorker(appContext, workerParams) {
        override suspend fun doWork(): Result =
            @Suppress("TooGenericExceptionCaught")
            try {
                runMode()
                Result.success()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                crashReporter.recordException(e)
                // Notification updates are best-effort. Retrying a 10s-window
                // expiry after a failure would likely land past the 30s
                // auto-dismiss anyway; better to drop than to re-fire late.
                Result.failure()
            }

        private fun runMode() {
            val notificationId = inputData.getInt(KEY_NOTIFICATION_ID, INVALID_ID)
            if (notificationId == INVALID_ID) return
            val mode = Mode.parse(inputData.getString(KEY_MODE)) ?: return

            when (mode) {
                Mode.EXPIRE -> {
                    val amountFormatted = inputData.getString(KEY_AMOUNT_FORMATTED).orEmpty()
                    val categoryName = inputData.getString(KEY_CATEGORY_NAME).orEmpty()
                    if (amountFormatted.isBlank() || categoryName.isBlank()) return
                    notificationHelper.updateQuickAddConfirmationWithoutUndo(
                        notificationId = notificationId,
                        amountFormatted = amountFormatted,
                        categoryName = categoryName,
                    )
                }
                Mode.DISMISS -> {
                    notificationHelper.cancelQuickAddConfirmation(notificationId)
                }
            }
        }

        enum class Mode {
            EXPIRE,
            DISMISS,
            ;

            companion object {
                fun parse(name: String?): Mode? =
                    try {
                        name?.let { valueOf(it) }
                    } catch (_: IllegalArgumentException) {
                        null
                    }
            }
        }

        companion object {
            const val KEY_NOTIFICATION_ID = "notification_id"
            const val KEY_MODE = "mode"
            const val KEY_AMOUNT_FORMATTED = "amount_formatted"
            const val KEY_CATEGORY_NAME = "category_name"
            private const val INVALID_ID = -1

            /**
             * Shared tag for both expiry + dismiss work items scheduled against
             * the same transaction. `UndoQuickAddReceiver` cancels by tag to
             * clear both in one call when the user taps Undo.
             */
            fun tag(transactionId: Long): String = "quick-add-notify-$transactionId"
        }
    }
