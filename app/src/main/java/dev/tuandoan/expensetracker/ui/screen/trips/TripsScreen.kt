package dev.tuandoan.expensetracker.ui.screen.trips

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.tuandoan.expensetracker.R
import dev.tuandoan.expensetracker.domain.model.Trip
import dev.tuandoan.expensetracker.ui.component.EmptyStateMessage
import dev.tuandoan.expensetracker.ui.theme.DesignSystemElevation
import dev.tuandoan.expensetracker.ui.theme.DesignSystemSpacing
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToCreate: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    viewModel: TripsViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it.asString(context))
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.trips)) },
                navigationIcon = {
                    val goBackDesc = stringResource(R.string.a11y_go_back)
                    IconButton(
                        onClick = onNavigateBack,
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
        floatingActionButton = {
            val addTripDesc = stringResource(R.string.a11y_add_trip)
            FloatingActionButton(
                onClick = onNavigateToCreate,
                modifier =
                    Modifier.semantics {
                        contentDescription = addTripDesc
                    },
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier,
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    val loadingDesc = stringResource(R.string.a11y_loading_trips)
                    CircularProgressIndicator(
                        modifier =
                            Modifier.semantics {
                                contentDescription = loadingDesc
                            },
                    )
                }
            }

            uiState.isEmpty -> {
                EmptyStateMessage(
                    title = stringResource(R.string.trips_empty_title),
                    subtitle = stringResource(R.string.trips_empty_subtitle),
                    modifier = Modifier.padding(innerPadding),
                )
            }

            else -> {
                LazyColumn(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(horizontal = DesignSystemSpacing.screenPadding),
                    verticalArrangement = Arrangement.spacedBy(DesignSystemSpacing.small),
                ) {
                    tripSection(
                        titleRes = R.string.trips_section_active,
                        trips = uiState.active,
                        onClick = onNavigateToDetail,
                    )
                    tripSection(
                        titleRes = R.string.trips_section_upcoming,
                        trips = uiState.upcoming,
                        onClick = onNavigateToDetail,
                    )
                    tripSection(
                        titleRes = R.string.trips_section_past,
                        trips = uiState.past,
                        onClick = onNavigateToDetail,
                    )
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.tripSection(
    titleRes: Int,
    trips: List<Trip>,
    onClick: (Long) -> Unit,
) {
    if (trips.isEmpty()) return
    item(key = "header-$titleRes") {
        Text(
            text = stringResource(titleRes),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier =
                Modifier.padding(
                    top = DesignSystemSpacing.medium,
                    bottom = DesignSystemSpacing.xs,
                ),
        )
    }
    items(items = trips, key = { trip -> trip.id }) { trip ->
        TripRow(trip = trip, onClick = { onClick(trip.id) })
    }
}

@Composable
private fun TripRow(
    trip: Trip,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dateRangeLabel =
        remember(trip.startDateEpochDay, trip.endDateEpochDay) {
            formatTripDateRange(trip.startDateEpochDay, trip.endDateEpochDay)
        }
    val rowDesc = stringResource(R.string.a11y_trip_row, trip.name, dateRangeLabel)

    Card(
        onClick = onClick,
        modifier =
            modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = rowDesc
                },
        elevation = CardDefaults.cardElevation(defaultElevation = DesignSystemElevation.low),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(DesignSystemSpacing.large),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = trip.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (!trip.destination.isNullOrBlank()) {
                    Text(
                        text = trip.destination,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = dateRangeLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = DesignSystemSpacing.xs),
                )
            }
            if (!trip.foreignCurrencyCode.isNullOrBlank()) {
                Text(
                    text = trip.foreignCurrencyCode,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = DesignSystemSpacing.small),
                )
            }
        }
    }
}

private val TRIP_DATE_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

private fun formatTripDateRange(
    startEpochDay: Long,
    endEpochDay: Long,
): String {
    val start = LocalDate.ofEpochDay(startEpochDay).format(TRIP_DATE_FORMATTER)
    val end = LocalDate.ofEpochDay(endEpochDay).format(TRIP_DATE_FORMATTER)
    return "$start – $end"
}
