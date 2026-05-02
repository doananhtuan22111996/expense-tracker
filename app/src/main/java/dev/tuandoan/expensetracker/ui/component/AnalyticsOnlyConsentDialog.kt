package dev.tuandoan.expensetracker.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import dev.tuandoan.expensetracker.R
import dev.tuandoan.expensetracker.ui.theme.DesignSystemSpacing

/**
 * Analytics-only consent dialog — the upgrade-path variant (v3.11.0, ADR-011).
 *
 * Shown to users who already resolved Crashlytics consent in a pre-bundle
 * v3.11.0 preview (i.e. `consentPromptShown = true` persisted, regardless
 * of whether their Crashlytics answer was yes or no), and have not yet seen
 * the new Analytics question (`analyticsEventsPromptShown = false`). The
 * Crashlytics question is intentionally NOT re-asked — their prior choice
 * stands.
 *
 * Fires [onResolved] with the current `shareAnalytics` checkbox state on
 * both the "Done" tap and any dismiss route (back-gesture / outside-tap),
 * mirroring the main dialog's commit-current-state semantics. The caller
 * persists `analyticsEventsConsent` AND marks `analyticsEventsPromptShown
 * = true` so this dialog does not re-appear.
 */
@Composable
fun AnalyticsOnlyConsentDialog(onResolved: (shareAnalytics: Boolean) -> Unit) {
    var shareAnalytics by rememberSaveable { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { onResolved(shareAnalytics) },
        properties =
            DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
            ),
        title = { Text(stringResource(R.string.consent_prompt_analytics_only_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(DesignSystemSpacing.medium),
            ) {
                Text(
                    text = stringResource(R.string.consent_prompt_analytics_only_intro),
                    style = MaterialTheme.typography.bodyMedium,
                )
                ConsentCheckboxRow(
                    title = stringResource(R.string.consent_prompt_analytics_title),
                    description = stringResource(R.string.consent_prompt_analytics_description),
                    checked = shareAnalytics,
                    onCheckedChange = { shareAnalytics = it },
                )
                Text(
                    text = stringResource(R.string.consent_prompt_footer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onResolved(shareAnalytics) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.consent_prompt_done))
            }
        },
    )
}
