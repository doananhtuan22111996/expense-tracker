package dev.tuandoan.expensetracker.ui.screen.trips

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.tuandoan.expensetracker.R
import dev.tuandoan.expensetracker.core.formatter.AmountFormatter
import dev.tuandoan.expensetracker.core.util.DateTimeUtil
import dev.tuandoan.expensetracker.domain.model.Transaction
import dev.tuandoan.expensetracker.domain.model.Trip
import dev.tuandoan.expensetracker.ui.component.DonutChart
import dev.tuandoan.expensetracker.ui.component.SectionTitle
import dev.tuandoan.expensetracker.ui.theme.DesignSystemElevation
import dev.tuandoan.expensetracker.ui.theme.DesignSystemSpacing
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (tripId: Long) -> Unit,
    onNavigateToEditTransaction: (transactionId: Long) -> Unit,
    viewModel: TripDetailViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(uiState.tripGone) {
        if (uiState.tripGone) {
            snackbarHostState.showSnackbar(context.getString(R.string.trip_gone_message))
            onNavigateBack()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg.asString(context))
            viewModel.clearError()
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TripDetailTopBar(
                title = uiState.trip?.name ?: "",
                isConversionOrigin = uiState.trip?.isConversionOrigin ?: false,
                onNavigateBack = onNavigateBack,
                onEdit = { uiState.trip?.let { onNavigateToEdit(it.id) } },
                onDelete = viewModel::requestDelete,
                onRevert = viewModel::requestRevert,
            )
        },
    ) { innerPadding ->
        when (uiState.pendingAction) {
            PendingTripAction.UNTAG_DELETE ->
                DeleteTripDialog(
                    onConfirm = viewModel::confirmAction,
                    onDismiss = viewModel::dismissAction,
                )
            PendingTripAction.REVERT ->
                RevertTripDialog(
                    originalCategoryName = uiState.trip?.originalCategoryNameSnapshot ?: "",
                    onConfirm = viewModel::confirmAction,
                    onDismiss = viewModel::dismissAction,
                )
            null -> Unit
        }

        if (uiState.isLoading) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val trip = uiState.trip ?: return@Scaffold

        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = DesignSystemSpacing.screenPadding),
            verticalArrangement = Arrangement.spacedBy(DesignSystemSpacing.xl),
        ) {
            // Header: destination, date range, status chip
            item {
                TripDetailHeader(trip = trip, tripStatus = uiState.tripStatus)
            }

            // KPI row
            item {
                KpiRow(
                    totalLabel = uiState.totalLabel,
                    dailyAvgLabel = uiState.dailyAvgLabel,
                    transactionCount = uiState.transactionCount,
                )
            }

            // Donut chart
            if (uiState.categoryTotals.isNotEmpty()) {
                item {
                    SectionTitle(title = stringResource(R.string.trip_detail_section_spending))
                    DonutChart(
                        categories = uiState.categoryTotals,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            // Day-by-day bar chart
            if (uiState.dailyPoints.isNotEmpty()) {
                item {
                    SectionTitle(title = stringResource(R.string.trip_detail_section_by_day))
                    DailyBarChart(
                        points = uiState.dailyPoints,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            // Transaction list
            item {
                SectionTitle(title = stringResource(R.string.trip_detail_section_transactions))
            }

            if (uiState.transactions.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.trip_detail_transactions_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(uiState.transactions, key = { it.id }) { tx ->
                    TripTransactionItem(
                        transaction = tx,
                        onClick = { onNavigateToEditTransaction(tx.id) },
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(DesignSystemSpacing.xl)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TripDetailTopBar(
    title: String,
    isConversionOrigin: Boolean,
    onNavigateBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onRevert: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val menuDesc = stringResource(R.string.a11y_trip_detail_menu)

    TopAppBar(
        title = {
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.a11y_navigate_back),
                )
            }
        },
        actions = {
            IconButton(
                onClick = { menuExpanded = true },
                modifier = Modifier.semantics { contentDescription = menuDesc },
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = null,
                )
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.trip_detail_edit)) },
                    onClick = {
                        menuExpanded = false
                        onEdit()
                    },
                )
                HorizontalDivider()
                if (isConversionOrigin) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.trip_detail_revert)) },
                        onClick = {
                            menuExpanded = false
                            onRevert()
                        },
                    )
                }
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(R.string.trip_detail_delete),
                            color = MaterialTheme.colorScheme.error,
                        )
                    },
                    onClick = {
                        menuExpanded = false
                        onDelete()
                    },
                )
            }
        },
    )
}

@Composable
private fun TripDetailHeader(
    trip: Trip,
    tripStatus: TripStatus,
    modifier: Modifier = Modifier,
) {
    val dateRange = formatTripDateRange(trip.startDateEpochDay, trip.endDateEpochDay)
    val statusLabel =
        when (tripStatus) {
            TripStatus.ACTIVE -> stringResource(R.string.trip_detail_status_active)
            TripStatus.UPCOMING -> stringResource(R.string.trip_detail_status_upcoming)
            TripStatus.PAST -> stringResource(R.string.trip_detail_status_past)
        }
    val statusContainerColor =
        when (tripStatus) {
            TripStatus.ACTIVE -> MaterialTheme.colorScheme.primaryContainer
            TripStatus.UPCOMING -> MaterialTheme.colorScheme.secondaryContainer
            TripStatus.PAST -> MaterialTheme.colorScheme.surfaceVariant
        }
    val statusContentColor =
        when (tripStatus) {
            TripStatus.ACTIVE -> MaterialTheme.colorScheme.onPrimaryContainer
            TripStatus.UPCOMING -> MaterialTheme.colorScheme.onSecondaryContainer
            TripStatus.PAST -> MaterialTheme.colorScheme.onSurfaceVariant
        }
    val statusDesc = stringResource(R.string.a11y_trip_status, statusLabel)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(DesignSystemSpacing.small)) {
        if (!trip.destination.isNullOrBlank()) {
            Text(
                text = trip.destination,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = dateRange,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SuggestionChip(
            onClick = {},
            label = { Text(text = statusLabel, style = MaterialTheme.typography.labelMedium) },
            modifier = Modifier.semantics { contentDescription = statusDesc },
            colors =
                SuggestionChipDefaults.suggestionChipColors(
                    containerColor = statusContainerColor,
                    labelColor = statusContentColor,
                ),
            border = null,
        )
    }
}

@Composable
private fun KpiRow(
    totalLabel: String?,
    dailyAvgLabel: String?,
    transactionCount: Int,
    modifier: Modifier = Modifier,
) {
    val noData = stringResource(R.string.trip_detail_kpi_no_data)
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        KpiTile(
            label = stringResource(R.string.trip_detail_kpi_total),
            value = totalLabel ?: noData,
            modifier = Modifier.weight(1f),
        )
        KpiTile(
            label = stringResource(R.string.trip_detail_kpi_daily_avg),
            value = dailyAvgLabel ?: noData,
            modifier = Modifier.weight(1f),
        )
        KpiTile(
            label = stringResource(R.string.trip_detail_kpi_count),
            value = transactionCount.toString(),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun KpiTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(DesignSystemSpacing.small),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(DesignSystemSpacing.xs),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Simple day-grain bar chart for trip spending. Renders up to 31 bars (or weekly
 * buckets for longer trips). Labels are the [DailyBarPoint.dayLabel] values produced
 * by [TripDetailViewModel].
 */
@Composable
private fun DailyBarChart(
    points: List<DailyBarPoint>,
    modifier: Modifier = Modifier,
) {
    if (points.isEmpty()) {
        Text(
            text = stringResource(R.string.trip_detail_empty_chart),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.padding(DesignSystemSpacing.large),
        )
        return
    }
    val maxTotal = points.maxOf { it.totalMinor }.coerceAtLeast(1L).toFloat()
    val barColor = MaterialTheme.colorScheme.primary
    val barRadius = CornerRadius(4.dp.value, 4.dp.value)

    Canvas(
        modifier =
            modifier
                .fillMaxWidth()
                .height(120.dp),
    ) {
        val barCount = points.size
        val totalWidth = size.width
        val chartHeight = size.height
        val barWidth = (totalWidth / barCount) * 0.6f
        val spacing = (totalWidth / barCount) * 0.4f / 2f

        points.forEachIndexed { index, point ->
            val barHeight = (point.totalMinor.toFloat() / maxTotal) * chartHeight * 0.85f
            val x = index * (totalWidth / barCount) + spacing
            val top = chartHeight - barHeight
            drawRoundRect(
                color = barColor,
                topLeft = Offset(x, top),
                size = Size(barWidth, barHeight),
                cornerRadius = barRadius,
            )
        }
    }
}

@Composable
private fun TripTransactionItem(
    transaction: Transaction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val formattedDate = DateTimeUtil.formatShortDate(transaction.timestamp)
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = DesignSystemElevation.low),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(DesignSystemSpacing.large),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.category.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!transaction.note.isNullOrBlank()) {
                    Text(
                        text = transaction.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text =
                    AmountFormatter.formatAmountWithCurrency(
                        transaction.amount,
                        transaction.currencyCode,
                    ),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = DesignSystemSpacing.medium),
            )
        }
    }
}

@Composable
private fun DeleteTripDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.trip_delete_dialog_title)) },
        text = { Text(stringResource(R.string.trip_delete_dialog_body)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(R.string.trip_action_delete),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
private fun RevertTripDialog(
    originalCategoryName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.trip_revert_dialog_title)) },
        text = {
            Text(stringResource(R.string.trip_revert_dialog_body, originalCategoryName))
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.trip_action_revert))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

private val TRIP_DETAIL_DATE_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

private fun formatTripDateRange(
    startEpochDay: Long,
    endEpochDay: Long,
): String {
    val start = LocalDate.ofEpochDay(startEpochDay).format(TRIP_DETAIL_DATE_FORMATTER)
    val end = LocalDate.ofEpochDay(endEpochDay).format(TRIP_DETAIL_DATE_FORMATTER)
    return "$start – $end"
}
