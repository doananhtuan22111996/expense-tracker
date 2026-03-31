package dev.tuandoan.expensetracker.ui.screen.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.tuandoan.expensetracker.core.util.DateRangeCalculator
import dev.tuandoan.expensetracker.core.util.ErrorUtils
import dev.tuandoan.expensetracker.core.util.UiText
import dev.tuandoan.expensetracker.domain.model.BudgetStatus
import dev.tuandoan.expensetracker.domain.model.MonthlyBarPoint
import dev.tuandoan.expensetracker.domain.model.MonthlySummary
import dev.tuandoan.expensetracker.domain.model.SupportedCurrencies
import dev.tuandoan.expensetracker.domain.repository.BudgetPreferences
import dev.tuandoan.expensetracker.domain.repository.SelectedMonthRepository
import dev.tuandoan.expensetracker.domain.repository.TransactionRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.YearMonth
import javax.inject.Inject

enum class SummaryMode { MONTH, YEAR }

@HiltViewModel
class SummaryViewModel
    @Inject
    constructor(
        private val transactionRepository: TransactionRepository,
        private val selectedMonthRepository: SelectedMonthRepository,
        private val dateRangeCalculator: DateRangeCalculator,
        private val budgetPreferences: BudgetPreferences,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(SummaryUiState())
        val uiState: StateFlow<SummaryUiState> = _uiState.asStateFlow()
        private var summaryJob: Job? = null

        private var selectedYear: Int = dateRangeCalculator.currentYear()

        init {
            viewModelScope.launch {
                selectedMonthRepository.selectedMonth.collect { month ->
                    if (_uiState.value.mode == SummaryMode.MONTH) {
                        loadSummary()
                    }
                }
            }
        }

        fun refresh() {
            loadSummary()
        }

        fun setMode(mode: SummaryMode) {
            if (_uiState.value.mode == mode) return
            _uiState.value = _uiState.value.copy(mode = mode)
            if (mode == SummaryMode.YEAR) {
                selectedYear = selectedMonthRepository.selectedMonth.value.year
            }
            loadSummary()
        }

        fun goToPreviousMonth() {
            selectedMonthRepository.goToPreviousMonth()
        }

        fun goToNextMonth() {
            selectedMonthRepository.goToNextMonth()
        }

        fun goToPreviousYear() {
            selectedYear--
            loadSummary()
        }

        fun goToNextYear() {
            selectedYear++
            loadSummary()
        }

        fun setMonth(yearMonth: YearMonth) {
            selectedMonthRepository.setMonth(yearMonth)
        }

        fun setYear(year: Int) {
            selectedYear = year
            loadSummary()
        }

        fun currentSelectedMonth(): YearMonth = selectedMonthRepository.selectedMonth.value

        fun currentSelectedYear(): Int = selectedYear

        fun navigateToMonth(
            year: Int,
            month: Int,
        ) {
            val yearMonth = YearMonth.of(year, month)
            selectedMonthRepository.setMonth(yearMonth)
            _uiState.value = _uiState.value.copy(mode = SummaryMode.MONTH)
            loadSummary()
        }

        fun setBudget(
            currencyCode: String,
            amount: Long,
        ) {
            viewModelScope.launch {
                budgetPreferences.setBudget(currencyCode, amount)
                loadSummary()
            }
        }

        fun clearBudget(currencyCode: String) {
            viewModelScope.launch {
                budgetPreferences.clearBudget(currencyCode)
                loadSummary()
            }
        }

        private suspend fun buildMonthlyBarData(
            summary: MonthlySummary,
            from: Long,
            to: Long,
        ): Map<String, List<MonthlyBarPoint>> {
            val result = mutableMapOf<String, List<MonthlyBarPoint>>()
            for (currencySummary in summary.currencySummaries) {
                if (currencySummary.totalExpense > 0L) {
                    val points =
                        transactionRepository.getMonthlyExpenseTotals(
                            from,
                            to,
                            currencySummary.currencyCode,
                        )
                    result[currencySummary.currencyCode] = points
                }
            }
            return result
        }

        private suspend fun buildBudgetStatuses(summary: MonthlySummary): List<BudgetStatus> {
            val budgets = budgetPreferences.getAllBudgets().first()
            return summary.currencySummaries.mapNotNull { cs ->
                val budget = budgets[cs.currencyCode] ?: return@mapNotNull null
                val currency = SupportedCurrencies.byCode(cs.currencyCode) ?: return@mapNotNull null
                BudgetStatus(
                    currency = currency,
                    budgetAmount = budget,
                    spentAmount = cs.totalExpense,
                )
            }
        }

        private fun loadSummary() {
            summaryJob?.cancel()
            summaryJob =
                viewModelScope.launch {
                    val mode = _uiState.value.mode
                    val periodLabel =
                        if (mode == SummaryMode.YEAR) {
                            selectedYear.toString()
                        } else {
                            dateRangeCalculator.displayLabel(selectedMonthRepository.selectedMonth.value)
                        }

                    _uiState.value =
                        _uiState.value.copy(
                            isLoading = true,
                            isError = false,
                            monthLabel = periodLabel,
                        )

                    val range =
                        if (mode == SummaryMode.YEAR) {
                            dateRangeCalculator.rangeOfYear(selectedYear)
                        } else {
                            dateRangeCalculator.rangeOf(selectedMonthRepository.selectedMonth.value)
                        }

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
                            val barData =
                                if (mode == SummaryMode.YEAR) {
                                    buildMonthlyBarData(summary, range.startMillis, range.endMillisExclusive)
                                } else {
                                    emptyMap()
                                }
                            val budgetStatuses =
                                if (mode == SummaryMode.MONTH) {
                                    buildBudgetStatuses(summary)
                                } else {
                                    emptyList()
                                }
                            _uiState.value =
                                _uiState.value.copy(
                                    summary = summary,
                                    monthlyBarData = barData,
                                    budgetStatuses = budgetStatuses,
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
    val errorMessage: UiText? = null,
    val monthLabel: String = "",
    val mode: SummaryMode = SummaryMode.MONTH,
    val monthlyBarData: Map<String, List<MonthlyBarPoint>> = emptyMap(),
    val budgetStatuses: List<BudgetStatus> = emptyList(),
)
