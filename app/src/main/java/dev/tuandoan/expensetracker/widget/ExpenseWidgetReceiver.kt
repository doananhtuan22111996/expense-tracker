package dev.tuandoan.expensetracker.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/**
 * `BroadcastReceiver` that bridges the Android widget framework to Glance
 * for [ExpenseWidget].
 *
 * Registered in `AndroidManifest.xml` with an `android.appwidget.provider`
 * meta-data pointing at `res/xml/expense_widget_info.xml`. Task 1.5 (this
 * change) is the first task that makes the widget visible in the launcher
 * picker — before this, the widget class existed but had no manifest
 * registration.
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
 * stateless and reads from DataStore / Room inside `provideGlance` once
 * Task 1.7 wires the Hilt `EntryPoint`.
 */
class ExpenseWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ExpenseWidget()
}
