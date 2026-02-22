package dev.tuandoan.expensetracker.ui.screen.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import dev.tuandoan.expensetracker.core.util.AppInfo
import dev.tuandoan.expensetracker.domain.model.CurrencyDefinition
import dev.tuandoan.expensetracker.domain.model.SupportedCurrencies
import dev.tuandoan.expensetracker.ui.component.SectionHeader
import dev.tuandoan.expensetracker.ui.component.SectionTitle
import dev.tuandoan.expensetracker.ui.theme.DesignSystemElevation
import dev.tuandoan.expensetracker.ui.theme.DesignSystemSpacing

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCurrencyDialog by remember { mutableStateOf(false) }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(DesignSystemSpacing.screenPadding)
                .verticalScroll(rememberScrollState()),
    ) {
        // Title
        SectionHeader(title = "Settings")

        // Preferences Section
        SettingsSection(title = "Preferences") {
            val selectedCurrency = SupportedCurrencies.byCode(uiState.selectedCurrencyCode)
            val displayText =
                if (selectedCurrency != null) {
                    "${selectedCurrency.code} - ${selectedCurrency.displayName} ${selectedCurrency.symbol}"
                } else {
                    uiState.selectedCurrencyCode
                }

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { showCurrencyDialog = true }
                        .padding(DesignSystemSpacing.large)
                        .semantics {
                            contentDescription = "Default currency: $displayText, tap to change"
                        },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Default Currency",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = displayText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = DesignSystemSpacing.xs),
                    )
                }
                Icon(
                    Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Text(
                text = "Used for new transactions only",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier =
                    Modifier.padding(
                        start = DesignSystemSpacing.large,
                        end = DesignSystemSpacing.large,
                        bottom = DesignSystemSpacing.large,
                    ),
            )
        }

        // App Information Section
        SettingsSection(title = "App Information") {
            SettingsItem(
                title = "Version",
                subtitle = AppInfo.getFullVersionInfo(),
            )

            HorizontalDivider()

            SettingsItem(
                title = "Contact Email",
                subtitle = "doananhtuan22111996@gmail.com",
            )
        }

        // Privacy Section
        SettingsSection(title = "Privacy") {
            Column(modifier = Modifier.padding(DesignSystemSpacing.large)) {
                Text(
                    text = "Privacy Statement",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = DesignSystemSpacing.small),
                )
                Text(
                    text = "All data is stored locally on your device. No data is collected or shared.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = DesignSystemSpacing.large),
                )

                Text(
                    text = "Data Privacy",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = DesignSystemSpacing.xs),
                )
                Text(
                    text =
                        "• All transaction data is stored locally on your device\n" +
                            "• No personal information is transmitted to external servers\n" +
                            "• No analytics or tracking is performed\n" +
                            "• No advertisements are displayed\n" +
                            "• No account creation or login is required",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = DesignSystemSpacing.large),
                )

                Text(
                    text = "Data Storage",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = DesignSystemSpacing.xs),
                )
                Text(
                    text =
                        "Your expense data is stored in a local database on your device. " +
                            "Uninstalling the app will permanently delete all data. " +
                            "We recommend backing up your data if needed.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // About Section
        SettingsSection(title = "About") {
            Column(modifier = Modifier.padding(DesignSystemSpacing.large)) {
                Text(
                    text = "Expense Tracker",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = DesignSystemSpacing.small),
                )
                Text(
                    text =
                        "A simple, offline-first personal expense tracking app for managing your " +
                            "income and expenses locally on your device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = DesignSystemSpacing.medium),
                )

                // Application details for support/debugging
                if (AppInfo.isDebugBuild()) {
                    Text(
                        text = "Debug Information",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = DesignSystemSpacing.xs),
                    )
                    Text(
                        text = "Application ID: ${AppInfo.getApplicationId()}\nBuild Type: ${AppInfo.getBuildType()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    )
                }
            }
        }
    }

    // Currency Selection Dialog
    if (showCurrencyDialog) {
        CurrencySelectionDialog(
            availableCurrencies = uiState.availableCurrencies,
            selectedCurrencyCode = uiState.selectedCurrencyCode,
            onCurrencySelected = { code ->
                viewModel.onCurrencySelected(code)
                showCurrencyDialog = false
            },
            onDismiss = { showCurrencyDialog = false },
        )
    }
}

@Composable
private fun CurrencySelectionDialog(
    availableCurrencies: List<CurrencyDefinition>,
    selectedCurrencyCode: String,
    onCurrencySelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Default Currency") },
        text = {
            LazyColumn {
                items(availableCurrencies) { currency ->
                    val isSelected = currency.code == selectedCurrencyCode
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable { onCurrencySelected(currency.code) }
                                .padding(DesignSystemSpacing.medium)
                                .semantics {
                                    contentDescription =
                                        if (isSelected) {
                                            "${currency.code} ${currency.displayName}, currently selected"
                                        } else {
                                            "Select ${currency.code} ${currency.displayName}"
                                        }
                                },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "${currency.code} - ${currency.displayName} ${currency.symbol}",
                            style = MaterialTheme.typography.bodyLarge,
                            color =
                                if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                            modifier = Modifier.weight(1f),
                        )
                        if (isSelected) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun SettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier.padding(bottom = DesignSystemSpacing.large)) {
        SectionTitle(title = title)

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = DesignSystemElevation.low),
        ) {
            content()
        }
    }
}

@Composable
private fun SettingsItem(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(DesignSystemSpacing.large),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = DesignSystemSpacing.xs),
        )
    }
}
