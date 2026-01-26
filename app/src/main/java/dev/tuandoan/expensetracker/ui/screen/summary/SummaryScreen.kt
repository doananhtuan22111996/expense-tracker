package dev.tuandoan.expensetracker.ui.screen.summary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import dev.tuandoan.expensetracker.domain.model.CategoryTotal
import dev.tuandoan.expensetracker.domain.model.MonthlySummary
import dev.tuandoan.expensetracker.domain.model.TransactionType
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
                    message = uiState.errorMessage ?: "An error occurred",
                    modifier = Modifier.fillMaxSize(),
                )
            }
            uiState.summary == null -> {
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
        item {
            SectionHeader(title = "This Month")
        }

        item {
            SummaryCards(summary = summary)
        }

        if (summary.topExpenseCategories.isNotEmpty()) {
            item {
                SectionTitle(
                    title = "Top Expense Categories",
                    modifier = Modifier.padding(top = DesignSystemSpacing.small),
                )
            }

            items(summary.topExpenseCategories) { categoryTotal ->
                CategoryTotalItem(categoryTotal = categoryTotal)
            }
        }
    }
}

@Composable
private fun SummaryCards(
    summary: MonthlySummary,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(DesignSystemSpacing.medium),
    ) {
        // Income Card
        SummaryCard(
            title = "Income",
            amount = summary.totalIncome,
            transactionType = TransactionType.INCOME,
        )

        // Expense Card
        SummaryCard(
            title = "Expenses",
            amount = summary.totalExpense,
            transactionType = TransactionType.EXPENSE,
        )

        // Balance Card
        SummaryCard(
            title = "Balance",
            amount = summary.balance,
            isBalance = true,
        )
    }
}

@Composable
private fun SummaryCard(
    title: String,
    amount: Long,
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

            if (isBalance) {
                AmountText(
                    amount = amount,
                    showSign = true,
                    fontWeight = FontWeight.Bold,
                    textStyle = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(top = DesignSystemSpacing.small),
                )
            } else {
                AmountText(
                    amount = amount,
                    transactionType = transactionType,
                    fontWeight = FontWeight.Bold,
                    textStyle = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(top = DesignSystemSpacing.small),
                )
            }
        }
    }
}

@Composable
private fun CategoryTotalItem(
    categoryTotal: CategoryTotal,
    modifier: Modifier = Modifier,
) {
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
                color = MaterialTheme.colorScheme.onSurface,
            )
            AmountText(
                amount = categoryTotal.total,
                transactionType = TransactionType.EXPENSE,
                fontWeight = FontWeight.Bold,
                textStyle = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}
