package dev.tuandoan.expensetracker.ui.screen.debug

import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.tuandoan.expensetracker.R
import dev.tuandoan.expensetracker.ui.theme.DesignSystemSpacing

/**
 * Hidden developer-only panel (v3.11.0, ADR-010 / PRD FR-13). Reachable by
 * tapping the Settings version text 7 times within 3 seconds — the Easter-egg
 * gesture is intentional so normal users don't trigger it. No TalkBack or
 * discoverability help; this screen is only for release-verification workflows.
 *
 * The "Trigger test crash" button posts a [RuntimeException] through
 * [Handler] so the throw happens on the main looper's next pass, **outside**
 * Compose's input-callback boundary. Throwing directly inside the `onClick`
 * lambda would be caught by Compose's gesture dispatcher and never reach the
 * default `UncaughtExceptionHandler` — which means Crashlytics would not
 * receive the crash. The `Handler.post` escape is mandatory for
 * release-verification to work.
 *
 * Once the throw escapes, the default JVM `UncaughtExceptionHandler` picks
 * it up and Firebase Crashlytics's installed handler (release builds only,
 * per ADR-010) forwards it to the dashboard as a fatal. Debug builds have
 * no Firebase on classpath, so the process simply dies locally.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugPanelScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.debug_panel_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.a11y_debug_panel_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        DebugPanelContent(innerPadding = innerPadding)
    }
}

@Composable
private fun DebugPanelContent(innerPadding: PaddingValues) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(DesignSystemSpacing.large),
    ) {
        Text(
            text = stringResource(R.string.debug_panel_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Button(
            onClick = {
                // Post to main looper so the throw escapes Compose's
                // onClick boundary — Crashlytics only hooks the platform
                // UncaughtExceptionHandler, which Compose would otherwise
                // shield us from.
                Handler(Looper.getMainLooper()).post {
                    throw RuntimeException("ADR-010 test crash")
                }
            },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = DesignSystemSpacing.large),
        ) {
            Text(stringResource(R.string.debug_panel_trigger_crash))
        }
    }
}
