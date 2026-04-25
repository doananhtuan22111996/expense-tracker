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
import kotlinx.coroutines.flow.first
import java.time.ZoneId

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
 * Data is fetched via [WidgetEntryPoint] — `provideGlance` takes a snapshot
 * of the current month's expenses, the default currency, and the budget,
 * then maps them through [mapExpenseWidgetState]. Subsequent refreshes
 * come from [GlanceWidgetUpdater.requestUpdate] (repository writes) and
 * the WorkManager periodic refresh (Task 1.8).
 */
class ExpenseWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode =
        SizeMode.Responsive(setOf(SMALL_SIZE, MEDIUM_SIZE))

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        val state = loadState(context)
        provideContent {
            GlanceTheme {
                ExpenseWidgetContent(state = state)
            }
        }
    }

    private suspend fun loadState(context: Context): ExpenseWidgetState {
        val entry = WidgetEntryPoint.get(context)
        val timeProvider = entry.timeProvider()
        val (from, to) = timeProvider.currentMonthRange()
        val currencyCode = entry.currencyPreferenceRepository().getDefaultCurrency()
        val monthExpenses = entry.transactionRepository().observeTransactions(from, to).first()
        val budgetAmount = entry.budgetPreferences().getBudget(currencyCode).first()
        return mapExpenseWidgetState(
            monthExpenses = monthExpenses,
            defaultCurrencyCode = currencyCode,
            budgetAmount = budgetAmount,
            nowMillis = timeProvider.currentTimeMillis(),
            zoneId = ZoneId.systemDefault(),
            formatter = entry.currencyFormatter(),
        )
    }

    companion object {
        /** 2×1 cells on a typical 70.dp-per-cell launcher. */
        val SMALL_SIZE: DpSize = DpSize(140.dp, 80.dp)

        /** 4×2 cells — the cutoff above which [ExpenseWidgetContent] renders the medium layout. */
        val MEDIUM_SIZE: DpSize = DpSize(280.dp, 160.dp)
    }
}
