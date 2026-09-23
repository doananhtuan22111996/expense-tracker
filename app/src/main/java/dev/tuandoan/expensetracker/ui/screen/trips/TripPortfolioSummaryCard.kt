package dev.tuandoan.expensetracker.ui.screen.trips

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.tuandoan.expensetracker.R
import dev.tuandoan.expensetracker.domain.model.ActiveTripHighlight
import dev.tuandoan.expensetracker.domain.model.TripPortfolioSummary
import dev.tuandoan.expensetracker.ui.component.AmountText
import dev.tuandoan.expensetracker.ui.theme.DesignSystemSpacing

/**
 * Top summary card for the Trips screen, mirroring [PortfolioSummaryCard] from the Gold scope.
 * Displays aggregate travel spending in the default currency, status counters, and an active trip banner.
 */
@Composable
fun TripPortfolioSummaryCard(
    summary: TripPortfolioSummary,
    modifier: Modifier = Modifier,
    onActiveTripClick: ((Long) -> Unit)? = null,
) {
    ElevatedCard(modifier = modifier) {
        Column(modifier = Modifier.padding(DesignSystemSpacing.large)) {
            Text(
                text = stringResource(R.string.trips_portfolio_summary_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(DesignSystemSpacing.medium))

            // Total Travel Spend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.trips_total_travel_spend),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                AmountText(
                    amount = summary.totalSpend,
                    currencyCode = summary.currencyCode,
                    textStyle =
                        MaterialTheme.typography.titleMedium.copy(
                            color = MaterialTheme.colorScheme.primary,
                        ),
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(Modifier.height(DesignSystemSpacing.medium))

            // Trip Counters & Transactions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    horizontalArrangement = Arrangement.spacedBy(DesignSystemSpacing.medium),
                ) {
                    if (summary.activeTripsCount > 0) {
                        StatItem(
                            label = stringResource(R.string.trips_stat_active),
                            value = summary.activeTripsCount.toString(),
                            valueColor = MaterialTheme.colorScheme.primary,
                        )
                    }
                    if (summary.upcomingTripsCount > 0) {
                        StatItem(
                            label = stringResource(R.string.trips_stat_upcoming),
                            value = summary.upcomingTripsCount.toString(),
                        )
                    }
                    if (summary.pastTripsCount > 0) {
                        StatItem(
                            label = stringResource(R.string.trips_stat_past),
                            value = summary.pastTripsCount.toString(),
                        )
                    }
                }

                Spacer(Modifier.width(DesignSystemSpacing.small))

                Text(
                    text =
                        pluralStringResource(
                            R.plurals.trip_row_count,
                            summary.totalTransactionsCount,
                            summary.totalTransactionsCount,
                        ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Active Trip Highlight Banner
            summary.activeTripHighlight?.let { highlight ->
                Spacer(Modifier.height(DesignSystemSpacing.medium))
                HorizontalDivider()
                Spacer(Modifier.height(DesignSystemSpacing.medium))

                ActiveTripBanner(
                    highlight = highlight,
                    onClick = { onActiveTripClick?.invoke(highlight.trip.id) },
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = valueColor,
        )
        Spacer(Modifier.width(DesignSystemSpacing.xs))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ActiveTripBanner(
    highlight: ActiveTripHighlight,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val a11yDesc =
        stringResource(
            R.string.a11y_active_trip_card,
            highlight.trip.name,
            highlight.currentDay,
            highlight.totalDays,
        )
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .semantics { contentDescription = a11yDesc },
    ) {
        Row(
            modifier = Modifier.padding(DesignSystemSpacing.medium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.FlightTakeoff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(Modifier.width(DesignSystemSpacing.medium))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${stringResource(R.string.trips_active_banner_title)}: ${highlight.trip.name}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val dayProgress =
                        stringResource(
                            R.string.trips_active_banner_day,
                            highlight.currentDay,
                            highlight.totalDays,
                        )
                    val spentText = highlight.totalSpentLabel ?: stringResource(R.string.trip_row_no_spending)
                    Text(
                        text = "$dayProgress • $spentText",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
