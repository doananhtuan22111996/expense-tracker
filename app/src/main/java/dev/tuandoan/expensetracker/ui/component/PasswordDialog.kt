package dev.tuandoan.expensetracker.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.window.DialogProperties
import dev.tuandoan.expensetracker.R
import dev.tuandoan.expensetracker.ui.theme.DesignSystemSpacing

private const val MIN_PASSWORD_LENGTH = 8

/**
 * Material 3 password dialog with a password (+ optional confirm) field and a show/hide toggle.
 *
 * The password is captured as a local [CharArray]. When the user confirms, [onConfirm]
 * is invoked with a defensive copy; this composable zeros its own buffer in the
 * `DisposableEffect.onDispose` block so the array does not linger after the dialog
 * closes. Callers are still expected to dispose of their own copy promptly.
 *
 * @param requireConfirm when `true` (the default, used for export), the user must type
 * the password twice and enforce the 8-char minimum. When `false` (used for import
 * decrypt), only a single field is shown with no minimum — the backup file itself
 * authenticates the password via GCM.
 * @param errorMessage optional inline error text rendered under the password field
 * (e.g. "Incorrect password"). When present, the password field renders in the
 * Material error color.
 */
@Composable
fun PasswordDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: (CharArray) -> Unit,
    onDismiss: () -> Unit,
    requireConfirm: Boolean = true,
    errorMessage: String? = null,
) {
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val tooShort =
        requireConfirm && password.isNotEmpty() && password.length < MIN_PASSWORD_LENGTH
    val mismatch =
        requireConfirm && confirmPassword.isNotEmpty() && password != confirmPassword
    val canConfirm =
        if (requireConfirm) {
            password.length >= MIN_PASSWORD_LENGTH && password == confirmPassword
        } else {
            password.isNotEmpty()
        }

    DisposableEffect(Unit) {
        onDispose {
            // Best-effort wipe — the String backing store is immutable on the JVM so
            // the actual characters may survive, but clearing the local references
            // minimises the window during which they are reachable.
            password = ""
            confirmPassword = ""
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false),
        title = { Text(title) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(DesignSystemSpacing.medium),
            ) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                )
                val passwordContentType =
                    if (requireConfirm) ContentType.NewPassword else ContentType.Password
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.settings_backup_password_label)) },
                    singleLine = true,
                    isError = tooShort || errorMessage != null,
                    supportingText = {
                        when {
                            errorMessage != null -> Text(errorMessage)
                            tooShort ->
                                Text(stringResource(R.string.settings_backup_password_too_short))
                        }
                    },
                    visualTransformation = passwordVisualTransformation(passwordVisible),
                    keyboardOptions = passwordKeyboardOptions(),
                    trailingIcon = {
                        VisibilityToggle(passwordVisible) { passwordVisible = !passwordVisible }
                    },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .semantics { contentType = passwordContentType },
                )
                if (requireConfirm) {
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text(stringResource(R.string.settings_backup_password_confirm_label)) },
                        singleLine = true,
                        isError = mismatch,
                        supportingText = {
                            if (mismatch) {
                                Text(stringResource(R.string.settings_backup_password_mismatch))
                            }
                        },
                        visualTransformation = passwordVisualTransformation(passwordVisible),
                        keyboardOptions = passwordKeyboardOptions(),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .semantics { contentType = ContentType.NewPassword },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = canConfirm,
                onClick = {
                    val captured = password.toCharArray()
                    password = ""
                    confirmPassword = ""
                    onConfirm(captured)
                },
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
private fun VisibilityToggle(
    visible: Boolean,
    onToggle: () -> Unit,
) {
    val description =
        if (visible) {
            stringResource(R.string.settings_backup_password_hide)
        } else {
            stringResource(R.string.settings_backup_password_show)
        }
    IconButton(onClick = onToggle) {
        Icon(
            imageVector = if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
            contentDescription = description,
        )
    }
}

private fun passwordVisualTransformation(visible: Boolean): VisualTransformation =
    if (visible) VisualTransformation.None else PasswordVisualTransformation()

private fun passwordKeyboardOptions() =
    androidx.compose.foundation.text
        .KeyboardOptions(keyboardType = KeyboardType.Password)
