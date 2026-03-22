package dev.tuandoan.expensetracker.ui.screen.gold

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.tuandoan.expensetracker.core.formatter.AmountFormatter
import dev.tuandoan.expensetracker.core.util.ErrorUtils
import dev.tuandoan.expensetracker.core.util.TimeProvider
import dev.tuandoan.expensetracker.domain.model.GoldHolding
import dev.tuandoan.expensetracker.domain.model.GoldType
import dev.tuandoan.expensetracker.domain.model.GoldWeightUnit
import dev.tuandoan.expensetracker.domain.repository.CurrencyPreferenceRepository
import dev.tuandoan.expensetracker.domain.repository.GoldRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddEditGoldHoldingViewModel
    @Inject
    constructor(
        private val goldRepository: GoldRepository,
        private val currencyPreferenceRepository: CurrencyPreferenceRepository,
        private val timeProvider: TimeProvider,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val holdingId: Long = savedStateHandle.get<Long>("holdingId") ?: 0L
        val isEditMode = holdingId > 0L

        private val _uiState = MutableStateFlow(AddEditGoldHoldingUiState())
        val uiState: StateFlow<AddEditGoldHoldingUiState> = _uiState.asStateFlow()

        init {
            loadInitialData()
        }

        fun onTypeChanged(type: GoldType) {
            _uiState.value = _uiState.value.copy(type = type)
        }

        fun onWeightChanged(text: String) {
            _uiState.value = _uiState.value.copy(weightText = text)
        }

        fun onWeightUnitChanged(unit: GoldWeightUnit) {
            _uiState.value = _uiState.value.copy(weightUnit = unit)
        }

        fun onBuyPriceChanged(text: String) {
            _uiState.value = _uiState.value.copy(buyPriceText = text)
        }

        fun onDateSelected(timestamp: Long) {
            _uiState.value = _uiState.value.copy(buyDateMillis = timestamp)
        }

        fun onNoteChanged(note: String) {
            _uiState.value = _uiState.value.copy(note = note)
        }

        fun clearError() {
            _uiState.value = _uiState.value.copy(errorMessage = null)
        }

        fun saveHolding(onSuccess: () -> Unit) {
            val state = _uiState.value

            val weight = state.weightText.toDoubleOrNull()
            if (weight == null || weight <= 0) {
                _uiState.value = state.copy(errorMessage = "Please enter a valid weight")
                return
            }

            val buyPrice = AmountFormatter.parseAmount(state.buyPriceText)
            if (buyPrice == null || buyPrice <= 0) {
                _uiState.value = state.copy(errorMessage = "Please enter a valid buy price")
                return
            }

            _uiState.value = state.copy(isSaving = true, errorMessage = null)

            viewModelScope.launch {
                try {
                    if (isEditMode) {
                        val original = state.originalHolding ?: return@launch
                        goldRepository.updateHolding(
                            original.copy(
                                type = state.type,
                                weightValue = weight,
                                weightUnit = state.weightUnit,
                                buyPricePerUnit = buyPrice,
                                buyDateMillis = state.buyDateMillis,
                                note = state.note.ifBlank { null },
                                updatedAt = timeProvider.currentTimeMillis(),
                            ),
                        )
                    } else {
                        goldRepository.addHolding(
                            GoldHolding(
                                type = state.type,
                                weightValue = weight,
                                weightUnit = state.weightUnit,
                                buyPricePerUnit = buyPrice,
                                currencyCode = state.currencyCode,
                                buyDateMillis = state.buyDateMillis,
                                note = state.note.ifBlank { null },
                            ),
                        )
                    }
                    onSuccess()
                } catch (e: Exception) {
                    _uiState.value =
                        _uiState.value.copy(
                            isSaving = false,
                            errorMessage = ErrorUtils.getErrorMessage(e),
                        )
                }
            }
        }

        private fun loadInitialData() {
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isLoading = true)

                try {
                    if (isEditMode) {
                        val holding = goldRepository.getHolding(holdingId)
                        if (holding != null) {
                            _uiState.value =
                                _uiState.value.copy(
                                    originalHolding = holding,
                                    type = holding.type,
                                    weightText = holding.weightValue.toString(),
                                    weightUnit = holding.weightUnit,
                                    buyPriceText = holding.buyPricePerUnit.toString(),
                                    buyDateMillis = holding.buyDateMillis,
                                    note = holding.note ?: "",
                                    currencyCode = holding.currencyCode,
                                    isLoading = false,
                                )
                        } else {
                            _uiState.value =
                                _uiState.value.copy(
                                    isLoading = false,
                                    errorMessage = "Holding not found",
                                )
                        }
                    } else {
                        val defaultCurrency = currencyPreferenceRepository.getDefaultCurrency()
                        _uiState.value =
                            _uiState.value.copy(
                                buyDateMillis = timeProvider.currentTimeMillis(),
                                currencyCode = defaultCurrency,
                                isLoading = false,
                            )
                    }
                } catch (e: Exception) {
                    _uiState.value =
                        _uiState.value.copy(
                            isLoading = false,
                            errorMessage = ErrorUtils.getErrorMessage(e),
                        )
                }
            }
        }
    }

data class AddEditGoldHoldingUiState(
    val originalHolding: GoldHolding? = null,
    val type: GoldType = GoldType.SJC,
    val weightText: String = "",
    val weightUnit: GoldWeightUnit = GoldWeightUnit.TAEL,
    val buyPriceText: String = "",
    val buyDateMillis: Long = System.currentTimeMillis(),
    val note: String = "",
    val currencyCode: String = "VND",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
) {
    val isFormValid: Boolean
        get() {
            val weight = weightText.toDoubleOrNull()
            val buyPrice = AmountFormatter.parseAmount(buyPriceText)
            return weight != null && weight > 0 && buyPrice != null && buyPrice > 0
        }

    val hasUnsavedChanges: Boolean
        get() {
            val original = originalHolding ?: return false
            val currentWeight = weightText.toDoubleOrNull() ?: 0.0
            val currentPrice = AmountFormatter.parseAmount(buyPriceText) ?: 0L
            val currentNote = note.ifBlank { null }

            return type != original.type ||
                currentWeight != original.weightValue ||
                weightUnit != original.weightUnit ||
                currentPrice != original.buyPricePerUnit ||
                buyDateMillis != original.buyDateMillis ||
                currentNote != original.note
        }

    val isSaveEnabled: Boolean
        get() {
            if (!isFormValid) return false
            return originalHolding == null || hasUnsavedChanges
        }
}
