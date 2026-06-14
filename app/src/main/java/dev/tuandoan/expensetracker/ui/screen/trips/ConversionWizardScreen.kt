package dev.tuandoan.expensetracker.ui.screen.trips

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.tuandoan.expensetracker.R
import dev.tuandoan.expensetracker.domain.model.Category
import dev.tuandoan.expensetracker.domain.model.ConversionDraft
import dev.tuandoan.expensetracker.domain.model.Transaction
import dev.tuandoan.expensetracker.ui.component.AmountText
import dev.tuandoan.expensetracker.ui.theme.ChartColors
import dev.tuandoan.expensetracker.ui.theme.DesignSystemElevation
import dev.tuandoan.expensetracker.ui.theme.DesignSystemSpacing
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

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

// ── Step 2 — Row decisions ───────────────────────────────────────────────────

@Composable
private fun WizardStep2RowDecisionsBody(
    uiState: ConversionWizardUiState,
    viewModel: ConversionWizardViewModel,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text =
                stringResource(
                    R.string.conversion_wizard_step2_subtitle,
                    uiState.transactions.size,
                ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = DesignSystemSpacing.small),
        )
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(uiState.transactions, key = { it.id }) { tx ->
                val decision = uiState.rowDecisions[tx.id]
                val isMigrate = decision is ConversionDraft.RowDecision.Migrate
                val currentCategoryId =
                    (decision as? ConversionDraft.RowDecision.Migrate)?.newCategoryId
                DecisionRow(
                    transaction = tx,
                    homeCurrencyCode = uiState.homeCurrencyCode,
                    isMigrate = isMigrate,
                    currentCategoryId = currentCategoryId,
                    availableCategories = uiState.availableCategories,
                    onMigrate = { newCategoryId ->
                        viewModel.onDecisionChanged(
                            tx.id,
                            ConversionDraft.RowDecision.Migrate(tx.id, newCategoryId),
                        )
                    },
                    onSkip = {
                        viewModel.onDecisionChanged(
                            tx.id,
                            ConversionDraft.RowDecision.Skip(tx.id),
                        )
                    },
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun DecisionRow(
    transaction: Transaction,
    homeCurrencyCode: String,
    isMigrate: Boolean,
    currentCategoryId: Long?,
    availableCategories: List<Category>,
    onMigrate: (newCategoryId: Long) -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dateFormatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }
    val dateText =
        remember(transaction.timestamp) {
            LocalDate
                .ofInstant(Instant.ofEpochMilli(transaction.timestamp), ZoneId.systemDefault())
                .format(dateFormatter)
        }
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(
                    horizontal = DesignSystemSpacing.medium,
                    vertical = DesignSystemSpacing.small,
                ),
        verticalArrangement = Arrangement.spacedBy(DesignSystemSpacing.xs),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                AmountText(
                    amount = transaction.amount,
                    transactionType = transaction.type,
                    showSign = false,
                    showCurrency = true,
                    currencyCode = homeCurrencyCode,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                transaction.note?.let { note ->
                    if (note.isNotBlank()) {
                        Text(
                            text = note,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(DesignSystemSpacing.xs)) {
                FilterChip(
                    selected = isMigrate,
                    onClick = {
                        if (!isMigrate) {
                            onMigrate(
                                currentCategoryId
                                    ?: availableCategories.firstOrNull()?.id
                                    ?: 0L,
                            )
                        }
                    },
                    label = { Text(stringResource(R.string.conversion_wizard_migrate)) },
                )
                FilterChip(
                    selected = !isMigrate,
                    onClick = { if (isMigrate) onSkip() },
                    label = { Text(stringResource(R.string.conversion_wizard_skip)) },
                )
            }
        }
        if (isMigrate && availableCategories.isNotEmpty()) {
            val selectedCategory = availableCategories.find { it.id == currentCategoryId }
            RowCategoryPicker(
                categories = availableCategories,
                selectedCategory = selectedCategory,
                onCategorySelected = { onMigrate(it.id) },
            )
        }
    }
}

@Composable
private fun RowCategoryPicker(
    categories: List<Category>,
    selectedCategory: Category?,
    onCategorySelected: (Category) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val colorScheme = MaterialTheme.colorScheme
    val swatch = ChartColors.categoryColor(selectedCategory?.colorKey, colorScheme)
    val pickerDesc =
        if (selectedCategory != null) {
            stringResource(R.string.a11y_selected_tap_to_change, selectedCategory.name)
        } else {
            stringResource(R.string.a11y_select_category_generic)
        }

    Box(modifier = modifier.fillMaxWidth()) {
        Card(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                expanded = true
            },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = pickerDesc },
            elevation = CardDefaults.cardElevation(defaultElevation = DesignSystemElevation.low),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(DesignSystemSpacing.medium),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DesignSystemSpacing.small),
            ) {
                Text(
                    text = stringResource(R.string.conversion_wizard_move_to),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Box(
                    modifier =
                        Modifier
                            .size(DesignSystemSpacing.large)
                            .background(
                                color = swatch,
                                shape = MaterialTheme.shapes.extraSmall,
                            ),
                )
                Text(
                    text = selectedCategory?.name ?: stringResource(R.string.select_category),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(DesignSystemSpacing.large),
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            categories.forEach { category ->
                val isSelected = category.id == selectedCategory?.id
                DropdownMenuItem(
                    text = {
                        Text(
                            category.name,
                            color =
                                if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                        )
                    },
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onCategorySelected(category)
                        expanded = false
                    },
                )
            }
        }
    }
}

// ── Step 3 — Preview ─────────────────────────────────────────────────────────

@Composable
private fun WizardStep3PreviewBody(uiState: ConversionWizardUiState) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(DesignSystemSpacing.large),
    ) {
        // Trip summary card
        PreviewSection(title = stringResource(R.string.conversion_wizard_preview_trip_header)) {
            PreviewRow(
                label = stringResource(R.string.conversion_wizard_preview_label_name),
                value = uiState.tripName,
            )
            if (uiState.tripDestination.isNotBlank()) {
                PreviewRow(
                    label = stringResource(R.string.conversion_wizard_preview_label_destination),
                    value = uiState.tripDestination,
                )
            }
            val startDay = uiState.startEpochDay
            val endDay = uiState.endEpochDay
            if (startDay != null && endDay != null) {
                PreviewRow(
                    label = stringResource(R.string.conversion_wizard_preview_label_dates),
                    value = tripFormatDateRange(startDay, endDay),
                )
            }
            if (uiState.isForeignCurrency && uiState.foreignCurrencyCode.isNotBlank()) {
                PreviewRow(
                    label = stringResource(R.string.conversion_wizard_preview_label_currency),
                    value =
                        if (uiState.rateText.isNotBlank()) {
                            stringResource(
                                R.string.conversion_wizard_preview_fx_rate,
                                uiState.foreignCurrencyCode,
                                uiState.rateText,
                                uiState.homeCurrencyCode,
                            )
                        } else {
                            uiState.foreignCurrencyCode
                        },
                )
            }
        }

        // Transaction decisions summary
        PreviewSection(title = stringResource(R.string.conversion_wizard_preview_decisions_header)) {
            if (uiState.migratedCount > 0) {
                PreviewRow(
                    label =
                        stringResource(
                            R.string.conversion_wizard_preview_label_migrate,
                            uiState.migratedCount,
                        ),
                    value = uiState.tripName,
                )
            }
            if (uiState.skippedCount > 0) {
                PreviewRow(
                    label =
                        stringResource(
                            R.string.conversion_wizard_preview_label_skip,
                            uiState.skippedCount,
                        ),
                    value = uiState.sourceCategoryName,
                )
            }
        }

        // Source category fate
        val isDelete = uiState.sourceDisposition == ConversionDraft.SourceDisposition.DELETE
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors =
                CardDefaults.cardColors(
                    containerColor =
                        if (isDelete) {
                            MaterialTheme.colorScheme.errorContainer
                        } else {
                            MaterialTheme.colorScheme.secondaryContainer
                        },
                ),
        ) {
            Text(
                text =
                    if (isDelete) {
                        stringResource(
                            R.string.conversion_wizard_source_will_be_deleted_named,
                            uiState.sourceCategoryName,
                        )
                    } else {
                        stringResource(
                            R.string.conversion_wizard_source_will_be_kept_named,
                            uiState.sourceCategoryName,
                        )
                    },
                style = MaterialTheme.typography.bodyMedium,
                color =
                    if (isDelete) {
                        MaterialTheme.colorScheme.onErrorContainer
                    } else {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    },
                modifier = Modifier.padding(DesignSystemSpacing.large),
            )
        }
    }
}

@Composable
private fun PreviewSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(DesignSystemSpacing.xs)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = DesignSystemElevation.low),
        ) {
            Column(
                modifier = Modifier.padding(DesignSystemSpacing.large),
                verticalArrangement = Arrangement.spacedBy(DesignSystemSpacing.small),
            ) {
                content()
            }
        }
    }
}

@Composable
private fun PreviewRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.4f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(0.6f),
        )
    }
}
