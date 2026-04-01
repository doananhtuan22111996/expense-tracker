package dev.tuandoan.expensetracker.ui.screen.addedit

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
import dev.tuandoan.expensetracker.domain.model.Category
import dev.tuandoan.expensetracker.domain.model.SupportedCurrencies
import dev.tuandoan.expensetracker.domain.model.TransactionType
import dev.tuandoan.expensetracker.ui.theme.DesignSystemElevation
import dev.tuandoan.expensetracker.ui.theme.DesignSystemSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionScreen(
    transactionId: Long?,
    onNavigateBack: () -> Unit,
    viewModel: AddEditTransactionViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val isEditMode = transactionId != null

    // Handle system back button
    BackHandler {
        if (uiState.hasUnsavedChanges) {
            viewModel.onBackPressed()
        } else {
            onNavigateBack()
        }
    }

    // Handle back navigation from ViewModel
    LaunchedEffect(uiState.showDiscardDialog) {
        // Dialog state is managed in the UI state
    }

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
                            stringResource(
                                R.string.edit_transaction_title,
                            )
                        } else {
                            stringResource(R.string.add_transaction_title)
                        },
                    )
                },
                navigationIcon = {
                    val hapticFeedback = LocalHapticFeedback.current
                    val backDescription =
                        if (uiState.hasUnsavedChanges) {
                            stringResource(R.string.a11y_go_back_prompt_save)
                        } else {
                            stringResource(R.string.a11y_go_back_to_home)
                        }
                    IconButton(
                        onClick = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (uiState.hasUnsavedChanges) {
                                viewModel.onBackPressed()
                            } else {
                                onNavigateBack()
                            }
                        },
                        modifier =
                            Modifier.semantics {
                                contentDescription = backDescription
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
                    isEditMode = isEditMode,
                    onSave = { viewModel.saveTransaction(onNavigateBack) },
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
                val loadingDescription =
                    if (isEditMode) {
                        stringResource(
                            R.string.a11y_loading_details,
                        )
                    } else {
                        stringResource(R.string.a11y_loading_form)
                    }
                CircularProgressIndicator(
                    modifier =
                        Modifier.semantics {
                            contentDescription = loadingDescription
                        },
                )
            }
        } else {
            TransactionForm(
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

    // Discard Changes Confirmation Dialog
    if (uiState.showDiscardDialog) {
        val hapticFeedback = LocalHapticFeedback.current
        val discardDescription = stringResource(R.string.a11y_discard_and_go_back)
        val cancelEditingDescription = stringResource(R.string.a11y_cancel_continue_editing)
        AlertDialog(
            onDismissRequest = { viewModel.onCancelDiscard() },
            title = { Text(stringResource(R.string.discard_changes_title)) },
            text = { Text(stringResource(R.string.discard_changes_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.onDiscardChanges()
                        onNavigateBack()
                    },
                    modifier =
                        Modifier.semantics {
                            contentDescription = discardDescription
                        },
                ) {
                    Text(stringResource(R.string.discard))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.onCancelDiscard()
                    },
                    modifier =
                        Modifier.semantics {
                            contentDescription = cancelEditingDescription
                        },
                ) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun TransactionForm(
    uiState: AddEditTransactionUiState,
    isEditMode: Boolean,
    viewModel: AddEditTransactionViewModel,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val amountFocusRequester = remember { FocusRequester() }

    // Auto-focus amount field in add mode
    LaunchedEffect(Unit) {
        if (!isEditMode) {
            amountFocusRequester.requestFocus()
        }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(DesignSystemSpacing.large),
    ) {
        // Amount (primary field — first for fastest input)
        val currency = SupportedCurrencies.byCode(uiState.currencyCode) ?: SupportedCurrencies.default()
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
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .focusRequester(amountFocusRequester),
                isError = uiState.amountText.isNotBlank() && !uiState.isFormValid,
                supportingText = {
                    if (uiState.amountText.isNotBlank() && !uiState.isFormValid) {
                        Text(
                            stringResource(R.string.error_invalid_amount),
                            color = MaterialTheme.colorScheme.error,
                        )
                    } else if (uiState.amountText.isEmpty()) {
                        if (currency.minorUnitDigits == 0) {
                            Text(
                                stringResource(R.string.hint_amount_no_decimals, currency.code),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            Text(
                                stringResource(R.string.hint_amount_minor_units, currency.code),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                textStyle = MaterialTheme.typography.headlineSmall,
            )
        }

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

        // Category Selection
        EnhancedCategoryDropdown(
            categories = uiState.categories,
            selectedCategory = uiState.selectedCategory,
            onCategorySelected = viewModel::onCategorySelected,
        )

        // Date Selection
        EnhancedDateSelector(
            timestamp = uiState.timestamp,
            onDateSelected = viewModel::onDateSelected,
        )

        // Note - Optional field with lower visual priority
        Column(verticalArrangement = Arrangement.spacedBy(DesignSystemSpacing.xs)) {
            Text(
                text = stringResource(R.string.label_note_optional),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedTextField(
                value = uiState.note,
                onValueChange = viewModel::onNoteChanged,
                placeholder = { Text(stringResource(R.string.hint_add_details)) },
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
private fun SaveBottomBar(
    uiState: AddEditTransactionUiState,
    isEditMode: Boolean,
    onSave: () -> Unit,
) {
    val hapticFeedback = LocalHapticFeedback.current
    val saveButtonDescription =
        if (uiState.isSaveEnabled && !uiState.isLoading) {
            if (isEditMode) {
                stringResource(R.string.a11y_save_changes)
            } else {
                stringResource(R.string.a11y_save_new_transaction)
            }
        } else if (uiState.isLoading) {
            stringResource(R.string.a11y_saving_please_wait)
        } else {
            stringResource(R.string.a11y_complete_form_to_save)
        }
    Surface(
        tonalElevation = 3.dp,
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
                onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSave()
                },
                enabled = uiState.isSaveEnabled && !uiState.isLoading,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = saveButtonDescription
                        },
            ) {
                if (uiState.isLoading) {
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
                                if (isEditMode) stringResource(R.string.updating) else stringResource(R.string.saving),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(start = DesignSystemSpacing.small),
                        )
                    }
                } else {
                    Text(
                        text =
                            if (isEditMode) {
                                stringResource(R.string.update_transaction)
                            } else {
                                stringResource(R.string.save_transaction)
                            },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            // Form status hint
            if (!uiState.isFormValid && uiState.amountText.isNotBlank()) {
                Text(
                    text = stringResource(R.string.hint_valid_amount_and_category),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = DesignSystemSpacing.small),
                )
            } else if (uiState.selectedCategory == null && uiState.amountText.isNotBlank()) {
                Text(
                    text = stringResource(R.string.hint_select_category),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = DesignSystemSpacing.small),
                )
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

    val expenseDescription =
        if (selectedType == TransactionType.EXPENSE) {
            stringResource(R.string.a11y_expense_type_selected)
        } else {
            stringResource(R.string.a11y_select_expense_type)
        }
    val incomeDescription =
        if (selectedType == TransactionType.INCOME) {
            stringResource(R.string.a11y_income_type_selected)
        } else {
            stringResource(R.string.a11y_select_income_type)
        }

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
            FilterChip(
                selected = selectedType == TransactionType.EXPENSE,
                onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    onTypeChanged(TransactionType.EXPENSE)
                },
                label = { Text(stringResource(R.string.type_expense)) },
                modifier =
                    Modifier.semantics {
                        contentDescription = expenseDescription
                    },
            )
            FilterChip(
                selected = selectedType == TransactionType.INCOME,
                onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    onTypeChanged(TransactionType.INCOME)
                },
                label = { Text(stringResource(R.string.type_income)) },
                modifier =
                    Modifier.semantics {
                        contentDescription = incomeDescription
                    },
            )
        }
    }
}

@Composable
private fun EnhancedCategoryDropdown(
    categories: List<Category>,
    selectedCategory: Category?,
    onCategorySelected: (Category) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val categoryCardDescription =
        if (selectedCategory != null) {
            stringResource(R.string.a11y_category_selected, selectedCategory.name)
        } else {
            stringResource(R.string.a11y_select_category_from, categories.size)
        }

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
        Card(
            onClick = {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                expanded = true
            },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = categoryCardDescription
                    },
            elevation = CardDefaults.cardElevation(defaultElevation = DesignSystemElevation.low),
            colors =
                CardDefaults.cardColors(
                    containerColor =
                        if (selectedCategory == null) {
                            MaterialTheme.colorScheme.surfaceVariant
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                ),
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
                        text = selectedCategory?.name ?: stringResource(R.string.select_category),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (selectedCategory != null) FontWeight.Medium else FontWeight.Normal,
                        color =
                            if (selectedCategory == null) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                    )
                    if (selectedCategory == null) {
                        Text(
                            text =
                                if (categories.isNotEmpty()) {
                                    stringResource(R.string.hint_choose_categories, categories.size)
                                } else {
                                    stringResource(R.string.hint_choose_categories_generic)
                                },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
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
            val defaultCategories = categories.filter { it.isDefault }
            val customCategories = categories.filter { !it.isDefault }

            if (defaultCategories.isNotEmpty()) {
                DropdownMenuItem(
                    text = {
                        Text(
                            stringResource(R.string.categories_section_default),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    onClick = {},
                    enabled = false,
                )
                defaultCategories.forEach { category ->
                    val selectCategoryDescription = stringResource(R.string.a11y_select_category_item, category.name)
                    DropdownMenuItem(
                        text = {
                            Text(
                                category.name,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        },
                        onClick = {
                            hapticFeedback.performHapticFeedback(
                                HapticFeedbackType.LongPress,
                            )
                            onCategorySelected(category)
                            expanded = false
                        },
                        modifier =
                            Modifier.semantics {
                                contentDescription = selectCategoryDescription
                            },
                    )
                }
            }
            if (customCategories.isNotEmpty()) {
                DropdownMenuItem(
                    text = {
                        Text(
                            stringResource(R.string.categories_section_custom),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    onClick = {},
                    enabled = false,
                )
                customCategories.forEach { category ->
                    val selectCategoryDescription = stringResource(R.string.a11y_select_category_item, category.name)
                    DropdownMenuItem(
                        text = {
                            Text(
                                category.name,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        },
                        onClick = {
                            hapticFeedback.performHapticFeedback(
                                HapticFeedbackType.LongPress,
                            )
                            onCategorySelected(category)
                            expanded = false
                        },
                        modifier =
                            Modifier.semantics {
                                contentDescription = selectCategoryDescription
                            },
                    )
                }
            }
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
    val displayText = "${selectedCurrency.code} - ${selectedCurrency.displayName} ${selectedCurrency.symbol}"
    val currencyCardDescription = stringResource(R.string.a11y_currency_tap_to_change, displayText)

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
        Card(
            onClick = {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                expanded = true
            },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = currencyCardDescription
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
                val currencyItemDescription =
                    if (isSelected) {
                        stringResource(R.string.a11y_currency_currently_selected, currency.code, currency.displayName)
                    } else {
                        stringResource(R.string.a11y_select_currency_item, currency.code, currency.displayName)
                    }
                DropdownMenuItem(
                    text = {
                        Text(
                            "${currency.code} - ${currency.displayName} ${currency.symbol}",
                            style = MaterialTheme.typography.bodyLarge,
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
                    modifier =
                        Modifier.semantics {
                            contentDescription = currencyItemDescription
                        },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EnhancedDateSelector(
    timestamp: Long,
    onDateSelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val hapticFeedback = LocalHapticFeedback.current
    val dateCardDescription = stringResource(R.string.a11y_selected_date, DateTimeUtil.formatTimestamp(timestamp))

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(DesignSystemSpacing.xs),
    ) {
        Text(
            text = stringResource(R.string.label_date),
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
                        contentDescription = dateCardDescription
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
                        text = stringResource(R.string.hint_tap_to_select_date),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    Icons.Default.CalendarToday,
                    contentDescription = stringResource(R.string.a11y_select_date),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }

    // Date Picker Dialog
    if (showDatePicker) {
        val datePickerState =
            rememberDatePickerState(
                initialSelectedDateMillis = timestamp,
            )

        val confirmDateDescription = stringResource(R.string.a11y_confirm_date)
        val cancelDateDescription = stringResource(R.string.a11y_cancel_date)
        DatePickerDialog(
            onDismissRequest = {
                showDatePicker = false
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        datePickerState.selectedDateMillis?.let { selectedDate ->
                            // Convert to start of day in local timezone for consistency
                            onDateSelected(selectedDate)
                        }
                        showDatePicker = false
                    },
                    modifier =
                        Modifier.semantics {
                            contentDescription = confirmDateDescription
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
                    modifier =
                        Modifier.semantics {
                            contentDescription = cancelDateDescription
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
                        text = stringResource(R.string.select_date_title),
                        modifier = Modifier.padding(16.dp),
                    )
                },
            )
        }
    }
}
