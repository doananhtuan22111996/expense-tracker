package dev.tuandoan.expensetracker.ui.screen.trips

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.tuandoan.expensetracker.core.util.ErrorUtils
import dev.tuandoan.expensetracker.core.util.UiText
import dev.tuandoan.expensetracker.domain.model.Trip
import dev.tuandoan.expensetracker.domain.model.TripFilter
import dev.tuandoan.expensetracker.domain.repository.TripRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/**
 * Backs [TripsScreen] (v3.13.0, T2.2). Combines the three time-relative
 * [TripFilter] streams into one [TripsUiState] so the screen can render
 * Active / Upcoming / Past sections without re-deriving `now` per row.
 *
 * `nowEpochDay` is captured once at init: trip dates change rarely enough
 * that a midnight rollover during a single screen visit is not worth
 * re-bucketing for. Same trade-off as `HomeViewModel`'s month snapshot.
 */
@HiltViewModel
class TripsViewModel
    @Inject
    constructor(
        private val tripRepository: TripRepository,
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
                    Triple(active, upcoming, past)
                }.catch { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = ErrorUtils.getErrorMessage(e),
                        )
                    }
                }.collect { (active, upcoming, past) ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            active = active,
                            upcoming = upcoming,
                            past = past,
                        )
                    }
                }
            }
        }
    }

data class TripsUiState(
    val active: List<Trip> = emptyList(),
    val upcoming: List<Trip> = emptyList(),
    val past: List<Trip> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: UiText? = null,
) {
    val isEmpty: Boolean
        get() = !isLoading && active.isEmpty() && upcoming.isEmpty() && past.isEmpty()
}
