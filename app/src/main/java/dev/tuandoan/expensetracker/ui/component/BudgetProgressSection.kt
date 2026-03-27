package dev.tuandoan.expensetracker.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import dev.tuandoan.expensetracker.R
import dev.tuandoan.expensetracker.core.formatter.CurrencyFormatter
import dev.tuandoan.expensetracker.domain.model.BudgetStatus
import dev.tuandoan.expensetracker.domain.model.BudgetStatusLevel
import dev.tuandoan.expensetracker.ui.theme.DesignSystemElevation
import dev.tuandoan.expensetracker.ui.theme.DesignSystemSpacing

private val AmberWarning = Color(0xFFFF9800)

@Composable
fun BudgetProgressSection(
    budgetStatus: BudgetStatus,
    currencyFormatter: CurrencyFormatter,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val progressColor =
        when (budgetStatus.status) {
            BudgetStatusLevel.OK -> MaterialTheme.colorScheme.primary
            BudgetStatusLevel.WARNING -> AmberWarning
            BudgetStatusLevel.OVER_BUDGET -> MaterialTheme.colorScheme.error
        }

    val spentFormatted = currencyFormatter.format(budgetStatus.spentAmount, budgetStatus.currency.code)
    val budgetFormatted = currencyFormatter.format(budgetStatus.budgetAmount, budgetStatus.currency.code)
    val percentage = (budgetStatus.progressFraction * 100).toInt()

    val statusText =
        if (budgetStatus.status == BudgetStatusLevel.OVER_BUDGET) {
            val overAmount = currencyFormatter.format(-budgetStatus.remainingAmount, budgetStatus.currency.code)
            stringResource(R.string.over_budget_by, overAmount)
        } else {
            stringResource(R.string.spent_of_budget, spentFormatted, budgetFormatted)
        }

    val accessibilityText = stringResource(R.string.budget_percentage, percentage)

    Card(
        onClick = onTap,
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = DesignSystemElevation.low),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(DesignSystemSpacing.large)
                    .semantics { contentDescription = "$statusText. $accessibilityText" },
            verticalArrangement = Arrangement.spacedBy(DesignSystemSpacing.small),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.monthly_budget),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.budget_percentage, percentage),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = progressColor,
                )
            }

            LinearProgressIndicator(
                progress = { budgetStatus.progressFraction.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
                color = progressColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )

            Text(
                text = statusText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun SetBudgetDialog(
    currencyCode: String,
    currentBudget: Long?,
    onSave: (Long) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    var amountText by remember {
        mutableStateOf(currentBudget?.toString() ?: "")
    }
    var errorText by remember { mutableStateOf<String?>(null) }
    val budgetErrorText = stringResource(R.string.budget_must_be_positive)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.set_monthly_budget),
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(DesignSystemSpacing.small),
            ) {
                Text(
                    text = currencyCode,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { newValue ->
                        amountText = newValue.filter { it.isDigit() }
                        errorText = null
                    },
                    label = { Text(stringResource(R.string.budget_amount)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = errorText != null,
                    supportingText =
                        errorText?.let { error ->
                            { Text(error) }
                        },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amount = amountText.toLongOrNull()
                    if (amount == null || amount <= 0L) {
                        errorText = budgetErrorText
                    } else {
                        onSave(amount)
                        onDismiss()
                    }
                },
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            Row {
                if (currentBudget != null) {
                    TextButton(
                        onClick = {
                            onClear()
                            onDismiss()
                        },
                    ) {
                        Text(stringResource(R.string.remove_budget))
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
            }
        },
    )
}
