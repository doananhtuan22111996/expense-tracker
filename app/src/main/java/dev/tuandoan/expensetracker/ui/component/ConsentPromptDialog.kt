package dev.tuandoan.expensetracker.ui.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import dev.tuandoan.expensetracker.R

/**
 * One-shot post-onboarding crash-reporting consent dialog (v3.11.0, ADR-008).
 *
 * Shown exactly once after onboarding completes (gated by
 * [dev.tuandoan.expensetracker.data.preferences.AnalyticsPreferences.consentPromptShown]).
 * Both [onAccept] and [onDecline] MUST mark the prompt as shown so it does
 * not re-appear on subsequent launches — the caller is responsible for that
 * persistence (see `HomeViewModel.onConsentAccepted` / `onConsentDeclined`).
 *
 * Back-gesture and scrim-tap resolve to [onDecline] per ADR-008: ambiguous
 * dismissals resolve to the privacy-safe default (no consent granted).
 *
 * Buttons arranged per Design doc: "Yes" as a full-width confirming action
 * nudging toward accept; "No thanks" as a [TextButton] dismissive action
 * that's still unmistakably a button (not an X or swipe-only dismiss).
 */
@Composable
fun ConsentPromptDialog(
    onAccept: () -> Unit,
    onDecline: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDecline,
        properties =
            DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
            ),
        title = { Text(stringResource(R.string.consent_prompt_title)) },
        text = { Text(stringResource(R.string.consent_prompt_body)) },
        confirmButton = {
            Button(
                onClick = onAccept,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.consent_prompt_yes))
            }
        },
        dismissButton = {
            TextButton(onClick = onDecline) {
                Text(stringResource(R.string.consent_prompt_no))
            }
        },
    )
}
