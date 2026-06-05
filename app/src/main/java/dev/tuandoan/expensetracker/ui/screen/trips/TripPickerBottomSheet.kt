package dev.tuandoan.expensetracker.ui.screen.trips

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.tuandoan.expensetracker.R
import dev.tuandoan.expensetracker.domain.model.Trip
import dev.tuandoan.expensetracker.ui.theme.DesignSystemSpacing
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripPickerBottomSheet(
    sheetState: SheetState,
    selectedTripId: Long?,
    onTripSelected: (Long?) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TripPickerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        start = DesignSystemSpacing.large,
                        end = DesignSystemSpacing.large,
                        bottom = DesignSystemSpacing.xl,
                    ),
        ) {
            Text(
                text = stringResource(R.string.trip_picker_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = DesignSystemSpacing.large),
            )

            val hasAnyTrip =
                uiState.active.isNotEmpty() ||
                    uiState.upcoming.isNotEmpty() ||
                    uiState.past.isNotEmpty()

            LazyColumn(modifier = Modifier.heightIn(max = PICKER_MAX_HEIGHT)) {
                // "No trip" option
                item {
                    val noneDesc = stringResource(R.string.a11y_trip_picker_none)
                    TripPickerNoneRow(
                        isSelected = selectedTripId == null,
                        onClick = {
                            onTripSelected(null)
                            onDismiss()
                        },
                        modifier =
                            Modifier.semantics {
                                contentDescription = noneDesc
                            },
                    )
                }

                if (!hasAnyTrip) {
                    item {
                        Text(
                            text = stringResource(R.string.trip_picker_no_trips),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier =
                                Modifier.padding(
                                    top = DesignSystemSpacing.large,
                                    bottom = DesignSystemSpacing.small,
                                ),
                        )
                    }
                }

                // Active section
                if (uiState.active.isNotEmpty()) {
                    item {
                        TripPickerSectionHeader(stringResource(R.string.trips_section_active))
                    }
                    items(uiState.active, key = { it.id }) { trip ->
                        TripPickerRow(
                            trip = trip,
                            isSelected = selectedTripId == trip.id,
                            onClick = {
                                onTripSelected(trip.id)
                                onDismiss()
                            },
                        )
                    }
                }

                // Upcoming section
                if (uiState.upcoming.isNotEmpty()) {
                    item {
                        TripPickerSectionHeader(stringResource(R.string.trips_section_upcoming))
                    }
                    items(uiState.upcoming, key = { it.id }) { trip ->
                        TripPickerRow(
                            trip = trip,
                            isSelected = selectedTripId == trip.id,
                            onClick = {
                                onTripSelected(trip.id)
                                onDismiss()
                            },
                        )
                    }
                }

                // Past section — collapsed by default
                if (uiState.past.isNotEmpty()) {
                    item {
                        TripPickerPastHeader(
                            isExpanded = uiState.isPastExpanded,
                            onToggle = viewModel::togglePastExpanded,
                        )
                    }
                    if (uiState.isPastExpanded) {
                        items(uiState.past, key = { it.id }) { trip ->
                            TripPickerRow(
                                trip = trip,
                                isSelected = selectedTripId == trip.id,
                                onClick = {
                                    onTripSelected(trip.id)
                                    onDismiss()
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TripPickerNoneRow(
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = DesignSystemSpacing.small),
    ) {
        RadioButton(selected = isSelected, onClick = onClick)
        Spacer(Modifier.width(DesignSystemSpacing.small))
        Column {
            Text(
                text = stringResource(R.string.trip_picker_none),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.trip_picker_none_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TripPickerRow(
    trip: Trip,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dateLabel =
        remember(trip.startDateEpochDay, trip.endDateEpochDay) {
            formatPickerDateRange(trip.startDateEpochDay, trip.endDateEpochDay)
        }
    val rowDesc = stringResource(R.string.a11y_trip_picker_item, trip.name, dateLabel)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = DesignSystemSpacing.small)
                .semantics { contentDescription = rowDesc },
    ) {
        RadioButton(selected = isSelected, onClick = onClick)
        Spacer(Modifier.width(DesignSystemSpacing.small))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = trip.name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text =
                    if (trip.destination.isNullOrBlank()) {
                        dateLabel
                    } else {
                        "${trip.destination} · $dateLabel"
                    },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (!trip.foreignCurrencyCode.isNullOrBlank()) {
            Text(
                text = trip.foreignCurrencyCode,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = DesignSystemSpacing.small),
            )
        }
    }
}

@Composable
private fun TripPickerSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(modifier = Modifier.padding(vertical = DesignSystemSpacing.small))
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = DesignSystemSpacing.xs),
        )
    }
}

@Composable
private fun TripPickerPastHeader(
    isExpanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(modifier = Modifier.padding(vertical = DesignSystemSpacing.small))
        TextButton(
            onClick = onToggle,
            modifier = Modifier.padding(horizontal = 0.dp),
        ) {
            Text(
                text =
                    stringResource(
                        if (isExpanded) R.string.trip_picker_past_hide else R.string.trip_picker_past_show,
                    ),
                style = MaterialTheme.typography.labelMedium,
            )
            Icon(
                imageVector =
                    if (isExpanded) {
                        Icons.Filled.KeyboardArrowUp
                    } else {
                        Icons.Filled.KeyboardArrowDown
                    },
                contentDescription = null,
            )
        }
    }
}

private val PICKER_DATE_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

private fun formatPickerDateRange(
    startEpochDay: Long,
    endEpochDay: Long,
): String {
    val start = LocalDate.ofEpochDay(startEpochDay).format(PICKER_DATE_FORMATTER)
    val end = LocalDate.ofEpochDay(endEpochDay).format(PICKER_DATE_FORMATTER)
    return "$start – $end"
}

private val PICKER_MAX_HEIGHT = 400.dp
