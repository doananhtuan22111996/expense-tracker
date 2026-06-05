package dev.tuandoan.expensetracker.ui.screen.trips

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
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
 * Backs [TripPickerBottomSheet] (T3.1). Combines Active / Upcoming / Past trip
 * streams into [TripPickerUiState] for display. The caller owns the currently
 * selected trip id and passes it in; this VM only manages the list + collapsed state.
 */
@HiltViewModel
class TripPickerViewModel
    @Inject
    constructor(
        private val tripRepository: TripRepository,
        clock: Clock,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(TripPickerUiState())
        val uiState: StateFlow<TripPickerUiState> = _uiState.asStateFlow()

        init {
            val nowEpochDay = LocalDate.now(clock).toEpochDay()
            observeTrips(nowEpochDay)
        }

        fun togglePastExpanded() {
            _uiState.update { it.copy(isPastExpanded = !it.isPastExpanded) }
        }

        private fun observeTrips(nowEpochDay: Long) {
            viewModelScope.launch {
                combine(
                    tripRepository.observeTrips(TripFilter.Active(nowEpochDay)),
                    tripRepository.observeTrips(TripFilter.Upcoming(nowEpochDay)),
                    tripRepository.observeTrips(TripFilter.Past(nowEpochDay)),
                ) { active, upcoming, past ->
                    _uiState.value.copy(
                        active = active,
                        upcoming = upcoming,
                        past = past,
                        isLoading = false,
                    )
                }.catch { _ ->
                    _uiState.update { it.copy(isLoading = false) }
                }.collect { state ->
                    _uiState.value = state
                }
            }
        }
    }

data class TripPickerUiState(
    val active: List<Trip> = emptyList(),
    val upcoming: List<Trip> = emptyList(),
    val past: List<Trip> = emptyList(),
    val isLoading: Boolean = true,
    val isPastExpanded: Boolean = false,
)
