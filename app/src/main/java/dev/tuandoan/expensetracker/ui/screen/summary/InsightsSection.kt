package dev.tuandoan.expensetracker.ui.screen.summary

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.tuandoan.expensetracker.R
import dev.tuandoan.expensetracker.domain.insights.InsightRow
import dev.tuandoan.expensetracker.ui.theme.DesignSystemElevation
import dev.tuandoan.expensetracker.ui.theme.DesignSystemSpacing

/**
 * Card-style Insights section anchored at the top of the Summary tab.
 *
 * Dispatches on [InsightsUiState]:
 *  - [InsightsUiState.Hidden] — returns without emitting any slot (handled by
 *    the caller; this composable assumes a non-Hidden state).
 *  - [InsightsUiState.Loading] — [InsightsShimmer] placeholder (FR-18).
 *  - [InsightsUiState.Error] — single-row "Insights unavailable" copy (FR-19).
 *  - [InsightsUiState.Populated] — header + body of [InsightRowItem]s, with
 *    collapse toggle persisting state (FR-21) via [onToggleCollapse].
 *
 * Does not render in year view — the [SummaryScreen] checks [InsightsUiState]
 * before calling in, so the section simply isn't emitted when mode = YEAR
 * (FR-20).
 */
@Composable
internal fun InsightsSection(
    state: InsightsUiState,
    onToggleCollapse: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state is InsightsUiState.Hidden) return

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = DesignSystemElevation.low),
    ) {
        Column(modifier = Modifier.padding(vertical = DesignSystemSpacing.small)) {
            val isCollapsed = (state as? InsightsUiState.Populated)?.isCollapsed ?: false
            val hasBody = state !is InsightsUiState.Populated || !isCollapsed

            SectionHeader(
                isCollapsed = isCollapsed,
                // Error and Loading states always show their body; only Populated
                // participates in the collapse flow (there's nothing meaningful
                // to hide for a shimmer or the "unavailable" copy).
                canCollapse = state is InsightsUiState.Populated,
                onToggleCollapse = onToggleCollapse,
            )

            AnimatedVisibility(visible = hasBody) {
                when (state) {
                    is InsightsUiState.Loading -> InsightsShimmer()
                    is InsightsUiState.Error ->
                        InsightRowItem(row = InsightRow.Error)
                    is InsightsUiState.Populated -> PopulatedBody(rows = state.result.rows)
                    InsightsUiState.Hidden -> Unit // unreachable — early return above
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    isCollapsed: Boolean,
    canCollapse: Boolean,
    onToggleCollapse: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val toggleDesc =
        if (isCollapsed) {
            stringResource(R.string.insights_collapse_expand)
        } else {
            stringResource(R.string.insights_collapse_collapse)
        }

    val rowModifier =
        if (canCollapse) {
            Modifier
                .fillMaxWidth()
                .clickable { onToggleCollapse(!isCollapsed) }
                .semantics {
                    role = Role.Button
                    contentDescription = toggleDesc
                }
        } else {
            Modifier.fillMaxWidth()
        }

    Row(
        modifier =
            modifier
                .then(rowModifier)
                .padding(
                    horizontal = DesignSystemSpacing.large,
                    vertical = DesignSystemSpacing.small,
                ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.insights_section_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (canCollapse) {
            Icon(
                imageVector =
                    if (isCollapsed) {
                        Icons.Filled.KeyboardArrowDown
                    } else {
                        Icons.Filled.KeyboardArrowUp
                    },
                contentDescription = null, // covered by Row's contentDescription
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
private fun PopulatedBody(rows: List<InsightRow>) {
    Column {
        rows.forEach { row ->
            InsightRowItem(row = row)
        }
    }
}
