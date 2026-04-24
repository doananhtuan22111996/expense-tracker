package dev.tuandoan.expensetracker.widget.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import dev.tuandoan.expensetracker.R
import dev.tuandoan.expensetracker.widget.BudgetDisplay
import dev.tuandoan.expensetracker.widget.ExpenseWidget
import dev.tuandoan.expensetracker.widget.ExpenseWidgetState

/**
 * Dispatches between the small (2×1) and medium (4×2) widget layouts based on
 * the current `LocalSize`. Android 12+ routes the right size directly from
 * [ExpenseWidget]'s `SizeMode.Responsive`; pre-12 uses the closest-fit rule
 * with the same threshold.
 *
 * Theme colors come from `GlanceTheme`. The containing `GlanceTheme { }`
 * wrapper (Material You dynamic color + brand fallback) lands in Task 1.9.
 * Until then these render with the Glance defaults — acceptable because
 * no receiver is registered yet (Task 1.5).
 */
@Composable
fun ExpenseWidgetContent(state: ExpenseWidgetState) {
    val size = LocalSize.current
    // Width is the reliable discriminator — launchers resize in cell increments
    // along the width axis more often than the height axis. Matches
    // ExpenseWidget.MEDIUM_SIZE's width.
    val isMedium = size.width >= ExpenseWidget.MEDIUM_SIZE.width
    if (isMedium) {
        MediumLayout(state = state)
    } else {
        SmallLayout(state = state)
    }
}

// --- Small layout (2×1) ---

@Composable
private fun SmallLayout(state: ExpenseWidgetState) {
    val context = LocalContext.current
    val loadingPlaceholder = context.getString(R.string.widget_amount_loading)
    Row(
        modifier =
            GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TodayAmount(
            todayLabel = context.getString(R.string.widget_today),
            amountFormatted = state.todayFormatted.ifEmpty { loadingPlaceholder },
            modifier = GlanceModifier.defaultWeight(),
        )
        AddButton()
    }
}

// --- Medium layout (4×2) ---

@Composable
private fun MediumLayout(state: ExpenseWidgetState) {
    val context = LocalContext.current
    val loadingPlaceholder = context.getString(R.string.widget_amount_loading)
    Column(
        modifier =
            GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AmountRow(
                label = context.getString(R.string.widget_today),
                amountFormatted = state.todayFormatted.ifEmpty { loadingPlaceholder },
                modifier = GlanceModifier.defaultWeight(),
            )
            AddButton()
        }
        Spacer(modifier = GlanceModifier.height(8.dp))
        AmountRow(
            label = context.getString(R.string.widget_this_month),
            amountFormatted = state.monthFormatted.ifEmpty { loadingPlaceholder },
        )
        if (state.budget != null) {
            Spacer(modifier = GlanceModifier.height(10.dp))
            BudgetProgress(budget = state.budget)
        }
    }
}

// --- Shared row primitives ---

@Composable
private fun TodayAmount(
    todayLabel: String,
    amountFormatted: String,
    modifier: GlanceModifier = GlanceModifier,
) {
    Column(modifier = modifier) {
        Text(
            text = todayLabel,
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
private fun AmountRow(
    label: String,
    amountFormatted: String,
    modifier: GlanceModifier = GlanceModifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = label,
            style =
                TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 12.sp,
                ),
            modifier = GlanceModifier.defaultWeight(),
        )
        Text(
            text = amountFormatted,
            style =
                TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                ),
            maxLines = 1,
        )
    }
}

@Composable
private fun BudgetProgress(budget: BudgetDisplay) {
    val context = LocalContext.current
    // Over-budget uses the error color + a ↑ glyph in the percent label so
    // colorblind users still get the "you're over" signal.
    val accentColor =
        if (budget.isOverBudget) GlanceTheme.colors.error else GlanceTheme.colors.primary
    val percent = (budget.progressFraction * 100f).toInt()
    val percentText =
        if (budget.isOverBudget) {
            context.getString(R.string.widget_budget_over_percent, percent)
        } else {
            context.getString(R.string.widget_budget_percent, percent)
        }
    val progressText =
        context.getString(
            R.string.widget_budget_progress,
            budget.spentFormatted,
            budget.budgetFormatted,
        )

    Column(modifier = GlanceModifier.fillMaxWidth()) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = progressText,
                style =
                    TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 11.sp,
                    ),
                maxLines = 1,
                modifier = GlanceModifier.defaultWeight(),
            )
            Text(
                text = percentText,
                style =
                    TextStyle(
                        color = accentColor,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp,
                    ),
            )
        }
        Spacer(modifier = GlanceModifier.height(4.dp))
        LinearProgressIndicator(
            progress = budget.progressFraction,
            modifier = GlanceModifier.fillMaxWidth().height(4.dp),
            color = accentColor,
            backgroundColor = GlanceTheme.colors.surfaceVariant,
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
