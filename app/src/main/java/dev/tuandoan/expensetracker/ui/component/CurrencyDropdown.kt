package dev.tuandoan.expensetracker.ui.component

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import dev.tuandoan.expensetracker.R
import dev.tuandoan.expensetracker.domain.model.SupportedCurrencies
import dev.tuandoan.expensetracker.ui.theme.DesignSystemElevation
import dev.tuandoan.expensetracker.ui.theme.DesignSystemSpacing

/**
 * Shared currency picker used across Add/Edit Transaction, Add/Edit Recurring,
 * and Create/Edit Trip (T2.5, v3.13.0). Replaces three near-identical
 * `private fun CurrencyDropdown` + the `CurrencyField` variant in CreateEditTrip.
 *
 * Variations exposed as parameters:
 * - [titleRes] — defaults to the generic `label_currency`. CreateEditTrip overrides
 *   with the trip-specific "Foreign currency" label.
 * - [disabledCodes] — set of currency codes that should render greyed and
 *   non-selectable. CreateEditTrip passes `setOf(homeCurrencyCode)`.
 * - [disabledMarkerSuffix] — optional suffix appended to disabled-item text
 *   (e.g. `" · Home"`). Null → no suffix.
 *
 * Per-item a11y descriptions (selected vs unselected) are always emitted —
 * this is strictly an improvement for screens that previously omitted them.
 */
@Composable
fun CurrencyDropdown(
    selectedCurrencyCode: String,
    onCurrencySelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    @StringRes titleRes: Int = R.string.label_currency,
    disabledCodes: Set<String> = emptySet(),
    disabledMarkerSuffix: String? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    val allCurrencies = remember { SupportedCurrencies.all() }
    val selectedCurrency =
        remember(selectedCurrencyCode) {
            SupportedCurrencies.byCode(selectedCurrencyCode) ?: SupportedCurrencies.default()
        }
    val displayText = "${selectedCurrency.code} - ${selectedCurrency.displayName} ${selectedCurrency.symbol}"
    val currencyCardDescription = stringResource(R.string.a11y_currency_tap_to_change, displayText)
    val hapticFeedback = LocalHapticFeedback.current

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(DesignSystemSpacing.xs),
    ) {
        Text(
            text = stringResource(titleRes),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Card(
            onClick = {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                expanded = true
            },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = currencyCardDescription
                    },
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
                    text = displayText,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = stringResource(R.string.a11y_open_currency_selection),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            allCurrencies.forEach { currency ->
                val isSelected = currency.code == selectedCurrencyCode
                val isDisabled = currency.code in disabledCodes
                val itemText =
                    "${currency.code} - ${currency.displayName} ${currency.symbol}" +
                        (if (isDisabled && disabledMarkerSuffix != null) disabledMarkerSuffix else "")
                val itemDescription =
                    when {
                        isSelected ->
                            stringResource(
                                R.string.a11y_currency_currently_selected,
                                currency.code,
                                currency.displayName,
                            )
                        isDisabled ->
                            stringResource(
                                R.string.a11y_currency_item_disabled,
                                currency.code,
                                currency.displayName,
                            )
                        else -> stringResource(R.string.a11y_select_currency_item, currency.code, currency.displayName)
                    }
                DropdownMenuItem(
                    text = {
                        Text(
                            itemText,
                            style = MaterialTheme.typography.bodyLarge,
                            color =
                                when {
                                    isSelected -> MaterialTheme.colorScheme.primary
                                    isDisabled -> MaterialTheme.colorScheme.onSurfaceVariant
                                    else -> MaterialTheme.colorScheme.onSurface
                                },
                        )
                    },
                    trailingIcon = {
                        if (isSelected) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    },
                    enabled = !isDisabled,
                    modifier = Modifier.semantics { contentDescription = itemDescription },
                    onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        onCurrencySelected(currency.code)
                        expanded = false
                    },
                )
            }
        }
    }
}
