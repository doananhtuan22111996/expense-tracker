package dev.tuandoan.expensetracker.ui.screen.summary

import dev.tuandoan.expensetracker.core.util.DateRangeCalculator
import dev.tuandoan.expensetracker.domain.model.CurrencyMonthlySummary
import dev.tuandoan.expensetracker.domain.model.MonthlySummary
import dev.tuandoan.expensetracker.domain.model.Transaction
import dev.tuandoan.expensetracker.domain.model.TransactionType
import dev.tuandoan.expensetracker.domain.repository.TransactionRepository
import dev.tuandoan.expensetracker.testutil.FakeSelectedMonthRepository
import dev.tuandoan.expensetracker.testutil.MainDispatcherRule
import dev.tuandoan.expensetracker.testutil.TestData
import dev.tuandoan.expensetracker.ui.screen.home.HomeViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
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
    private lateinit var dateRangeCalculator: DateRangeCalculator

    private val fixedZone: ZoneId = ZoneId.of("UTC")
    private val fixedClock: Clock = Clock.fixed(Instant.parse("2026-03-15T12:00:00Z"), fixedZone)

    @Before
    fun setup() {
        fakeRepository = FakeTransactionRepository()
        fakeSelectedMonth = FakeSelectedMonthRepository()
        dateRangeCalculator = DateRangeCalculator(fixedClock, fixedZone)
    }

    private fun createViewModel(): SummaryViewModel =
        SummaryViewModel(fakeRepository, fakeSelectedMonth, dateRangeCalculator)

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

    // --- Shared period consistency ---

    @Test
    fun sharedMonth_homeAndSummary_seeConsistentState() =
        runTest(mainDispatcherRule.testDispatcher) {
            // Both view models share the same FakeSelectedMonthRepository
            val homeVm =
                HomeViewModel(
                    FakeTransactionRepository().also { it.summaryToEmit = TestData.sampleMonthlySummary },
                    fakeSelectedMonth,
                    dateRangeCalculator,
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

    private class FakeTransactionRepository : TransactionRepository {
        var summaryToEmit: MonthlySummary = TestData.sampleMonthlySummary
        var shouldThrow = false
        var lastFrom: Long? = null
        var lastTo: Long? = null

        override fun observeTransactions(
            from: Long,
            to: Long,
            filterType: TransactionType?,
        ): Flow<List<Transaction>> = flow { emit(emptyList()) }

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
    }
}
