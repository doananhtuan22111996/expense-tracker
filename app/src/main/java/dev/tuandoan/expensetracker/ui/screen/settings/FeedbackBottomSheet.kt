package dev.tuandoan.expensetracker.ui.screen.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.tuandoan.expensetracker.R
import dev.tuandoan.expensetracker.core.util.AppInfo
import dev.tuandoan.expensetracker.ui.theme.DesignSystemSpacing

/**
 * Bottom sheet for collecting user feedback.
 *
 * Two paths:
 * - "Loving it!" dismisses the sheet (caller may trigger in-app review).
 * - "Found an issue" shows a text field and sends feedback via email intent.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackBottomSheet(
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onPositiveFeedback: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var showIssueForm by remember { mutableStateOf(false) }
    var issueDescription by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        start = DesignSystemSpacing.large,
                        end = DesignSystemSpacing.large,
                        bottom = DesignSystemSpacing.xl,
                    ),
        ) {
            Text(
                text = stringResource(R.string.settings_send_feedback),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = DesignSystemSpacing.large),
            )

            if (!showIssueForm) {
                ListItem(
                    headlineContent = {
                        Text(text = stringResource(R.string.feedback_loving_it))
                    },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Outlined.ThumbUp,
                            contentDescription = stringResource(R.string.feedback_loving_it),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                onPositiveFeedback()
                                onDismiss()
                            },
                )

                ListItem(
                    headlineContent = {
                        Text(text = stringResource(R.string.feedback_found_issue))
                    },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Outlined.BugReport,
                            contentDescription = stringResource(R.string.feedback_found_issue),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable { showIssueForm = true },
                )
            } else {
                OutlinedTextField(
                    value = issueDescription,
                    onValueChange = { issueDescription = it },
                    label = { Text(text = stringResource(R.string.feedback_describe_issue)) },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(FEEDBACK_TEXT_FIELD_HEIGHT),
                    maxLines = 5,
                )

                Spacer(modifier = Modifier.height(DesignSystemSpacing.medium))

                Button(
                    onClick = {
                        sendFeedbackEmail(context, issueDescription)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = issueDescription.isNotBlank(),
                ) {
                    Text(text = stringResource(R.string.feedback_send))
                }

                TextButton(
                    onClick = {
                        showIssueForm = false
                        issueDescription = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = stringResource(R.string.feedback_cancel))
                }
            }
        }
    }
}

private val FEEDBACK_TEXT_FIELD_HEIGHT = 150.dp

/**
 * Launches an email intent with device info and the user's issue description.
 * Only includes app version, device model, and Android version -- no PII or user data.
 */
private fun sendFeedbackEmail(
    context: Context,
    issueDescription: String,
) {
    val appVersion = AppInfo.getVersionName()
    val deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}"
    val androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"

    val subject = context.getString(R.string.feedback_email_subject)
    val bodyPrefix =
        context.getString(
            R.string.feedback_email_body_prefix,
            appVersion,
            deviceModel,
            androidVersion,
        )
    val fullBody = bodyPrefix + issueDescription

    val intent =
        Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf("doananhtuan22111996@gmail.com"))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, fullBody)
        }

    @Suppress("TooGenericExceptionCaught")
    try {
        context.startActivity(Intent.createChooser(intent, subject))
    } catch (_: Exception) {
        // No email client available -- silently ignore
    }
}
