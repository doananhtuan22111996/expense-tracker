package dev.tuandoan.expensetracker.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.tuandoan.expensetracker.core.util.DateRangeCalculator
import dev.tuandoan.expensetracker.core.util.ErrorUtils
import dev.tuandoan.expensetracker.core.util.UiText
import dev.tuandoan.expensetracker.domain.model.Category
import dev.tuandoan.expensetracker.domain.model.SearchScope
import dev.tuandoan.expensetracker.domain.model.Transaction
import dev.tuandoan.expensetracker.domain.model.TransactionType
import dev.tuandoan.expensetracker.domain.repository.CategoryRepository
import dev.tuandoan.expensetracker.domain.repository.SelectedMonthRepository
import dev.tuandoan.expensetracker.domain.repository.TransactionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        private val transactionRepository: TransactionRepository,
        private val selectedMonthRepository: SelectedMonthRepository,
        private val categoryRepository: CategoryRepository,
        private val dateRangeCalculator: DateRangeCalculator,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(HomeUiState())
        val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
        private val searchQueryFlow = MutableStateFlow("")
        private val filterFlow = MutableStateFlow<TransactionType?>(null)
        private val searchScopeFlow = MutableStateFlow(SearchScope.CURRENT_MONTH)
        private val selectedCategoryIdFlow = MutableStateFlow<Long?>(null)
        private val retryTrigger = MutableStateFlow(0)

        val expenseCategories: StateFlow<List<Category>> =
            categoryRepository
                .observeCategories(TransactionType.EXPENSE)
                .catch { emit(emptyList()) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

        val incomeCategories: StateFlow<List<Category>> =
            categoryRepository
                .observeCategories(TransactionType.INCOME)
                .catch { emit(emptyList()) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

        init {
            @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
            viewModelScope.launch {
                combine(
                    selectedMonthRepository.selectedMonth,
                    searchQueryFlow.debounce(SEARCH_DEBOUNCE_MS).distinctUntilChanged(),
                    filterFlow,
                    searchScopeFlow,
                    selectedCategoryIdFlow,
                ) { month, query, filter, scope, categoryId ->
                    FilterParams(month, query.trim(), filter, scope, categoryId)
                }.combine(retryTrigger) { params, _ -> params }
                    .flatMapLatest { params ->
                        val currentState = _uiState.value
                        _uiState.value =
                            currentState.copy(
                                isLoading = true,
                                isError = false,
                                monthLabel = dateRangeCalculator.displayLabel(params.month),
                            )

                        val range = dateRangeCalculator.rangeOf(params.month)
                        val dateRangeFrom = currentState.dateRangeStart?.let { dateToMillis(it) }
                        val dateRangeTo = currentState.dateRangeEnd?.let { dateToEndOfDayMillis(it) }
                        val hasDateRange = dateRangeFrom != null
                        val hasAdvancedFilters =
                            params.scope == SearchScope.ALL_MONTHS || params.categoryId != null || hasDateRange

                        val sourceFlow =
                            if (hasAdvancedFilters) {
                                val from =
                                    when {
                                        hasDateRange -> dateRangeFrom
                                        params.scope == SearchScope.ALL_MONTHS -> null
                                        else -> range.startMillis
                                    }
                                val to =
                                    when {
                                        hasDateRange -> dateRangeTo
                                        params.scope == SearchScope.ALL_MONTHS -> null
                                        else -> range.endMillisExclusive
                                    }
                                transactionRepository.searchTransactionsAdvanced(
                                    from = from,
                                    to = to,
                                    query = params.query,
                                    filterType = params.filter,
                                    categoryId = params.categoryId,
                                )
                            } else if (params.query.isEmpty()) {
                                transactionRepository.observeTransactions(
                                    from = range.startMillis,
                                    to = range.endMillisExclusive,
                                    filterType = params.filter,
                                )
                            } else {
                                transactionRepository.searchTransactions(
                                    from = range.startMillis,
                                    to = range.endMillisExclusive,
                                    query = params.query,
                                    filterType = params.filter,
                                )
                            }
                        sourceFlow.catch { e ->
                            _uiState.value =
                                _uiState.value.copy(
                                    isLoading = false,
                                    isError = true,
                                    errorMessage = ErrorUtils.getErrorMessage(e),
                                )
                        }
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

        fun onSearchScopeChanged(scope: SearchScope) {
            _uiState.value = _uiState.value.copy(searchScope = scope)
            searchScopeFlow.value = scope
        }

        fun onCategorySelected(categoryId: Long?) {
            selectedCategoryIdFlow.value = categoryId
            if (categoryId == null) {
                _uiState.value =
                    _uiState.value.copy(
                        selectedCategoryId = null,
                        selectedCategoryName = null,
                    )
            } else {
                _uiState.value = _uiState.value.copy(selectedCategoryId = categoryId)
                viewModelScope.launch {
                    val category = categoryRepository.getCategory(categoryId)
                    _uiState.value = _uiState.value.copy(selectedCategoryName = category?.name)
                }
            }
        }

        fun onDateRangeSelected(
            startMillis: Long,
            endMillis: Long,
        ) {
            val zone = ZoneId.systemDefault()
            val startDate = Instant.ofEpochMilli(startMillis).atZone(zone).toLocalDate()
            val endDate = Instant.ofEpochMilli(endMillis).atZone(zone).toLocalDate()
            _uiState.value =
                _uiState.value.copy(
                    dateRangeStart = startDate,
                    dateRangeEnd = endDate,
                )
            // Date range implies ALL_MONTHS scope — triggers re-query via searchScopeFlow
            onSearchScopeChanged(SearchScope.ALL_MONTHS)
        }

        fun clearDateRange() {
            _uiState.value =
                _uiState.value.copy(
                    dateRangeStart = null,
                    dateRangeEnd = null,
                )
            // Trigger re-query
            retryTrigger.value++
        }

        fun clearAllFilters() {
            onFilterChanged(null)
            onSearchScopeChanged(SearchScope.CURRENT_MONTH)
            onCategorySelected(null)
            clearDateRange()
            clearSearch()
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

        fun deleteTransaction(transaction: Transaction) {
            viewModelScope.launch {
                try {
                    _uiState.value = _uiState.value.copy(lastDeletedTransaction = transaction)
                    transactionRepository.deleteTransaction(transaction.id)
                } catch (e: Exception) {
                    _uiState.value =
                        _uiState.value.copy(
                            lastDeletedTransaction = null,
                            isError = true,
                            errorMessage = ErrorUtils.getErrorMessage(e),
                        )
                }
            }
        }

        fun undoDelete() {
            val transaction = _uiState.value.lastDeletedTransaction ?: return
            viewModelScope.launch {
                try {
                    transactionRepository.addTransaction(
                        type = transaction.type,
                        amount = transaction.amount,
                        categoryId = transaction.category.id,
                        note = transaction.note,
                        timestamp = transaction.timestamp,
                        currencyCode = transaction.currencyCode,
                    )
                    _uiState.value = _uiState.value.copy(lastDeletedTransaction = null)
                } catch (e: Exception) {
                    _uiState.value =
                        _uiState.value.copy(
                            isError = true,
                            errorMessage = ErrorUtils.getErrorMessage(e),
                        )
                }
            }
        }

        fun clearLastDeleted() {
            _uiState.value = _uiState.value.copy(lastDeletedTransaction = null)
        }

        fun clearError() {
            _uiState.value = _uiState.value.copy(isError = false, errorMessage = null)
        }

        fun retry() {
            clearError()
            retryTrigger.value++
        }

        private fun dateToMillis(date: LocalDate): Long =
            date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        private fun dateToEndOfDayMillis(date: LocalDate): Long =
            date
                .plusDays(1)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()

        private data class FilterParams(
            val month: YearMonth,
            val query: String,
            val filter: TransactionType?,
            val scope: SearchScope,
            val categoryId: Long?,
        )

        companion object {
            const val SEARCH_DEBOUNCE_MS = 300L
            const val STOP_TIMEOUT_MS = 5000L
        }
    }

data class HomeUiState(
    val transactions: List<Transaction> = emptyList(),
    val filter: TransactionType? = null,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val isError: Boolean = false,
    val errorMessage: UiText? = null,
    val monthLabel: String = "",
    val lastDeletedTransaction: Transaction? = null,
    val searchScope: SearchScope = SearchScope.CURRENT_MONTH,
    val selectedCategoryId: Long? = null,
    val selectedCategoryName: String? = null,
    val dateRangeStart: LocalDate? = null,
    val dateRangeEnd: LocalDate? = null,
) {
    val activeFilterCount: Int
        get() {
            var count = 0
            if (filter != null) count++
            if (searchScope == SearchScope.ALL_MONTHS) count++
            if (selectedCategoryId != null) count++
            if (dateRangeStart != null) count++
            return count
        }

    val hasActiveFilters: Boolean
        get() = activeFilterCount > 0
}
