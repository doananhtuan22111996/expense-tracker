package dev.tuandoan.expensetracker.ui.screen.security

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.tuandoan.expensetracker.R
import dev.tuandoan.expensetracker.domain.security.AppLockManager
import dev.tuandoan.expensetracker.domain.security.BiometricAuthHelper
import dev.tuandoan.expensetracker.domain.security.BiometricAuthResult
import dev.tuandoan.expensetracker.ui.theme.DesignSystemSpacing

/**
 * Top-level security gate wrapping app content (v3.16.0, ADR-018).
 *
 * When [AppLockManager.isLocked] is true, renders [AppLockScreen] and initiates
 * biometric/device credential authentication, protecting sensitive screens from being
 * displayed until authentication succeeds.
 */
@Composable
fun AppLockGate(
    activity: FragmentActivity,
    appLockManager: AppLockManager,
    biometricAuthHelper: BiometricAuthHelper,
    content: @Composable () -> Unit,
) {
    val isLocked by appLockManager.isLocked.collectAsStateWithLifecycle()
    val isInitialized by appLockManager.isInitialized.collectAsStateWithLifecycle()

    if (!isInitialized) {
        // Neutral background while preferences load to prevent unauthenticated content flashes
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
        )
        return
    }

    if (isLocked) {
        AppLockScreen(
            activity = activity,
            appLockManager = appLockManager,
            biometricAuthHelper = biometricAuthHelper,
        )
    } else {
        content()
    }
}

@Composable
fun AppLockScreen(
    activity: FragmentActivity,
    appLockManager: AppLockManager,
    biometricAuthHelper: BiometricAuthHelper,
    modifier: Modifier = Modifier,
) {
    BackHandler {
        activity.finish()
    }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    val unlockTitle = stringResource(R.string.security_unlock_title)
    val unlockSubtitle = stringResource(R.string.security_unlock_subtitle)
    val genericAuthFailed = stringResource(R.string.security_auth_failed)

    val triggerAuth = {
        errorMessage = null
        biometricAuthHelper.authenticate(
            activity = activity,
            title = unlockTitle,
            subtitle = unlockSubtitle,
        ) { result ->
            when (result) {
                is BiometricAuthResult.Success -> {
                    errorMessage = null
                    appLockManager.unlock()
                }
                is BiometricAuthResult.Failed -> {
                    errorMessage = genericAuthFailed
                }
                is BiometricAuthResult.Error -> {
                    errorMessage = result.errString.toString()
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        triggerAuth()
    }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(DesignSystemSpacing.screenPadding),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = DesignSystemSpacing.large),
        ) {
            Surface(
                modifier = Modifier.size(80.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(DesignSystemSpacing.large))

            Text(
                text = stringResource(R.string.security_locked_description),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Spacer(modifier = Modifier.height(DesignSystemSpacing.small))

            Text(
                text = stringResource(R.string.security_unlock_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (!errorMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(DesignSystemSpacing.medium))
                Text(
                    text = errorMessage.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(modifier = Modifier.height(DesignSystemSpacing.xxl))

            Button(
                onClick = triggerAuth,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
            ) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = null,
                    modifier = Modifier.padding(end = DesignSystemSpacing.small),
                )
                Text(text = stringResource(R.string.security_unlock_button))
            }
        }
    }
}
