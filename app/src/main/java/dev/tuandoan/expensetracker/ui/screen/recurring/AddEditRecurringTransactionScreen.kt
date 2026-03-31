package dev.tuandoan.expensetracker.ui.screen.recurring

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import dev.tuandoan.expensetracker.domain.model.Category
import dev.tuandoan.expensetracker.domain.model.RecurrenceFrequency
import dev.tuandoan.expensetracker.domain.model.SupportedCurrencies
import dev.tuandoan.expensetracker.domain.model.TransactionType
import dev.tuandoan.expensetracker.ui.theme.DesignSystemElevation
import dev.tuandoan.expensetracker.ui.theme.DesignSystemSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditRecurringTransactionScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddEditRecurringTransactionViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current
    val isEditMode = viewModel.isEditMode
    var showDiscardDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val handleBack: () -> Unit = {
        if (uiState.hasUnsavedChanges) {
            showDiscardDialog = true
        } else {
            onNavigateBack()
        }
    }

    BackHandler { handleBack() }

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
                            stringResource(R.string.edit_recurring)
                        } else {
                            stringResource(R.string.add_recurring)
                        },
                    )
                },
                navigationIcon = {
                    val hapticBack = LocalHapticFeedback.current
                    val goBackDesc = stringResource(R.string.a11y_go_back)
                    IconButton(
                        onClick = {
                            hapticBack.performHapticFeedback(HapticFeedbackType.LongPress)
                            handleBack()
                        },
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
            RecurringSaveBottomBar(
                uiState = uiState,
                onSave = { viewModel.save(onNavigateBack) },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier,
    ) { innerPadding ->
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
            // Transaction Type
            TransactionTypeSelector(
                selectedType = uiState.type,
                onTypeChanged = viewModel::onTypeChanged,
            )

            // Currency Selection
            CurrencyDropdown(
                selectedCurrencyCode = uiState.currencyCode,
                onCurrencySelected = viewModel::onCurrencyChanged,
            )

            // Amount
            val currency =
                SupportedCurrencies.byCode(uiState.currencyCode) ?: SupportedCurrencies.default()
            val amountPlaceholder =
                if (currency.minorUnitDigits == 0) {
                    AmountFormatter.formatAmount(1000000L, currency.code)
                } else {
                    AmountFormatter.formatAmount(100000L, currency.code)
                }

            Column(verticalArrangement = Arrangement.spacedBy(DesignSystemSpacing.xs)) {
                Text(
                    text = stringResource(R.string.label_amount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                OutlinedTextField(
                    value = uiState.amountText,
                    onValueChange = { input ->
                        val cleanInput = input.replace("[^0-9]".toRegex(), "")
                        viewModel.onAmountChanged(cleanInput)
                    },
                    label = { Text(stringResource(R.string.label_enter_amount, currency.code)) },
                    placeholder = { Text(amountPlaceholder) },
                    suffix = {
                        Text(
                            currency.symbol,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    },
                    visualTransformation =
                        remember(currency.code) {
                            CurrencyAmountVisualTransformation(currency.code)
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
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.headlineSmall,
                )
            }

            // Category Selection
            CategoryDropdown(
                categories = uiState.categories,
                selectedCategory = uiState.selectedCategory,
                onCategorySelected = viewModel::onCategorySelected,
            )

            // Frequency Selection
            FrequencyDropdown(
                selectedFrequency = uiState.frequency,
                onFrequencySelected = viewModel::onFrequencyChanged,
            )

            // Start Date
            StartDateSelector(
                timestamp = uiState.startDate,
                onDateSelected = viewModel::onStartDateSelected,
            )

            // Note
            Column(verticalArrangement = Arrangement.spacedBy(DesignSystemSpacing.xs)) {
                Text(
                    text = stringResource(R.string.label_note_optional),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = uiState.note,
                    onValueChange = viewModel::onNoteChanged,
                    placeholder = { Text(stringResource(R.string.hint_add_details_short)) },
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

    if (showDiscardDialog) {
        val hapticDiscard = LocalHapticFeedback.current
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text(stringResource(R.string.recurring_discard_title)) },
            text = { Text(stringResource(R.string.recurring_discard_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        hapticDiscard.performHapticFeedback(HapticFeedbackType.LongPress)
                        showDiscardDialog = false
                        onNavigateBack()
                    },
                ) {
                    Text(stringResource(R.string.recurring_discard))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        hapticDiscard.performHapticFeedback(HapticFeedbackType.LongPress)
                        showDiscardDialog = false
                    },
                ) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun TransactionTypeSelector(
    selectedType: TransactionType,
    onTypeChanged: (TransactionType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hapticFeedback = LocalHapticFeedback.current
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(DesignSystemSpacing.xs),
    ) {
        Text(
            text = stringResource(R.string.label_transaction_type),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(DesignSystemSpacing.small)) {
            val expenseDesc =
                if (selectedType == TransactionType.EXPENSE) {
                    stringResource(R.string.a11y_expense_type_selected)
                } else {
                    stringResource(R.string.a11y_select_expense_type_short)
                }
            FilterChip(
                selected = selectedType == TransactionType.EXPENSE,
                onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    onTypeChanged(TransactionType.EXPENSE)
                },
                label = { Text(stringResource(R.string.type_expense)) },
                modifier =
                    Modifier.semantics {
                        contentDescription = expenseDesc
                    },
            )
            val incomeDesc =
                if (selectedType == TransactionType.INCOME) {
                    stringResource(R.string.a11y_income_type_selected)
                } else {
                    stringResource(R.string.a11y_select_income_type_short)
                }
            FilterChip(
                selected = selectedType == TransactionType.INCOME,
                onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    onTypeChanged(TransactionType.INCOME)
                },
                label = { Text(stringResource(R.string.type_income)) },
                modifier =
                    Modifier.semantics {
                        contentDescription = incomeDesc
                    },
            )
        }
    }
}

@Composable
private fun CurrencyDropdown(
    selectedCurrencyCode: String,
    onCurrencySelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val allCurrencies = remember { SupportedCurrencies.all() }
    val selectedCurrency =
        remember(selectedCurrencyCode) {
            SupportedCurrencies.byCode(selectedCurrencyCode) ?: SupportedCurrencies.default()
        }
    val displayText =
        "${selectedCurrency.code} - ${selectedCurrency.displayName} ${selectedCurrency.symbol}"

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(DesignSystemSpacing.xs),
    ) {
        Text(
            text = stringResource(R.string.label_currency),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        val hapticFeedback = LocalHapticFeedback.current
        val currencyDesc = stringResource(R.string.a11y_currency_tap_to_change, displayText)
        Card(
            onClick = {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                expanded = true
            },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = currencyDesc
                    },
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
                    text = displayText,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = stringResource(R.string.a11y_open_currency_selection),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            allCurrencies.forEach { currency ->
                val isSelected = currency.code == selectedCurrencyCode
                DropdownMenuItem(
                    text = {
                        Text(
                            "${currency.code} - ${currency.displayName} ${currency.symbol}",
                            color =
                                if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                        )
                    },
                    trailingIcon = {
                        if (isSelected) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    },
                    onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        onCurrencySelected(currency.code)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun CategoryDropdown(
    categories: List<Category>,
    selectedCategory: Category?,
    onCategorySelected: (Category) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(DesignSystemSpacing.xs),
    ) {
        Text(
            text = stringResource(R.string.label_category),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        val hapticFeedback = LocalHapticFeedback.current
        val categoryDesc =
            if (selectedCategory != null) {
                stringResource(R.string.a11y_selected_tap_to_change, selectedCategory.name)
            } else {
                stringResource(R.string.a11y_select_category_generic)
            }
        Card(
            onClick = {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                expanded = true
            },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = categoryDesc
                    },
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
                    text = selectedCategory?.name ?: stringResource(R.string.select_category),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight =
                        if (selectedCategory != null) FontWeight.Medium else FontWeight.Normal,
                    color =
                        if (selectedCategory == null) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                )
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = stringResource(R.string.a11y_open_category_selection),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.name) },
                    onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        onCategorySelected(category)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun FrequencyDropdown(
    selectedFrequency: RecurrenceFrequency,
    onFrequencySelected: (RecurrenceFrequency) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val frequencies = RecurrenceFrequency.entries

    val frequencyLabels =
        mapOf(
            RecurrenceFrequency.DAILY to stringResource(R.string.frequency_daily),
            RecurrenceFrequency.WEEKLY to stringResource(R.string.frequency_weekly),
            RecurrenceFrequency.MONTHLY to stringResource(R.string.frequency_monthly),
            RecurrenceFrequency.YEARLY to stringResource(R.string.frequency_yearly),
        )

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(DesignSystemSpacing.xs),
    ) {
        Text(
            text = stringResource(R.string.frequency),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        val hapticFeedback = LocalHapticFeedback.current
        val frequencyDesc =
            stringResource(
                R.string.a11y_frequency_tap_to_change,
                frequencyLabels[selectedFrequency] ?: "",
            )
        Card(
            onClick = {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                expanded = true
            },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = frequencyDesc
                    },
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
                    text = frequencyLabels[selectedFrequency] ?: "",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = stringResource(R.string.a11y_open_frequency_selection),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            frequencies.forEach { frequency ->
                val isSelected = frequency == selectedFrequency
                DropdownMenuItem(
                    text = {
                        Text(
                            frequencyLabels[frequency] ?: "",
                            color =
                                if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                        )
                    },
                    trailingIcon = {
                        if (isSelected) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    },
                    onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        onFrequencySelected(frequency)
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StartDateSelector(
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
            text = stringResource(R.string.start_date),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        val startDateDesc =
            stringResource(R.string.a11y_start_date_tap_to_change, DateTimeUtil.formatTimestamp(timestamp))
        Card(
            onClick = {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                showDatePicker = true
            },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = startDateDesc
                    },
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
                Column {
                    Text(
                        text = DateTimeUtil.formatTimestamp(timestamp),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.hint_tap_to_select_start_date),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    Icons.Default.CalendarToday,
                    contentDescription = stringResource(R.string.a11y_select_start_date),
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
                        text = stringResource(R.string.start_date),
                        modifier = Modifier.padding(16.dp),
                    )
                },
            )
        }
    }
}

@Composable
private fun RecurringSaveBottomBar(
    uiState: AddEditRecurringUiState,
    onSave: () -> Unit,
) {
    val hapticFeedback = LocalHapticFeedback.current
    val saveButtonDescription =
        if (uiState.isFormValid && !uiState.isSaving) {
            stringResource(R.string.a11y_save_recurring)
        } else {
            stringResource(R.string.a11y_complete_form_recurring)
        }
    Surface(tonalElevation = 3.dp) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .imePadding()
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
                enabled = uiState.isFormValid && !uiState.isSaving,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = saveButtonDescription
                        },
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
                            text = stringResource(R.string.saving),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(start = DesignSystemSpacing.small),
                        )
                    }
                } else {
                    Text(
                        text = stringResource(R.string.save),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}
