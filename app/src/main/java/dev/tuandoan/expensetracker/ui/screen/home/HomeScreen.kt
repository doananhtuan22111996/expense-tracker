package dev.tuandoan.expensetracker.ui.screen.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
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
import dev.tuandoan.expensetracker.domain.model.Transaction
import dev.tuandoan.expensetracker.domain.model.TransactionType
import dev.tuandoan.expensetracker.ui.component.AmountText
import dev.tuandoan.expensetracker.ui.component.EmptyStateMessage
import dev.tuandoan.expensetracker.ui.component.MonthSelector
import dev.tuandoan.expensetracker.ui.component.MonthYearPickerDialog
import dev.tuandoan.expensetracker.ui.theme.DesignSystemElevation
import dev.tuandoan.expensetracker.ui.theme.DesignSystemSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToAddTransaction: () -> Unit,
    onNavigateToEditTransaction: (Long) -> Unit,
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showMonthPicker by remember { mutableStateOf(false) }

    val transactionDeletedMsg = stringResource(R.string.transaction_deleted)
    val undoMsg = stringResource(R.string.undo)

    LaunchedEffect(uiState.isError) {
        val message = uiState.errorMessage
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

    val hapticFeedback = LocalHapticFeedback.current
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
                    contentDescription = "Add new transaction",
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
            // Month selector
            MonthSelector(
                monthLabel = uiState.monthLabel,
                onPreviousMonth = viewModel::goToPreviousMonth,
                onNextMonth = viewModel::goToNextMonth,
                onMonthLabelClick = { showMonthPicker = true },
            )

            // Search bar
            SearchBar(
                query = uiState.searchQuery,
                onQueryChanged = viewModel::onSearchQueryChanged,
                onClear = viewModel::clearSearch,
                modifier = Modifier.padding(bottom = DesignSystemSpacing.small),
            )

            // Filter chips
            FilterChips(
                selectedFilter = uiState.filter,
                onFilterChanged = viewModel::onFilterChanged,
                modifier = Modifier.padding(bottom = DesignSystemSpacing.large),
            )

            // Content
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            modifier =
                                Modifier.semantics {
                                    contentDescription = "Loading transactions"
                                },
                        )
                    }
                }
                uiState.transactions.isEmpty() -> {
                    if (uiState.searchQuery.isNotEmpty()) {
                        EmptyStateMessage(
                            title = "No results found",
                            subtitle = "Try a different search term",
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        EmptyStateMessage(
                            title = "No transactions yet",
                            subtitle = "Tap the + button to add your first transaction",
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
private fun FilterChips(
    selectedFilter: TransactionType?,
    onFilterChanged: (TransactionType?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hapticFeedback = LocalHapticFeedback.current

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(DesignSystemSpacing.small),
        modifier = modifier,
    ) {
        item {
            FilterChip(
                selected = selectedFilter == null,
                onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    onFilterChanged(null)
                },
                label = { Text("All") },
                modifier =
                    Modifier.semantics {
                        contentDescription =
                            if (selectedFilter == null) {
                                "All transactions filter selected"
                            } else {
                                "Filter by all transactions"
                            }
                        role = Role.Tab
                        stateDescription = if (selectedFilter == null) "Selected" else "Not selected"
                    },
            )
        }
        item {
            FilterChip(
                selected = selectedFilter == TransactionType.EXPENSE,
                onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    onFilterChanged(TransactionType.EXPENSE)
                },
                label = { Text("Expenses") },
                modifier =
                    Modifier.semantics {
                        contentDescription =
                            if (selectedFilter == TransactionType.EXPENSE) {
                                "Expense transactions filter selected"
                            } else {
                                "Filter by expense transactions"
                            }
                        role = Role.Tab
                        stateDescription = if (selectedFilter == TransactionType.EXPENSE) "Selected" else "Not selected"
                    },
            )
        }
        item {
            FilterChip(
                selected = selectedFilter == TransactionType.INCOME,
                onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    onFilterChanged(TransactionType.INCOME)
                },
                label = { Text("Income") },
                modifier =
                    Modifier.semantics {
                        contentDescription =
                            if (selectedFilter == TransactionType.INCOME) {
                                "Income transactions filter selected"
                            } else {
                                "Filter by income transactions"
                            }
                        role = Role.Tab
                        stateDescription = if (selectedFilter == TransactionType.INCOME) "Selected" else "Not selected"
                    },
            )
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

    Card(
        onClick = {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        modifier =
            modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription =
                        buildString {
                            append("${transaction.type.name.lowercase()} transaction")
                            append(", ${transaction.category.name}")
                            append(
                                ", ${AmountFormatter.formatAmountWithCurrency(
                                    transaction.amount,
                                    transaction.currencyCode,
                                )}",
                            )
                            if (!transaction.note.isNullOrBlank()) {
                                append(", note: ${transaction.note}")
                            }
                            append(", ${DateTimeUtil.formatShortDate(transaction.timestamp)}")
                            append(", double tap to edit")
                        }
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
                                contentDescription = "Delete ${transaction.category.name} transaction of " +
                                    AmountFormatter.formatAmountWithCurrency(
                                        transaction.amount,
                                        transaction.currencyCode,
                                    )
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
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChanged,
        placeholder = { Text("Search by note") },
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
                        contentDescription = "Clear search",
                    )
                }
            }
        },
        singleLine = true,
        modifier =
            modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = "Search transactions by note"
                },
    )
}
