package dev.tuandoan.expensetracker.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import dev.tuandoan.expensetracker.widget.ui.ExpenseWidgetContent

/**
 * Home-screen widget entry point. A thin shell whose only job is to hand
 * the current [ExpenseWidgetState] to the Glance composable tree.
 *
 * In v3.10.0 Task 1.3 (this change) the widget renders the loading
 * placeholder — real data fetching is wired in Task 1.7 via a Hilt
 * `EntryPoint` analogous to Task Tracker v1.5.0's `WidgetEntryPoint`.
 * Receiver registration + manifest entry land in Task 1.5, so until then
 * no user sees this widget.
 */
class ExpenseWidget : GlanceAppWidget() {
    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        provideContent {
            ExpenseWidgetContent(state = ExpenseWidgetState.LOADING)
        }
    }
}
