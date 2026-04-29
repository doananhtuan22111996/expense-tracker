package dev.tuandoan.expensetracker.ui.screen.summary

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.TrendingDown
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.tuandoan.expensetracker.R
import dev.tuandoan.expensetracker.domain.insights.InsightRow
import dev.tuandoan.expensetracker.ui.theme.DesignSystemSpacing
import kotlin.math.absoluteValue

/**
 * Single-row render for an [InsightRow]. Builds the headline string from the
 * engine's pre-formatted fields (amounts and percentages never formatted here
 * per PRD FR-03) and attaches an optional delta chip.
 *
 * Rows are read-only (PRD FR-04: no drill-down). A single coherent
 * `contentDescription` on the row container replaces the otherwise-fragmented
 * multi-node TalkBack reading — same pattern as the widget accessibility pass
 * in PR #90.
 */
@Composable
internal fun InsightRowItem(
    row: InsightRow,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val icon = row.icon()
    val headline = row.headline(context)
    val delta = row.deltaChipData()

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(
                    horizontal = DesignSystemSpacing.large,
                    vertical = DesignSystemSpacing.medium,
                ).semantics {
                    contentDescription = headline
                },
        horizontalArrangement = Arrangement.spacedBy(DesignSystemSpacing.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp),
        )
        Text(
            text = headline,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (delta != null) {
            DeltaChip(
                percent = delta.absolutePercent,
                direction = delta.direction,
            )
        }
    }
}

@Composable
private fun DeltaChip(
    percent: Int,
    direction: InsightRow.Direction,
) {
    // errorContainer for up, secondaryContainer for down — keeps the chip
    // responsive to light/dark theme without bespoke palette work. Colorblind
    // severity carried by the ↑/↓ glyph, not the hue.
    val up = direction == InsightRow.Direction.UP
    val format =
        if (up) R.string.insights_delta_up_format else R.string.insights_delta_down_format
    val bg: Color =
        if (up) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            MaterialTheme.colorScheme.secondaryContainer
        }
    val fg: Color =
        if (up) {
            MaterialTheme.colorScheme.onErrorContainer
        } else {
            MaterialTheme.colorScheme.onSecondaryContainer
        }

    Box(
        modifier =
            Modifier
                .background(color = bg, shape = RoundedCornerShape(DesignSystemSpacing.small))
                .padding(
                    horizontal = DesignSystemSpacing.small,
                    vertical = DesignSystemSpacing.xs,
                ),
    ) {
        Text(
            text = stringResource(format, percent),
            style = MaterialTheme.typography.labelMedium,
            color = fg,
        )
    }
}

private data class DeltaChipData(
    val absolutePercent: Int,
    val direction: InsightRow.Direction,
)

private fun InsightRow.icon(): ImageVector =
    when (this) {
        is InsightRow.BiggestMover ->
            if (direction == InsightRow.Direction.UP) {
                Icons.Filled.TrendingUp
            } else {
                Icons.Filled.TrendingDown
            }
        is InsightRow.DailyPace -> Icons.Outlined.Speed
        is InsightRow.NoBudgetFallback -> Icons.Outlined.Speed
        is InsightRow.DayOfMonth ->
            when (direction) {
                InsightRow.Direction.UP -> Icons.Outlined.TrendingUp
                InsightRow.Direction.DOWN -> Icons.Outlined.TrendingDown
                null -> Icons.Outlined.Schedule
            }
        InsightRow.Empty -> Icons.Outlined.Info
        InsightRow.Error -> Icons.Outlined.ErrorOutline
    }

private fun InsightRow.headline(context: android.content.Context): String =
    when (this) {
        is InsightRow.BiggestMover -> {
            val format =
                if (direction == InsightRow.Direction.UP) {
                    R.string.insights_biggest_mover_up_format
                } else {
                    R.string.insights_biggest_mover_down_format
                }
            context.getString(
                format,
                categoryName,
                percentChange.absoluteValue,
                previousFormatted,
                currentFormatted,
            )
        }
        is InsightRow.DailyPace ->
            when (status) {
                InsightRow.PaceStatus.ON_PACE ->
                    context.getString(
                        R.string.insights_daily_pace_on_pace_format,
                        projectedFormatted,
                    )
                InsightRow.PaceStatus.OVER ->
                    context.getString(
                        R.string.insights_daily_pace_over_format,
                        differenceFormatted.orEmpty(),
                        budgetFormatted,
                    )
                InsightRow.PaceStatus.UNDER ->
                    context.getString(
                        R.string.insights_daily_pace_under_format,
                        differenceFormatted.orEmpty(),
                        budgetFormatted,
                    )
            }
        is InsightRow.NoBudgetFallback ->
            context.getString(
                R.string.insights_no_budget_fallback_format,
                monthSpendFormatted,
                dailyAverageFormatted,
            )
        is InsightRow.DayOfMonth -> {
            val pct = percentChange
            val dir = direction
            when {
                pct == null || dir == null ->
                    context.getString(
                        R.string.insights_day_of_month_neutral_format,
                        dayOfMonth,
                        currentFormatted,
                    )
                dir == InsightRow.Direction.UP ->
                    context.getString(
                        R.string.insights_day_of_month_up_format,
                        dayOfMonth,
                        currentFormatted,
                        pct.absoluteValue,
                    )
                else ->
                    context.getString(
                        R.string.insights_day_of_month_down_format,
                        dayOfMonth,
                        currentFormatted,
                        pct.absoluteValue,
                    )
            }
        }
        InsightRow.Empty ->
            context.getString(R.string.insights_empty_title) +
                " — " +
                context.getString(R.string.insights_empty_subtitle)
        InsightRow.Error -> context.getString(R.string.insights_error_title)
    }

private fun InsightRow.deltaChipData(): DeltaChipData? =
    when (this) {
        is InsightRow.BiggestMover ->
            DeltaChipData(
                absolutePercent = percentChange.absoluteValue,
                direction = direction,
            )
        is InsightRow.DayOfMonth -> {
            val pct = percentChange ?: return null
            val dir = direction ?: return null
            DeltaChipData(absolutePercent = pct.absoluteValue, direction = dir)
        }
        is InsightRow.DailyPace,
        is InsightRow.NoBudgetFallback,
        InsightRow.Empty,
        InsightRow.Error,
        -> null
    }
