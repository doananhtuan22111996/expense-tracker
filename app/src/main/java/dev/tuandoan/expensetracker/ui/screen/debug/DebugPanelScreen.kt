package dev.tuandoan.expensetracker.ui.screen.debug

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
 * The "Trigger test crash" button throws a [RuntimeException] on the main
 * thread. The default JVM `UncaughtExceptionHandler` catches it and forwards
 * to the bound [dev.tuandoan.expensetracker.domain.crash.CrashReporter]:
 * release builds → Firebase Crashlytics; debug builds → NoOp (crash still
 * happens, no data leaves the device).
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
            onClick = { throw RuntimeException("ADR-010 test crash") },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = DesignSystemSpacing.large),
        ) {
            Text(stringResource(R.string.debug_panel_trigger_crash))
        }
    }
}
