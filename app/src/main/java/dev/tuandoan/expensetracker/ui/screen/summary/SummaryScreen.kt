package dev.tuandoan.expensetracker.ui.screen.summary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import dev.tuandoan.expensetracker.R
import dev.tuandoan.expensetracker.domain.model.CategoryTotal
import dev.tuandoan.expensetracker.domain.model.CurrencyMonthlySummary
import dev.tuandoan.expensetracker.domain.model.MonthlySummary
import dev.tuandoan.expensetracker.domain.model.SupportedCurrencies
import dev.tuandoan.expensetracker.domain.model.TransactionType
import dev.tuandoan.expensetracker.repository.TransactionRepositoryImpl
import dev.tuandoan.expensetracker.ui.component.AmountText
import dev.tuandoan.expensetracker.ui.component.EmptyStateMessage
import dev.tuandoan.expensetracker.ui.component.ErrorStateMessage
import dev.tuandoan.expensetracker.ui.component.SectionHeader
import dev.tuandoan.expensetracker.ui.component.SectionTitle
import dev.tuandoan.expensetracker.ui.theme.DesignSystemElevation
import dev.tuandoan.expensetracker.ui.theme.DesignSystemSpacing

@Composable
fun SummaryScreen(
    viewModel: SummaryViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(modifier = modifier.fillMaxSize()) {
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
                EmptyStateMessage(
                    title = "No data for this month",
                    subtitle = "Start adding transactions to see your summary",
                    modifier = Modifier.fillMaxSize(),
                )
            }
            else -> {
                val summary = uiState.summary
                if (summary != null) {
                    SummaryContent(
                        summary = summary,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryContent(
    summary: MonthlySummary,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.padding(DesignSystemSpacing.screenPadding),
        verticalArrangement = Arrangement.spacedBy(DesignSystemSpacing.large),
    ) {
        item(key = "header") {
            SectionHeader(title = "This Month")
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
        // Income Card
        SummaryCard(
            title = "Income",
            amount = currencySummary.totalIncome,
            currencyCode = currencySummary.currencyCode,
            transactionType = TransactionType.INCOME,
        )

        // Expense Card
        SummaryCard(
            title = "Expenses",
            amount = currencySummary.totalExpense,
            currencyCode = currencySummary.currencyCode,
            transactionType = TransactionType.EXPENSE,
        )

        // Balance Card
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
