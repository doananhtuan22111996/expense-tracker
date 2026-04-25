package dev.tuandoan.expensetracker.data.worker

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for [WidgetRefreshWorker].
 *
 * The worker is a thin delegation layer to [dev.tuandoan.expensetracker.domain.widget.WidgetUpdater.requestUpdate].
 * The widget update itself is exercised by the Glance runtime on a real
 * device (manual matrix in Task 1.13); these tests just guard the companion
 * constants WorkManager uses to find the unique periodic work, since
 * changing them silently would leave the old work scheduled forever.
 */
class WidgetRefreshWorkerTest {
    @Test
    fun workName_isStableConstant() {
        assertEquals(
            "widget_refresh_worker",
            WidgetRefreshWorker.WORK_NAME,
        )
    }

    @Test
    fun periodicWorkName_isStableConstant() {
        assertEquals(
            "widget_refresh_periodic",
            WidgetRefreshWorker.PERIODIC_WORK_NAME,
        )
    }
}
