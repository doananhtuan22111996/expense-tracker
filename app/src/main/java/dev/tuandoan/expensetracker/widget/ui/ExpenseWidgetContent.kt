package dev.tuandoan.expensetracker.widget.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import dev.tuandoan.expensetracker.widget.ExpenseWidgetState

/**
 * Small (2×1) home-screen layout for the expense widget.
 *
 * Shows the "Today" label + today's expense total, plus a circular "+" affordance
 * that lands the user on the Add Transaction screen once click actions are wired
 * in Task 1.6. This composable is intentionally click-free in Task 1.3 so the
 * layout can land and be reviewed in isolation.
 *
 * Theme colors come from `GlanceTheme` — the containing `GlanceTheme { }` wrapper
 * is added in Task 1.9 along with the Material You dynamic-color fallback palette.
 * Until then this renders with the Glance defaults, which is fine because no
 * receiver is registered (Task 1.5).
 */
@Composable
fun ExpenseWidgetContent(state: ExpenseWidgetState) {
    Row(
        modifier =
            GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TodayAmount(
            amountFormatted = state.todayFormatted.ifEmpty { "—" },
            modifier = GlanceModifier.defaultWeight(),
        )
        AddButton()
    }
}

@Composable
private fun TodayAmount(
    amountFormatted: String,
    modifier: GlanceModifier = GlanceModifier,
) {
    Column(modifier = modifier) {
        Text(
            text = "Today",
            style =
                TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 11.sp,
                ),
        )
        Text(
            text = amountFormatted,
            style =
                TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontWeight = FontWeight.Medium,
                    fontSize = 18.sp,
                ),
            maxLines = 1,
        )
    }
}

@Composable
private fun AddButton() {
    Box(
        modifier =
            GlanceModifier
                .size(40.dp)
                .cornerRadius(20.dp)
                .background(GlanceTheme.colors.primary),
        contentAlignment = Alignment.Center,
    ) {
        // Click action wired in Task 1.6 — deep links to AddEditTransactionScreen.
        Text(
            text = "+",
            style =
                TextStyle(
                    color = GlanceTheme.colors.onPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                ),
        )
    }
}
