package dev.tuandoan.expensetracker.ui.screen.trips

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.tuandoan.expensetracker.R
import dev.tuandoan.expensetracker.core.util.ErrorUtils
import dev.tuandoan.expensetracker.core.util.UiText
import dev.tuandoan.expensetracker.domain.model.SupportedCurrencies
import dev.tuandoan.expensetracker.domain.model.Trip
import dev.tuandoan.expensetracker.domain.repository.CurrencyPreferenceRepository
import dev.tuandoan.expensetracker.domain.repository.TripRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/**
 * Backs [CreateEditTripScreen] (v3.13.0, T2.4). Add mode when route arg
 * `tripId == 0L`; edit mode otherwise.
 *
 * Snapshot fields ([Trip.originalCategoryId] etc.) are pass-through-only on
 * update — surfaced read-only by the screen, never mutated here. Per ADR-001
 * they only get written via the Epic 4 conversion-wizard commit path.
 *
 * FX validation is "code differs from home + rate > 0" only. The repo-level
 * "can't change `foreignCurrencyCode` when any tx has a foreign amount"
 * invariant in [TripRepository.updateTrip]'s KDoc lands with T3.7 / Epic 3
 * (needs the per-tx foreign-amount query); not in this PR.
 */
@HiltViewModel
class CreateEditTripViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val tripRepository: TripRepository,
        private val currencyPreferenceRepository: CurrencyPreferenceRepository,
        clock: Clock,
    ) : ViewModel() {
        private val tripId: Long = savedStateHandle["tripId"] ?: 0L
        val isEditMode: Boolean = tripId != 0L

        private val today: Long = LocalDate.now(clock).toEpochDay()
        private val homeCurrencyDefault: String = SupportedCurrencies.default().code

        private val _uiState = MutableStateFlow(CreateEditTripUiState())
        val uiState: StateFlow<CreateEditTripUiState> = _uiState.asStateFlow()

        init {
            if (isEditMode) loadExisting() else loadDefaults()
        }

        fun onNameChange(value: String) {
            _uiState.update { it.copy(name = value) }
        }

        fun onDestinationChange(value: String) {
            _uiState.update { it.copy(destination = value) }
        }

        fun onDatesSelected(
            startEpochDay: Long,
            endEpochDay: Long,
        ) {
            _uiState.update { it.copy(startEpochDay = startEpochDay, endEpochDay = endEpochDay) }
        }

        fun onToggleForeignCurrency(enabled: Boolean) {
            _uiState.update { state ->
                if (enabled) {
                    val initial =
                        state.foreignCurrencyCode.ifBlank {
                            // Pick a currency different from home as the first suggestion.
                            SupportedCurrencies
                                .all()
                                .firstOrNull { it.code != state.homeCurrencyCode }
                                ?.code
                                ?: ""
                        }
                    state.copy(isForeignCurrency = true, foreignCurrencyCode = initial)
                } else {
                    state.copy(isForeignCurrency = false, rateText = "")
                }
            }
        }

        fun onForeignCurrencyChange(code: String) {
            if (SupportedCurrencies.byCode(code) == null) return
            _uiState.update { it.copy(foreignCurrencyCode = code) }
        }

        fun onRateTextChange(value: String) {
            // Accept comma decimals (some locales) by normalising before storing.
            val normalised = value.replace(',', '.')
            _uiState.update { it.copy(rateText = normalised) }
        }

        fun clearError() {
            _uiState.update { it.copy(errorMessage = null) }
        }

        fun save(onSuccess: () -> Unit) {
            val state = _uiState.value
            if (!state.isValid) return
            val start = state.startEpochDay ?: return
            val end = state.endEpochDay ?: return
            val foreignCode = if (state.isForeignCurrency) state.foreignCurrencyCode else null
            val foreignRate = if (state.isForeignCurrency) state.rateText.toDoubleOrNull() else null

            _uiState.update { it.copy(isSaving = true) }

            viewModelScope.launch {
                try {
                    if (isEditMode) {
                        val original = state.originalTrip ?: error("edit-mode save without loaded trip")
                        tripRepository.updateTrip(
                            original.copy(
                                name = state.name.trim(),
                                destination = state.destination.trim().ifBlank { null },
                                startDateEpochDay = start,
                                endDateEpochDay = end,
                                foreignCurrencyCode = foreignCode,
                                foreignToHomeRate = foreignRate,
                                // Snapshot fields ALWAYS pass through unchanged — see KDoc.
                            ),
                        )
                    } else {
                        tripRepository.createTrip(
                            name = state.name.trim(),
                            destination = state.destination.trim().ifBlank { null },
                            startDateEpochDay = start,
                            endDateEpochDay = end,
                            foreignCurrencyCode = foreignCode,
                            foreignToHomeRate = foreignRate,
                        )
                    }
                    onSuccess()
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = ErrorUtils.getErrorMessage(e),
                        )
                    }
                }
            }
        }

        private fun loadDefaults() {
            viewModelScope.launch {
                val home =
                    runCatching { currencyPreferenceRepository.getDefaultCurrency() }
                        .getOrDefault(homeCurrencyDefault)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        homeCurrencyCode = home,
                        startEpochDay = today,
                        endEpochDay = null,
                    )
                }
            }
        }

        private fun loadExisting() {
            viewModelScope.launch {
                try {
                    val trip = tripRepository.getTripById(tripId)
                    if (trip == null) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = UiText.StringResource(R.string.error_trip_not_found),
                            )
                        }
                        return@launch
                    }
                    val home =
                        runCatching { currencyPreferenceRepository.getDefaultCurrency() }
                            .getOrDefault(homeCurrencyDefault)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            homeCurrencyCode = home,
                            name = trip.name,
                            destination = trip.destination.orEmpty(),
                            startEpochDay = trip.startDateEpochDay,
                            endEpochDay = trip.endDateEpochDay,
                            isForeignCurrency = trip.foreignCurrencyCode != null,
                            foreignCurrencyCode = trip.foreignCurrencyCode.orEmpty(),
                            rateText = trip.foreignToHomeRate?.toPlainString().orEmpty(),
                            isConversionOrigin = trip.isConversionOrigin,
                            originalTrip = trip,
                        )
                    }
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = ErrorUtils.getErrorMessage(e),
                        )
                    }
                }
            }
        }

        private fun Double.toPlainString(): String =
            // Avoid scientific notation for small/large rates; trim trailing zero
            // so a stored 165.0 round-trips to "165" not "165.0".
            java.math
                .BigDecimal(this)
                .stripTrailingZeros()
                .toPlainString()
    }

data class CreateEditTripUiState(
    val name: String = "",
    val destination: String = "",
    val startEpochDay: Long? = null,
    val endEpochDay: Long? = null,
    val isForeignCurrency: Boolean = false,
    val foreignCurrencyCode: String = "",
    val rateText: String = "",
    val homeCurrencyCode: String = "VND",
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: UiText? = null,
    val isConversionOrigin: Boolean = false,
    val originalTrip: Trip? = null,
) {
    val nameError: UiText?
        get() =
            when {
                name.isBlank() -> null
                name.trim().length > MAX_NAME_LENGTH ->
                    UiText.StringResource(
                        R.string.error_trip_name_too_long,
                        listOf(MAX_NAME_LENGTH),
                    )
                else -> null
            }

    val dateError: UiText?
        get() {
            val s = startEpochDay
            val e = endEpochDay
            return if (s != null && e != null && e < s) {
                UiText.StringResource(R.string.error_trip_dates_invalid)
            } else {
                null
            }
        }

    val rateError: UiText?
        get() {
            if (!isForeignCurrency) return null
            if (foreignCurrencyCode.isBlank()) return null
            if (foreignCurrencyCode == homeCurrencyCode) {
                return UiText.StringResource(R.string.error_trip_fx_code_same_as_home)
            }
            if (rateText.isBlank()) return null
            val parsed = rateText.toDoubleOrNull()
            return if (parsed == null || parsed <= 0.0 || parsed >= MAX_RATE) {
                UiText.StringResource(R.string.error_trip_fx_rate_invalid)
            } else {
                null
            }
        }

    /** True when every required field is set and no validation errors are active. */
    val isValid: Boolean
        get() {
            val nameOk = name.trim().isNotBlank() && nameError == null
            val datesOk = startEpochDay != null && endEpochDay != null && dateError == null
            val fxOk =
                if (!isForeignCurrency) {
                    true
                } else {
                    foreignCurrencyCode.isNotBlank() &&
                        foreignCurrencyCode != homeCurrencyCode &&
                        rateText.isNotBlank() &&
                        rateError == null
                }
            return nameOk && datesOk && fxOk && !isSaving
        }

    val hasUnsavedChanges: Boolean
        get() {
            val o = originalTrip
            return if (o != null) {
                name.trim() != o.name ||
                    destination.trim().ifBlank { null } != o.destination ||
                    startEpochDay != o.startDateEpochDay ||
                    endEpochDay != o.endDateEpochDay ||
                    isForeignCurrency != (o.foreignCurrencyCode != null) ||
                    (isForeignCurrency && foreignCurrencyCode != o.foreignCurrencyCode.orEmpty()) ||
                    (
                        isForeignCurrency &&
                            rateText.toDoubleOrNull() != o.foreignToHomeRate
                    )
            } else {
                name.isNotBlank() ||
                    destination.isNotBlank() ||
                    isForeignCurrency
            }
        }

    companion object {
        const val MAX_NAME_LENGTH = 60

        // Bounds the rate to keep `amount × rate` well below Long.MAX_VALUE for
        // reasonable amounts (e.g. ¥1B × 1M still fits). Defensive only.
        const val MAX_RATE = 1_000_000.0
    }
}
