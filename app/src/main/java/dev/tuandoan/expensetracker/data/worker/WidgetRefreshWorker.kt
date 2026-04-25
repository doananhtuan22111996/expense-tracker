package dev.tuandoan.expensetracker.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dev.tuandoan.expensetracker.domain.crash.CrashReporter
import dev.tuandoan.expensetracker.domain.widget.WidgetUpdater
import kotlin.coroutines.cancellation.CancellationException

/**
 * Periodic background refresh for the home-screen widget. Backstops the
 * repository-write hooks (Task 1.7) so the widget doesn't go stale when the
 * app isn't used — e.g., clock crosses midnight and "Today" needs to reset.
 *
 * Thin delegation layer: all heavy lifting lives in [WidgetUpdater]'s Glance
 * implementation ([dev.tuandoan.expensetracker.widget.GlanceWidgetUpdater]).
 * `CancellationException` is re-thrown so WorkManager can propagate
 * cancellation semantics correctly; any other exception is recorded and the
 * work is retried with WorkManager's default exponential backoff.
 */
@HiltWorker
class WidgetRefreshWorker
    @AssistedInject
    constructor(
        @Assisted appContext: Context,
        @Assisted workerParams: WorkerParameters,
        private val widgetUpdater: WidgetUpdater,
        private val crashReporter: CrashReporter,
    ) : CoroutineWorker(appContext, workerParams) {
        override suspend fun doWork(): Result =
            @Suppress("TooGenericExceptionCaught")
            try {
                widgetUpdater.requestUpdate()
                Result.success()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                crashReporter.recordException(e)
                Result.retry()
            }

        companion object {
            const val WORK_NAME = "widget_refresh_worker"
            const val PERIODIC_WORK_NAME = "widget_refresh_periodic"
        }
    }
