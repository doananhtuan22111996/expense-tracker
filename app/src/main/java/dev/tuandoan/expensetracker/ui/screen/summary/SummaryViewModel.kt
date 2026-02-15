package dev.tuandoan.expensetracker.ui.screen.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.tuandoan.expensetracker.core.util.ErrorUtils
import dev.tuandoan.expensetracker.core.util.TimeProvider
import dev.tuandoan.expensetracker.domain.model.MonthlySummary
import dev.tuandoan.expensetracker.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SummaryViewModel
    @Inject
    constructor(
        private val transactionRepository: TransactionRepository,
        private val timeProvider: TimeProvider,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(SummaryUiState())
        val uiState: StateFlow<SummaryUiState> = _uiState.asStateFlow()

        init {
            loadMonthlySummary()
        }

        fun refresh() {
            loadMonthlySummary()
        }

        private fun loadMonthlySummary() {
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isLoading = true, isError = false)

                val (startMillis, endMillis) = timeProvider.currentMonthRange()

                transactionRepository
                    .observeMonthlySummary(startMillis, endMillis)
                    .catch { e ->
                        _uiState.value =
                            _uiState.value.copy(
                                isLoading = false,
                                isError = true,
                                errorMessage = ErrorUtils.getErrorMessage(e),
                            )
                    }.collect { summary ->
                        _uiState.value =
                            _uiState.value.copy(
                                summary = summary,
                                isLoading = false,
                                isError = false,
                            )
                    }
            }
        }
    }

data class SummaryUiState(
    val summary: MonthlySummary? = null,
    val isLoading: Boolean = false,
    val isError: Boolean = false,
    val errorMessage: String? = null,
)
