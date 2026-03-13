package dev.tuandoan.expensetracker.ui.screen.recurring

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import dev.tuandoan.expensetracker.R
import dev.tuandoan.expensetracker.core.formatter.AmountFormatter
import dev.tuandoan.expensetracker.core.util.DateTimeUtil
import dev.tuandoan.expensetracker.domain.model.RecurringTransaction
import dev.tuandoan.expensetracker.domain.model.TransactionType
import dev.tuandoan.expensetracker.ui.component.EmptyStateMessage
import dev.tuandoan.expensetracker.ui.theme.DesignSystemElevation
import dev.tuandoan.expensetracker.ui.theme.DesignSystemSpacing
import dev.tuandoan.expensetracker.ui.theme.FinancialColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringTransactionsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAdd: () -> Unit,
    viewModel: RecurringTransactionsViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var deleteConfirmId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    val recurringDeletedLabel = stringResource(R.string.recurring_deleted)
    val undoLabel = stringResource(R.string.undo)

    LaunchedEffect(uiState.pendingDeleteId) {
        if (uiState.pendingDeleteId != null) {
            val result =
                snackbarHostState.showSnackbar(
                    message = recurringDeletedLabel,
                    actionLabel = undoLabel,
                    duration = SnackbarDuration.Short,
                )
            when (result) {
                SnackbarResult.ActionPerformed -> viewModel.undoDelete()
                SnackbarResult.Dismissed -> viewModel.confirmDelete()
            }
        }
    }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.recurring_transactions)) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier =
                            Modifier.semantics {
                                contentDescription = "Go back"
                            },
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAdd,
                modifier =
                    Modifier.semantics {
                        contentDescription = "Add recurring transaction"
                    },
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier,
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier =
                            Modifier.semantics {
                                contentDescription = "Loading recurring transactions"
                            },
                    )
                }
            }

            uiState.visibleRecurringTransactions.isEmpty() -> {
                EmptyStateMessage(
                    title = stringResource(R.string.no_recurring),
                    subtitle = stringResource(R.string.no_recurring_subtitle),
                    modifier = Modifier.padding(innerPadding),
                )
            }

            else -> {
                LazyColumn(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(horizontal = DesignSystemSpacing.screenPadding),
                    verticalArrangement = Arrangement.spacedBy(DesignSystemSpacing.small),
                ) {
                    items(
                        items = uiState.visibleRecurringTransactions,
                        key = { it.id },
                    ) { item ->
                        RecurringTransactionRow(
                            item = item,
                            onToggleActive = { active ->
                                viewModel.toggleActive(item.id, active)
                            },
                            onDelete = { deleteConfirmId = item.id },
                        )
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    deleteConfirmId?.let { id ->
        AlertDialog(
            onDismissRequest = { deleteConfirmId = null },
            title = { Text(stringResource(R.string.delete_recurring_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.requestDelete(id)
                        deleteConfirmId = null
                    },
                ) {
                    Text(
                        stringResource(R.string.delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmId = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun RecurringTransactionRow(
    item: RecurringTransaction,
    onToggleActive: (Boolean) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val frequencyLabel =
        when (item.frequency) {
            dev.tuandoan.expensetracker.domain.model.RecurrenceFrequency.DAILY ->
                stringResource(R.string.frequency_daily)
            dev.tuandoan.expensetracker.domain.model.RecurrenceFrequency.WEEKLY ->
                stringResource(R.string.frequency_weekly)
            dev.tuandoan.expensetracker.domain.model.RecurrenceFrequency.MONTHLY ->
                stringResource(R.string.frequency_monthly)
            dev.tuandoan.expensetracker.domain.model.RecurrenceFrequency.YEARLY ->
                stringResource(R.string.frequency_yearly)
        }

    val formattedAmount =
        AmountFormatter.formatAmountWithCurrency(item.amount, item.currencyCode)

    val nowMillis = System.currentTimeMillis()
    val nextDueLabel = DateTimeUtil.formatNextDueLabel(item.nextDueMillis, nowMillis)
    val daysDiff =
        java.util.concurrent.TimeUnit.MILLISECONDS
            .toDays(item.nextDueMillis - nowMillis)
            .toInt()
    val nextDueColor =
        when {
            daysDiff < 0 -> MaterialTheme.colorScheme.error
            daysDiff <= 1 -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }

    val statusLabel =
        if (item.isActive) {
            stringResource(R.string.active)
        } else {
            stringResource(R.string.paused)
        }

    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription =
                        "${item.categoryName}, $formattedAmount, $frequencyLabel, $statusLabel"
                },
        elevation = CardDefaults.cardElevation(defaultElevation = DesignSystemElevation.low),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(DesignSystemSpacing.large),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.categoryName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = formattedAmount,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color =
                        if (item.type == TransactionType.INCOME) {
                            FinancialColors.incomeColor()
                        } else {
                            FinancialColors.expenseColor()
                        },
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(DesignSystemSpacing.small),
                ) {
                    Text(
                        text = frequencyLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = nextDueLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = nextDueColor,
                    )
                }
                if (!item.note.isNullOrBlank()) {
                    Text(
                        text = item.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        modifier = Modifier.padding(top = DesignSystemSpacing.xs),
                    )
                }
            }

            Switch(
                checked = item.isActive,
                onCheckedChange = onToggleActive,
                modifier =
                    Modifier
                        .padding(horizontal = DesignSystemSpacing.small)
                        .semantics {
                            contentDescription =
                                if (item.isActive) "Pause recurring" else "Resume recurring"
                        },
            )

            IconButton(
                onClick = onDelete,
                modifier =
                    Modifier.semantics {
                        contentDescription = "Delete recurring transaction"
                    },
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}
