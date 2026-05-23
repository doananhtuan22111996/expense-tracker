package dev.tuandoan.expensetracker.ui.screen.trips

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.tuandoan.expensetracker.core.formatter.CurrencyFormatter
import dev.tuandoan.expensetracker.core.util.ErrorUtils
import dev.tuandoan.expensetracker.core.util.UiText
import dev.tuandoan.expensetracker.domain.model.Trip
import dev.tuandoan.expensetracker.domain.model.TripFilter
import dev.tuandoan.expensetracker.domain.repository.CurrencyPreferenceRepository
import dev.tuandoan.expensetracker.domain.repository.TripRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/**
 * Backs [TripsScreen] (v3.13.0, T2.2 + T2.3). Combines the three time-relative
 * [TripFilter] streams into one [TripsUiState] so the screen can render
 * Active / Upcoming / Past sections without re-deriving `now` per row.
 *
 * Each row carries a [TripCardUi] with a pre-formatted home-currency total +
 * transaction count (T2.3). Aggregations are sourced from the per-trip flows
 * shipped in PR #154; the home currency comes from
 * [CurrencyPreferenceRepository] so a currency change re-renders every row.
 *
 * `nowEpochDay` is captured once at init: trip dates change rarely enough
 * that a midnight rollover during a single screen visit is not worth
 * re-bucketing for. Same trade-off as `HomeViewModel`'s month snapshot.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TripsViewModel
    @Inject
    constructor(
        private val tripRepository: TripRepository,
        private val currencyPreferenceRepository: CurrencyPreferenceRepository,
        private val currencyFormatter: CurrencyFormatter,
        clock: Clock,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(TripsUiState())
        val uiState: StateFlow<TripsUiState> = _uiState.asStateFlow()

        init {
            val nowEpochDay = LocalDate.now(clock).toEpochDay()
            observeTrips(nowEpochDay)
        }

        fun clearError() {
            _uiState.update { it.copy(errorMessage = null) }
        }

        private fun observeTrips(nowEpochDay: Long) {
            viewModelScope.launch {
                combine(
                    tripRepository.observeTrips(TripFilter.Active(nowEpochDay)),
                    tripRepository.observeTrips(TripFilter.Upcoming(nowEpochDay)),
                    tripRepository.observeTrips(TripFilter.Past(nowEpochDay)),
                ) { active, upcoming, past ->
                    Sections(active = active, upcoming = upcoming, past = past)
                }.flatMapLatest { sections ->
                    val allTrips = sections.active + sections.upcoming + sections.past
                    if (allTrips.isEmpty()) {
                        // combine() over an empty list never emits — short-circuit so
                        // the empty state surfaces instead of hanging on isLoading.
                        flowOf(sections to emptyMap())
                    } else {
                        enrichmentFlow(allTrips).map { aggMap -> sections to aggMap }
                    }
                }.catch { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = ErrorUtils.getErrorMessage(e),
                        )
                    }
                }.collect { (sections, aggMap) ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            active = sections.active.map { trip -> trip.toUi(aggMap) },
                            upcoming = sections.upcoming.map { trip -> trip.toUi(aggMap) },
                            past = sections.past.map { trip -> trip.toUi(aggMap) },
                        )
                    }
                }
            }
        }

        // Currency stream lives in the SAME outer combine as totals/counts so
        // a currency switch only re-emits the format step — it does NOT cancel
        // and re-attach every per-trip Room observer (the older flatMapLatest
        // shape did, which is needlessly expensive for a Settings-rare event).
        private fun enrichmentFlow(trips: List<Trip>): Flow<Map<Long, TripAggregates>> {
            val ids = trips.map { it.id }
            val totalsFlow: Flow<List<Long?>> =
                combine(ids.map { tripRepository.observeTripTotal(it) }) { it.toList() }
            val countsFlow: Flow<List<Int>> =
                combine(ids.map { tripRepository.observeTripTransactionCount(it) }) { it.toList() }
            val currencyFlow: Flow<String> =
                currencyPreferenceRepository
                    .observeDefaultCurrency()
                    .distinctUntilChanged()
            return combine(totalsFlow, countsFlow, currencyFlow) { totals, counts, code ->
                ids.indices.associate { i ->
                    ids[i] to
                        TripAggregates(
                            totalLabel = totals[i]?.let { currencyFormatter.format(it, code) },
                            transactionCount = counts[i],
                        )
                }
            }
        }

        private fun Trip.toUi(aggMap: Map<Long, TripAggregates>): TripCardUi {
            val agg = aggMap[id]
            return TripCardUi(
                trip = this,
                totalLabel = agg?.totalLabel,
                transactionCount = agg?.transactionCount ?: 0,
            )
        }

        private data class Sections(
            val active: List<Trip>,
            val upcoming: List<Trip>,
            val past: List<Trip>,
        )

        private data class TripAggregates(
            val totalLabel: String?,
            val transactionCount: Int,
        )
    }

data class TripCardUi(
    val trip: Trip,
    val totalLabel: String?,
    val transactionCount: Int,
)

data class TripsUiState(
    val active: List<TripCardUi> = emptyList(),
    val upcoming: List<TripCardUi> = emptyList(),
    val past: List<TripCardUi> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: UiText? = null,
) {
    val isEmpty: Boolean
        get() = !isLoading && active.isEmpty() && upcoming.isEmpty() && past.isEmpty()
}
