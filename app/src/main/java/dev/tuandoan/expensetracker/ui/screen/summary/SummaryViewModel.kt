package dev.tuandoan.expensetracker.ui.screen.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.tuandoan.expensetracker.core.util.DateRangeCalculator
import dev.tuandoan.expensetracker.core.util.ErrorUtils
import dev.tuandoan.expensetracker.core.util.TimeProvider
import dev.tuandoan.expensetracker.domain.model.MonthlySummary
import dev.tuandoan.expensetracker.domain.repository.TransactionRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class SummaryViewModel
    @Inject
    constructor(
        private val transactionRepository: TransactionRepository,
        private val timeProvider: TimeProvider,
        private val dateRangeCalculator: DateRangeCalculator,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(SummaryUiState())
        val uiState: StateFlow<SummaryUiState> = _uiState.asStateFlow()
        private var summaryJob: Job? = null

        private var selectedMonth: YearMonth = dateRangeCalculator.currentMonth()

        init {
            loadMonthlySummary()
        }

        fun refresh() {
            loadMonthlySummary()
        }

        fun goToPreviousMonth() {
            selectedMonth = dateRangeCalculator.previousMonth(selectedMonth)
            loadMonthlySummary()
        }

        fun goToNextMonth() {
            selectedMonth = dateRangeCalculator.nextMonth(selectedMonth)
            loadMonthlySummary()
        }

        private fun loadMonthlySummary() {
            summaryJob?.cancel()
            summaryJob =
                viewModelScope.launch {
                    _uiState.value =
                        _uiState.value.copy(
                            isLoading = true,
                            isError = false,
                            monthLabel = dateRangeCalculator.displayLabel(selectedMonth),
                        )

                    val range = dateRangeCalculator.rangeOf(selectedMonth)

                    transactionRepository
                        .observeMonthlySummary(range.startMillis, range.endMillisExclusive)
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
    val monthLabel: String = "",
)
