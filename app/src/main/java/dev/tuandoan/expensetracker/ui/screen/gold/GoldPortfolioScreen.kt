package dev.tuandoan.expensetracker.ui.screen.gold

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Paid
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.tuandoan.expensetracker.R
import dev.tuandoan.expensetracker.core.formatter.CurrencyAmountVisualTransformation
import dev.tuandoan.expensetracker.core.util.DateTimeUtil
import dev.tuandoan.expensetracker.domain.model.GoldHolding
import dev.tuandoan.expensetracker.domain.model.GoldHoldingWithPnL
import dev.tuandoan.expensetracker.domain.model.GoldPortfolioSummary
import dev.tuandoan.expensetracker.domain.model.GoldPrice
import dev.tuandoan.expensetracker.domain.model.GoldSale
import dev.tuandoan.expensetracker.domain.model.GoldType
import dev.tuandoan.expensetracker.domain.model.GoldTypeAllocation
import dev.tuandoan.expensetracker.domain.model.GoldWeightUnit
import dev.tuandoan.expensetracker.ui.component.AmountText
import dev.tuandoan.expensetracker.ui.component.ErrorStateMessage
import dev.tuandoan.expensetracker.ui.theme.DesignSystemElevation
import dev.tuandoan.expensetracker.ui.theme.DesignSystemSpacing
import dev.tuandoan.expensetracker.ui.theme.FinancialColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToLong

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoldPortfolioScreen(
    viewModel: GoldPortfolioViewModel,
    modifier: Modifier = Modifier,
    bottomContentPadding: Dp = 0.dp,
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
    val saleSuccessMsg = stringResource(R.string.gold_sale_success)
    val saleDeletedMsg = stringResource(R.string.gold_sale_deleted)

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

    LaunchedEffect(uiState.showSaleSuccess) {
        if (uiState.showSaleSuccess) {
            snackbarHostState.showSnackbar(saleSuccessMsg)
            viewModel.clearSaleSuccessFlag()
        }
    }

    LaunchedEffect(uiState.lastDeletedSale) {
        if (uiState.lastDeletedSale != null) {
            snackbarHostState.showSnackbar(saleDeletedMsg)
            viewModel.clearLastDeletedSale()
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

    uiState.holdingToSell?.let { holdingWithPnL ->
        SellGoldHoldingBottomSheet(
            holdingWithPnL = holdingWithPnL,
            currencyCode = uiState.currencyCode,
            onConfirmSale = { holdingId, soldWeight, sellPrice, dateMillis, note ->
                viewModel.recordSale(
                    holdingId = holdingId,
                    soldWeight = soldWeight,
                    sellPricePerUnit = sellPrice,
                    saleDateMillis = dateMillis,
                    note = note,
                )
            },
            onDismiss = viewModel::cancelSell,
        )
    }

    val totalItems = uiState.totalHoldingsCount + uiState.totalSalesCount
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.gold_portfolio_title)) },
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            if (totalItems > 0) {
                val addHoldingDesc = stringResource(R.string.gold_add_holding)
                FloatingActionButton(
                    onClick = onNavigateToAddHolding,
                    modifier =
                        Modifier
                            .padding(bottom = bottomContentPadding)
                            .semantics {
                                contentDescription = addHoldingDesc
                            },
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                }
            }
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(bottom = bottomContentPadding),
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
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

            uiState.isError && totalItems == 0 -> {
                ErrorStateMessage(
                    title = stringResource(R.string.error_load_portfolio),
                    message = uiState.errorMessage?.asString() ?: stringResource(R.string.error_unexpected),
                    onRetry = viewModel::retry,
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                )
            }

            totalItems == 0 -> {
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
                    onStartSell = viewModel::startSell,
                    onDeleteSale = viewModel::deleteSale,
                    onTypeFilterChanged = viewModel::setTypeFilter,
                    onSortOptionChanged = viewModel::setSortOption,
                    onTabSelected = viewModel::selectTab,
                    contentPadding = PaddingValues(bottom = bottomContentPadding + DesignSystemSpacing.fabClearance),
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
    onStartSell: (GoldHoldingWithPnL) -> Unit,
    onDeleteSale: (GoldSale) -> Unit,
    onTypeFilterChanged: (GoldType?) -> Unit,
    onSortOptionChanged: (GoldHoldingSortOption) -> Unit,
    onTabSelected: (GoldPortfolioTab) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    LazyColumn(
        modifier = modifier.padding(horizontal = DesignSystemSpacing.screenPadding),
        contentPadding = contentPadding,
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

        // Portfolio Allocation Card
        if (uiState.allocations.isNotEmpty()) {
            item(key = "allocations") {
                PortfolioAllocationCard(
                    allocations = uiState.allocations,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        if (uiState.summary == null && uiState.totalHoldingsCount > 0) {
            item(key = "no_prices") {
                Card(
                    onClick = onUpdatePrices,
                    modifier = Modifier.fillMaxWidth(),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                ) {
                    Column(modifier = Modifier.padding(DesignSystemSpacing.large)) {
                        Row(
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
                        if (uiState.totalWeightGrams > 0.0) {
                            Spacer(Modifier.height(DesignSystemSpacing.small))
                            Text(
                                text =
                                    stringResource(
                                        R.string.gold_total_weight_pill,
                                        formatWeight(uiState.totalWeightTaels),
                                        stringResource(R.string.gold_unit_tael),
                                        formatWeight(uiState.totalWeightGrams),
                                        stringResource(R.string.gold_unit_gram),
                                    ),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
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
                            Column {
                                // Sell price row
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
                                // Buy-back price row
                                val buyBack = price.buyBackPricePerUnit
                                if (buyBack != null && buyBack > 0) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Text(
                                            text =
                                                stringResource(
                                                    R.string.gold_dealer_buyback_price,
                                                    goldTypeLabel(price.type),
                                                    goldUnitLabel(price.unit),
                                                ),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        AmountText(
                                            amount = buyBack,
                                            currencyCode = price.currencyCode,
                                            textStyle = MaterialTheme.typography.bodySmall,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Segmented Tabs: Holdings vs Sales History
        item(key = "portfolio_tabs") {
            Spacer(Modifier.height(DesignSystemSpacing.small))
            val holdingsTabDesc = stringResource(R.string.a11y_gold_holdings_tab)
            val salesTabDesc = stringResource(R.string.a11y_gold_sales_tab)
            PrimaryTabRow(
                selectedTabIndex = if (uiState.selectedTab == GoldPortfolioTab.HOLDINGS) 0 else 1,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Tab(
                    selected = uiState.selectedTab == GoldPortfolioTab.HOLDINGS,
                    onClick = { onTabSelected(GoldPortfolioTab.HOLDINGS) },
                    text = {
                        Text(
                            text = stringResource(R.string.gold_tab_holdings_count, uiState.totalHoldingsCount),
                            fontWeight =
                                if (uiState.selectedTab == GoldPortfolioTab.HOLDINGS) {
                                    FontWeight.Bold
                                } else {
                                    FontWeight.Normal
                                },
                        )
                    },
                    modifier =
                        Modifier.semantics {
                            contentDescription = holdingsTabDesc
                        },
                )
                Tab(
                    selected = uiState.selectedTab == GoldPortfolioTab.SALES,
                    onClick = { onTabSelected(GoldPortfolioTab.SALES) },
                    text = {
                        Text(
                            text = stringResource(R.string.gold_tab_sales_count, uiState.totalSalesCount),
                            fontWeight =
                                if (uiState.selectedTab == GoldPortfolioTab.SALES) {
                                    FontWeight.Bold
                                } else {
                                    FontWeight.Normal
                                },
                        )
                    },
                    modifier =
                        Modifier.semantics {
                            contentDescription = salesTabDesc
                        },
                )
            }
        }

        // Content based on selected tab
        when (uiState.selectedTab) {
            GoldPortfolioTab.HOLDINGS -> {
                // Holdings header & sorting
                item(key = "holdings_header") {
                    Spacer(Modifier.height(DesignSystemSpacing.xs))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val holdingsCountText =
                            if (uiState.hasActiveFilter) {
                                stringResource(R.string.gold_holdings_count, uiState.holdings.size) +
                                    " / ${uiState.totalHoldingsCount}"
                            } else {
                                stringResource(R.string.gold_holdings_count, uiState.holdings.size)
                            }
                        Text(
                            text = holdingsCountText,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )

                        // Sort dropdown button
                        Box {
                            var showSortMenu by remember { mutableStateOf(false) }
                            val sortMenuDesc = stringResource(R.string.a11y_gold_sort_menu)
                            TextButton(
                                onClick = { showSortMenu = true },
                                modifier = Modifier.semantics { contentDescription = sortMenuDesc },
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.Sort,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(DesignSystemSpacing.xs))
                                Text(
                                    text = sortOptionLabel(uiState.sortOption),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }

                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false },
                            ) {
                                GoldHoldingSortOption.entries.forEach { option ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = sortOptionLabel(option),
                                                fontWeight =
                                                    if (option == uiState.sortOption) {
                                                        FontWeight.Bold
                                                    } else {
                                                        FontWeight.Normal
                                                    },
                                            )
                                        },
                                        trailingIcon = {
                                            if (option == uiState.sortOption) {
                                                Icon(
                                                    Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                )
                                            }
                                        },
                                        onClick = {
                                            onSortOptionChanged(option)
                                            showSortMenu = false
                                        },
                                    )
                                }
                            }
                        }
                    }
                }

                // Filter chips row
                item(key = "filter_chips") {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(DesignSystemSpacing.small),
                        contentPadding = PaddingValues(vertical = DesignSystemSpacing.xs),
                    ) {
                        item {
                            val allDesc =
                                stringResource(
                                    R.string.a11y_gold_filter_chip,
                                    stringResource(R.string.gold_filter_all),
                                )
                            FilterChip(
                                selected = uiState.selectedTypeFilter == null,
                                onClick = { onTypeFilterChanged(null) },
                                label = { Text(stringResource(R.string.gold_filter_all)) },
                                modifier = Modifier.semantics { contentDescription = allDesc },
                            )
                        }
                        items(GoldType.entries.toTypedArray()) { type ->
                            val typeLabel = goldTypeLabel(type)
                            val typeDesc = stringResource(R.string.a11y_gold_filter_chip, typeLabel)
                            FilterChip(
                                selected = uiState.selectedTypeFilter == type,
                                onClick = {
                                    onTypeFilterChanged(if (uiState.selectedTypeFilter == type) null else type)
                                },
                                label = { Text(typeLabel) },
                                modifier = Modifier.semantics { contentDescription = typeDesc },
                            )
                        }
                    }
                }

                if (uiState.totalHoldingsCount == 0) {
                    item(key = "empty_holdings_tab") {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(DesignSystemSpacing.xl),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = stringResource(R.string.gold_no_active_holdings),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                } else if (uiState.holdings.isEmpty() && uiState.hasActiveFilter) {
                    item(key = "empty_filtered") {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(DesignSystemSpacing.xl),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = stringResource(R.string.gold_no_holdings_match_filter),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                items(
                    items = uiState.holdings,
                    key = { it.holding.id },
                ) { holdingWithPnL ->
                    SwipeToDismissHoldingCard(
                        holdingWithPnL = holdingWithPnL,
                        onEdit = { onEditHolding(holdingWithPnL.holding.id) },
                        onDelete = { onDeleteHolding(holdingWithPnL.holding) },
                        onSell = { onStartSell(holdingWithPnL) },
                        modifier = Modifier.fillMaxWidth().animateItem(),
                    )
                }
            }

            GoldPortfolioTab.SALES -> {
                if (uiState.sales.isEmpty()) {
                    item(key = "empty_sales") {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors =
                                CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                ),
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(DesignSystemSpacing.xl),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    text = stringResource(R.string.gold_sales_empty_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Spacer(Modifier.height(DesignSystemSpacing.xs))
                                Text(
                                    text = stringResource(R.string.gold_sales_empty_subtitle),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                } else {
                    items(
                        items = uiState.sales,
                        key = { it.id },
                    ) { sale ->
                        SwipeToDismissSaleCard(
                            sale = sale,
                            onDelete = { onDeleteSale(sale) },
                            modifier = Modifier.fillMaxWidth().animateItem(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PortfolioSummaryCard(
    summary: GoldPortfolioSummary,
    modifier: Modifier = Modifier,
) {
    val hasLiquidation = summary.totalLiquidationValue != null
    val primaryPnL = if (hasLiquidation) summary.liquidationPnL!! else summary.marketPnL
    val primaryPercent = if (hasLiquidation) summary.liquidationPnLPercent!! else summary.marketPnLPercent
    val primaryColor = FinancialColors.balanceColor(primaryPnL >= 0)
    val primarySign = if (primaryPnL >= 0) "+" else ""
    val primaryPercentText = "$primarySign%.1f%%".format(primaryPercent)

    ElevatedCard(modifier = modifier) {
        Column(modifier = Modifier.padding(DesignSystemSpacing.large)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.gold_portfolio_summary),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                if (summary.totalWeightGrams > 0.0) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Text(
                            text =
                                stringResource(
                                    R.string.gold_total_weight_pill,
                                    formatWeight(summary.totalWeightTaels),
                                    stringResource(R.string.gold_unit_tael),
                                    formatWeight(summary.totalWeightGrams),
                                    stringResource(R.string.gold_unit_gram),
                                ),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier =
                                Modifier.padding(
                                    horizontal = DesignSystemSpacing.small,
                                    vertical = DesignSystemSpacing.xs,
                                ),
                        )
                    }
                }
            }
            Spacer(Modifier.height(DesignSystemSpacing.medium))

            // Total Cost
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

            // Market Value
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    stringResource(R.string.gold_market_value),
                    style = MaterialTheme.typography.bodyMedium,
                )
                AmountText(
                    amount = summary.totalMarketValue,
                    currencyCode = summary.currencyCode,
                    textStyle = MaterialTheme.typography.bodyMedium,
                )
            }

            // Liquidation Value (only when buy-back prices exist)
            summary.totalLiquidationValue?.let { liquidationValue ->
                Spacer(Modifier.height(DesignSystemSpacing.xs))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        stringResource(R.string.gold_liquidation_value),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    AmountText(
                        amount = liquidationValue,
                        currencyCode = summary.currencyCode,
                        textStyle = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = DesignSystemSpacing.small))

            // Primary P&L (liquidation if available, market otherwise)
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
                        if (primaryPnL >= 0) {
                            stringResource(R.string.a11y_gold_profit)
                        } else {
                            stringResource(R.string.a11y_gold_loss)
                        }
                    Box(
                        modifier =
                            Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(primaryColor)
                                .semantics {
                                    contentDescription = pnlDesc
                                },
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AmountText(
                        amount = primaryPnL,
                        currencyCode = summary.currencyCode,
                        showSign = true,
                        textStyle = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.width(DesignSystemSpacing.small))
                    Text(
                        text = primaryPercentText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = primaryColor,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            // Secondary P&L line: show market P&L when liquidation is primary
            if (hasLiquidation) {
                Spacer(Modifier.height(DesignSystemSpacing.xs))
                val marketColor = FinancialColors.balanceColor(summary.marketPnL >= 0)
                val marketSign = if (summary.marketPnL >= 0) "+" else ""
                val marketPercentText = "$marketSign%.1f%%".format(summary.marketPnLPercent)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.gold_market_value),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AmountText(
                            amount = summary.marketPnL,
                            currencyCode = summary.currencyCode,
                            showSign = true,
                            textStyle = MaterialTheme.typography.bodySmall,
                        )
                        Spacer(Modifier.width(DesignSystemSpacing.xs))
                        Text(
                            text = marketPercentText,
                            style = MaterialTheme.typography.bodySmall,
                            color = marketColor,
                        )
                    }
                }
            }

            // Realized P&L (when sales exist)
            if (summary.totalRealizedPnL != 0L || summary.totalRealizedProceeds > 0L) {
                Spacer(Modifier.height(DesignSystemSpacing.xs))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.gold_realized_pnl),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    AmountText(
                        amount = summary.totalRealizedPnL,
                        currencyCode = summary.currencyCode,
                        showSign = true,
                        textStyle = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                // All-time Net P&L (Unrealized + Realized)
                Spacer(Modifier.height(DesignSystemSpacing.xs))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.gold_total_net_pnl),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    AmountText(
                        amount = summary.totalNetPnL,
                        currencyCode = summary.currencyCode,
                        showSign = true,
                        textStyle = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun PortfolioAllocationCard(
    allocations: List<GoldTypeAllocation>,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
    ) {
        Column(modifier = Modifier.padding(DesignSystemSpacing.large)) {
            Text(
                text = stringResource(R.string.gold_portfolio_allocation),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(DesignSystemSpacing.medium))

            // Segmented progress bar
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp)),
            ) {
                allocations.forEachIndexed { index, alloc ->
                    if (index > 0) {
                        Spacer(Modifier.width(2.dp))
                    }
                    Box(
                        modifier =
                            Modifier
                                .weight(alloc.percentageOfPortfolio.toFloat().coerceAtLeast(0.01f))
                                .height(10.dp)
                                .background(goldTypeColor(alloc.type)),
                    )
                }
            }

            Spacer(Modifier.height(DesignSystemSpacing.medium))

            // Allocation legend breakdown
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(DesignSystemSpacing.medium),
                verticalArrangement = Arrangement.spacedBy(DesignSystemSpacing.xs),
            ) {
                allocations.forEach { alloc ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(DesignSystemSpacing.xs),
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(goldTypeColor(alloc.type)),
                        )
                        Text(
                            text = "${goldTypeLabel(alloc.type)} %.1f%%".format(alloc.percentageOfPortfolio),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text =
                                "(${formatWeight(alloc.totalWeightTaels)} ${stringResource(R.string.gold_unit_tael)})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun sortOptionLabel(option: GoldHoldingSortOption): String =
    when (option) {
        GoldHoldingSortOption.BUY_DATE_DESC -> stringResource(R.string.gold_sort_date_desc)
        GoldHoldingSortOption.BUY_DATE_ASC -> stringResource(R.string.gold_sort_date_asc)
        GoldHoldingSortOption.VALUE_DESC -> stringResource(R.string.gold_sort_value_desc)
        GoldHoldingSortOption.PNL_DESC -> stringResource(R.string.gold_sort_pnl_desc)
        GoldHoldingSortOption.WEIGHT_DESC -> stringResource(R.string.gold_sort_weight_desc)
    }

@Composable
internal fun goldTypeColor(type: GoldType): Color =
    when (type) {
        GoldType.SJC -> Color(0xFFD4A017)
        GoldType.GOLD_24K -> Color(0xFFFFC107)
        GoldType.GOLD_18K -> Color(0xFFFF9800)
        GoldType.OTHER -> Color(0xFF78909C)
    }

internal fun formatWeight(weight: Double): String {
    val roundedTo2Decimals = (weight * 100).roundToLong() / 100.0
    return if (roundedTo2Decimals == roundedTo2Decimals.toLong().toDouble()) {
        roundedTo2Decimals.toLong().toString()
    } else {
        String.format(Locale.getDefault(), "%.2f", roundedTo2Decimals).trimEnd('0').trimEnd('.', ',')
    }
}

private fun formatWeightQuick(weight: Double): String {
    val rounded = (weight * 10000).roundToLong() / 10000.0
    return if (rounded == rounded.toLong().toDouble()) {
        rounded.toLong().toString()
    } else {
        String.format(Locale.US, "%.4f", rounded).trimEnd('0').trimEnd('.')
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDismissHoldingCard(
    holdingWithPnL: GoldHoldingWithPnL,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSell: () -> Unit,
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
            onSell = onSell,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun HoldingCard(
    holdingWithPnL: GoldHoldingWithPnL,
    onClick: () -> Unit = {},
    onSell: () -> Unit = {},
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
            // Row 1: Type + Weight and Sell button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text =
                        "${goldTypeLabel(
                            holding.type,
                        )} · ${formatWeight(holding.weightValue)} ${goldUnitLabel(holding.weightUnit)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                val sellDesc = stringResource(R.string.a11y_gold_sell_holding, goldTypeLabel(holding.type))
                FilledTonalButton(
                    onClick = onSell,
                    contentPadding = PaddingValues(horizontal = DesignSystemSpacing.medium, vertical = 0.dp),
                    modifier =
                        Modifier
                            .height(32.dp)
                            .semantics { contentDescription = sellDesc },
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Paid,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(DesignSystemSpacing.xs))
                    Text(
                        text = stringResource(R.string.gold_sell_action),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }

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

            if (holdingWithPnL.currentSellPricePerUnit != null) {
                val hasBuyBack = holdingWithPnL.currentBuyBackPricePerUnit != null
                val displayValue =
                    if (hasBuyBack) holdingWithPnL.liquidationValue ?: 0L else holdingWithPnL.marketValue ?: 0L
                val pnl =
                    if (hasBuyBack) holdingWithPnL.liquidationPnL ?: 0L else holdingWithPnL.marketPnL ?: 0L
                val pnlPercent =
                    if (hasBuyBack) {
                        holdingWithPnL.liquidationPnLPercent ?: 0.0
                    } else {
                        holdingWithPnL.marketPnLPercent ?: 0.0
                    }
                val isEstimated = !hasBuyBack

                // Row 3: Cost → Value
                Text(
                    text =
                        stringResource(
                            R.string.gold_cost_to_value,
                            formatAmountShort(holdingWithPnL.totalCost, holding.currencyCode),
                            formatAmountShort(displayValue, holding.currencyCode),
                        ),
                    style = MaterialTheme.typography.bodyMedium,
                )

                Spacer(Modifier.height(DesignSystemSpacing.xs))

                // Row 4: P&L with estimated indicator
                val pnlColor = FinancialColors.balanceColor(pnl >= 0)
                val sign = if (pnl >= 0) "+" else ""
                val estimatedPrefix =
                    if (isEstimated) stringResource(R.string.gold_estimated_pnl_indicator) else ""

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier =
                            if (isEstimated) {
                                val estimatedDesc = stringResource(R.string.a11y_gold_estimated_pnl)
                                Modifier.semantics { contentDescription = estimatedDesc }
                            } else {
                                Modifier
                            },
                    ) {
                        if (isEstimated) {
                            Text(
                                text =
                                    "$estimatedPrefix$sign${formatAmountShort(pnl, holding.currencyCode)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            AmountText(
                                amount = pnl,
                                currencyCode = holding.currencyCode,
                                showSign = true,
                                textStyle = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        Spacer(Modifier.width(DesignSystemSpacing.xs))
                        Text(
                            text = "$estimatedPrefix($sign%.1f%%)".format(pnlPercent),
                            style = MaterialTheme.typography.bodySmall,
                            color =
                                if (isEstimated) {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                } else {
                                    pnlColor
                                },
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

@Composable
private fun SaleCard(
    sale: GoldSale,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
    ) {
        Column(modifier = Modifier.padding(DesignSystemSpacing.large)) {
            // Row 1: Type + Sold Weight and Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text =
                        "${goldTypeLabel(
                            sale.type,
                        )} · ${formatWeight(sale.soldWeight)} ${goldUnitLabel(sale.weightUnit)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = DateTimeUtil.formatTimestamp(sale.saleDateMillis),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(DesignSystemSpacing.xs))

            // Row 2: Bought price → Sold price
            Text(
                text =
                    stringResource(
                        R.string.gold_sale_price_spread,
                        formatAmountShort(sale.buyPricePerUnit, sale.currencyCode),
                        formatAmountShort(sale.sellPricePerUnit, sale.currencyCode),
                    ) + " / ${goldUnitLabel(sale.weightUnit)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(DesignSystemSpacing.small))

            // Row 3: Cost → Proceeds
            Text(
                text =
                    stringResource(
                        R.string.gold_sale_cost_proceeds,
                        formatAmountShort(sale.totalCost, sale.currencyCode),
                        formatAmountShort(sale.totalProceeds, sale.currencyCode),
                    ),
                style = MaterialTheme.typography.bodyMedium,
            )

            Spacer(Modifier.height(DesignSystemSpacing.xs))

            // Row 4: Realized P&L
            val pnl = sale.realizedPnL
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
                        currencyCode = sale.currencyCode,
                        showSign = true,
                        textStyle = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.width(DesignSystemSpacing.xs))
                    Text(
                        text = "($sign%.1f%%)".format(sale.realizedPnLPercent),
                        style = MaterialTheme.typography.bodySmall,
                        color = pnlColor,
                        fontWeight = FontWeight.Medium,
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

            if (!sale.note.isNullOrBlank()) {
                Spacer(Modifier.height(DesignSystemSpacing.xs))
                Text(
                    text = sale.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDismissSaleCard(
    sale: GoldSale,
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
                label = "swipe_sale_bg",
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
        SaleCard(
            sale = sale,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SellGoldHoldingBottomSheet(
    holdingWithPnL: GoldHoldingWithPnL,
    currencyCode: String,
    onConfirmSale: (
        holdingId: Long,
        soldWeight: Double,
        sellPricePerUnit: Long,
        saleDateMillis: Long,
        note: String?,
    ) -> Unit,
    onDismiss: () -> Unit,
) {
    val holding = holdingWithPnL.holding
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var weightText by remember { mutableStateOf("") }
    val initialPrice =
        holdingWithPnL.currentBuyBackPricePerUnit?.takeIf { it > 0 }
            ?: holdingWithPnL.currentSellPricePerUnit?.takeIf { it > 0 } ?: 0L
    var sellPriceText by remember {
        mutableStateOf(if (initialPrice > 0) initialPrice.toString() else "")
    }
    var saleDateMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var noteText by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }

    val parsedWeight = weightText.toDoubleOrNull()
    val isWeightValid = parsedWeight != null && parsedWeight > 0.0 && parsedWeight <= holding.weightValue
    val isWeightError = weightText.isNotBlank() && !isWeightValid

    val parsedSellPrice = sellPriceText.replace("[^0-9]".toRegex(), "").toLongOrNull()
    val isPriceValid = parsedSellPrice != null && parsedSellPrice > 0L
    val isPriceError = sellPriceText.isNotBlank() && !isPriceValid

    val visualTransformation = remember(currencyCode) { CurrencyAmountVisualTransformation(currencyCode) }

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
            verticalArrangement = Arrangement.spacedBy(DesignSystemSpacing.medium),
        ) {
            Text(
                text = stringResource(R.string.gold_sell_holding_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )

            // Holding info banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Column(modifier = Modifier.padding(DesignSystemSpacing.medium)) {
                    Text(
                        text = "${goldTypeLabel(
                            holding.type,
                        )} · ${formatWeight(holding.weightValue)} ${goldUnitLabel(holding.weightUnit)}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(DesignSystemSpacing.xs))
                    Text(
                        text =
                            stringResource(
                                R.string.gold_buy_price_per_unit,
                                formatAmountShort(holding.buyPricePerUnit, holding.currencyCode),
                                goldUnitLabel(holding.weightUnit),
                            ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Weight input
            Column {
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { input -> weightText = input },
                    label = { Text(stringResource(R.string.gold_sell_weight_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = isWeightError,
                    supportingText = {
                        Text(
                            stringResource(
                                R.string.gold_sell_weight_max,
                                formatWeight(holding.weightValue),
                                goldUnitLabel(holding.weightUnit),
                            ),
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                )

                // Quick percentage chips
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = DesignSystemSpacing.xs),
                    horizontalArrangement = Arrangement.spacedBy(DesignSystemSpacing.small),
                ) {
                    FilterChip(
                        selected = false,
                        onClick = { weightText = formatWeightQuick(holding.weightValue * 0.25) },
                        label = { Text(stringResource(R.string.gold_sell_quick_25)) },
                    )
                    FilterChip(
                        selected = false,
                        onClick = { weightText = formatWeightQuick(holding.weightValue * 0.50) },
                        label = { Text(stringResource(R.string.gold_sell_quick_50)) },
                    )
                    FilterChip(
                        selected = false,
                        onClick = { weightText = formatWeightQuick(holding.weightValue) },
                        label = { Text(stringResource(R.string.gold_sell_quick_all)) },
                    )
                }
            }

            // Sell price input
            OutlinedTextField(
                value = sellPriceText,
                onValueChange = { input -> sellPriceText = input.replace("[^0-9]".toRegex(), "") },
                label = { Text(stringResource(R.string.gold_sell_price_label, goldUnitLabel(holding.weightUnit))) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                visualTransformation = visualTransformation,
                singleLine = true,
                isError = isPriceError,
                modifier = Modifier.fillMaxWidth(),
            )

            // Live P&L Preview
            val weight = parsedWeight
            val sellPrice = parsedSellPrice
            if (weight != null &&
                sellPrice != null &&
                weight > 0.0 &&
                weight <= holding.weightValue &&
                sellPrice > 0L
            ) {
                val cost = (holding.buyPricePerUnit * weight).toLong()
                val proceeds = (sellPrice * weight).toLong()
                val pnl = proceeds - cost
                val pnlPercent = if (cost > 0) (pnl.toDouble() / cost) * 100.0 else 0.0
                val pnlColor = FinancialColors.balanceColor(pnl >= 0)
                val sign = if (pnl >= 0) "+" else ""

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Column(modifier = Modifier.padding(DesignSystemSpacing.medium)) {
                        Text(
                            text = stringResource(R.string.gold_sell_live_preview),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(DesignSystemSpacing.xs))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = stringResource(R.string.gold_sell_cost, formatAmountShort(cost, currencyCode)),
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Text(
                                text =
                                    stringResource(
                                        R.string.gold_sell_proceeds,
                                        formatAmountShort(proceeds, currencyCode),
                                    ),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Spacer(Modifier.height(DesignSystemSpacing.xs))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(R.string.gold_realized_pnl),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AmountText(
                                    amount = pnl,
                                    currencyCode = currencyCode,
                                    showSign = true,
                                    textStyle = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                                Spacer(Modifier.width(DesignSystemSpacing.xs))
                                Text(
                                    text = "($sign%.1f%%)".format(pnlPercent),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = pnlColor,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                    }
                }
            }

            // Sale Date selector
            Card(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = DesignSystemElevation.low),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(DesignSystemSpacing.large),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.gold_sell_date_label),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = DateTimeUtil.formatTimestamp(saleDateMillis),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                    Icon(
                        Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            if (showDatePicker) {
                val datePickerState = rememberDatePickerState(initialSelectedDateMillis = saleDateMillis)
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                datePickerState.selectedDateMillis?.let { saleDateMillis = it }
                                showDatePicker = false
                            },
                        ) {
                            Text(stringResource(R.string.action_ok))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) {
                            Text(stringResource(R.string.cancel))
                        }
                    },
                ) {
                    androidx.compose.material3.DatePicker(state = datePickerState)
                }
            }

            // Optional note input
            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it },
                label = { Text(stringResource(R.string.gold_note_label)) },
                placeholder = { Text(stringResource(R.string.gold_note_placeholder)) },
                maxLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )

            // Confirm Sale button
            Button(
                onClick = {
                    if (parsedWeight != null && parsedSellPrice != null) {
                        onConfirmSale(
                            holding.id,
                            parsedWeight,
                            parsedSellPrice,
                            saleDateMillis,
                            noteText.ifBlank { null },
                        )
                    }
                },
                enabled = isWeightValid && isPriceValid,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.gold_sell_confirm))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UpdatePricesBottomSheet(
    currentPrices: List<GoldPrice>,
    currencyCode: String,
    onSave: (Map<Pair<GoldType, GoldWeightUnit>, PriceInput>) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val sellInputs =
        remember {
            mutableStateMapOf<Pair<GoldType, GoldWeightUnit>, String>().apply {
                currentPrices.forEach { price ->
                    val key = price.type to price.unit
                    this[key] = if (price.sellPricePerUnit > 0) price.sellPricePerUnit.toString() else ""
                }
            }
        }
    val buyBackInputs =
        remember {
            mutableStateMapOf<Pair<GoldType, GoldWeightUnit>, String>().apply {
                currentPrices.forEach { price ->
                    val key = price.type to price.unit
                    this[key] =
                        price.buyBackPricePerUnit?.let { if (it > 0) it.toString() else "" } ?: ""
                }
            }
        }
    val validationErrors =
        remember { mutableStateMapOf<Pair<GoldType, GoldWeightUnit>, Boolean>() }

    val buyBackExceedsSellMsg = stringResource(R.string.gold_buyback_exceeds_sell)
    val hasValidationErrors = validationErrors.any { it.value }

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
                val typeLabel = goldTypeLabel(price.type)
                val unitLabel = goldUnitLabel(price.unit)
                val sellLabel = stringResource(R.string.gold_dealer_sell_price, typeLabel, unitLabel)
                val buyBackLabel =
                    stringResource(R.string.gold_dealer_buyback_price, typeLabel, unitLabel)
                val sellA11y =
                    stringResource(R.string.a11y_gold_dealer_sell_price, typeLabel, unitLabel)
                val buyBackA11y =
                    stringResource(R.string.a11y_gold_dealer_buyback_price, typeLabel, unitLabel)
                val visualTransformation =
                    remember(currencyCode) { CurrencyAmountVisualTransformation(currencyCode) }
                val isError = validationErrors[key] == true

                // Sell price field
                OutlinedTextField(
                    value = sellInputs[key] ?: "",
                    onValueChange = { input ->
                        val cleaned = input.replace("[^0-9]".toRegex(), "")
                        sellInputs[key] = cleaned
                        val sell = cleaned.toLongOrNull() ?: 0L
                        val buyBack = buyBackInputs[key]?.toLongOrNull() ?: 0L
                        validationErrors[key] = buyBack > 0 && sell > 0 && buyBack > sell
                    },
                    label = { Text(sellLabel) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = visualTransformation,
                    singleLine = true,
                    modifier =
                        Modifier.fillMaxWidth().semantics {
                            contentDescription = sellA11y
                        },
                )

                Spacer(Modifier.height(DesignSystemSpacing.small))

                // Buy-back price field
                OutlinedTextField(
                    value = buyBackInputs[key] ?: "",
                    onValueChange = { input ->
                        val cleaned = input.replace("[^0-9]".toRegex(), "")
                        buyBackInputs[key] = cleaned
                        val sell = sellInputs[key]?.toLongOrNull() ?: 0L
                        val buyBack = cleaned.toLongOrNull() ?: 0L
                        validationErrors[key] = buyBack > 0 && sell > 0 && buyBack > sell
                    },
                    label = { Text(buyBackLabel) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = visualTransformation,
                    singleLine = true,
                    isError = isError,
                    supportingText =
                        if (isError) {
                            { Text(buyBackExceedsSellMsg) }
                        } else {
                            null
                        },
                    modifier =
                        Modifier.fillMaxWidth().semantics {
                            contentDescription = buyBackA11y
                        },
                )

                Spacer(Modifier.height(DesignSystemSpacing.medium))
            }

            Spacer(Modifier.height(DesignSystemSpacing.small))

            FilledTonalButton(
                onClick = {
                    val parsed =
                        sellInputs
                            .mapValues { (key, text) ->
                                val sell = text.toLongOrNull() ?: 0L
                                val buyBack = buyBackInputs[key]?.toLongOrNull()
                                PriceInput(
                                    sellPrice = sell,
                                    buyBackPrice = buyBack?.takeIf { it > 0 },
                                )
                            }.filter { it.value.sellPrice > 0 }
                    onSave(parsed)
                },
                enabled = !hasValidationErrors,
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
