package dev.tuandoan.expensetracker.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import dev.tuandoan.expensetracker.R
import dev.tuandoan.expensetracker.ui.theme.DesignSystemSpacing

/**
 * Post-onboarding consent dialog, dual-checkbox variant (v3.11.0, ADR-011).
 *
 * Replaces the single-question [ConsentPromptDialog] from PR #105 for fresh
 * installs once Task 5.6 rewires `HomeScreen` to branch on the four-key
 * consent state. Until then this composable is defined but not yet mounted;
 * keeping the old dialog in place lets the prefs + tests land first.
 *
 * ### Resolution semantics
 *
 * Both checkboxes default to unchecked — privacy-safe per ADR-011. The user
 * commits the current state by tapping "Done", OR by dismissing via back
 * gesture / outside-tap (ADR-008: ambiguous dismissal resolves to the
 * privacy-safe default, which is exactly what "current unchecked state"
 * expresses). Either way [onResolved] fires with the current booleans and
 * the caller persists both `*Consent` values AND marks both `*PromptShown`
 * flags as `true` so the dialog does not re-appear.
 *
 * Local checkbox state uses [rememberSaveable] so a configuration change
 * (rotation, dark mode) preserves in-progress choices. Process death
 * intentionally resets to unchecked — the privacy-safe default.
 */
@Composable
fun ConsentPromptDialogV2(onResolved: (shareCrashes: Boolean, shareAnalytics: Boolean) -> Unit) {
    var shareCrashes by rememberSaveable { mutableStateOf(false) }
    var shareAnalytics by rememberSaveable { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { onResolved(shareCrashes, shareAnalytics) },
        properties =
            DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
            ),
        title = { Text(stringResource(R.string.consent_prompt_v2_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(DesignSystemSpacing.medium),
            ) {
                Text(
                    text = stringResource(R.string.consent_prompt_v2_intro),
                    style = MaterialTheme.typography.bodyMedium,
                )
                ConsentCheckboxRow(
                    title = stringResource(R.string.consent_prompt_crash_title),
                    description = stringResource(R.string.consent_prompt_crash_description),
                    checked = shareCrashes,
                    onCheckedChange = { shareCrashes = it },
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
                onClick = { onResolved(shareCrashes, shareAnalytics) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.consent_prompt_done))
            }
        },
    )
}

/**
 * Row with a leading checkbox, title, and one-line description. The entire
 * row is [clickable] (not just the checkbox widget) so the touch target is
 * a full 56dp-tall band — matches Material 3 list-item guidance and doubles
 * the hit area at font-scale 200%.
 *
 * `Role.Checkbox` on the row's clickable semantics means TalkBack announces
 * "{title}, {description}, checkbox, checked/unchecked. Double-tap to toggle."
 * The trailing [Checkbox] widget itself intentionally has no independent
 * click handler — the row owns the gesture so state stays in one place.
 */
@Composable
internal fun ConsentCheckboxRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 56.dp)
                .clickable(
                    role = Role.Checkbox,
                    onClick = { onCheckedChange(!checked) },
                ).padding(vertical = DesignSystemSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = null,
        )
        Spacer(modifier = Modifier.width(DesignSystemSpacing.small))
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
