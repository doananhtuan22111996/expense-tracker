package dev.tuandoan.expensetracker.ui.screen.trips

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.tuandoan.expensetracker.R
import dev.tuandoan.expensetracker.core.formatter.AmountFormatter
import dev.tuandoan.expensetracker.core.util.DateTimeUtil
import dev.tuandoan.expensetracker.domain.model.BudgetStatusLevel
import dev.tuandoan.expensetracker.domain.model.Transaction
import dev.tuandoan.expensetracker.domain.model.Trip
import dev.tuandoan.expensetracker.domain.model.TripBudgetStatus
import dev.tuandoan.expensetracker.ui.component.DonutChart
import dev.tuandoan.expensetracker.ui.component.SectionTitle
import dev.tuandoan.expensetracker.ui.theme.DesignSystemElevation
import dev.tuandoan.expensetracker.ui.theme.DesignSystemSpacing
import kotlinx.coroutines.launch
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

    val tripGoneMessage = stringResource(R.string.trip_gone_message)
    val tripSummaryCopiedMessage = stringResource(R.string.trip_summary_copied)
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    val defaultCsvFileName = "trip_${uiState.trip?.name?.replace(Regex("[^\\p{L}0-9_-]"), "_") ?: "export"}.csv"
    val csvLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("text/csv"),
        ) { uri: Uri? ->
            if (uri != null) {
                viewModel.exportTripCsv(uri)
            }
        }

    LaunchedEffect(uiState.tripGone) {
        if (uiState.tripGone) {
            snackbarHostState.showSnackbar(tripGoneMessage)
            onNavigateBack()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg.asString(context))
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg.asString(context))
            viewModel.clearUserMessage()
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
                onExportCsv = { csvLauncher.launch(defaultCsvFileName) },
                onShareSummary = {
                    val summary = viewModel.getShareableTripSummary()
                    if (summary != null) {
                        clipboardManager.setText(AnnotatedString(summary))
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(tripSummaryCopiedMessage)
                        }
                    }
                },
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
            // Header: destination, date range, status chip, duration
            item {
                TripDetailHeader(
                    trip = trip,
                    tripStatus = uiState.tripStatus,
                    totalDays = uiState.totalDays,
                    daysElapsed = uiState.daysElapsed,
                    daysRemaining = uiState.daysRemaining,
                    daysUntilStart = uiState.daysUntilStart,
                )
            }

            // KPI row
            item {
                KpiRow(
                    totalLabel = uiState.totalLabel,
                    dailyAvgLabel = uiState.dailyAvgLabel,
                    transactionCount = uiState.transactionCount,
                )
            }

            // Trip budget progress card (v3.15.0)
            if (uiState.budgetStatus != null) {
                item {
                    TripBudgetProgressCard(
                        budgetStatus = uiState.budgetStatus!!,
                        budgetLabel = uiState.budgetLabel.orEmpty(),
                        totalLabel = uiState.totalLabel.orEmpty(),
                        remainingLabel = uiState.budgetRemainingLabel.orEmpty(),
                        dailyAllowanceLabel = uiState.dailyAllowanceLabel,
                        tripStatus = uiState.tripStatus,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            // Donut chart
            if (uiState.categoryTotals.isNotEmpty()) {
                item {
                    SectionTitle(title = stringResource(R.string.trip_detail_section_spending))
                    DonutChart(
                        categories = uiState.categoryTotals,
                        selectedCategoryId = uiState.selectedCategoryId,
                        onCategoryClick = viewModel::onCategoryClick,
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SectionTitle(title = stringResource(R.string.trip_detail_section_transactions))
                    if (uiState.selectedCategoryId != null) {
                        val selectedCategory =
                            uiState.categoryTotals
                                .firstOrNull { it.category.id == uiState.selectedCategoryId }
                                ?.category
                        if (selectedCategory != null) {
                            FilterChip(
                                selected = true,
                                onClick = viewModel::clearCategoryFilter,
                                label = { Text(selectedCategory.name) },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription =
                                            stringResource(
                                                R.string.a11y_clear_filter,
                                                selectedCategory.name,
                                            ),
                                        modifier = Modifier.padding(start = 2.dp),
                                    )
                                },
                            )
                        }
                    }
                }
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
                    val isSelectedCategory = uiState.selectedCategoryId != null
                    val matchesCategory = tx.category.id == uiState.selectedCategoryId
                    TripTransactionItem(
                        transaction = tx,
                        foreignCurrencyCode = uiState.trip?.foreignCurrencyCode,
                        isHighlighted = isSelectedCategory && matchesCategory,
                        isDimmed = isSelectedCategory && !matchesCategory,
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
    onExportCsv: () -> Unit,
    onShareSummary: () -> Unit,
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
                    text = { Text(stringResource(R.string.trip_detail_export_csv)) },
                    onClick = {
                        menuExpanded = false
                        onExportCsv()
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.trip_detail_share_summary)) },
                    onClick = {
                        menuExpanded = false
                        onShareSummary()
                    },
                )
                HorizontalDivider()
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
    totalDays: Int,
    daysElapsed: Int,
    daysRemaining: Int,
    daysUntilStart: Int,
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
    val durationLabel =
        when (tripStatus) {
            TripStatus.ACTIVE -> {
                when (daysRemaining) {
                    0 -> stringResource(R.string.trip_detail_days_active_last_day, daysElapsed, totalDays)
                    1 -> stringResource(R.string.trip_detail_days_active_single_left, daysElapsed, totalDays)
                    else ->
                        stringResource(
                            R.string.trip_detail_days_active_multiple_left,
                            daysElapsed,
                            totalDays,
                            daysRemaining,
                        )
                }
            }
            TripStatus.UPCOMING -> {
                if (daysUntilStart == 1) {
                    stringResource(R.string.trip_detail_days_upcoming_single, totalDays)
                } else {
                    stringResource(R.string.trip_detail_days_upcoming_multiple, daysUntilStart, totalDays)
                }
            }
            TripStatus.PAST -> {
                stringResource(R.string.trip_detail_days_past, totalDays)
            }
        }

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
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DesignSystemSpacing.small),
        ) {
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
            Text(
                text = durationLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
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
    foreignCurrencyCode: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isHighlighted: Boolean = false,
    isDimmed: Boolean = false,
) {
    val formattedDate = DateTimeUtil.formatShortDate(transaction.timestamp)
    val cardAlpha = if (isDimmed) 0.35f else 1f
    val containerColor =
        if (isHighlighted) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        } else {
            MaterialTheme.colorScheme.surface
        }

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
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
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = cardAlpha),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!transaction.note.isNullOrBlank()) {
                    Text(
                        text = transaction.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = cardAlpha),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = cardAlpha),
                )
            }
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.padding(start = DesignSystemSpacing.medium),
            ) {
                if (transaction.amountForeignMinor != null && !foreignCurrencyCode.isNullOrBlank()) {
                    Text(
                        text =
                            AmountFormatter.formatAmountWithCurrency(
                                transaction.amountForeignMinor,
                                foreignCurrencyCode,
                            ),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = cardAlpha),
                    )
                    Text(
                        text =
                            AmountFormatter.formatAmountWithCurrency(
                                transaction.amount,
                                transaction.currencyCode,
                            ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = cardAlpha),
                    )
                } else {
                    Text(
                        text =
                            AmountFormatter.formatAmountWithCurrency(
                                transaction.amount,
                                transaction.currencyCode,
                            ),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = cardAlpha),
                    )
                }
            }
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

@Composable
internal fun TripBudgetProgressCard(
    budgetStatus: TripBudgetStatus,
    budgetLabel: String,
    totalLabel: String,
    remainingLabel: String,
    dailyAllowanceLabel: String?,
    tripStatus: TripStatus,
    modifier: Modifier = Modifier,
) {
    val progressPercent = (budgetStatus.progressFraction * 100).toInt()
    val a11yDescription =
        stringResource(
            R.string.a11y_trip_budget_progress,
            progressPercent,
            totalLabel,
            budgetLabel,
        )

    val (statusLabel, statusColor, statusContainerColor) =
        when (budgetStatus.status) {
            BudgetStatusLevel.OVER_BUDGET ->
                Triple(
                    stringResource(R.string.trip_detail_budget_status_over),
                    MaterialTheme.colorScheme.error,
                    MaterialTheme.colorScheme.errorContainer,
                )
            BudgetStatusLevel.WARNING ->
                Triple(
                    stringResource(R.string.trip_detail_budget_status_warning),
                    MaterialTheme.colorScheme.tertiary,
                    MaterialTheme.colorScheme.tertiaryContainer,
                )
            BudgetStatusLevel.OK ->
                Triple(
                    stringResource(R.string.trip_detail_budget_status_ok),
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.primaryContainer,
                )
        }

    Card(
        modifier = modifier.semantics { contentDescription = a11yDescription },
        elevation = CardDefaults.cardElevation(defaultElevation = DesignSystemElevation.low),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
    ) {
        Column(
            modifier = Modifier.padding(DesignSystemSpacing.large),
            verticalArrangement = Arrangement.spacedBy(DesignSystemSpacing.medium),
        ) {
            // Header: Title + Status Chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.trip_detail_budget_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                SuggestionChip(
                    onClick = {},
                    label = {
                        Text(
                            text = statusLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                        )
                    },
                    colors =
                        SuggestionChipDefaults.suggestionChipColors(
                            containerColor = statusContainerColor,
                            labelColor = statusColor,
                        ),
                    border = null,
                )
            }

            // Progress bar
            LinearProgressIndicator(
                progress = { budgetStatus.progressFraction.coerceIn(0f, 1f) },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                color = statusColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )

            // Numbers row: Spent · Budget · Remaining / Over
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.trip_detail_budget_label_spent, totalLabel),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.trip_detail_budget_label_budget, budgetLabel),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text =
                        if (budgetStatus.status == BudgetStatusLevel.OVER_BUDGET) {
                            stringResource(R.string.trip_detail_budget_over_budget_banner, remainingLabel)
                        } else {
                            stringResource(R.string.trip_detail_budget_label_remaining, remainingLabel)
                        },
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color =
                        if (budgetStatus.status == BudgetStatusLevel.OVER_BUDGET) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                )
            }

            // Daily allowance section or Over-budget banner
            if (budgetStatus.status == BudgetStatusLevel.OVER_BUDGET) {
                Card(
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                        ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(DesignSystemSpacing.small),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(DesignSystemSpacing.small),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                        )
                        Text(
                            text = stringResource(R.string.trip_detail_budget_over_budget_banner, remainingLabel),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
            } else if (dailyAllowanceLabel != null) {
                Card(
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(DesignSystemSpacing.small),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(
                                text =
                                    if (tripStatus == TripStatus.ACTIVE) {
                                        stringResource(R.string.trip_detail_budget_daily_allowance, dailyAllowanceLabel)
                                    } else {
                                        stringResource(
                                            R.string.trip_detail_budget_planned_allowance,
                                            dailyAllowanceLabel,
                                        )
                                    },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            val days = budgetStatus.remainingDays ?: 0
                            Text(
                                text =
                                    if (tripStatus == TripStatus.ACTIVE) {
                                        stringResource(R.string.trip_detail_budget_days_remaining, days)
                                    } else {
                                        stringResource(R.string.trip_detail_budget_total_days, days)
                                    },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
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
