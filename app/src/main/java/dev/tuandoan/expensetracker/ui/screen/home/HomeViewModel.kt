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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
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
        private val searchQueryFlow = MutableStateFlow("")
        private val filterFlow = MutableStateFlow<TransactionType?>(null)

        init {
            @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
            viewModelScope.launch {
                combine(
                    selectedMonthRepository.selectedMonth,
                    searchQueryFlow.debounce(SEARCH_DEBOUNCE_MS).distinctUntilChanged(),
                    filterFlow,
                ) { month, query, filter ->
                    Triple(month, query.trim(), filter)
                }.flatMapLatest { (month, query, filter) ->
                    _uiState.value =
                        _uiState.value.copy(
                            isLoading = true,
                            isError = false,
                            monthLabel = dateRangeCalculator.displayLabel(month),
                        )

                    val range = dateRangeCalculator.rangeOf(month)

                    if (query.isEmpty()) {
                        transactionRepository.observeTransactions(
                            from = range.startMillis,
                            to = range.endMillisExclusive,
                            filterType = filter,
                        )
                    } else {
                        transactionRepository.searchTransactions(
                            from = range.startMillis,
                            to = range.endMillisExclusive,
                            query = query,
                            filterType = filter,
                        )
                    }
                }.catch { e ->
                    _uiState.value =
                        _uiState.value.copy(
                            isLoading = false,
                            isError = true,
                            errorMessage = ErrorUtils.getErrorMessage(e),
                        )
                }.collectLatest { transactions ->
                    _uiState.value =
                        _uiState.value.copy(
                            transactions = transactions,
                            isLoading = false,
                            isError = false,
                        )
                }
            }
        }

        fun onSearchQueryChanged(query: String) {
            _uiState.value = _uiState.value.copy(searchQuery = query)
            searchQueryFlow.value = query
        }

        fun clearSearch() {
            onSearchQueryChanged("")
        }

        fun onFilterChanged(filter: TransactionType?) {
            _uiState.value = _uiState.value.copy(filter = filter)
            filterFlow.value = filter
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

        companion object {
            const val SEARCH_DEBOUNCE_MS = 300L
        }
    }

data class HomeUiState(
    val transactions: List<Transaction> = emptyList(),
    val filter: TransactionType? = null,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val isError: Boolean = false,
    val errorMessage: String? = null,
    val monthLabel: String = "",
)
