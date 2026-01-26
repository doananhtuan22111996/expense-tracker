package dev.tuandoan.expensetracker.ui.screen.addedit

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.tuandoan.expensetracker.core.formatter.AmountFormatter
import dev.tuandoan.expensetracker.core.util.DateTimeUtil
import dev.tuandoan.expensetracker.domain.model.Category
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
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val isEditMode = transactionId != null

    // Handle system back button
    BackHandler {
        viewModel.onBackPressed()
        if (!isEditMode || !uiState.hasUnsavedChanges) {
            onNavigateBack()
        }
    }

    // Handle back navigation from ViewModel
    LaunchedEffect(uiState.showDiscardDialog) {
        // Dialog state is managed in the UI state
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "Edit Transaction" else "Add Transaction") },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            viewModel.onBackPressed()
                            if (!isEditMode || !uiState.hasUnsavedChanges) {
                                onNavigateBack()
                            }
                        },
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
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
            TransactionForm(
                uiState = uiState,
                isEditMode = isEditMode,
                viewModel = viewModel,
                onNavigateBack = onNavigateBack,
                modifier =
                    Modifier
                        .fillMaxSize()
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
        AlertDialog(
            onDismissRequest = { viewModel.onCancelDiscard() },
            title = { Text("Discard changes?") },
            text = { Text("You have unsaved changes.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.onDiscardChanges()
                        onNavigateBack()
                    },
                ) {
                    Text("Discard")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onCancelDiscard() }) {
                    Text("Cancel")
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
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(DesignSystemSpacing.large),
    ) {
        // Transaction Type
        TransactionTypeSelector(
            selectedType = uiState.type,
            onTypeChanged = viewModel::onTypeChanged,
        )

        // Primary section: Amount (most important field)
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
                    // Format input with commas as user types for better UX
                    val cleanInput = input.replace("[^0-9]".toRegex(), "")
                    if (cleanInput.isNotEmpty()) {
                        val formattedInput = AmountFormatter.formatAmount(cleanInput.toLongOrNull() ?: 0L)
                        viewModel.onAmountChanged(formattedInput)
                    } else {
                        viewModel.onAmountChanged("")
                    }
                },
                label = { Text("Enter amount in VND") },
                placeholder = { Text("1,000,000") },
                suffix = {
                    Text(
                        "₫",
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
                isError = uiState.amountText.isNotBlank() && !uiState.isFormValid,
                supportingText = {
                    if (uiState.amountText.isNotBlank() && !uiState.isFormValid) {
                        Text(
                            "Please enter a valid amount",
                            color = MaterialTheme.colorScheme.error,
                        )
                    } else if (uiState.amountText.isEmpty()) {
                        Text(
                            "Enter amount without decimals (VND doesn't use cents)",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                textStyle = MaterialTheme.typography.headlineSmall,
            )
        }

        // Category Selection - Enhanced dropdown
        EnhancedCategoryDropdown(
            categories = uiState.categories,
            selectedCategory = uiState.selectedCategory,
            onCategorySelected = viewModel::onCategorySelected,
        )

        // Date Selection - Enhanced selector
        EnhancedDateSelector(
            timestamp = uiState.timestamp,
            onDateSelected = viewModel::onDateSelected,
        )

        // Note - Optional field with lower visual priority
        Column(verticalArrangement = Arrangement.spacedBy(DesignSystemSpacing.xs)) {
            Text(
                text = "Note (optional)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedTextField(
                value = uiState.note,
                onValueChange = viewModel::onNoteChanged,
                placeholder = { Text("Add details about this transaction...") },
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

        // Save Button - Enhanced visual weight when enabled
        Button(
            onClick = { viewModel.saveTransaction(onNavigateBack) },
            enabled = uiState.isSaveEnabled && !uiState.isLoading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = if (isEditMode) "Update Transaction" else "Save Transaction",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
            )
        }

        // Form status hint
        if (!uiState.isFormValid && uiState.amountText.isNotBlank()) {
            Text(
                text = "Please enter a valid amount and select a category",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = DesignSystemSpacing.small),
            )
        } else if (uiState.selectedCategory == null && uiState.amountText.isNotBlank()) {
            Text(
                text = "Please select a category",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = DesignSystemSpacing.small),
            )
        }
    }
}

@Composable
private fun TransactionTypeSelector(
    selectedType: TransactionType,
    onTypeChanged: (TransactionType) -> Unit,
    modifier: Modifier = Modifier,
) {
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
                onClick = { onTypeChanged(TransactionType.EXPENSE) },
                label = { Text("Expense") },
            )
            FilterChip(
                selected = selectedType == TransactionType.INCOME,
                onClick = { onTypeChanged(TransactionType.INCOME) },
                label = { Text("Income") },
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

        Card(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
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
                        text = selectedCategory?.name ?: "Select Category",
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
                            text = "Choose from your ${if (categories.isNotEmpty()) {
                                "${categories.size} categories"
                            } else {
                                "categories"
                            }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
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
                    text = {
                        Text(
                            category.name,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    },
                    onClick = {
                        onCategorySelected(category)
                        expanded = false
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

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(DesignSystemSpacing.xs),
    ) {
        Text(
            text = "Date",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Card(
            onClick = {
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
                Column {
                    Text(
                        text = DateTimeUtil.formatTimestamp(timestamp),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Tap to select date",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    Icons.Default.CalendarToday,
                    contentDescription = "Select date",
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

        DatePickerDialog(
            onDismissRequest = {
                showDatePicker = false
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { selectedDate ->
                            // Convert to start of day in local timezone for consistency
                            onDateSelected(selectedDate)
                        }
                        showDatePicker = false
                    },
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDatePicker = false
                    },
                ) {
                    Text("Cancel")
                }
            },
        ) {
            DatePicker(
                state = datePickerState,
                title = {
                    Text(
                        text = "Select Date",
                        modifier = Modifier.padding(16.dp),
                    )
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateSelector(
    timestamp: Long,
    onDateSelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDatePicker by remember { mutableStateOf(false) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(DesignSystemSpacing.xs),
    ) {
        Text(
            text = "Date",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Card(
            onClick = {
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
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Icon(
                    Icons.Default.CalendarToday,
                    contentDescription = "Select date",
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

        DatePickerDialog(
            onDismissRequest = {
                showDatePicker = false
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { selectedDate ->
                            // Convert to start of day in local timezone for consistency
                            onDateSelected(selectedDate)
                        }
                        showDatePicker = false
                    },
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDatePicker = false
                    },
                ) {
                    Text("Cancel")
                }
            },
        ) {
            DatePicker(
                state = datePickerState,
                title = {
                    Text(
                        text = "Select Date",
                        modifier = Modifier.padding(16.dp),
                    )
                },
            )
        }
    }
}
