package dev.tuandoan.expensetracker.widget.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
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
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import dev.tuandoan.expensetracker.R
import dev.tuandoan.expensetracker.domain.widget.PinnedCategorySlot
import dev.tuandoan.expensetracker.widget.BudgetDisplay
import dev.tuandoan.expensetracker.widget.ExpenseWidget
import dev.tuandoan.expensetracker.widget.ExpenseWidgetState
import dev.tuandoan.expensetracker.widget.openAddTransactionAction
import dev.tuandoan.expensetracker.widget.openAppAction

/**
 * Dispatches between the small (2×1) and medium (4×2) widget layouts based on
 * the current `LocalSize`. Android 12+ routes the right size directly from
 * [ExpenseWidget]'s `SizeMode.Responsive`; pre-12 uses the closest-fit rule
 * with the same threshold.
 *
 * Click targets follow the widget PRD: tapping the "+" button opens the
 * add-transaction screen; tapping anywhere else on the widget opens the app
 * on its Home tab. Both routes go through `MainActivity` (see [openAppAction]
 * / [openAddTransactionAction] in `WidgetActions`).
 *
 * Theme colors come from `GlanceTheme`. The wrapping `GlanceTheme { }` in
 * [ExpenseWidget.provideGlance] picks up dynamic color on Android 12+ and
 * falls back to Glance's neutral scheme on 8–11.
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
    val rowDescription =
        if (state.todayFormatted.isEmpty()) {
            context.getString(R.string.a11y_widget_loading)
        } else {
            context.getString(R.string.a11y_widget_small, state.todayFormatted)
        }
    // Background click = open app (Home tab). The AddButton below declares
    // its own `clickable`, which takes precedence on its hit area.
    //
    // The whole row carries a single `contentDescription` so TalkBack reads
    // one coherent sentence per widget focus instead of announcing each
    // label + amount node separately.
    Row(
        modifier =
            GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .clickable(openAppAction(context))
                .semantics { contentDescription = rowDescription }
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
    val columnDescription = mediumA11yDescription(context, state)
    // Variant A layout: today/month/budget information occupies the top, then
    // the quick-add tile strip (3 tiles + "+" button) spans the bottom row.
    //
    // Background click = open app (Home tab). AddButton and the tiles declare
    // their own `clickable`s, which take precedence on their hit areas.
    //
    // The outer Column carries one `contentDescription` covering the info
    // section; tile-level content descriptions live on each tile composable.
    Column(
        modifier =
            GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .clickable(openAppAction(context))
                .semantics { contentDescription = columnDescription }
                .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        AmountRow(
            label = context.getString(R.string.widget_today),
            amountFormatted = state.todayFormatted.ifEmpty { loadingPlaceholder },
        )
        Spacer(modifier = GlanceModifier.height(8.dp))
        AmountRow(
            label = context.getString(R.string.widget_this_month),
            amountFormatted = state.monthFormatted.ifEmpty { loadingPlaceholder },
        )
        if (state.budget != null) {
            Spacer(modifier = GlanceModifier.height(10.dp))
            BudgetProgress(budget = state.budget)
        }
        Spacer(modifier = GlanceModifier.defaultWeight())
        QuickAddTileStrip(pinnedCategories = state.pinnedCategories)
    }
}

/**
 * Bottom 4-column strip: 3 quick-add category tiles + the existing "+" button.
 *
 * Tile content is rendered by [TileSlotPlaceholder] for now — T2.4 replaces the
 * filled branch with the real `CategoryTile`, T2.5 replaces the empty branch
 * with the dashed "+ Set up" placeholder. Keeping both branches behind a
 * single internal composable means T2.3 only owns layout proportions.
 *
 * Uses `defaultWeight` on each slot so the four columns share width equally
 * and adapt to medium-widget cell-width drift across launchers.
 */
@Composable
private fun QuickAddTileStrip(pinnedCategories: List<PinnedCategorySlot>) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val slots = pinnedCategories.take(3)
        // Always render three slot positions, even if the upstream list is
        // shorter — keeps the four-column grid stable across emissions.
        for (i in 0 until 3) {
            TileSlotPlaceholder(
                slot = slots.getOrNull(i),
                modifier = GlanceModifier.defaultWeight().padding(horizontal = 2.dp),
            )
        }
        AddButton()
    }
}

/**
 * Temporary visual stub for a tile slot. Renders a neutral rounded container
 * so the bottom strip has the correct visual weight + tap target size; the
 * real tile composables (T2.4 filled, T2.5 empty placeholder) will replace
 * this in follow-up PRs without changing the surrounding row geometry.
 */
@Composable
private fun TileSlotPlaceholder(
    slot: PinnedCategorySlot?,
    modifier: GlanceModifier = GlanceModifier,
) {
    // `slot` is carried into the stub now so T2.4/T2.5 can wire tile behavior
    // to the pinned content without touching `QuickAddTileStrip`. For T2.3 the
    // visual is intentionally identical regardless of Filled vs. Empty —
    // layout-only scope.
    @Suppress("UNUSED_PARAMETER")
    slot
    Box(
        modifier =
            modifier
                .height(40.dp)
                .cornerRadius(12.dp)
                .background(GlanceTheme.colors.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        // Deliberately empty — the real tile content arrives in T2.4/T2.5.
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
    val context = LocalContext.current
    val buttonDescription = context.getString(R.string.a11y_widget_add_button)
    Box(
        modifier =
            GlanceModifier
                .size(40.dp)
                .cornerRadius(20.dp)
                .background(GlanceTheme.colors.primary)
                .clickable(openAddTransactionAction(context))
                .semantics { contentDescription = buttonDescription },
        contentAlignment = Alignment.Center,
    ) {
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

/**
 * Builds the medium-layout TalkBack sentence. Branches on loading / no-budget
 * / on-budget / over-budget so the screen reader never reads a raw ↑ glyph or
 * a stale "0%" when no budget exists. Separate from the composable so it's
 * pure, trivially inspectable in review, and reusable if a larger layout lands.
 */
private fun mediumA11yDescription(
    context: Context,
    state: ExpenseWidgetState,
): String {
    if (state.todayFormatted.isEmpty() || state.monthFormatted.isEmpty()) {
        return context.getString(R.string.a11y_widget_loading)
    }
    val budget = state.budget
    if (budget == null) {
        return context.getString(
            R.string.a11y_widget_medium_no_budget,
            state.todayFormatted,
            state.monthFormatted,
        )
    }
    // `progressFraction` is clamped to [0,1] for the visual bar, so the percent
    // reads as 100 when over-budget even though the raw ratio may be e.g. 110%.
    // That's acceptable — the "Over budget by X" clause carries the severity,
    // so TalkBack says: "Over budget by 50.000 ₫, 100% of 1.000.000 ₫."
    val percent = (budget.progressFraction * 100f).toInt()
    return if (budget.isOverBudget && budget.overByFormatted != null) {
        context.getString(
            R.string.a11y_widget_medium_over_budget,
            state.todayFormatted,
            state.monthFormatted,
            budget.overByFormatted,
            percent,
            budget.budgetFormatted,
        )
    } else {
        context.getString(
            R.string.a11y_widget_medium_with_budget,
            state.todayFormatted,
            state.monthFormatted,
            percent,
            budget.spentFormatted,
            budget.budgetFormatted,
        )
    }
}
