package dev.tuandoan.expensetracker.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dev.tuandoan.expensetracker.domain.crash.CrashReporter
import dev.tuandoan.expensetracker.domain.notification.QuickAddNotificationSurface
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
 *
 * ### Testability
 * Interesting dispatch logic lives in [Dispatcher] — a pure-Kotlin class
 * that takes a [QuickAddNotificationSurface] interface. Unit tests in
 * `QuickAddNotifierWorkerDispatcherTest` exercise that directly without
 * instantiating the Worker.
 */
@HiltWorker
class QuickAddNotifierWorker
    @AssistedInject
    constructor(
        @Assisted appContext: Context,
        @Assisted workerParams: WorkerParameters,
        private val notificationSurface: QuickAddNotificationSurface,
        private val crashReporter: CrashReporter,
    ) : CoroutineWorker(appContext, workerParams) {
        override suspend fun doWork(): Result =
            @Suppress("TooGenericExceptionCaught")
            try {
                Dispatcher(notificationSurface).dispatch(
                    notificationId = inputData.getInt(KEY_NOTIFICATION_ID, INVALID_ID),
                    rawMode = inputData.getString(KEY_MODE),
                    amountFormatted = inputData.getString(KEY_AMOUNT_FORMATTED),
                    categoryName = inputData.getString(KEY_CATEGORY_NAME),
                )
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

        /**
         * Pure-Kotlin dispatcher for the worker's two modes. Extracted so
         * unit tests can exercise the validation + branching logic against
         * a fake [QuickAddNotificationSurface] without instantiating a
         * WorkManager-backed worker.
         */
        internal class Dispatcher(
            private val notificationSurface: QuickAddNotificationSurface,
        ) {
            fun dispatch(
                notificationId: Int,
                rawMode: String?,
                amountFormatted: String?,
                categoryName: String?,
            ) {
                if (notificationId == INVALID_ID) return
                val mode = Mode.parse(rawMode) ?: return
                when (mode) {
                    Mode.EXPIRE -> {
                        if (amountFormatted.isNullOrBlank() || categoryName.isNullOrBlank()) return
                        notificationSurface.updateQuickAddConfirmationWithoutUndo(
                            notificationId = notificationId,
                            amountFormatted = amountFormatted,
                            categoryName = categoryName,
                        )
                    }
                    Mode.DISMISS -> {
                        notificationSurface.cancelQuickAddConfirmation(notificationId)
                    }
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
            internal const val INVALID_ID = -1

            /**
             * Shared tag for both expiry + dismiss work items scheduled against
             * the same transaction. `UndoQuickAddReceiver` cancels by tag to
             * clear both in one call when the user taps Undo.
             */
            fun tag(transactionId: Long): String = "quick-add-notify-$transactionId"
        }
    }
