package dev.tuandoan.expensetracker.widget

import android.content.Context
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import dev.tuandoan.expensetracker.widget.ui.ExpenseWidgetContent

/**
 * Home-screen widget entry point. A thin shell whose only job is to hand
 * the current [ExpenseWidgetState] to the Glance composable tree.
 *
 * Declares a responsive [SizeMode] with two preset sizes — a 2×1 small
 * layout (Today + add button) and a 4×2 medium layout (Today + Month +
 * budget progress). Android 12+ picks the view based on the actual widget
 * size at render time; older versions fall back to the closest fit.
 *
 * Wraps content in [GlanceTheme], which on Android 12+ pulls dynamic color
 * from the user's wallpaper palette and on 8–11 falls back to Glance's
 * built-in neutral scheme. No custom `ColorProviders` is needed for v1 —
 * the widget is intentionally palette-neutral so it blends into whatever
 * launcher theme the user has set.
 *
 * Real data fetching is still pending — `provideGlance` hands
 * [ExpenseWidgetState.LOADING] to the composable until Task 1.7 wires a
 * Hilt `EntryPoint` analogous to Task Tracker v1.5.0's `WidgetEntryPoint`.
 */
class ExpenseWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode =
        SizeMode.Responsive(setOf(SMALL_SIZE, MEDIUM_SIZE))

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        provideContent {
            GlanceTheme {
                ExpenseWidgetContent(state = ExpenseWidgetState.LOADING)
            }
        }
    }

    companion object {
        /** 2×1 cells on a typical 70.dp-per-cell launcher. */
        val SMALL_SIZE: DpSize = DpSize(140.dp, 80.dp)

        /** 4×2 cells — the cutoff above which [ExpenseWidgetContent] renders the medium layout. */
        val MEDIUM_SIZE: DpSize = DpSize(280.dp, 160.dp)
    }
}
