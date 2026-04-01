package dev.tuandoan.expensetracker.ui.screen.gold

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
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.tuandoan.expensetracker.R
import dev.tuandoan.expensetracker.core.formatter.AmountFormatter
import dev.tuandoan.expensetracker.core.formatter.CurrencyAmountVisualTransformation
import dev.tuandoan.expensetracker.core.util.DateTimeUtil
import dev.tuandoan.expensetracker.domain.model.GoldType
import dev.tuandoan.expensetracker.domain.model.GoldWeightUnit
import dev.tuandoan.expensetracker.ui.theme.DesignSystemElevation
import dev.tuandoan.expensetracker.ui.theme.DesignSystemSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditGoldHoldingScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddEditGoldHoldingViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val isEditMode = viewModel.isEditMode
    var showDiscardDialog by remember { mutableStateOf(false) }

    val handleBack: () -> Unit = {
        if (uiState.hasUnsavedChanges) {
            showDiscardDialog = true
        } else {
            onNavigateBack()
        }
    }

    BackHandler { handleBack() }

    val context = LocalContext.current

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message.asString(context))
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isEditMode) {
                            stringResource(R.string.gold_edit_title)
                        } else {
                            stringResource(R.string.gold_add_title)
                        },
                    )
                },
                navigationIcon = {
                    val hapticFeedback = LocalHapticFeedback.current
                    IconButton(
                        onClick = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            handleBack()
                        },
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.a11y_navigate_back),
                        )
                    }
                },
            )
        },
        bottomBar = {
            if (!uiState.isLoading) {
                GoldSaveBottomBar(
                    uiState = uiState,
                    isEditMode = isEditMode,
                    onSave = { viewModel.saveHolding(onNavigateBack) },
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier,
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                val loadingDesc = stringResource(R.string.a11y_loading_gold_holding)
                CircularProgressIndicator(
                    modifier =
                        Modifier.semantics {
                            contentDescription = loadingDesc
                        },
                )
            }
        } else {
            GoldHoldingForm(
                uiState = uiState,
                isEditMode = isEditMode,
                viewModel = viewModel,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(innerPadding)
                        .padding(
                            horizontal = DesignSystemSpacing.screenPadding,
                            vertical = DesignSystemSpacing.small,
                        ).verticalScroll(rememberScrollState()),
            )
        }
    }

    if (showDiscardDialog) {
        val hapticFeedback = LocalHapticFeedback.current
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text(stringResource(R.string.gold_discard_title)) },
            text = { Text(stringResource(R.string.gold_discard_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        showDiscardDialog = false
                        onNavigateBack()
                    },
                ) {
                    Text(stringResource(R.string.gold_discard))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        showDiscardDialog = false
                    },
                ) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoldHoldingForm(
    uiState: AddEditGoldHoldingUiState,
    isEditMode: Boolean,
    viewModel: AddEditGoldHoldingViewModel,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val weightFocusRequester = remember { FocusRequester() }

    // Auto-focus weight field in add mode
    LaunchedEffect(Unit) {
        if (!isEditMode) {
            weightFocusRequester.requestFocus()
        }
    }

    val weight = uiState.weightText.toDoubleOrNull()
    val isWeightInvalid = uiState.weightText.isNotBlank() && (weight == null || weight <= 0)
    val buyPrice = AmountFormatter.parseAmount(uiState.buyPriceText)
    val isPriceInvalid = uiState.buyPriceText.isNotBlank() && (buyPrice == null || buyPrice <= 0)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(DesignSystemSpacing.large),
    ) {
        // Weight + Unit (primary field — first for fastest input)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(DesignSystemSpacing.medium),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(DesignSystemSpacing.xs),
            ) {
                Text(
                    text = stringResource(R.string.gold_weight_label),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                )
                OutlinedTextField(
                    value = uiState.weightText,
                    onValueChange = viewModel::onWeightChanged,
                    placeholder = { Text(stringResource(R.string.gold_weight_placeholder)) },
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next,
                        ),
                    keyboardActions =
                        KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) },
                        ),
                    singleLine = true,
                    isError = isWeightInvalid,
                    supportingText =
                        if (isWeightInvalid) {
                            {
                                Text(
                                    stringResource(R.string.error_invalid_weight),
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        } else {
                            null
                        },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .focusRequester(weightFocusRequester),
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(DesignSystemSpacing.xs),
            ) {
                Text(
                    text = stringResource(R.string.gold_weight_unit_label),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                )
                GoldUnitSelector(
                    selectedUnit = uiState.weightUnit,
                    onUnitChanged = viewModel::onWeightUnitChanged,
                )
            }
        }

        // Buy Price
        Column(verticalArrangement = Arrangement.spacedBy(DesignSystemSpacing.xs)) {
            Text(
                text = stringResource(R.string.gold_buy_price_label),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
            )
            OutlinedTextField(
                value = uiState.buyPriceText,
                onValueChange = { input ->
                    val cleanInput = input.replace("[^0-9]".toRegex(), "")
                    viewModel.onBuyPriceChanged(cleanInput)
                },
                label = { Text("${uiState.currencyCode} / ${goldUnitLabel(uiState.weightUnit)}") },
                visualTransformation =
                    remember(uiState.currencyCode) {
                        CurrencyAmountVisualTransformation(uiState.currencyCode)
                    },
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next,
                    ),
                keyboardActions =
                    KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) },
                    ),
                singleLine = true,
                isError = isPriceInvalid,
                supportingText =
                    if (isPriceInvalid) {
                        {
                            Text(
                                stringResource(R.string.error_invalid_buy_price),
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    } else {
                        null
                    },
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.headlineSmall,
            )
        }

        // Gold Type
        GoldTypeSelector(
            selectedType = uiState.type,
            onTypeChanged = viewModel::onTypeChanged,
        )

        // Buy Date
        GoldDateSelector(
            timestamp = uiState.buyDateMillis,
            onDateSelected = viewModel::onDateSelected,
        )

        // Note
        Column(verticalArrangement = Arrangement.spacedBy(DesignSystemSpacing.xs)) {
            Text(
                text = stringResource(R.string.gold_note_label),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = uiState.note,
                onValueChange = viewModel::onNoteChanged,
                placeholder = { Text(stringResource(R.string.gold_note_placeholder)) },
                keyboardOptions =
                    KeyboardOptions(
                        imeAction = ImeAction.Done,
                    ),
                keyboardActions =
                    KeyboardActions(
                        onDone = { focusManager.clearFocus() },
                    ),
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
            )
        }
    }
}

@Composable
private fun GoldTypeSelector(
    selectedType: GoldType,
    onTypeChanged: (GoldType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hapticFeedback = LocalHapticFeedback.current

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(DesignSystemSpacing.xs),
    ) {
        Text(
            text = stringResource(R.string.gold_type_label),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(DesignSystemSpacing.small)) {
            GoldType.entries.forEach { type ->
                FilterChip(
                    selected = selectedType == type,
                    onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        onTypeChanged(type)
                    },
                    label = { Text(goldTypeLabel(type)) },
                )
            }
        }
    }
}

@Composable
private fun GoldUnitSelector(
    selectedUnit: GoldWeightUnit,
    onUnitChanged: (GoldWeightUnit) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hapticFeedback = LocalHapticFeedback.current

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(DesignSystemSpacing.xs),
    ) {
        GoldWeightUnit.entries.forEach { unit ->
            FilterChip(
                selected = selectedUnit == unit,
                onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    onUnitChanged(unit)
                },
                label = { Text(goldUnitLabel(unit)) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoldDateSelector(
    timestamp: Long,
    onDateSelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val hapticFeedback = LocalHapticFeedback.current

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(DesignSystemSpacing.xs),
    ) {
        Text(
            text = stringResource(R.string.gold_buy_date_label),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
        )

        Card(
            onClick = {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                showDatePicker = true
            },
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = DesignSystemElevation.low),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(DesignSystemSpacing.large),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = DateTimeUtil.formatTimestamp(timestamp),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
                Icon(
                    Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }

    if (showDatePicker) {
        val datePickerState =
            rememberDatePickerState(
                initialSelectedDateMillis = timestamp,
            )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        datePickerState.selectedDateMillis?.let { onDateSelected(it) }
                        showDatePicker = false
                    },
                ) {
                    Text(stringResource(R.string.action_ok))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        showDatePicker = false
                    },
                ) {
                    Text(stringResource(R.string.cancel))
                }
            },
        ) {
            DatePicker(
                state = datePickerState,
                title = {
                    Text(
                        text = stringResource(R.string.gold_buy_date_label),
                        modifier = Modifier.padding(16.dp),
                    )
                },
            )
        }
    }
}

@Composable
private fun GoldSaveBottomBar(
    uiState: AddEditGoldHoldingUiState,
    isEditMode: Boolean,
    onSave: () -> Unit,
) {
    val hapticFeedback = LocalHapticFeedback.current
    Surface(tonalElevation = 3.dp) {
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
                onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSave()
                },
                enabled = uiState.isSaveEnabled && !uiState.isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (uiState.isSaving) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = ButtonDefaults.buttonColors().contentColor,
                        )
                        Text(
                            text =
                                if (isEditMode) {
                                    stringResource(R.string.gold_updating)
                                } else {
                                    stringResource(R.string.gold_saving)
                                },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(start = DesignSystemSpacing.small),
                        )
                    }
                } else {
                    Text(
                        text =
                            if (isEditMode) {
                                stringResource(R.string.gold_update_holding)
                            } else {
                                stringResource(R.string.gold_save_holding)
                            },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}
