package dev.tuandoan.expensetracker.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.tuandoan.expensetracker.core.util.DateRangeCalculator
import dev.tuandoan.expensetracker.core.util.ErrorUtils
import dev.tuandoan.expensetracker.domain.model.Transaction
import dev.tuandoan.expensetracker.domain.model.TransactionType
import dev.tuandoan.expensetracker.domain.repository.SelectedMonthRepository
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
class HomeViewModel
    @Inject
    constructor(
        private val transactionRepository: TransactionRepository,
        private val selectedMonthRepository: SelectedMonthRepository,
        private val dateRangeCalculator: DateRangeCalculator,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(HomeUiState())
        val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
        private var transactionJob: Job? = null

        init {
            viewModelScope.launch {
                selectedMonthRepository.selectedMonth.collect { month ->
                    loadTransactions(month)
                }
            }
        }

        fun onFilterChanged(filter: TransactionType?) {
            _uiState.value = _uiState.value.copy(filter = filter)
            loadTransactions(selectedMonthRepository.selectedMonth.value)
        }

        fun goToPreviousMonth() {
            selectedMonthRepository.goToPreviousMonth()
        }

        fun goToNextMonth() {
            selectedMonthRepository.goToNextMonth()
        }

        fun setMonth(yearMonth: YearMonth) {
            selectedMonthRepository.setMonth(yearMonth)
        }

        fun currentSelectedMonth(): YearMonth = selectedMonthRepository.selectedMonth.value

        fun deleteTransaction(transactionId: Long) {
            viewModelScope.launch {
                try {
                    transactionRepository.deleteTransaction(transactionId)
                } catch (e: Exception) {
                    _uiState.value =
                        _uiState.value.copy(
                            isError = true,
                            errorMessage = ErrorUtils.getErrorMessage(e),
                        )
                }
            }
        }

        fun clearError() {
            _uiState.value = _uiState.value.copy(isError = false, errorMessage = null)
        }

        private fun loadTransactions(selectedMonth: YearMonth) {
            transactionJob?.cancel()
            transactionJob =
                viewModelScope.launch {
                    _uiState.value =
                        _uiState.value.copy(
                            isLoading = true,
                            isError = false,
                            monthLabel = dateRangeCalculator.displayLabel(selectedMonth),
                        )

                    val range = dateRangeCalculator.rangeOf(selectedMonth)

                    transactionRepository
                        .observeTransactions(
                            from = range.startMillis,
                            to = range.endMillisExclusive,
                            filterType = _uiState.value.filter,
                        ).catch { e ->
                            _uiState.value =
                                _uiState.value.copy(
                                    isLoading = false,
                                    isError = true,
                                    errorMessage = ErrorUtils.getErrorMessage(e),
                                )
                        }.collect { transactions ->
                            _uiState.value =
                                _uiState.value.copy(
                                    transactions = transactions,
                                    isLoading = false,
                                    isError = false,
                                )
                        }
                }
        }
    }

data class HomeUiState(
    val transactions: List<Transaction> = emptyList(),
    val filter: TransactionType? = null,
    val isLoading: Boolean = false,
    val isError: Boolean = false,
    val errorMessage: String? = null,
    val monthLabel: String = "",
)
