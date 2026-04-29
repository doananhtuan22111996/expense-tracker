package dev.tuandoan.expensetracker.ui.screen.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.tuandoan.expensetracker.core.formatter.CurrencyFormatter
import dev.tuandoan.expensetracker.core.util.DateRangeCalculator
import dev.tuandoan.expensetracker.core.util.ErrorUtils
import dev.tuandoan.expensetracker.core.util.TimeProvider
import dev.tuandoan.expensetracker.core.util.UiText
import dev.tuandoan.expensetracker.data.preferences.InsightsCollapsePreferences
import dev.tuandoan.expensetracker.di.IoDispatcher
import dev.tuandoan.expensetracker.domain.insights.computeInsights
import dev.tuandoan.expensetracker.domain.model.BudgetStatus
import dev.tuandoan.expensetracker.domain.model.MonthlyBarPoint
import dev.tuandoan.expensetracker.domain.model.MonthlySummary
import dev.tuandoan.expensetracker.domain.model.SupportedCurrencies
import dev.tuandoan.expensetracker.domain.model.TransactionType
import dev.tuandoan.expensetracker.domain.repository.BudgetPreferences
import dev.tuandoan.expensetracker.domain.repository.CurrencyPreferenceRepository
import dev.tuandoan.expensetracker.domain.repository.SelectedMonthRepository
import dev.tuandoan.expensetracker.domain.repository.TransactionRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.ZoneId
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
        private val currencyPreferenceRepository: CurrencyPreferenceRepository,
        private val insightsCollapsePreferences: InsightsCollapsePreferences,
        private val currencyFormatter: CurrencyFormatter,
        private val timeProvider: TimeProvider,
        private val zoneId: ZoneId,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(SummaryUiState())
        val uiState: StateFlow<SummaryUiState> = _uiState.asStateFlow()
        private var summaryJob: Job? = null

        private var selectedYear: Int = dateRangeCalculator.currentYear()

        /**
         * Stream of [InsightsUiState] consumed by the Summary screen.
         *
         * ### Pipeline
         *
         * - Gate on [SummaryUiState.mode] so YEAR view short-circuits to
         *   [InsightsUiState.Hidden] (PRD FR-20) without touching the repo.
         * - In MONTH mode, `combine` the five inputs the engine needs:
         *   current-month expenses, previous-month expenses, default currency,
         *   its budget, and the collapse preference.
         * - `debounce(200ms)` coalesces the emission storm during bulk restore
         *   (PRD Open Question 5 default). A single restore can fire 1000+
         *   transaction writes back-to-back; without debounce we'd recompute
         *   insights once per write.
         * - `flowOn(ioDispatcher)` runs the engine off the main thread even
         *   though it's pure Kotlin — the upstream combine can be heavy when
         *   the current month has thousands of rows.
         * - `catch { emit(Error) }` honors FR-19 (data-layer failure).
         * - `stateIn(WhileSubscribed(5000L))` survives rotation; stops
         *   subscribing to Room flows when the screen is off.
         */
        @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
        val insightsState: StateFlow<InsightsUiState> =
            uiState
                .map { it.mode }
                .distinctUntilChanged()
                .flatMapLatest { mode ->
                    if (mode == SummaryMode.YEAR) {
                        flowOf(InsightsUiState.Hidden)
                    } else {
                        buildInsightsFlow()
                            .debounce(INSIGHTS_DEBOUNCE_MILLIS)
                            .catch { emit(InsightsUiState.Error) }
                            .flowOn(ioDispatcher)
                    }
                }.stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000L),
                    initialValue = InsightsUiState.Loading,
                )

        @OptIn(ExperimentalCoroutinesApi::class)
        private fun buildInsightsFlow(): Flow<InsightsUiState> =
            selectedMonthRepository.selectedMonth.flatMapLatest { currentMonth ->
                val currentRange = dateRangeCalculator.rangeOf(currentMonth)
                val previousRange = dateRangeCalculator.rangeOf(currentMonth.minusMonths(1))

                val currentExpenses =
                    transactionRepository.observeTransactions(
                        from = currentRange.startMillis,
                        to = currentRange.endMillisExclusive,
                        filterType = TransactionType.EXPENSE,
                    )
                val previousExpenses =
                    transactionRepository.observeTransactions(
                        from = previousRange.startMillis,
                        to = previousRange.endMillisExclusive,
                        filterType = TransactionType.EXPENSE,
                    )

                // Budget has to live downstream of currency: getBudget(code) takes
                // the currency code as input, so we can't put them into the same
                // combine without flatMapLatest-ing on currency first.
                // distinctUntilChanged defends against the DataStore flow re-emitting
                // the same currency code after unrelated preference writes — without
                // it, every such emit would cancel + re-subscribe the budget / collapse
                // flows and force a needless engine recompute.
                currencyPreferenceRepository
                    .observeDefaultCurrency()
                    .distinctUntilChanged()
                    .flatMapLatest { code ->
                        combine(
                            currentExpenses,
                            previousExpenses,
                            budgetPreferences.getBudget(code),
                            insightsCollapsePreferences.collapsed,
                        ) { curr, prev, budgetAmount, collapsed ->
                            val budgetStatus =
                                budgetAmount?.let { amount ->
                                    val currencyDef = SupportedCurrencies.byCode(code) ?: return@let null
                                    BudgetStatus(
                                        currency = currencyDef,
                                        budgetAmount = amount,
                                        spentAmount = curr.sumOf { it.amount },
                                    )
                                }
                            val result =
                                computeInsights(
                                    currentMonthExpenses = curr,
                                    previousMonthExpenses = prev,
                                    defaultCurrencyCode = code,
                                    budgetStatus = budgetStatus,
                                    nowMillis = timeProvider.currentTimeMillis(),
                                    zoneId = zoneId,
                                    formatter = currencyFormatter,
                                )
                            InsightsUiState.Populated(result = result, isCollapsed = collapsed)
                        }
                    }
            }

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

        /** Persist the user's tap on the Insights section header. */
        fun setInsightsCollapsed(collapsed: Boolean) {
            viewModelScope.launch {
                insightsCollapsePreferences.setCollapsed(collapsed)
            }
        }

        companion object {
            // PRD Open Question 5 default — enough to coalesce a bulk-restore
            // storm without introducing visible UI lag on manual edits.
            internal const val INSIGHTS_DEBOUNCE_MILLIS = 200L
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
