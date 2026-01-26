package dev.tuandoan.expensetracker.ui.screen.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import dev.tuandoan.expensetracker.core.util.AppInfo
import dev.tuandoan.expensetracker.ui.component.SectionHeader
import dev.tuandoan.expensetracker.ui.component.SectionTitle
import dev.tuandoan.expensetracker.ui.theme.DesignSystemElevation
import dev.tuandoan.expensetracker.ui.theme.DesignSystemSpacing

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(DesignSystemSpacing.screenPadding)
                .verticalScroll(rememberScrollState()),
    ) {
        // Title
        SectionHeader(title = "Settings")

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
