package dev.tuandoan.expensetracker.ui.screen.summary

import app.cash.turbine.test
import dev.tuandoan.expensetracker.core.formatter.CurrencyFormatter
import dev.tuandoan.expensetracker.core.util.DateRangeCalculator
import dev.tuandoan.expensetracker.data.preferences.InsightsCollapsePreferences
import dev.tuandoan.expensetracker.domain.insights.InsightRow
import dev.tuandoan.expensetracker.domain.model.BudgetStatusLevel
import dev.tuandoan.expensetracker.domain.model.Category
import dev.tuandoan.expensetracker.domain.model.CategoryWithCount
import dev.tuandoan.expensetracker.domain.model.CurrencyMonthlySummary
import dev.tuandoan.expensetracker.domain.model.MonthlyBarPoint
import dev.tuandoan.expensetracker.domain.model.MonthlySummary
import dev.tuandoan.expensetracker.domain.model.Transaction
import dev.tuandoan.expensetracker.domain.model.TransactionType
import dev.tuandoan.expensetracker.domain.repository.BudgetPreferences
import dev.tuandoan.expensetracker.domain.repository.CategoryRepository
import dev.tuandoan.expensetracker.domain.repository.TransactionRepository
import dev.tuandoan.expensetracker.testutil.FakeCurrencyPreferenceRepository
import dev.tuandoan.expensetracker.testutil.FakeSelectedMonthRepository
import dev.tuandoan.expensetracker.testutil.FakeTimeProvider
import dev.tuandoan.expensetracker.testutil.MainDispatcherRule
import dev.tuandoan.expensetracker.testutil.TestData
import dev.tuandoan.expensetracker.ui.screen.home.HomeViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class SummaryViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeRepository: FakeTransactionRepository
    private lateinit var fakeSelectedMonth: FakeSelectedMonthRepository
    private lateinit var fakeBudgetPreferences: FakeBudgetPreferences
    private lateinit var fakeCurrencyPreferences: FakeCurrencyPreferenceRepository
    private lateinit var fakeInsightsCollapse: FakeInsightsCollapsePreferences
    private lateinit var fakeTimeProvider: FakeTimeProvider
    private lateinit var dateRangeCalculator: DateRangeCalculator

    private val fixedZone: ZoneId = ZoneId.of("UTC")
    private val fixedClock: Clock = Clock.fixed(Instant.parse("2026-03-15T12:00:00Z"), fixedZone)

    @Before
    fun setup() {
        fakeRepository = FakeTransactionRepository()
        fakeSelectedMonth = FakeSelectedMonthRepository()
        fakeBudgetPreferences = FakeBudgetPreferences()
        fakeCurrencyPreferences = FakeCurrencyPreferenceRepository(initialCurrency = "VND")
        fakeInsightsCollapse = FakeInsightsCollapsePreferences()
        // 2026-03-15T12:00:00Z matches the fixedClock used by DateRangeCalculator.
        fakeTimeProvider = FakeTimeProvider(currentMillis = 1773921600000L)
        dateRangeCalculator = DateRangeCalculator(fixedClock, fixedZone)
    }

    private fun createViewModel(): SummaryViewModel =
        SummaryViewModel(
            transactionRepository = fakeRepository,
            selectedMonthRepository = fakeSelectedMonth,
            dateRangeCalculator = dateRangeCalculator,
            budgetPreferences = fakeBudgetPreferences,
            currencyPreferenceRepository = fakeCurrencyPreferences,
            insightsCollapsePreferences = fakeInsightsCollapse,
            currencyFormatter = fakeFormatter,
            timeProvider = fakeTimeProvider,
            zoneId = fixedZone,
            ioDispatcher = mainDispatcherRule.testDispatcher,
        )

    private val fakeFormatter =
        object : CurrencyFormatter {
            override fun format(
                amountMinor: Long,
                currencyCode: String,
            ): String = "$amountMinor $currencyCode"

            override fun formatWithSign(
                amountMinor: Long,
                currencyCode: String,
                isIncome: Boolean,
            ): String = (if (isIncome) "+" else "-") + format(amountMinor, currencyCode)

            override fun formatBareAmount(
                amountMinor: Long,
                currencyCode: String,
            ): String = amountMinor.toString()
        }

    @Test
    fun init_loadsSummary() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeRepository.summaryToEmit = TestData.sampleMonthlySummary

            val viewModel = createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertNotNull(state.summary)
            assertFalse(state.summary!!.isEmpty)
            assertEquals(1, state.summary!!.currencySummaries.size)

            val vnd = state.summary!!.currencySummaries[0]
            assertEquals("VND", vnd.currencyCode)
            assertEquals(200000L, vnd.totalExpense)
            assertEquals(10000000L, vnd.totalIncome)
            assertEquals(9800000L, vnd.balance)
            assertFalse(state.isLoading)
            assertFalse(state.isError)
        }

    @Test
    fun init_error_setsErrorState() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeRepository.shouldThrow = true

            val viewModel = createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state.isError)
            assertFalse(state.isLoading)
            assertNotNull(state.errorMessage)
        }

    @Test
    fun refresh_reloadsSummary() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeRepository.summaryToEmit = TestData.sampleMonthlySummary
            val viewModel = createViewModel()
            advanceUntilIdle()

            val updatedSummary =
                MonthlySummary(
                    currencySummaries =
                        listOf(
                            CurrencyMonthlySummary(
                                currencyCode = "VND",
                                totalExpense = 500000L,
                                totalIncome = 10000000L,
                                balance = 9500000L,
                                topExpenseCategories = emptyList(),
                            ),
                        ),
                )
            fakeRepository.summaryToEmit = updatedSummary

            viewModel.refresh()
            advanceUntilIdle()

            val vnd =
                viewModel.uiState.value.summary!!
                    .currencySummaries[0]
            assertEquals(500000L, vnd.totalExpense)
        }

    @Test
    fun init_usesDateRangeCalculatorForCurrentMonth() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeRepository.summaryToEmit = TestData.sampleMonthlySummary

            createViewModel()
            advanceUntilIdle()

            // March 2026 UTC
            assertEquals(1772323200000L, fakeRepository.lastFrom)
            assertEquals(1775001600000L, fakeRepository.lastTo)
        }

    @Test
    fun init_emptySummary_showsIsEmpty() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeRepository.summaryToEmit =
                MonthlySummary(
                    currencySummaries = emptyList(),
                )

            val viewModel = createViewModel()
            advanceUntilIdle()

            val summary = viewModel.uiState.value.summary!!
            assertTrue(summary.isEmpty)
            assertTrue(summary.currencySummaries.isEmpty())
        }

    @Test
    fun init_multiCurrencySummary_loadsCorrectly() =
        runTest(mainDispatcherRule.testDispatcher) {
            val multiCurrencySummary =
                MonthlySummary(
                    currencySummaries =
                        listOf(
                            CurrencyMonthlySummary(
                                currencyCode = "VND",
                                totalExpense = 200000L,
                                totalIncome = 10000000L,
                                balance = 9800000L,
                                topExpenseCategories = emptyList(),
                            ),
                            CurrencyMonthlySummary(
                                currencyCode = "USD",
                                totalExpense = 5000L,
                                totalIncome = 300000L,
                                balance = 295000L,
                                topExpenseCategories = emptyList(),
                            ),
                        ),
                )
            fakeRepository.summaryToEmit = multiCurrencySummary

            val viewModel = createViewModel()
            advanceUntilIdle()

            val summary = viewModel.uiState.value.summary!!
            assertFalse(summary.isEmpty)
            assertEquals(2, summary.currencySummaries.size)
            assertEquals("VND", summary.currencySummaries[0].currencyCode)
            assertEquals("USD", summary.currencySummaries[1].currencyCode)
        }

    @Test
    fun initialState_isDefault() {
        val state = SummaryUiState()
        assertNull(state.summary)
        assertFalse(state.isLoading)
        assertFalse(state.isError)
        assertNull(state.errorMessage)
        assertEquals("", state.monthLabel)
    }

    @Test
    fun init_monthLabel_showsCurrentMonth() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeRepository.summaryToEmit = TestData.sampleMonthlySummary

            val viewModel = createViewModel()
            advanceUntilIdle()

            assertEquals("Mar 2026", viewModel.uiState.value.monthLabel)
        }

    // --- Month navigation ---

    @Test
    fun goToPreviousMonth_queriesFebruary() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeRepository.summaryToEmit = TestData.sampleMonthlySummary
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.goToPreviousMonth()
            advanceUntilIdle()

            assertEquals("Feb 2026", viewModel.uiState.value.monthLabel)
            assertEquals(1769904000000L, fakeRepository.lastFrom)
            assertEquals(1772323200000L, fakeRepository.lastTo)
        }

    @Test
    fun goToNextMonth_queriesApril() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeRepository.summaryToEmit = TestData.sampleMonthlySummary
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.goToNextMonth()
            advanceUntilIdle()

            assertEquals("Apr 2026", viewModel.uiState.value.monthLabel)
        }

    @Test
    fun goToPreviousMonth_crossesYearBoundary() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeSelectedMonth.setMonth(YearMonth.of(2026, 1))
            fakeRepository.summaryToEmit = TestData.sampleMonthlySummary

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.goToPreviousMonth()
            advanceUntilIdle()

            assertEquals("Dec 2025", viewModel.uiState.value.monthLabel)
        }

    @Test
    fun goToNextMonth_crossesYearBoundary() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeSelectedMonth.setMonth(YearMonth.of(2025, 12))
            fakeRepository.summaryToEmit = TestData.sampleMonthlySummary

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.goToNextMonth()
            advanceUntilIdle()

            assertEquals("Jan 2026", viewModel.uiState.value.monthLabel)
        }

    // --- Shared month / setMonth ---

    @Test
    fun setMonth_jumpsToSelectedMonth() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeRepository.summaryToEmit = TestData.sampleMonthlySummary
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.setMonth(YearMonth.of(2024, 6))
            advanceUntilIdle()

            assertEquals("Jun 2024", viewModel.uiState.value.monthLabel)
        }

    @Test
    fun currentSelectedMonth_returnsSharedState() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeRepository.summaryToEmit = TestData.sampleMonthlySummary
            val viewModel = createViewModel()
            advanceUntilIdle()

            assertEquals(YearMonth.of(2026, 3), viewModel.currentSelectedMonth())

            viewModel.setMonth(YearMonth.of(2025, 1))
            assertEquals(YearMonth.of(2025, 1), viewModel.currentSelectedMonth())
        }

    @Test
    fun externalMonthChange_triggersReload() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeRepository.summaryToEmit = TestData.sampleMonthlySummary
            val viewModel = createViewModel()
            advanceUntilIdle()

            assertEquals("Mar 2026", viewModel.uiState.value.monthLabel)

            // External change (e.g., from Home screen via shared repo)
            fakeSelectedMonth.setMonth(YearMonth.of(2025, 11))
            advanceUntilIdle()

            assertEquals("Nov 2025", viewModel.uiState.value.monthLabel)
        }

    // --- Year mode ---

    @Test
    fun setMode_year_showsYearLabel() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeRepository.summaryToEmit = TestData.sampleMonthlySummary
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.setMode(SummaryMode.YEAR)
            advanceUntilIdle()

            assertEquals(SummaryMode.YEAR, viewModel.uiState.value.mode)
            assertEquals("2026", viewModel.uiState.value.monthLabel)
        }

    @Test
    fun setMode_year_queriesFullYearRange() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeRepository.summaryToEmit = TestData.sampleMonthlySummary
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.setMode(SummaryMode.YEAR)
            advanceUntilIdle()

            // 2026-01-01T00:00:00Z to 2027-01-01T00:00:00Z
            assertEquals(1767225600000L, fakeRepository.lastFrom)
            assertEquals(1798761600000L, fakeRepository.lastTo)
        }

    @Test
    fun goToPreviousYear_shows2025() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeRepository.summaryToEmit = TestData.sampleMonthlySummary
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.setMode(SummaryMode.YEAR)
            advanceUntilIdle()

            viewModel.goToPreviousYear()
            advanceUntilIdle()

            assertEquals("2025", viewModel.uiState.value.monthLabel)
            assertEquals(2025, viewModel.currentSelectedYear())
        }

    @Test
    fun goToNextYear_shows2027() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeRepository.summaryToEmit = TestData.sampleMonthlySummary
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.setMode(SummaryMode.YEAR)
            advanceUntilIdle()

            viewModel.goToNextYear()
            advanceUntilIdle()

            assertEquals("2027", viewModel.uiState.value.monthLabel)
            assertEquals(2027, viewModel.currentSelectedYear())
        }

    @Test
    fun setMode_backToMonth_restoresMonthLabel() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeRepository.summaryToEmit = TestData.sampleMonthlySummary
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.setMode(SummaryMode.YEAR)
            advanceUntilIdle()
            assertEquals(SummaryMode.YEAR, viewModel.uiState.value.mode)

            viewModel.setMode(SummaryMode.MONTH)
            advanceUntilIdle()

            assertEquals(SummaryMode.MONTH, viewModel.uiState.value.mode)
            assertEquals("Mar 2026", viewModel.uiState.value.monthLabel)
        }

    @Test
    fun setMode_sameMode_doesNotReload() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeRepository.summaryToEmit = TestData.sampleMonthlySummary
            val viewModel = createViewModel()
            advanceUntilIdle()

            val firstFrom = fakeRepository.lastFrom

            // Setting the same mode again should be a no-op
            viewModel.setMode(SummaryMode.MONTH)
            advanceUntilIdle()

            assertEquals(firstFrom, fakeRepository.lastFrom)
        }

    @Test
    fun yearMode_externalMonthChange_doesNotReload() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeRepository.summaryToEmit = TestData.sampleMonthlySummary
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.setMode(SummaryMode.YEAR)
            advanceUntilIdle()
            val yearLabel = viewModel.uiState.value.monthLabel

            // External month change should not affect year mode
            fakeSelectedMonth.setMonth(YearMonth.of(2025, 6))
            advanceUntilIdle()

            assertEquals(SummaryMode.YEAR, viewModel.uiState.value.mode)
            assertEquals(yearLabel, viewModel.uiState.value.monthLabel)
        }

    @Test
    fun initialState_modeIsMonth() {
        val state = SummaryUiState()
        assertEquals(SummaryMode.MONTH, state.mode)
        assertTrue(state.budgetStatuses.isEmpty())
    }

    @Test
    fun yearMode_monthlyBarData_populatedWith12Months() =
        runTest(mainDispatcherRule.testDispatcher) {
            val barPoints =
                (1..12).map { month ->
                    MonthlyBarPoint(
                        month = month,
                        totalExpense = if (month == 3) 500000L else 0L,
                    )
                }
            fakeRepository.monthlyBarPointsToEmit = mapOf("VND" to barPoints)
            fakeRepository.summaryToEmit = TestData.sampleMonthlySummary

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.setMode(SummaryMode.YEAR)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state.monthlyBarData.containsKey("VND"))
            val vndPoints = state.monthlyBarData["VND"]!!
            assertEquals(12, vndPoints.size)
            assertEquals(500000L, vndPoints[2].totalExpense)
            assertEquals(0L, vndPoints[0].totalExpense)
        }

    @Test
    fun monthMode_monthlyBarData_isEmpty() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeRepository.summaryToEmit = TestData.sampleMonthlySummary

            val viewModel = createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(SummaryMode.MONTH, state.mode)
            assertTrue(state.monthlyBarData.isEmpty())
        }

    // --- Shared period consistency ---

    @Test
    fun sharedMonth_homeAndSummary_seeConsistentState() =
        runTest(mainDispatcherRule.testDispatcher) {
            // Both view models share the same FakeSelectedMonthRepository
            val homeVm =
                HomeViewModel(
                    FakeTransactionRepository().also { it.summaryToEmit = TestData.sampleMonthlySummary },
                    fakeSelectedMonth,
                    StubCategoryRepository(),
                    dateRangeCalculator,
                    dev.tuandoan.expensetracker.testutil
                        .FakeSearchFilterPreferences(),
                )
            val summaryVm = createViewModel()
            advanceUntilIdle()

            // Both start at March 2026
            assertEquals("Mar 2026", homeVm.uiState.value.monthLabel)
            assertEquals("Mar 2026", summaryVm.uiState.value.monthLabel)

            // Home navigates to February
            homeVm.goToPreviousMonth()
            advanceUntilIdle()

            // Both should now show February
            assertEquals("Feb 2026", homeVm.uiState.value.monthLabel)
            assertEquals("Feb 2026", summaryVm.uiState.value.monthLabel)
        }

    @Test
    fun monthMode_budgetStatuses_populatedForCurrenciesWithBudget() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeRepository.summaryToEmit = TestData.sampleMonthlySummary
            fakeBudgetPreferences.setBudget("VND", 500000L)

            val viewModel = createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(1, state.budgetStatuses.size)
            val vndBudget = state.budgetStatuses[0]
            assertEquals("VND", vndBudget.currency.code)
            assertEquals(500000L, vndBudget.budgetAmount)
            assertEquals(200000L, vndBudget.spentAmount)
            assertEquals(BudgetStatusLevel.OK, vndBudget.status)
        }

    @Test
    fun monthMode_noBudgetSet_budgetStatusesEmpty() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeRepository.summaryToEmit = TestData.sampleMonthlySummary

            val viewModel = createViewModel()
            advanceUntilIdle()

            assertTrue(
                viewModel.uiState.value.budgetStatuses
                    .isEmpty(),
            )
        }

    @Test
    fun navigateToMonth_setsMonthAndSwitchesToMonthMode() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeRepository.summaryToEmit = TestData.sampleMonthlySummary
            val viewModel = createViewModel()
            advanceUntilIdle()

            // Switch to YEAR mode first
            viewModel.setMode(SummaryMode.YEAR)
            advanceUntilIdle()
            assertEquals(SummaryMode.YEAR, viewModel.uiState.value.mode)

            // Navigate to a specific month
            viewModel.navigateToMonth(2025, 3)
            advanceUntilIdle()

            assertEquals(SummaryMode.MONTH, viewModel.uiState.value.mode)
            assertEquals(YearMonth.of(2025, 3), viewModel.currentSelectedMonth())
            assertEquals("Mar 2025", viewModel.uiState.value.monthLabel)
        }

    @Test
    fun yearMode_budgetStatuses_alwaysEmpty() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeRepository.summaryToEmit = TestData.sampleMonthlySummary
            fakeBudgetPreferences.setBudget("VND", 500000L)

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.setMode(SummaryMode.YEAR)
            advanceUntilIdle()

            assertTrue(
                viewModel.uiState.value.budgetStatuses
                    .isEmpty(),
            )
        }

    private class FakeBudgetPreferences : BudgetPreferences {
        private val budgets = MutableStateFlow<Map<String, Long>>(emptyMap())

        override fun getBudget(currencyCode: String): Flow<Long?> = budgets.map { it[currencyCode] }

        override suspend fun setBudget(
            currencyCode: String,
            amount: Long,
        ) {
            budgets.value = budgets.value + (currencyCode to amount)
        }

        override suspend fun clearBudget(currencyCode: String) {
            budgets.value = budgets.value - currencyCode
        }

        override fun getAllBudgets(): Flow<Map<String, Long>> = budgets
    }

    private class FakeTransactionRepository : TransactionRepository {
        var summaryToEmit: MonthlySummary = TestData.sampleMonthlySummary
        var shouldThrow = false
        var lastFrom: Long? = null
        var lastTo: Long? = null

        // Insights tests inject transactions per range; key = "from-to".
        val expensesByRange: MutableMap<Pair<Long, Long>, MutableStateFlow<List<Transaction>>> =
            mutableMapOf()
        var observeTransactionsShouldThrow = false

        override fun observeTransactions(
            from: Long,
            to: Long,
            filterType: TransactionType?,
        ): Flow<List<Transaction>> {
            if (observeTransactionsShouldThrow) {
                return flow { throw RuntimeException("observeTransactions failed") }
            }
            return expensesByRange.getOrPut(from to to) { MutableStateFlow(emptyList()) }
        }

        fun setExpenses(
            from: Long,
            to: Long,
            transactions: List<Transaction>,
        ) {
            expensesByRange.getOrPut(from to to) { MutableStateFlow(emptyList()) }.value = transactions
        }

        override suspend fun addTransaction(
            type: TransactionType,
            amount: Long,
            categoryId: Long,
            note: String?,
            timestamp: Long,
            currencyCode: String,
        ): Long = 1L

        override suspend fun updateTransaction(transaction: Transaction) {}

        override suspend fun deleteTransaction(id: Long) {}

        override suspend fun getTransaction(id: Long): Transaction? = null

        override fun observeMonthlySummary(
            from: Long,
            to: Long,
        ): Flow<MonthlySummary> {
            lastFrom = from
            lastTo = to
            return if (shouldThrow) {
                flow { throw RuntimeException("Test error") }
            } else {
                flow { emit(summaryToEmit) }
            }
        }

        override fun searchTransactions(
            from: Long,
            to: Long,
            query: String,
            filterType: TransactionType?,
        ): Flow<List<Transaction>> = flow { emit(emptyList()) }

        override suspend fun getMonthlyExpenseTotals(
            from: Long,
            to: Long,
            currencyCode: String,
        ): List<MonthlyBarPoint> =
            monthlyBarPointsToEmit[currencyCode]
                ?: (1..12).map { MonthlyBarPoint(month = it, totalExpense = 0L) }

        var monthlyBarPointsToEmit: Map<String, List<MonthlyBarPoint>> = emptyMap()

        override fun searchTransactionsAdvanced(
            from: Long?,
            to: Long?,
            query: String,
            filterType: TransactionType?,
            categoryId: Long?,
        ): Flow<List<Transaction>> = MutableStateFlow(emptyList())
    }

    private class FakeInsightsCollapsePreferences : InsightsCollapsePreferences {
        private val state = MutableStateFlow(false)

        override val collapsed: Flow<Boolean> = state

        override suspend fun setCollapsed(value: Boolean) {
            state.value = value
        }
    }

    // --- Task 2.8 / 2.16: insightsState pipeline ---

    private val marchStart = 1772323200000L // 2026-03-01T00:00:00Z
    private val marchEnd = 1775001600000L // 2026-04-01T00:00:00Z
    private val februaryStart = 1769904000000L // 2026-02-01T00:00:00Z
    private val februaryEnd = marchStart

    private val foodCategory =
        Category(
            id = 1L,
            name = "Food",
            type = TransactionType.EXPENSE,
            iconKey = "restaurant",
            colorKey = "red",
            isDefault = true,
        )

    private fun vndExpense(
        id: Long,
        amount: Long,
        timestamp: Long,
    ): Transaction =
        Transaction(
            id = id,
            type = TransactionType.EXPENSE,
            amount = amount,
            currencyCode = "VND",
            category = foodCategory,
            note = null,
            timestamp = timestamp,
            createdAt = timestamp,
            updatedAt = timestamp,
        )

    @Test
    fun insightsState_happyPath_emitsPopulatedWithPaceAndMover() =
        runTest(mainDispatcherRule.testDispatcher) {
            // Current month (March 2026) has a Food txn pair; previous month
            // (February 2026) has smaller Food pair. With a budget set, the
            // engine should surface BiggestMover + DailyPace.
            val marchMid = 1773921600000L // 2026-03-15
            val febMid = 1771588800000L // 2026-02-20
            fakeRepository.setExpenses(
                from = marchStart,
                to = marchEnd,
                transactions =
                    listOf(
                        vndExpense(id = 1, amount = 300_000L, timestamp = marchMid),
                        vndExpense(id = 2, amount = 300_000L, timestamp = marchMid),
                    ),
            )
            fakeRepository.setExpenses(
                from = februaryStart,
                to = februaryEnd,
                transactions =
                    listOf(
                        vndExpense(id = 3, amount = 100_000L, timestamp = febMid),
                        vndExpense(id = 4, amount = 100_000L, timestamp = febMid),
                    ),
            )
            fakeBudgetPreferences.setBudget("VND", 2_000_000L)
            fakeRepository.summaryToEmit = TestData.sampleMonthlySummary

            val viewModel = createViewModel()

            viewModel.insightsState.test {
                // Initial Loading → eventually Populated after debounce settles.
                assertEquals(InsightsUiState.Loading, awaitItem())
                advanceTimeBy(250) // clear the 200ms debounce window
                val populated = awaitItem() as InsightsUiState.Populated
                val rows = populated.result.rows
                assertTrue(rows.any { it is InsightRow.BiggestMover })
                assertTrue(rows.any { it is InsightRow.DailyPace })
                assertFalse(populated.isCollapsed)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun insightsState_collapsePreferenceFlip_propagatesAsIsCollapsedTrue() =
        runTest(mainDispatcherRule.testDispatcher) {
            val marchMid = 1773921600000L
            fakeRepository.setExpenses(
                from = marchStart,
                to = marchEnd,
                transactions = listOf(vndExpense(id = 1, amount = 500_000L, timestamp = marchMid)),
            )

            val viewModel = createViewModel()

            viewModel.insightsState.test {
                assertEquals(InsightsUiState.Loading, awaitItem())
                advanceTimeBy(250)
                val first = awaitItem() as InsightsUiState.Populated
                assertFalse(first.isCollapsed)

                viewModel.setInsightsCollapsed(true)
                advanceTimeBy(250)

                val second = awaitItem() as InsightsUiState.Populated
                assertTrue(second.isCollapsed)
                // Content didn't change — only the collapse flag did.
                assertEquals(first.result, second.result)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun insightsState_yearMode_emitsHidden() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeRepository.summaryToEmit = TestData.sampleMonthlySummary
            val viewModel = createViewModel()

            viewModel.insightsState.test {
                assertEquals(InsightsUiState.Loading, awaitItem())
                // Switch to YEAR before the initial debounce completes.
                viewModel.setMode(SummaryMode.YEAR)
                advanceUntilIdle()
                // Drain any interim emissions until we land on Hidden.
                var latest = awaitItem()
                while (latest !is InsightsUiState.Hidden) {
                    latest = awaitItem()
                }
                assertEquals(InsightsUiState.Hidden, latest)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun insightsState_repositoryThrows_emitsError() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeRepository.observeTransactionsShouldThrow = true
            val viewModel = createViewModel()

            viewModel.insightsState.test {
                assertEquals(InsightsUiState.Loading, awaitItem())
                advanceTimeBy(250)
                assertEquals(InsightsUiState.Error, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun insightsState_bulkEmissions_debouncedToSingleResult() =
        runTest(mainDispatcherRule.testDispatcher) {
            val marchMid = 1773921600000L
            // Seed one baseline txn so the first Populated emission has real content.
            fakeRepository.setExpenses(
                from = marchStart,
                to = marchEnd,
                transactions = listOf(vndExpense(id = 1, amount = 100_000L, timestamp = marchMid)),
            )

            val viewModel = createViewModel()

            viewModel.insightsState.test {
                assertEquals(InsightsUiState.Loading, awaitItem())
                advanceTimeBy(250)
                awaitItem() // first Populated after baseline

                // Fire 10 rapid updates within the 200ms debounce window. A
                // naïve (un-debounced) pipeline would emit 10 times; we expect
                // at most ONE emission after settling.
                repeat(10) { i ->
                    fakeRepository.setExpenses(
                        from = marchStart,
                        to = marchEnd,
                        transactions =
                            listOf(
                                vndExpense(id = 1, amount = 100_000L + (i + 1) * 1_000L, timestamp = marchMid),
                            ),
                    )
                    advanceTimeBy(10) // 10ms per update; total 100ms << 200ms debounce
                }
                // Settle the debounce window.
                advanceTimeBy(300)

                val coalesced = awaitItem() as InsightsUiState.Populated
                // Only the FINAL emission should reach the downstream state.
                // Last amount was 100_000 + 10 * 1_000 = 110_000.
                assertEquals(
                    110_000L,
                    fakeRepository.expensesByRange[marchStart to marchEnd]!!
                        .value[0]
                        .amount,
                )
                assertNotNull(coalesced.result)
                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
            }
        }

    private class StubCategoryRepository : CategoryRepository {
        override fun observeCategories(type: TransactionType): Flow<List<Category>> = flow { emit(emptyList()) }

        override suspend fun getCategory(id: Long): Category? = null

        override suspend fun createCategory(
            name: String,
            type: TransactionType,
            iconKey: String?,
            colorKey: String?,
        ): Long = 0L

        override suspend fun updateCategory(
            id: Long,
            name: String,
            iconKey: String?,
            colorKey: String?,
        ) {}

        override suspend fun deleteCategory(id: Long) {}

        override fun getCategoriesWithTransactionCount(): Flow<List<CategoryWithCount>> = flow { emit(emptyList()) }
    }
}
