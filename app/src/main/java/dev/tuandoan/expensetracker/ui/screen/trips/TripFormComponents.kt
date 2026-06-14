package dev.tuandoan.expensetracker.ui.screen.trips

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import dev.tuandoan.expensetracker.R
import dev.tuandoan.expensetracker.core.util.EpochDayConverters
import dev.tuandoan.expensetracker.ui.component.CurrencyDropdown
import dev.tuandoan.expensetracker.ui.theme.DesignSystemElevation
import dev.tuandoan.expensetracker.ui.theme.DesignSystemSpacing
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
internal fun TripNameField(
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
internal fun TripDestinationField(
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
internal fun TripDateRangeRow(
    startEpochDay: Long?,
    endEpochDay: Long?,
    error: String?,
    onClick: () -> Unit,
) {
    val rangeText =
        if (startEpochDay != null && endEpochDay != null) {
            tripFormatDateRange(startEpochDay, endEpochDay)
        } else if (startEpochDay != null) {
            stringResource(R.string.create_edit_trip_dates_partial, tripFormatDate(startEpochDay))
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
internal fun TripForeignCurrencyToggle(
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
internal fun TripRateField(
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TripDateRangeSheet(
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

/** Shared foreign-currency section: dropdown + rate field, shown when [isForeignCurrency] is true. */
@Composable
internal fun TripForeignCurrencySection(
    isForeignCurrency: Boolean,
    foreignCurrencyCode: String,
    homeCurrencyCode: String,
    rateText: String,
    rateError: String?,
    onCurrencyChange: (String) -> Unit,
    onRateChange: (String) -> Unit,
    onImeDone: () -> Unit,
) {
    if (!isForeignCurrency) return
    CurrencyDropdown(
        selectedCurrencyCode = foreignCurrencyCode.ifBlank { homeCurrencyCode },
        onCurrencySelected = onCurrencyChange,
        disabledCodes = setOf(homeCurrencyCode),
        disabledMarkerSuffix = " · Home",
    )
    TripRateField(
        value = rateText,
        homeCode = homeCurrencyCode,
        foreignCode = foreignCurrencyCode,
        error = rateError,
        onChange = onRateChange,
        onImeDone = onImeDone,
    )
}

private val TRIP_DATE_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

private fun tripFormatDate(epochDay: Long): String = LocalDate.ofEpochDay(epochDay).format(TRIP_DATE_FORMATTER)

internal fun tripFormatDateRange(
    startEpochDay: Long,
    endEpochDay: Long,
): String = "${tripFormatDate(startEpochDay)} – ${tripFormatDate(endEpochDay)}"
