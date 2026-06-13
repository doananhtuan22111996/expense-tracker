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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.tuandoan.expensetracker.R
import dev.tuandoan.expensetracker.core.util.EpochDayConverters
import dev.tuandoan.expensetracker.ui.component.CurrencyDropdown
import dev.tuandoan.expensetracker.ui.theme.DesignSystemElevation
import dev.tuandoan.expensetracker.ui.theme.DesignSystemSpacing
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

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

                NameField(
                    value = uiState.name,
                    error = uiState.nameError?.asString(context),
                    onChange = viewModel::onNameChange,
                    focusRequester = nameFocusRequester,
                    onImeNext = { focusManager.moveFocus(FocusDirection.Down) },
                )

                DestinationField(
                    value = uiState.destination,
                    onChange = viewModel::onDestinationChange,
                    onImeNext = { focusManager.moveFocus(FocusDirection.Down) },
                )

                DateRangeRow(
                    startEpochDay = uiState.startEpochDay,
                    endEpochDay = uiState.endEpochDay,
                    error = uiState.dateError?.asString(context),
                    onClick = { showDatePicker = true },
                )

                ForeignCurrencyToggle(
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
                    RateField(
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
private fun NameField(
    value: String,
    error: String?,
    onChange: (String) -> Unit,
    focusRequester: FocusRequester,
    onImeNext: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(DesignSystemSpacing.xs)) {
        Text(
            text = stringResource(R.string.create_edit_trip_label_name),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            placeholder = { Text(stringResource(R.string.create_edit_trip_hint_name)) },
            singleLine = true,
            isError = error != null,
            supportingText = error?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
            keyboardOptions =
                KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next,
                ),
            keyboardActions = KeyboardActions(onNext = { onImeNext() }),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
        )
    }
}

@Composable
private fun DestinationField(
    value: String,
    onChange: (String) -> Unit,
    onImeNext: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(DesignSystemSpacing.xs)) {
        Text(
            text = stringResource(R.string.create_edit_trip_label_destination),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            placeholder = { Text(stringResource(R.string.create_edit_trip_hint_destination)) },
            singleLine = true,
            keyboardOptions =
                KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next,
                ),
            keyboardActions = KeyboardActions(onNext = { onImeNext() }),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun DateRangeRow(
    startEpochDay: Long?,
    endEpochDay: Long?,
    error: String?,
    onClick: () -> Unit,
) {
    val rangeText =
        if (startEpochDay != null && endEpochDay != null) {
            formatDateRange(startEpochDay, endEpochDay)
        } else if (startEpochDay != null) {
            stringResource(R.string.create_edit_trip_dates_partial, formatDate(startEpochDay))
        } else {
            stringResource(R.string.create_edit_trip_dates_unset)
        }
    val rowDesc = stringResource(R.string.a11y_create_edit_trip_dates, rangeText)

    Column(verticalArrangement = Arrangement.spacedBy(DesignSystemSpacing.xs)) {
        Text(
            text = stringResource(R.string.create_edit_trip_label_dates),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Card(
            onClick = onClick,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = rowDesc },
            elevation = CardDefaults.cardElevation(defaultElevation = DesignSystemElevation.low),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(DesignSystemSpacing.large),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DesignSystemSpacing.medium),
            ) {
                Icon(
                    Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = rangeText,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        if (error != null) {
            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ForeignCurrencyToggle(
    enabled: Boolean,
    onChange: (Boolean) -> Unit,
    toggleEnabled: Boolean = true,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.create_edit_trip_label_foreign_toggle),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.create_edit_trip_foreign_toggle_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = enabled, onCheckedChange = onChange, enabled = toggleEnabled)
    }
}

@Composable
private fun RateField(
    value: String,
    homeCode: String,
    foreignCode: String,
    error: String?,
    onChange: (String) -> Unit,
    onImeDone: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(DesignSystemSpacing.xs)) {
        Text(
            text = stringResource(R.string.create_edit_trip_label_rate),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        OutlinedTextField(
            value = value,
            onValueChange = { input ->
                // Allow digits, comma and dot only.
                val cleaned = input.replace("[^0-9.,]".toRegex(), "")
                onChange(cleaned)
            },
            placeholder = { Text(stringResource(R.string.create_edit_trip_hint_rate)) },
            singleLine = true,
            isError = error != null,
            supportingText = {
                if (error != null) {
                    Text(error, color = MaterialTheme.colorScheme.error)
                } else if (foreignCode.isNotBlank()) {
                    Text(
                        stringResource(R.string.create_edit_trip_rate_helper, foreignCode, homeCode),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            keyboardOptions =
                KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done,
                ),
            keyboardActions = KeyboardActions(onDone = { onImeDone() }),
            modifier = Modifier.fillMaxWidth(),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TripDateRangeSheet(
    initialStartEpochDay: Long?,
    initialEndEpochDay: Long?,
    onConfirm: (startEpochDay: Long, endEpochDay: Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val initialStartMillis = initialStartEpochDay?.let { EpochDayConverters.epochDayToUtcMillis(it) }
    val initialEndMillis = initialEndEpochDay?.let { EpochDayConverters.epochDayToUtcMillis(it) }
    val state =
        rememberDateRangePickerState(
            initialSelectedStartDateMillis = initialStartMillis,
            initialSelectedEndDateMillis = initialEndMillis,
        )
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = DesignSystemSpacing.xl),
        ) {
            DateRangePicker(
                state = state,
                title = {
                    Text(
                        text = stringResource(R.string.create_edit_trip_dates_picker_title),
                        style = MaterialTheme.typography.titleLarge,
                        modifier =
                            Modifier.padding(
                                start = DesignSystemSpacing.xl,
                                top = DesignSystemSpacing.large,
                            ),
                    )
                },
                modifier = Modifier.weight(1f, fill = false),
            )
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = DesignSystemSpacing.large),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
                val startMs = state.selectedStartDateMillis
                val endMs = state.selectedEndDateMillis
                TextButton(
                    onClick = {
                        if (startMs != null && endMs != null) {
                            onConfirm(
                                EpochDayConverters.utcMillisToEpochDay(startMs),
                                EpochDayConverters.utcMillisToEpochDay(endMs),
                            )
                        }
                    },
                    enabled = startMs != null && endMs != null,
                ) {
                    Text(stringResource(R.string.save))
                }
            }
        }
    }
}

private val TRIP_DATE_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

private fun formatDate(epochDay: Long): String = LocalDate.ofEpochDay(epochDay).format(TRIP_DATE_FORMATTER)

private fun formatDateRange(
    startEpochDay: Long,
    endEpochDay: Long,
): String = "${formatDate(startEpochDay)} – ${formatDate(endEpochDay)}"
