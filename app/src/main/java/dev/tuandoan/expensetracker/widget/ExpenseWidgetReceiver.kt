package dev.tuandoan.expensetracker.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import dev.tuandoan.expensetracker.domain.analytics.AnalyticsEvent
import dev.tuandoan.expensetracker.domain.analytics.WidgetSize

/**
 * `BroadcastReceiver` that bridges the Android widget framework to Glance
 * for [ExpenseWidget].
 *
 * Registered in `AndroidManifest.xml` with an `android.appwidget.provider`
 * meta-data pointing at `res/xml/expense_widget_info.xml`. Task 1.5
 * originally made the widget visible in the launcher picker; v3.11.0's
 * Task 6.2 adds analytics instrumentation for install/uninstall lifecycle.
 *
 * The provider XML's `minWidth` / `minHeight` / `minResizeWidth` /
 * `minResizeHeight` are sized to fit [ExpenseWidget.SMALL_SIZE] (2×1 cells)
 * at the lower bound, with `targetCellWidth`/`targetCellHeight = 2/1` so
 * Android 12+ places the widget at the small layout by default. The
 * `SizeMode.Responsive` on [ExpenseWidget] handles the small/medium layout
 * dispatch as the user resizes up to 4×2.
 *
 * Stateless: a fresh [ExpenseWidget] instance is handed to Glance on each
 * receive call. That's the documented pattern — the widget itself is also
 * stateless and reads from DataStore / Room inside `provideGlance` via
 * [WidgetEntryPoint] (Task 1.7).
 *
 * Analytics (Task 6.2):
 * - [onEnabled] fires once when the first instance system-wide is added;
 *   we log [AnalyticsEvent.WidgetAdded] with the best-effort size of the
 *   first placed instance.
 * - [onDisabled] fires once when the last instance is removed; we log
 *   [AnalyticsEvent.WidgetRemoved] with no parameters.
 * - No per-instance event (e.g. adding a second widget) — PRD FR-A6
 *   ties events to `onEnabled`/`onDisabled`, not per-instance lifecycle,
 *   which keeps the event rate bounded and matches the coarse
 *   install/uninstall signal we actually care about.
 */
class ExpenseWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ExpenseWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        val size = resolveFirstInstanceSize(context)
        WidgetEntryPoint.get(context).analytics().logEvent(
            AnalyticsEvent.WidgetAdded(size = size),
        )
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        WidgetEntryPoint.get(context).analytics().logEvent(
            AnalyticsEvent.WidgetRemoved,
        )
    }

    /**
     * Best-effort size classification for the widget_added event.
     *
     * `onEnabled` fires when the first instance is added system-wide, but
     * the per-instance size options may not yet be populated by the
     * launcher at this exact moment. We sample the first widget id's
     * [AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH] and compare against
     * [ExpenseWidget.SMALL_SIZE]'s 140dp threshold. If no instance is
     * discoverable yet (empty id array, stripped options), we fall back
     * to [WidgetSize.SMALL] — matches the provider XML's
     * `targetCellWidth=2` default.
     *
     * Signal is coarse by design: the PRD's product question is "do users
     * install the widget at all", not "which cell count". The parameter
     * exists for the rare case where medium-vs-small adoption diverges
     * significantly; default-to-small is fine for v1.
     */
    private fun resolveFirstInstanceSize(context: Context): WidgetSize {
        val manager = AppWidgetManager.getInstance(context) ?: return WidgetSize.SMALL
        val provider = android.content.ComponentName(context, ExpenseWidgetReceiver::class.java)
        val ids = manager.getAppWidgetIds(provider) ?: return WidgetSize.SMALL
        val firstId = ids.firstOrNull() ?: return WidgetSize.SMALL
        val minWidthDp =
            manager.getAppWidgetOptions(firstId)?.getInt(
                AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,
                0,
            ) ?: 0
        return if (minWidthDp > ExpenseWidget.SMALL_SIZE.width.value) {
            WidgetSize.MEDIUM
        } else {
            WidgetSize.SMALL
        }
    }
}
