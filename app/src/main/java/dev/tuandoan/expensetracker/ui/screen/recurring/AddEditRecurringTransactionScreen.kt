package dev.tuandoan.expensetracker.ui.screen.recurring

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.tuandoan.expensetracker.R
import dev.tuandoan.expensetracker.core.formatter.AmountFormatter
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
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.add_recurring)) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier =
                            Modifier.semantics {
                                contentDescription = "Go back"
                            },
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier,
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
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
                    text = "Amount",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                OutlinedTextField(
                    value = uiState.amountText,
                    onValueChange = { input ->
                        val cleanInput = input.replace("[^0-9]".toRegex(), "")
                        if (cleanInput.isNotEmpty()) {
                            val formattedInput =
                                AmountFormatter.formatAmount(
                                    cleanInput.toLongOrNull() ?: 0L,
                                    currency.code,
                                )
                            viewModel.onAmountChanged(formattedInput)
                        } else {
                            viewModel.onAmountChanged("")
                        }
                    },
                    label = { Text("Enter amount in ${currency.code}") },
                    placeholder = { Text(amountPlaceholder) },
                    suffix = {
                        Text(
                            currency.symbol,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
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
                    text = "Note (optional)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = uiState.note,
                    onValueChange = viewModel::onNoteChanged,
                    placeholder = { Text("Add details...") },
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

            Spacer(modifier = Modifier.padding(DesignSystemSpacing.medium))

            // Save Button
            val hapticFeedback = LocalHapticFeedback.current
            Button(
                onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.save(onNavigateBack)
                },
                enabled = uiState.isFormValid && !uiState.isSaving,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription =
                                if (uiState.isFormValid && !uiState.isSaving) {
                                    "Save recurring transaction"
                                } else {
                                    "Complete form to enable save"
                                }
                        },
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = ButtonDefaults.buttonColors().contentColor,
                    )
                    Text(
                        text = "Saving...",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(start = DesignSystemSpacing.small),
                    )
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
            text = "Transaction Type",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(DesignSystemSpacing.small)) {
            FilterChip(
                selected = selectedType == TransactionType.EXPENSE,
                onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    onTypeChanged(TransactionType.EXPENSE)
                },
                label = { Text("Expense") },
                modifier =
                    Modifier.semantics {
                        contentDescription =
                            if (selectedType == TransactionType.EXPENSE) {
                                "Expense type selected"
                            } else {
                                "Select expense type"
                            }
                    },
            )
            FilterChip(
                selected = selectedType == TransactionType.INCOME,
                onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    onTypeChanged(TransactionType.INCOME)
                },
                label = { Text("Income") },
                modifier =
                    Modifier.semantics {
                        contentDescription =
                            if (selectedType == TransactionType.INCOME) {
                                "Income type selected"
                            } else {
                                "Select income type"
                            }
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
            text = "Currency",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        val hapticFeedback = LocalHapticFeedback.current
        Card(
            onClick = {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                expanded = true
            },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = "Currency: $displayText, tap to change"
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
                    contentDescription = "Open currency selection",
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
            text = "Category",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        val hapticFeedback = LocalHapticFeedback.current
        Card(
            onClick = {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                expanded = true
            },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription =
                            if (selectedCategory != null) {
                                "${selectedCategory.name} selected, tap to change"
                            } else {
                                "Select category"
                            }
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
                    text = selectedCategory?.name ?: "Select Category",
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
                    contentDescription = "Open category selection",
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
        Card(
            onClick = {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                expanded = true
            },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription =
                            "Frequency: ${frequencyLabels[selectedFrequency]}, tap to change"
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
                    contentDescription = "Open frequency selection",
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
        Card(
            onClick = {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                showDatePicker = true
            },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription =
                            "Start date: ${DateTimeUtil.formatTimestamp(timestamp)}, tap to change"
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
                        text = "Tap to select start date",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    Icons.Default.CalendarToday,
                    contentDescription = "Select start date",
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
                    Text("OK")
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
