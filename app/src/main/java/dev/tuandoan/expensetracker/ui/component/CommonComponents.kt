package dev.tuandoan.expensetracker.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.tuandoan.expensetracker.core.formatter.AmountFormatter
import dev.tuandoan.expensetracker.domain.model.TransactionType
import dev.tuandoan.expensetracker.ui.theme.DesignSystemSpacing
import dev.tuandoan.expensetracker.ui.theme.FinancialColors

/**
 * Reusable component for displaying VND amounts with consistent formatting
 */
@Composable
fun AmountText(
    amount: Long,
    transactionType: TransactionType? = null,
    showSign: Boolean = false,
    showCurrency: Boolean = true,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight = FontWeight.Normal,
    textStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyLarge,
) {
    val formattedAmount =
        when {
            showSign && transactionType != null ->
                AmountFormatter.formatAmountWithSign(amount, transactionType == TransactionType.INCOME)
            showCurrency ->
                AmountFormatter.formatAmountWithCurrency(amount)
            else ->
                AmountFormatter.formatAmount(amount)
        }

    val color =
        when {
            transactionType == TransactionType.INCOME -> FinancialColors.incomeColor()
            transactionType == TransactionType.EXPENSE -> FinancialColors.expenseColor()
            else -> MaterialTheme.colorScheme.onSurface
        }

    Text(
        text = formattedAmount,
        style = textStyle,
        fontWeight = fontWeight,
        color = color,
        modifier = modifier,
    )
}

/**
 * Reusable component for screen and section titles
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.headlineMedium,
) {
    Text(
        text = title,
        style = style,
        modifier = modifier.padding(bottom = DesignSystemSpacing.large),
    )
}

/**
 * Consistent empty state component
 */
@Composable
fun EmptyStateMessage(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = DesignSystemSpacing.small),
            )
        }
    }
}

/**
 * Enhanced error state component with retry functionality
 */
@Composable
fun ErrorStateMessage(
    title: String = "Something went wrong",
    message: String,
    onRetry: (() -> Unit)? = null,
    retryButtonText: String = "Try Again",
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(DesignSystemSpacing.large),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.error,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier =
                    Modifier.padding(
                        top = DesignSystemSpacing.small,
                        bottom = if (onRetry != null) DesignSystemSpacing.large else 0.dp,
                    ),
            )

            // Show retry button if onRetry callback is provided
            onRetry?.let { retry ->
                Button(
                    onClick = retry,
                    modifier = Modifier.padding(top = DesignSystemSpacing.medium),
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.padding(end = DesignSystemSpacing.xs),
                    )
                    Text(retryButtonText)
                }
            }
        }
    }
}

/**
 * Reusable component for consistent section titles
 */
@Composable
fun SectionTitle(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(bottom = DesignSystemSpacing.small),
    )
}
