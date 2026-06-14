package dev.tuandoan.expensetracker.ui.screen.trips

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.tuandoan.expensetracker.R
import dev.tuandoan.expensetracker.domain.model.ConversionDraft
import dev.tuandoan.expensetracker.ui.theme.DesignSystemSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversionWizardScreen(
    onNavigateBack: () -> Unit,
    viewModel: ConversionWizardViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.done) {
        if (uiState.done) onNavigateBack()
    }

    BackHandler { viewModel.onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text =
                            when (uiState.step) {
                                WizardStep.Loading -> ""
                                WizardStep.Metadata -> stringResource(R.string.conversion_wizard_step1_title)
                                WizardStep.RowDecisions -> stringResource(R.string.conversion_wizard_step2_title)
                                WizardStep.Preview -> stringResource(R.string.conversion_wizard_step3_title)
                            },
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                uiState.step == WizardStep.Metadata -> {
                    WizardStepScaffold(
                        content = { WizardStep1MetadataBody(uiState = uiState, viewModel = viewModel) },
                        nextLabel = stringResource(R.string.conversion_wizard_next),
                        onNext = viewModel::onNext,
                    )
                }
                uiState.step == WizardStep.RowDecisions -> {
                    WizardStepScaffold(
                        content = { WizardStep2RowDecisionsBody(uiState = uiState, viewModel = viewModel) },
                        nextLabel = stringResource(R.string.conversion_wizard_next),
                        onNext = viewModel::onNext,
                    )
                }
                uiState.step == WizardStep.Preview -> {
                    WizardStepScaffold(
                        content = { WizardStep3PreviewBody(uiState = uiState) },
                        nextLabel = stringResource(R.string.conversion_wizard_commit),
                        onNext = viewModel::onNext,
                    )
                }
            }
        }
    }
}

@Composable
private fun WizardStepScaffold(
    content: @Composable () -> Unit,
    nextLabel: String,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(DesignSystemSpacing.large),
    ) {
        Box(modifier = Modifier.weight(1f)) {
            content()
        }
        Spacer(modifier = Modifier.height(DesignSystemSpacing.large))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            Button(onClick = onNext) {
                Text(nextLabel)
            }
        }
    }
}

// ── Step 1 — Trip metadata ───────────────────────────────────────────────────

@Composable
private fun WizardStep1MetadataBody(
    uiState: ConversionWizardUiState,
    viewModel: ConversionWizardViewModel,
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val nameFocusRequester = remember { FocusRequester() }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) { nameFocusRequester.requestFocus() }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(DesignSystemSpacing.large),
    ) {
        Text(
            text = stringResource(R.string.conversion_wizard_step1_subtitle, uiState.sourceCategoryName),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        TripNameField(
            value = uiState.tripName,
            error = uiState.nameError?.asString(context),
            onChange = viewModel::onTripNameChange,
            focusRequester = nameFocusRequester,
            onImeNext = { focusManager.clearFocus() },
        )

        TripDestinationField(
            value = uiState.tripDestination,
            onChange = viewModel::onTripDestinationChange,
            onImeNext = { focusManager.clearFocus() },
        )

        TripDateRangeRow(
            startEpochDay = uiState.startEpochDay,
            endEpochDay = uiState.endEpochDay,
            error = uiState.dateError?.asString(context),
            onClick = { showDatePicker = true },
        )

        TripForeignCurrencyToggle(
            enabled = uiState.isForeignCurrency,
            onChange = viewModel::onForeignCurrencyToggle,
        )

        TripForeignCurrencySection(
            isForeignCurrency = uiState.isForeignCurrency,
            foreignCurrencyCode = uiState.foreignCurrencyCode,
            homeCurrencyCode = uiState.homeCurrencyCode,
            rateText = uiState.rateText,
            rateError = uiState.rateError?.asString(context),
            onCurrencyChange = viewModel::onForeignCurrencyChange,
            onRateChange = viewModel::onRateTextChange,
            onImeDone = { focusManager.clearFocus() },
        )
    }

    if (showDatePicker) {
        TripDateRangeSheet(
            initialStartEpochDay = uiState.startEpochDay,
            initialEndEpochDay = uiState.endEpochDay,
            onConfirm = { start, end ->
                viewModel.onDatesSelected(start, end)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false },
        )
    }
}

// ── Step 2 — Row decisions (stub for T4.4) ───────────────────────────────────

@Composable
private fun WizardStep2RowDecisionsBody(
    uiState: ConversionWizardUiState,
    viewModel: ConversionWizardViewModel,
) {
    Column(verticalArrangement = Arrangement.spacedBy(DesignSystemSpacing.medium)) {
        Text(
            text =
                stringResource(
                    R.string.conversion_wizard_step2_subtitle,
                    uiState.transactions.size,
                ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.conversion_wizard_step2_placeholder),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ── Step 3 — Preview (stub for T4.5) ─────────────────────────────────────────

@Composable
private fun WizardStep3PreviewBody(uiState: ConversionWizardUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(DesignSystemSpacing.medium)) {
        Text(
            text =
                stringResource(
                    R.string.conversion_wizard_step3_subtitle,
                    uiState.migratedCount,
                    uiState.skippedCount,
                ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text =
                if (uiState.sourceDisposition == ConversionDraft.SourceDisposition.DELETE) {
                    stringResource(R.string.conversion_wizard_source_will_be_deleted)
                } else {
                    stringResource(R.string.conversion_wizard_source_will_be_kept)
                },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
