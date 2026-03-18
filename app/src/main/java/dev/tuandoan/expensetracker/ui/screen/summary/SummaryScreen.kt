package dev.tuandoan.expensetracker.ui.screen.summary

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.tuandoan.expensetracker.R
import dev.tuandoan.expensetracker.core.formatter.DefaultCurrencyFormatter
import dev.tuandoan.expensetracker.domain.model.CategoryTotal
import dev.tuandoan.expensetracker.domain.model.CurrencyMonthlySummary
import dev.tuandoan.expensetracker.domain.model.MonthlySummary
import dev.tuandoan.expensetracker.domain.model.SupportedCurrencies
import dev.tuandoan.expensetracker.domain.model.TransactionType
import dev.tuandoan.expensetracker.repository.TransactionRepositoryImpl
import dev.tuandoan.expensetracker.ui.component.AmountText
import dev.tuandoan.expensetracker.ui.component.BudgetProgressSection
import dev.tuandoan.expensetracker.ui.component.DonutChart
import dev.tuandoan.expensetracker.ui.component.EmptyStateMessage
import dev.tuandoan.expensetracker.ui.component.ErrorStateMessage
import dev.tuandoan.expensetracker.ui.component.MonthSelector
import dev.tuandoan.expensetracker.ui.component.MonthYearPickerDialog
import dev.tuandoan.expensetracker.ui.component.MonthlyBarChart
import dev.tuandoan.expensetracker.ui.component.SectionTitle
import dev.tuandoan.expensetracker.ui.component.SetBudgetDialog
import dev.tuandoan.expensetracker.ui.theme.DesignSystemElevation
import dev.tuandoan.expensetracker.ui.theme.DesignSystemSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(
    viewModel: SummaryViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    var showMonthPicker by remember { mutableStateOf(false) }
    var showBudgetDialog by remember { mutableStateOf<String?>(null) }

    if (showMonthPicker) {
        MonthYearPickerDialog(
            currentSelection = viewModel.currentSelectedMonth(),
            onMonthSelected = { viewModel.setMonth(it) },
            onDismiss = { showMonthPicker = false },
        )
    }

    showBudgetDialog?.let { currencyCode ->
        val budgetStatus = uiState.budgetStatuses.find { it.currency.code == currencyCode }
        SetBudgetDialog(
            currencyCode = currencyCode,
            currentBudget = budgetStatus?.budgetAmount,
            onSave = { amount -> viewModel.setBudget(currencyCode, amount) },
            onClear = { viewModel.clearBudget(currencyCode) },
            onDismiss = { showBudgetDialog = null },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.screen_title_summary)) },
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = modifier,
    ) { innerPadding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
                uiState.isError -> {
                    ErrorStateMessage(
                        title = "Unable to load summary",
                        message = uiState.errorMessage ?: "Failed to load financial data. Please try again.",
                        onRetry = viewModel::refresh,
                        retryButtonText = "Retry",
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                uiState.summary == null || uiState.summary?.isEmpty != false -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier =
                                Modifier.padding(
                                    horizontal = DesignSystemSpacing.screenPadding,
                                ),
                        ) {
                            SummaryModeChips(
                                mode = uiState.mode,
                                onModeChanged = viewModel::setMode,
                            )
                            PeriodSelector(
                                uiState = uiState,
                                onPreviousMonth = viewModel::goToPreviousMonth,
                                onNextMonth = viewModel::goToNextMonth,
                                onPreviousYear = viewModel::goToPreviousYear,
                                onNextYear = viewModel::goToNextYear,
                                onMonthLabelClick = { showMonthPicker = true },
                            )
                        }
                        EmptyStateMessage(
                            title =
                                if (uiState.mode == SummaryMode.YEAR) {
                                    "No data for this year"
                                } else {
                                    "No data for this month"
                                },
                            subtitle = "Start adding transactions to see your summary",
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                else -> {
                    val summary = uiState.summary
                    if (summary != null) {
                        SummaryContent(
                            summary = summary,
                            uiState = uiState,
                            onModeChanged = viewModel::setMode,
                            onPreviousMonth = viewModel::goToPreviousMonth,
                            onNextMonth = viewModel::goToNextMonth,
                            onPreviousYear = viewModel::goToPreviousYear,
                            onNextYear = viewModel::goToNextYear,
                            onMonthLabelClick = { showMonthPicker = true },
                            onBudgetTap = { currencyCode -> showBudgetDialog = currencyCode },
                            onMonthTapped = { month ->
                                viewModel.navigateToMonth(
                                    viewModel.currentSelectedYear(),
                                    month,
                                )
                            },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryModeChips(
    mode: SummaryMode,
    onModeChanged: (SummaryMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(DesignSystemSpacing.small),
    ) {
        FilterChip(
            selected = mode == SummaryMode.MONTH,
            onClick = { onModeChanged(SummaryMode.MONTH) },
            label = { Text("Month") },
            modifier =
                Modifier.semantics {
                    contentDescription =
                        if (mode == SummaryMode.MONTH) "Monthly summary selected" else "Switch to monthly summary"
                },
        )
        FilterChip(
            selected = mode == SummaryMode.YEAR,
            onClick = { onModeChanged(SummaryMode.YEAR) },
            label = { Text("Year") },
            modifier =
                Modifier.semantics {
                    contentDescription =
                        if (mode == SummaryMode.YEAR) "Yearly summary selected" else "Switch to yearly summary"
                },
        )
    }
}

@Composable
private fun PeriodSelector(
    uiState: SummaryUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onPreviousYear: () -> Unit,
    onNextYear: () -> Unit,
    onMonthLabelClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (uiState.mode == SummaryMode.YEAR) {
        MonthSelector(
            monthLabel = uiState.monthLabel,
            onPreviousMonth = onPreviousYear,
            onNextMonth = onNextYear,
            periodType = "year",
            modifier = modifier,
        )
    } else {
        MonthSelector(
            monthLabel = uiState.monthLabel,
            onPreviousMonth = onPreviousMonth,
            onNextMonth = onNextMonth,
            onMonthLabelClick = onMonthLabelClick,
            modifier = modifier,
        )
    }
}

@Composable
private fun SummaryContent(
    summary: MonthlySummary,
    uiState: SummaryUiState,
    onModeChanged: (SummaryMode) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onPreviousYear: () -> Unit,
    onNextYear: () -> Unit,
    onMonthLabelClick: () -> Unit,
    onBudgetTap: (String) -> Unit,
    onMonthTapped: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val currencyFormatter = remember { DefaultCurrencyFormatter() }
    LazyColumn(modifier = modifier.padding(horizontal = DesignSystemSpacing.screenPadding)) {
        item(key = "mode_chips") {
            SummaryModeChips(
                mode = uiState.mode,
                onModeChanged = onModeChanged,
            )
        }

        item(key = "header") {
            PeriodSelector(
                uiState = uiState,
                onPreviousMonth = onPreviousMonth,
                onNextMonth = onNextMonth,
                onPreviousYear = onPreviousYear,
                onNextYear = onNextYear,
                onMonthLabelClick = onMonthLabelClick,
            )
        }

        item(key = "disclaimer") {
            DisclaimerText()
        }

        itemsIndexed(
            items = summary.currencySummaries,
            key = { _, cs -> "currency_${cs.currencyCode}" },
        ) { index, currencySummary ->
            Column(
                verticalArrangement = Arrangement.spacedBy(DesignSystemSpacing.large),
            ) {
                if (index > 0) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = DesignSystemSpacing.small),
                    )
                }

                CurrencySectionHeader(currencyCode = currencySummary.currencyCode)

                SummaryCards(
                    currencySummary = currencySummary,
                )

                if (uiState.mode == SummaryMode.MONTH) {
                    val budgetStatus =
                        uiState.budgetStatuses.find {
                            it.currency.code == currencySummary.currencyCode
                        }
                    if (budgetStatus != null) {
                        BudgetProgressSection(
                            budgetStatus = budgetStatus,
                            currencyFormatter = currencyFormatter,
                            onTap = { onBudgetTap(currencySummary.currencyCode) },
                        )
                    } else {
                        Card(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onBudgetTap(currencySummary.currencyCode)
                                    },
                            elevation =
                                CardDefaults.cardElevation(
                                    defaultElevation = DesignSystemElevation.low,
                                ),
                        ) {
                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(DesignSystemSpacing.large),
                                horizontalArrangement =
                                    Arrangement.spacedBy(DesignSystemSpacing.medium),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Outlined.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp),
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text =
                                            stringResource(
                                                R.string.budget_setup_title,
                                            ),
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        text =
                                            stringResource(
                                                R.string.budget_setup_subtitle,
                                            ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color =
                                            MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }

                if (uiState.mode == SummaryMode.YEAR) {
                    val barPoints = uiState.monthlyBarData[currencySummary.currencyCode]
                    if (barPoints != null && barPoints.any { it.totalExpense > 0L }) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = DesignSystemElevation.low),
                        ) {
                            Column(
                                modifier = Modifier.padding(DesignSystemSpacing.large),
                            ) {
                                Text(
                                    text = stringResource(R.string.monthly_expenses),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(bottom = DesignSystemSpacing.small),
                                )
                                MonthlyBarChart(
                                    points = barPoints,
                                    emptyLabel = stringResource(R.string.no_expenses_this_year),
                                    onMonthTapped = onMonthTapped,
                                )
                            }
                        }
                    }
                }

                if (currencySummary.topExpenseCategories.isNotEmpty()) {
                    SectionTitle(
                        title = "Top Expense Categories",
                        modifier = Modifier.padding(top = DesignSystemSpacing.small),
                    )

                    currencySummary.topExpenseCategories.forEach { categoryTotal ->
                        CategoryTotalItem(
                            categoryTotal = categoryTotal,
                            currencyCode = currencySummary.currencyCode,
                        )
                    }

                    if (uiState.mode == SummaryMode.MONTH) {
                        Card(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(top = DesignSystemSpacing.small),
                            elevation = CardDefaults.cardElevation(defaultElevation = DesignSystemElevation.low),
                        ) {
                            Column(
                                modifier = Modifier.padding(DesignSystemSpacing.large),
                            ) {
                                Text(
                                    text = stringResource(R.string.expense_distribution),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(bottom = DesignSystemSpacing.small),
                                )
                                DonutChart(
                                    categories = currencySummary.topExpenseCategories,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CurrencySectionHeader(
    currencyCode: String,
    modifier: Modifier = Modifier,
) {
    val currencyDef = SupportedCurrencies.byCode(currencyCode)
    val label =
        if (currencyDef != null) {
            "${currencyDef.symbol} ${currencyDef.displayName} ($currencyCode)"
        } else {
            currencyCode
        }

    Text(
        text = label,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier =
            modifier.semantics {
                heading()
                contentDescription = "Currency section: $label"
            },
    )
}

@Composable
private fun DisclaimerText(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.summary_disclaimer),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

@Composable
private fun SummaryCards(
    currencySummary: CurrencyMonthlySummary,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(DesignSystemSpacing.medium),
    ) {
        SummaryCard(
            title = "Income",
            amount = currencySummary.totalIncome,
            currencyCode = currencySummary.currencyCode,
            transactionType = TransactionType.INCOME,
        )
        SummaryCard(
            title = "Expenses",
            amount = currencySummary.totalExpense,
            currencyCode = currencySummary.currencyCode,
            transactionType = TransactionType.EXPENSE,
        )
        SummaryCard(
            title = "Balance",
            amount = currencySummary.balance,
            currencyCode = currencySummary.currencyCode,
            transactionType =
                if (currencySummary.balance >= 0L) {
                    TransactionType.INCOME
                } else {
                    TransactionType.EXPENSE
                },
            isBalance = true,
        )
    }
}

@Composable
private fun SummaryCard(
    title: String,
    amount: Long,
    currencyCode: String,
    transactionType: TransactionType? = null,
    isBalance: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = DesignSystemElevation.medium),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(DesignSystemSpacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            AmountText(
                amount = if (isBalance && amount < 0L) -amount else amount,
                showSign = isBalance,
                transactionType = transactionType,
                currencyCode = currencyCode,
                fontWeight = FontWeight.Bold,
                textStyle = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(top = DesignSystemSpacing.small),
            )
        }
    }
}

@Composable
private fun CategoryTotalItem(
    categoryTotal: CategoryTotal,
    currencyCode: String,
    modifier: Modifier = Modifier,
) {
    val isOtherCategory = categoryTotal.category.id == TransactionRepositoryImpl.OTHER_CATEGORY_ID

    Card(
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
            Text(
                text = categoryTotal.category.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color =
                    if (isOtherCategory) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
            )
            AmountText(
                amount = categoryTotal.total,
                transactionType = TransactionType.EXPENSE,
                currencyCode = currencyCode,
                fontWeight = FontWeight.Bold,
                textStyle = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}
