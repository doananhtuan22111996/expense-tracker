package dev.tuandoan.expensetracker.ui.screen.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.tuandoan.expensetracker.R
import dev.tuandoan.expensetracker.core.formatter.AmountFormatter
import dev.tuandoan.expensetracker.core.util.DateTimeUtil
import dev.tuandoan.expensetracker.domain.model.SearchScope
import dev.tuandoan.expensetracker.domain.model.Transaction
import dev.tuandoan.expensetracker.domain.model.TransactionType
import dev.tuandoan.expensetracker.ui.component.AmountText
import dev.tuandoan.expensetracker.ui.component.EmptyStateMessage
import dev.tuandoan.expensetracker.ui.component.ErrorStateMessage
import dev.tuandoan.expensetracker.ui.component.MonthSelector
import dev.tuandoan.expensetracker.ui.component.MonthYearPickerDialog
import dev.tuandoan.expensetracker.ui.theme.DesignSystemElevation
import dev.tuandoan.expensetracker.ui.theme.DesignSystemSpacing
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    onNavigateToAddTransaction: () -> Unit,
    onNavigateToEditTransaction: (Long) -> Unit,
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val expenseCategories by viewModel.expenseCategories.collectAsStateWithLifecycle()
    val incomeCategories by viewModel.incomeCategories.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showMonthPicker by remember { mutableStateOf(false) }
    var showCategorySheet by remember { mutableStateOf(false) }
    var showDateRangePicker by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val transactionDeletedMsg = stringResource(R.string.transaction_deleted)
    val undoMsg = stringResource(R.string.undo)

    LaunchedEffect(uiState.isError) {
        val message = uiState.errorMessage?.asString(context)
        if (uiState.isError && !message.isNullOrBlank()) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.lastDeletedTransaction) {
        uiState.lastDeletedTransaction?.let {
            val result =
                snackbarHostState.showSnackbar(
                    message = transactionDeletedMsg,
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

    if (showMonthPicker) {
        MonthYearPickerDialog(
            currentSelection = viewModel.currentSelectedMonth(),
            onMonthSelected = { viewModel.setMonth(it) },
            onDismiss = { showMonthPicker = false },
        )
    }

    if (showCategorySheet) {
        CategoryFilterBottomSheet(
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            expenseCategories = expenseCategories,
            incomeCategories = incomeCategories,
            selectedCategoryId = uiState.selectedCategoryId,
            onCategorySelected = viewModel::onCategorySelected,
            onDismiss = { showCategorySheet = false },
        )
    }

    if (showDateRangePicker) {
        DateRangePickerSheet(
            onConfirm = { startMillis, endMillis ->
                viewModel.onDateRangeSelected(startMillis, endMillis)
                showDateRangePicker = false
            },
            onDismiss = { showDateRangePicker = false },
        )
    }

    val hapticFeedback = LocalHapticFeedback.current
    val isAllMonths = uiState.searchScope == SearchScope.ALL_MONTHS
    val dateFormatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT) }
    val formattedDateRange =
        if (uiState.dateRangeStart != null && uiState.dateRangeEnd != null) {
            "${dateFormatter.format(uiState.dateRangeStart)} – ${dateFormatter.format(uiState.dateRangeEnd)}"
        } else {
            null
        }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                windowInsets = WindowInsets(0, 0, 0, 0),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    onNavigateToAddTransaction()
                },
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.a11y_add_new_transaction),
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = modifier,
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = DesignSystemSpacing.screenPadding),
        ) {
            // Month selector — disabled when All Months is active
            MonthSelector(
                monthLabel = uiState.monthLabel,
                onPreviousMonth = viewModel::goToPreviousMonth,
                onNextMonth = viewModel::goToNextMonth,
                enabled = !isAllMonths,
                onMonthLabelClick =
                    if (isAllMonths) {
                        null
                    } else {
                        { showMonthPicker = true }
                    },
            )

            // Search bar
            SearchBar(
                query = uiState.searchQuery,
                onQueryChanged = viewModel::onSearchQueryChanged,
                onClear = viewModel::clearSearch,
                modifier = Modifier.padding(bottom = DesignSystemSpacing.small),
            )

            // Filter chips row (type + scope + category + date range)
            FilterChipsRow(
                selectedFilter = uiState.filter,
                onFilterChanged = viewModel::onFilterChanged,
                searchScope = uiState.searchScope,
                onSearchScopeChanged = viewModel::onSearchScopeChanged,
                onCategoryChipClick = { showCategorySheet = true },
                selectedCategoryName = uiState.selectedCategoryName,
                onDateRangeChipClick = { showDateRangePicker = true },
                hasDateRange = uiState.dateRangeStart != null,
                dateRangeLabel = formattedDateRange,
                modifier = Modifier.padding(bottom = DesignSystemSpacing.small),
            )

            // Active filter badges
            if (uiState.hasActiveFilters) {
                ActiveFilterBar(
                    uiState = uiState,
                    onClearScope = {
                        viewModel.onSearchScopeChanged(SearchScope.CURRENT_MONTH)
                        viewModel.clearDateRange()
                    },
                    onClearCategory = { viewModel.onCategorySelected(null) },
                    onClearDateRange = viewModel::clearDateRange,
                    onClearType = { viewModel.onFilterChanged(null) },
                    onClearAll = viewModel::clearAllFilters,
                    modifier = Modifier.padding(bottom = DesignSystemSpacing.small),
                )
            }

            // Content
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        val loadingDesc = stringResource(R.string.a11y_loading_transactions)
                        CircularProgressIndicator(
                            modifier =
                                Modifier.semantics {
                                    contentDescription = loadingDesc
                                },
                        )
                    }
                }
                uiState.isError && uiState.transactions.isEmpty() -> {
                    ErrorStateMessage(
                        title = stringResource(R.string.error_load_transactions),
                        message = uiState.errorMessage?.asString() ?: stringResource(R.string.error_unexpected),
                        onRetry = viewModel::retry,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                uiState.transactions.isEmpty() -> {
                    if (uiState.searchQuery.isNotEmpty() || uiState.hasActiveFilters) {
                        EmptyStateMessage(
                            title = stringResource(R.string.home_no_results),
                            subtitle = stringResource(R.string.home_no_results_subtitle),
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        EmptyStateMessage(
                            title = stringResource(R.string.home_no_transactions),
                            subtitle = stringResource(R.string.home_no_transactions_subtitle),
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
                else -> {
                    TransactionsList(
                        transactions = uiState.transactions,
                        onTransactionClick = onNavigateToEditTransaction,
                        onDeleteTransaction = viewModel::deleteTransaction,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterChipsRow(
    selectedFilter: TransactionType?,
    onFilterChanged: (TransactionType?) -> Unit,
    searchScope: SearchScope,
    onSearchScopeChanged: (SearchScope) -> Unit,
    onCategoryChipClick: () -> Unit,
    selectedCategoryName: String?,
    onDateRangeChipClick: () -> Unit,
    hasDateRange: Boolean,
    dateRangeLabel: String? = null,
    modifier: Modifier = Modifier,
) {
    val hapticFeedback = LocalHapticFeedback.current
    val isAllMonths = searchScope == SearchScope.ALL_MONTHS

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(DesignSystemSpacing.small),
        modifier = modifier,
    ) {
        // Type filter: All
        item {
            val allFilterDesc =
                if (selectedFilter == null) {
                    stringResource(R.string.a11y_filter_all_selected)
                } else {
                    stringResource(R.string.a11y_filter_all)
                }
            val allStateDesc =
                if (selectedFilter == null) {
                    stringResource(R.string.a11y_selected)
                } else {
                    stringResource(R.string.a11y_not_selected)
                }
            FilterChip(
                selected = selectedFilter == null,
                onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    onFilterChanged(null)
                },
                label = { Text(stringResource(R.string.filter_all)) },
                modifier =
                    Modifier.semantics {
                        contentDescription = allFilterDesc
                        role = Role.Tab
                        stateDescription = allStateDesc
                    },
            )
        }
        // Type filter: Expenses
        item {
            val expenseFilterDesc =
                if (selectedFilter == TransactionType.EXPENSE) {
                    stringResource(R.string.a11y_filter_expense_selected)
                } else {
                    stringResource(R.string.a11y_filter_expense)
                }
            val expenseStateDesc =
                if (selectedFilter == TransactionType.EXPENSE) {
                    stringResource(R.string.a11y_selected)
                } else {
                    stringResource(R.string.a11y_not_selected)
                }
            FilterChip(
                selected = selectedFilter == TransactionType.EXPENSE,
                onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    onFilterChanged(TransactionType.EXPENSE)
                },
                label = { Text(stringResource(R.string.filter_expenses)) },
                modifier =
                    Modifier.semantics {
                        contentDescription = expenseFilterDesc
                        role = Role.Tab
                        stateDescription = expenseStateDesc
                    },
            )
        }
        // Type filter: Income
        item {
            val incomeFilterDesc =
                if (selectedFilter == TransactionType.INCOME) {
                    stringResource(R.string.a11y_filter_income_selected)
                } else {
                    stringResource(R.string.a11y_filter_income)
                }
            val incomeStateDesc =
                if (selectedFilter == TransactionType.INCOME) {
                    stringResource(R.string.a11y_selected)
                } else {
                    stringResource(R.string.a11y_not_selected)
                }
            FilterChip(
                selected = selectedFilter == TransactionType.INCOME,
                onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    onFilterChanged(TransactionType.INCOME)
                },
                label = { Text(stringResource(R.string.filter_income)) },
                modifier =
                    Modifier.semantics {
                        contentDescription = incomeFilterDesc
                        role = Role.Tab
                        stateDescription = incomeStateDesc
                    },
            )
        }
        // All Months toggle
        item {
            val allMonthsDesc =
                if (isAllMonths) {
                    stringResource(R.string.a11y_all_months_selected)
                } else {
                    stringResource(R.string.a11y_all_months)
                }
            val allMonthsStateDesc =
                if (isAllMonths) {
                    stringResource(R.string.a11y_selected)
                } else {
                    stringResource(R.string.a11y_not_selected)
                }
            FilterChip(
                selected = isAllMonths,
                onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    val newScope =
                        if (isAllMonths) SearchScope.CURRENT_MONTH else SearchScope.ALL_MONTHS
                    onSearchScopeChanged(newScope)
                },
                label = { Text(stringResource(R.string.filter_all_months)) },
                modifier =
                    Modifier.semantics {
                        contentDescription = allMonthsDesc
                        stateDescription = allMonthsStateDesc
                    },
            )
        }
        // Category filter
        item {
            val categoryLabel =
                selectedCategoryName ?: stringResource(R.string.filter_category)
            val categoryDesc = stringResource(R.string.a11y_open_category_filter)
            FilterChip(
                selected = selectedCategoryName != null,
                onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    onCategoryChipClick()
                },
                label = { Text(categoryLabel) },
                modifier =
                    Modifier.semantics {
                        contentDescription = categoryDesc
                    },
            )
        }
        // Date range filter
        item {
            val chipLabel =
                if (hasDateRange && dateRangeLabel != null) {
                    dateRangeLabel
                } else {
                    stringResource(R.string.filter_date_range)
                }
            val dateRangeDesc = stringResource(R.string.a11y_open_date_range_filter)
            FilterChip(
                selected = hasDateRange,
                onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    onDateRangeChipClick()
                },
                label = { Text(chipLabel) },
                modifier =
                    Modifier.semantics {
                        contentDescription = dateRangeDesc
                    },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActiveFilterBar(
    uiState: HomeUiState,
    onClearScope: () -> Unit,
    onClearCategory: () -> Unit,
    onClearDateRange: () -> Unit,
    onClearType: () -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dateFormatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT) }

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(DesignSystemSpacing.small),
        verticalArrangement = Arrangement.spacedBy(DesignSystemSpacing.xs),
        modifier = modifier.fillMaxWidth(),
    ) {
        if (uiState.searchScope == SearchScope.ALL_MONTHS && uiState.dateRangeStart == null) {
            DismissibleFilterChip(
                label = stringResource(R.string.filter_all_months),
                onDismiss = onClearScope,
            )
        }
        if (uiState.filter != null) {
            val typeLabel =
                when (uiState.filter) {
                    TransactionType.EXPENSE -> stringResource(R.string.filter_expenses)
                    TransactionType.INCOME -> stringResource(R.string.filter_income)
                }
            DismissibleFilterChip(
                label = typeLabel,
                onDismiss = onClearType,
            )
        }
        if (uiState.selectedCategoryName != null) {
            DismissibleFilterChip(
                label = uiState.selectedCategoryName,
                onDismiss = onClearCategory,
            )
        }
        if (uiState.dateRangeStart != null && uiState.dateRangeEnd != null) {
            val rangeLabel = "${dateFormatter.format(
                uiState.dateRangeStart,
            )} – ${dateFormatter.format(uiState.dateRangeEnd)}"
            DismissibleFilterChip(
                label = rangeLabel,
                onDismiss = onClearDateRange,
            )
        }
        if (uiState.activeFilterCount > 1) {
            val clearAllDesc = stringResource(R.string.a11y_clear_all_filters)
            TextButton(
                onClick = onClearAll,
                modifier =
                    Modifier.semantics {
                        contentDescription = clearAllDesc
                    },
            ) {
                Text(
                    text = stringResource(R.string.filter_clear_all),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

@Composable
private fun DismissibleFilterChip(
    label: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val clearDesc = stringResource(R.string.a11y_clear_filter, label)
    InputChip(
        selected = true,
        onClick = onDismiss,
        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
        trailingIcon = {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = clearDesc,
                modifier = Modifier.size(InputChipDefaults.IconSize),
            )
        },
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateRangePickerSheet(
    onConfirm: (startMillis: Long, endMillis: Long) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val todayMillis =
        remember {
            System.currentTimeMillis()
        }
    val dateRangePickerState =
        rememberDateRangePickerState(
            selectableDates =
                object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis <= todayMillis
                },
        )
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = DesignSystemSpacing.xl),
        ) {
            DateRangePicker(
                state = dateRangePickerState,
                title = {
                    Text(
                        text = stringResource(R.string.filter_date_range_title),
                        style = MaterialTheme.typography.titleLarge,
                        modifier =
                            Modifier.padding(
                                start = DesignSystemSpacing.xl,
                                top = DesignSystemSpacing.large,
                            ),
                    )
                },
                showModeToggle = false,
                modifier = Modifier.weight(1f),
            )
            Row(
                horizontalArrangement = Arrangement.End,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = DesignSystemSpacing.large),
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
                TextButton(
                    onClick = {
                        val start = dateRangePickerState.selectedStartDateMillis
                        val end = dateRangePickerState.selectedEndDateMillis
                        if (start != null && end != null) {
                            onConfirm(start, end)
                        }
                    },
                    enabled =
                        dateRangePickerState.selectedStartDateMillis != null &&
                            dateRangePickerState.selectedEndDateMillis != null,
                ) {
                    Text(stringResource(R.string.action_ok))
                }
            }
        }
    }
}

@Composable
private fun TransactionsList(
    transactions: List<Transaction>,
    onTransactionClick: (Long) -> Unit,
    onDeleteTransaction: (Transaction) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(DesignSystemSpacing.listItemSpacing),
        modifier = modifier,
    ) {
        items(transactions, key = { it.id }) { transaction ->
            TransactionItem(
                transaction = transaction,
                onClick = { onTransactionClick(transaction.id) },
                onDeleteClick = { onDeleteTransaction(transaction) },
            )
        }
    }
}

@Composable
private fun TransactionItem(
    transaction: Transaction,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hapticFeedback = LocalHapticFeedback.current
    val formattedAmount =
        AmountFormatter.formatAmountWithCurrency(
            transaction.amount,
            transaction.currencyCode,
        )
    val formattedDate = DateTimeUtil.formatShortDate(transaction.timestamp)
    val typeName = transaction.type.name.lowercase()
    val itemDesc =
        if (!transaction.note.isNullOrBlank()) {
            stringResource(
                R.string.a11y_transaction_item_with_note,
                typeName,
                transaction.category.name,
                formattedAmount,
                transaction.note,
                formattedDate,
            )
        } else {
            stringResource(
                R.string.a11y_transaction_item,
                typeName,
                transaction.category.name,
                formattedAmount,
                formattedDate,
            )
        }
    val deleteDesc =
        stringResource(
            R.string.a11y_delete_transaction_detail,
            transaction.category.name,
            formattedAmount,
        )

    Card(
        onClick = {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        modifier =
            modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = itemDesc
                },
        elevation = CardDefaults.cardElevation(defaultElevation = DesignSystemElevation.low),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(DesignSystemSpacing.large),
            verticalAlignment = Alignment.Top,
        ) {
            // Main content column with improved hierarchy
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(DesignSystemSpacing.xs),
            ) {
                // Primary row: Category name (most important)
                Text(
                    text = transaction.category.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                // Secondary row: Note (if present)
                if (!transaction.note.isNullOrBlank()) {
                    Text(
                        text = transaction.note,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Tertiary row: Date
                Text(
                    text = DateTimeUtil.formatShortDate(transaction.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Amount and action section
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(DesignSystemSpacing.small),
            ) {
                // Amount with type indicator icon
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(DesignSystemSpacing.xs),
                ) {
                    if (transaction.type == TransactionType.EXPENSE) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowDownward,
                            contentDescription = stringResource(R.string.a11y_expense_indicator),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp),
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.ArrowUpward,
                            contentDescription = stringResource(R.string.a11y_income_indicator),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    AmountText(
                        amount = transaction.amount,
                        transactionType = transaction.type,
                        showSign = true,
                        currencyCode = transaction.currencyCode,
                        fontWeight = FontWeight.Bold,
                        textStyle = MaterialTheme.typography.bodyLarge,
                    )
                }

                // Delete button with proper 48dp touch target
                IconButton(
                    onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        onDeleteClick()
                    },
                    modifier =
                        Modifier
                            .size(48.dp)
                            .semantics {
                                contentDescription = deleteDesc
                            },
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null, // Using semantics above instead
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChanged: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val searchDesc = stringResource(R.string.a11y_search_transactions)
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChanged,
        placeholder = { Text(stringResource(R.string.home_search_placeholder)) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = stringResource(R.string.a11y_clear_search),
                    )
                }
            }
        },
        singleLine = true,
        modifier =
            modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = searchDesc
                },
    )
}
