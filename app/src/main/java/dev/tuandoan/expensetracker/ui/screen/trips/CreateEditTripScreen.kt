package dev.tuandoan.expensetracker.ui.screen.trips

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.tuandoan.expensetracker.R
import dev.tuandoan.expensetracker.ui.component.CurrencyDropdown
import dev.tuandoan.expensetracker.ui.theme.DesignSystemElevation
import dev.tuandoan.expensetracker.ui.theme.DesignSystemSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEditTripScreen(
    onNavigateBack: () -> Unit,
    viewModel: CreateEditTripViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val isEditMode = viewModel.isEditMode
    // rememberSaveable so an open dialog / picker survives rotation —
    // rememberDateRangePickerState already preserves the picker's selection
    // internally; this just keeps the sheet visible across the config change.
    var showDiscardDialog by rememberSaveable { mutableStateOf(false) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    val handleBack: () -> Unit = {
        if (uiState.hasUnsavedChanges) {
            showDiscardDialog = true
        } else {
            onNavigateBack()
        }
    }

    BackHandler { handleBack() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg.asString(context))
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isEditMode) {
                            stringResource(R.string.create_edit_trip_title_edit)
                        } else {
                            stringResource(R.string.create_edit_trip_title_create)
                        },
                    )
                },
                navigationIcon = {
                    val goBackDesc = stringResource(R.string.a11y_go_back)
                    IconButton(
                        onClick = handleBack,
                        modifier =
                            Modifier.semantics {
                                contentDescription = goBackDesc
                            },
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
        bottomBar = {
            if (!uiState.isLoading) {
                SaveBottomBar(
                    uiState = uiState,
                    onSave = { viewModel.save(onNavigateBack) },
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier,
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            val nameFocusRequester = remember { FocusRequester() }
            LaunchedEffect(Unit) {
                if (!isEditMode) nameFocusRequester.requestFocus()
            }

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(innerPadding)
                        .padding(
                            horizontal = DesignSystemSpacing.screenPadding,
                            vertical = DesignSystemSpacing.small,
                        ).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(DesignSystemSpacing.large),
            ) {
                if (uiState.isConversionOrigin) {
                    ConversionOriginNotice()
                }

                TripNameField(
                    value = uiState.name,
                    error = uiState.nameError?.asString(context),
                    onChange = viewModel::onNameChange,
                    focusRequester = nameFocusRequester,
                    onImeNext = { focusManager.moveFocus(FocusDirection.Down) },
                )

                TripDestinationField(
                    value = uiState.destination,
                    onChange = viewModel::onDestinationChange,
                    onImeNext = { focusManager.moveFocus(FocusDirection.Down) },
                )

                TripDateRangeRow(
                    startEpochDay = uiState.startEpochDay,
                    endEpochDay = uiState.endEpochDay,
                    error = uiState.dateError?.asString(context),
                    onClick = { showDatePicker = true },
                )

                TripForeignCurrencyToggle(
                    enabled = uiState.isForeignCurrency,
                    onChange = viewModel::onToggleForeignCurrency,
                    toggleEnabled = !uiState.isFxCurrencyLocked,
                )

                if (uiState.isForeignCurrency) {
                    val homeMarkerSuffix =
                        " · ${stringResource(R.string.create_edit_trip_home_marker)}"
                    CurrencyDropdown(
                        selectedCurrencyCode = uiState.foreignCurrencyCode,
                        onCurrencySelected = viewModel::onForeignCurrencyChange,
                        titleRes = R.string.create_edit_trip_label_foreign_currency,
                        disabledCodes = setOf(uiState.homeCurrencyCode),
                        disabledMarkerSuffix = homeMarkerSuffix,
                        enabled = !uiState.isFxCurrencyLocked,
                    )
                    if (uiState.isFxCurrencyLocked) {
                        Text(
                            text = stringResource(R.string.create_edit_trip_fx_currency_locked),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TripRateField(
                        value = uiState.rateText,
                        homeCode = uiState.homeCurrencyCode,
                        foreignCode = uiState.foreignCurrencyCode,
                        error = uiState.rateError?.asString(context),
                        onChange = viewModel::onRateTextChange,
                        onImeDone = { focusManager.clearFocus() },
                    )
                }
            }
        }
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

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text(stringResource(R.string.recurring_discard_title)) },
            text = { Text(stringResource(R.string.recurring_discard_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                        onNavigateBack()
                    },
                ) {
                    Text(stringResource(R.string.recurring_discard))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun ConversionOriginNotice(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = DesignSystemElevation.low),
    ) {
        Text(
            text = stringResource(R.string.create_edit_trip_conversion_origin_notice),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(DesignSystemSpacing.large),
        )
    }
}

@Composable
private fun SaveBottomBar(
    uiState: CreateEditTripUiState,
    onSave: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = DesignSystemSpacing.screenPadding,
                    vertical = DesignSystemSpacing.small,
                ),
    ) {
        Button(
            onClick = onSave,
            enabled = uiState.isValid && !uiState.isSaving,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (uiState.isSaving) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = ButtonDefaults.buttonColors().contentColor,
                    )
                    Text(
                        text = stringResource(R.string.saving),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(start = DesignSystemSpacing.small),
                    )
                }
            } else {
                Text(
                    text = stringResource(R.string.save),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}
