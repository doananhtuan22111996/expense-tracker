package dev.tuandoan.expensetracker.ui.screen.gold

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Paid
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.tuandoan.expensetracker.R
import dev.tuandoan.expensetracker.core.formatter.CurrencyAmountVisualTransformation
import dev.tuandoan.expensetracker.domain.model.GoldHolding
import dev.tuandoan.expensetracker.domain.model.GoldHoldingWithPnL
import dev.tuandoan.expensetracker.domain.model.GoldPortfolioSummary
import dev.tuandoan.expensetracker.domain.model.GoldPrice
import dev.tuandoan.expensetracker.domain.model.GoldType
import dev.tuandoan.expensetracker.domain.model.GoldWeightUnit
import dev.tuandoan.expensetracker.ui.component.AmountText
import dev.tuandoan.expensetracker.ui.component.ErrorStateMessage
import dev.tuandoan.expensetracker.ui.theme.DesignSystemSpacing
import dev.tuandoan.expensetracker.ui.theme.FinancialColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoldPortfolioScreen(
    viewModel: GoldPortfolioViewModel,
    modifier: Modifier = Modifier,
    onNavigateToAddHolding: () -> Unit = {},
    onNavigateToEditHolding: (holdingId: Long) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showPriceSheet by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val holdingDeletedMsg = stringResource(R.string.gold_holding_deleted)
    val undoMsg = stringResource(R.string.undo)
    val pricesUpdatedMsg = stringResource(R.string.gold_prices_updated)

    LaunchedEffect(uiState.isError) {
        val message = uiState.errorMessage
        if (uiState.isError && message != null) {
            snackbarHostState.showSnackbar(message.asString(context))
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.lastDeletedHolding) {
        uiState.lastDeletedHolding?.let {
            val result =
                snackbarHostState.showSnackbar(
                    message = holdingDeletedMsg,
                    actionLabel = undoMsg,
                    duration = SnackbarDuration.Short,
                )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undoDelete()
            } else {
                viewModel.clearLastDeleted()
            }
        }
    }

    LaunchedEffect(uiState.showPricesUpdated) {
        if (uiState.showPricesUpdated) {
            snackbarHostState.showSnackbar(pricesUpdatedMsg)
            viewModel.clearPricesUpdatedFlag()
        }
    }

    if (showPriceSheet && uiState.currentPrices.isNotEmpty()) {
        UpdatePricesBottomSheet(
            currentPrices = uiState.currentPrices,
            currencyCode = uiState.currencyCode,
            onSave = { prices ->
                viewModel.savePrices(prices)
                showPriceSheet = false
            },
            onDismiss = { showPriceSheet = false },
        )
    }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.gold_portfolio_title)) },
                windowInsets = WindowInsets(0, 0, 0, 0),
            )
        },
        floatingActionButton = {
            if (uiState.holdings.isNotEmpty()) {
                val addHoldingDesc = stringResource(R.string.gold_add_holding)
                FloatingActionButton(
                    onClick = onNavigateToAddHolding,
                    modifier =
                        Modifier.semantics {
                            contentDescription = addHoldingDesc
                        },
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    val loadingDesc = stringResource(R.string.a11y_loading_gold_portfolio)
                    CircularProgressIndicator(
                        modifier =
                            Modifier.semantics {
                                contentDescription = loadingDesc
                            },
                    )
                }
            }

            uiState.isError && uiState.holdings.isEmpty() -> {
                ErrorStateMessage(
                    title = stringResource(R.string.error_load_portfolio),
                    message = uiState.errorMessage?.asString() ?: stringResource(R.string.error_unexpected),
                    onRetry = viewModel::retry,
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                )
            }

            uiState.holdings.isEmpty() -> {
                GoldEmptyState(
                    onAddHolding = onNavigateToAddHolding,
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                )
            }

            else -> {
                GoldPortfolioContent(
                    uiState = uiState,
                    onUpdatePrices = { showPriceSheet = true },
                    onEditHolding = onNavigateToEditHolding,
                    onDeleteHolding = viewModel::deleteHolding,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                )
            }
        }
    }
}

@Composable
private fun GoldEmptyState(
    onAddHolding: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Outlined.Paid,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(DesignSystemSpacing.large))
            Text(
                text = stringResource(R.string.gold_empty_title),
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(Modifier.height(DesignSystemSpacing.small))
            Text(
                text = stringResource(R.string.gold_empty_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(DesignSystemSpacing.xl))
            FilledTonalButton(onClick = onAddHolding) {
                Text(stringResource(R.string.gold_add_first))
            }
        }
    }
}

@Composable
private fun GoldPortfolioContent(
    uiState: GoldPortfolioUiState,
    onUpdatePrices: () -> Unit,
    onEditHolding: (holdingId: Long) -> Unit,
    onDeleteHolding: (GoldHolding) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.padding(horizontal = DesignSystemSpacing.screenPadding),
        verticalArrangement = Arrangement.spacedBy(DesignSystemSpacing.componentSpacing),
    ) {
        // Portfolio Summary
        uiState.summary?.let { summary ->
            item(key = "summary") {
                PortfolioSummaryCard(
                    summary = summary,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        if (uiState.summary == null && uiState.holdings.isNotEmpty()) {
            item(key = "no_prices") {
                Card(
                    onClick = onUpdatePrices,
                    modifier = Modifier.fillMaxWidth(),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                ) {
                    Row(
                        modifier = Modifier.padding(DesignSystemSpacing.large),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Outlined.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.width(DesignSystemSpacing.small))
                        Text(
                            text = stringResource(R.string.gold_no_prices_set),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        // Current Prices section
        if (uiState.currentPrices.isNotEmpty()) {
            item(key = "prices_header") {
                Spacer(Modifier.height(DesignSystemSpacing.small))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.gold_current_prices),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    val updatePricesDesc = stringResource(R.string.a11y_update_gold_prices)
                    TextButton(
                        onClick = onUpdatePrices,
                        modifier =
                            Modifier.semantics {
                                contentDescription = updatePricesDesc
                            },
                    ) {
                        Text(stringResource(R.string.gold_update_prices))
                    }
                }
            }

            item(key = "prices_list") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                ) {
                    Column(modifier = Modifier.padding(DesignSystemSpacing.large)) {
                        uiState.currentPrices.forEachIndexed { index, price ->
                            if (index > 0) {
                                HorizontalDivider(
                                    modifier =
                                        Modifier.padding(
                                            vertical = DesignSystemSpacing.small,
                                        ),
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text =
                                        "${goldTypeLabel(price.type)} / ${goldUnitLabel(price.unit)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                if (price.sellPricePerUnit > 0) {
                                    AmountText(
                                        amount = price.sellPricePerUnit,
                                        currencyCode = price.currencyCode,
                                        textStyle = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                    )
                                } else {
                                    Text(
                                        text = "—",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Holdings section
        item(key = "holdings_header") {
            Spacer(Modifier.height(DesignSystemSpacing.small))
            Text(
                text = stringResource(R.string.gold_holdings_count, uiState.holdings.size),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }

        items(
            items = uiState.holdings,
            key = { it.holding.id },
        ) { holdingWithPnL ->
            SwipeToDismissHoldingCard(
                holdingWithPnL = holdingWithPnL,
                onEdit = { onEditHolding(holdingWithPnL.holding.id) },
                onDelete = { onDeleteHolding(holdingWithPnL.holding) },
                modifier = Modifier.fillMaxWidth().animateItem(),
            )
        }

        item(key = "bottom_spacer") {
            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
private fun PortfolioSummaryCard(
    summary: GoldPortfolioSummary,
    modifier: Modifier = Modifier,
) {
    val pnlColor =
        FinancialColors.balanceColor(summary.totalPnL >= 0)
    val pnlSign = if (summary.totalPnL >= 0) "+" else ""
    val pnlPercentText = "$pnlSign%.1f%%".format(summary.pnLPercent)

    ElevatedCard(modifier = modifier) {
        Column(modifier = Modifier.padding(DesignSystemSpacing.large)) {
            Text(
                text = stringResource(R.string.gold_portfolio_summary),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(DesignSystemSpacing.medium))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    stringResource(R.string.gold_total_cost),
                    style = MaterialTheme.typography.bodyMedium,
                )
                AmountText(
                    amount = summary.totalCost,
                    currencyCode = summary.currencyCode,
                    textStyle = MaterialTheme.typography.bodyMedium,
                )
            }

            Spacer(Modifier.height(DesignSystemSpacing.xs))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    stringResource(R.string.gold_current_value),
                    style = MaterialTheme.typography.bodyMedium,
                )
                AmountText(
                    amount = summary.totalCurrentValue,
                    currencyCode = summary.currencyCode,
                    textStyle = MaterialTheme.typography.bodyMedium,
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = DesignSystemSpacing.small))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.gold_pnl),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Spacer(Modifier.width(DesignSystemSpacing.small))
                    val pnlDesc =
                        if (summary.totalPnL >= 0) {
                            stringResource(R.string.a11y_gold_profit)
                        } else {
                            stringResource(R.string.a11y_gold_loss)
                        }
                    Box(
                        modifier =
                            Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(pnlColor)
                                .semantics {
                                    contentDescription = pnlDesc
                                },
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AmountText(
                        amount = summary.totalPnL,
                        currencyCode = summary.currencyCode,
                        showSign = true,
                        textStyle = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.width(DesignSystemSpacing.small))
                    Text(
                        text = pnlPercentText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = pnlColor,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDismissHoldingCard(
    holdingWithPnL: GoldHoldingWithPnL,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dismissState =
        rememberSwipeToDismissBoxState(
            confirmValueChange = { value ->
                if (value == SwipeToDismissBoxValue.EndToStart) {
                    onDelete()
                    true
                } else {
                    false
                }
            },
        )

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            val color by animateColorAsState(
                targetValue =
                    if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                label = "swipe_bg",
            )
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(color, RoundedCornerShape(12.dp))
                        .padding(horizontal = DesignSystemSpacing.large),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.delete),
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        },
    ) {
        HoldingCard(
            holdingWithPnL = holdingWithPnL,
            onClick = onEdit,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun HoldingCard(
    holdingWithPnL: GoldHoldingWithPnL,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val holding = holdingWithPnL.holding
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val buyDate = dateFormat.format(Date(holding.buyDateMillis))

    Card(
        onClick = onClick,
        modifier = modifier,
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
    ) {
        Column(modifier = Modifier.padding(DesignSystemSpacing.large)) {
            // Row 1: Type + Weight
            Text(
                text =
                    "${goldTypeLabel(holding.type)} · ${holding.weightValue} ${goldUnitLabel(holding.weightUnit)}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )

            Spacer(Modifier.height(DesignSystemSpacing.xs))

            // Row 2: Buy price + date
            Text(
                text =
                    stringResource(
                        R.string.gold_buy_price_per_unit,
                        formatAmountShort(holding.buyPricePerUnit, holding.currencyCode),
                        goldUnitLabel(holding.weightUnit),
                    ) + " · $buyDate",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(DesignSystemSpacing.small))

            if (holdingWithPnL.currentPricePerUnit != null) {
                val currentValue = holdingWithPnL.currentValue ?: 0L
                val pnl = holdingWithPnL.pnL ?: 0L
                val pnlPercent = holdingWithPnL.pnLPercent ?: 0.0

                // Row 3: Cost → Value
                Text(
                    text =
                        stringResource(
                            R.string.gold_cost_to_value,
                            formatAmountShort(holdingWithPnL.totalCost, holding.currencyCode),
                            formatAmountShort(currentValue, holding.currencyCode),
                        ),
                    style = MaterialTheme.typography.bodyMedium,
                )

                Spacer(Modifier.height(DesignSystemSpacing.xs))

                // Row 4: P&L
                val pnlColor = FinancialColors.balanceColor(pnl >= 0)
                val sign = if (pnl >= 0) "+" else ""

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AmountText(
                            amount = pnl,
                            currencyCode = holding.currencyCode,
                            showSign = true,
                            textStyle = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.width(DesignSystemSpacing.xs))
                        Text(
                            text = "($sign%.1f%%)".format(pnlPercent),
                            style = MaterialTheme.typography.bodySmall,
                            color = pnlColor,
                        )
                    }
                    Box(
                        modifier =
                            Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(pnlColor),
                    )
                }
            } else {
                // No current price
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(DesignSystemSpacing.xs))
                    Text(
                        text = stringResource(R.string.gold_set_price),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UpdatePricesBottomSheet(
    currentPrices: List<GoldPrice>,
    currencyCode: String,
    onSave: (Map<Pair<GoldType, GoldWeightUnit>, Long>) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val priceInputs =
        remember {
            mutableStateMapOf<Pair<GoldType, GoldWeightUnit>, String>().apply {
                currentPrices.forEach { price ->
                    val key = price.type to price.unit
                    this[key] = if (price.sellPricePerUnit > 0) price.sellPricePerUnit.toString() else ""
                }
            }
        }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DesignSystemSpacing.screenPadding)
                    .padding(bottom = DesignSystemSpacing.xxl),
        ) {
            Text(
                text = stringResource(R.string.gold_update_prices_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )

            Spacer(Modifier.height(DesignSystemSpacing.large))

            currentPrices.forEach { price ->
                val key = price.type to price.unit
                val label =
                    stringResource(
                        R.string.gold_price_per_unit,
                        goldTypeLabel(price.type),
                        goldUnitLabel(price.unit),
                    )

                OutlinedTextField(
                    value = priceInputs[key] ?: "",
                    onValueChange = { input ->
                        priceInputs[key] = input.replace("[^0-9]".toRegex(), "")
                    },
                    label = { Text(label) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation =
                        remember(currencyCode) {
                            CurrencyAmountVisualTransformation(currencyCode)
                        },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(DesignSystemSpacing.medium))
            }

            Spacer(Modifier.height(DesignSystemSpacing.small))

            FilledTonalButton(
                onClick = {
                    val parsed =
                        priceInputs
                            .mapValues { (_, text) ->
                                text.toLongOrNull() ?: 0L
                            }.filter { it.value > 0 }
                    onSave(parsed)
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.gold_save_prices))
            }
        }
    }
}

@Composable
internal fun goldTypeLabel(type: GoldType): String =
    when (type) {
        GoldType.SJC -> stringResource(R.string.gold_type_sjc)
        GoldType.GOLD_24K -> stringResource(R.string.gold_type_24k)
        GoldType.GOLD_18K -> stringResource(R.string.gold_type_18k)
        GoldType.OTHER -> stringResource(R.string.gold_type_other)
    }

@Composable
internal fun goldUnitLabel(unit: GoldWeightUnit): String =
    when (unit) {
        GoldWeightUnit.TAEL -> stringResource(R.string.gold_unit_tael)
        GoldWeightUnit.GRAM -> stringResource(R.string.gold_unit_gram)
        GoldWeightUnit.OUNCE -> stringResource(R.string.gold_unit_ounce)
    }

@Composable
private fun formatAmountShort(
    amount: Long,
    currencyCode: String,
): String {
    val formatter =
        remember {
            dev.tuandoan.expensetracker.core.formatter
                .DefaultCurrencyFormatter()
        }
    return formatter.format(amount, currencyCode)
}
