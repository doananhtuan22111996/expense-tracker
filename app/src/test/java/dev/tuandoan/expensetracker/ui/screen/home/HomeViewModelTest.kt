package dev.tuandoan.expensetracker.ui.screen.home

import dev.tuandoan.expensetracker.core.util.DateRangeCalculator
import dev.tuandoan.expensetracker.domain.model.MonthlySummary
import dev.tuandoan.expensetracker.domain.model.Transaction
import dev.tuandoan.expensetracker.domain.model.TransactionType
import dev.tuandoan.expensetracker.domain.repository.TransactionRepository
import dev.tuandoan.expensetracker.testutil.FakeSelectedMonthRepository
import dev.tuandoan.expensetracker.testutil.MainDispatcherRule
import dev.tuandoan.expensetracker.testutil.TestData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
class HomeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeRepository: FakeTransactionRepository
    private lateinit var fakeSelectedMonth: FakeSelectedMonthRepository
    private lateinit var dateRangeCalculator: DateRangeCalculator

    private val fixedZone: ZoneId = ZoneId.of("UTC")

    // 2026-03-15T12:00:00Z -> YearMonth = March 2026
    private val fixedClock: Clock = Clock.fixed(Instant.parse("2026-03-15T12:00:00Z"), fixedZone)

    @Before
    fun setup() {
        fakeRepository = FakeTransactionRepository()
        fakeSelectedMonth = FakeSelectedMonthRepository()
        dateRangeCalculator = DateRangeCalculator(fixedClock, fixedZone)
    }

    private fun createViewModel(): HomeViewModel = HomeViewModel(fakeRepository, fakeSelectedMonth, dateRangeCalculator)

    @Test
    fun init_loadsTransactions() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeRepository.transactionsToEmit = listOf(TestData.sampleExpenseTransaction)

            val viewModel = createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(1, state.transactions.size)
            assertFalse(state.isLoading)
            assertFalse(state.isError)
        }

    @Test
    fun init_emptyTransactions() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeRepository.transactionsToEmit = emptyList()

            val viewModel = createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state.transactions.isEmpty())
            assertFalse(state.isLoading)
        }

    @Test
    fun init_error_setsErrorState() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeRepository.shouldThrowOnObserve = true

            val viewModel = createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state.isError)
            assertFalse(state.isLoading)
        }

    @Test
    fun onFilterChanged_updatesFilterAndReloads() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeRepository.transactionsToEmit = listOf(TestData.sampleExpenseTransaction)
            val viewModel = createViewModel()
            advanceUntilIdle()

            fakeRepository.transactionsToEmit = listOf(TestData.sampleIncomeTransaction)
            viewModel.onFilterChanged(TransactionType.INCOME)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(TransactionType.INCOME, state.filter)
            assertEquals(1, state.transactions.size)
        }

    @Test
    fun onFilterChanged_null_clearsFilter() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeRepository.transactionsToEmit = listOf(TestData.sampleExpenseTransaction)
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onFilterChanged(TransactionType.EXPENSE)
            advanceUntilIdle()

            viewModel.onFilterChanged(null)
            advanceUntilIdle()

            assertNull(viewModel.uiState.value.filter)
        }

    @Test
    fun deleteTransaction_success() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeRepository.transactionsToEmit = listOf(TestData.sampleExpenseTransaction)
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.deleteTransaction(1L)
            advanceUntilIdle()

            assertEquals(1L, fakeRepository.lastDeletedId)
            assertFalse(viewModel.uiState.value.isError)
        }

    @Test
    fun deleteTransaction_error_setsErrorState() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeRepository.transactionsToEmit = listOf(TestData.sampleExpenseTransaction)
            val viewModel = createViewModel()
            advanceUntilIdle()

            fakeRepository.shouldThrowOnDelete = true
            viewModel.deleteTransaction(1L)
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.isError)
        }

    @Test
    fun clearError_resetsErrorState() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeRepository.shouldThrowOnObserve = true
            val viewModel = createViewModel()
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.isError)

            viewModel.clearError()

            assertFalse(viewModel.uiState.value.isError)
            assertNull(viewModel.uiState.value.errorMessage)
        }

    @Test
    fun init_loadsTransactions_preservesCurrencyCode() =
        runTest(mainDispatcherRule.testDispatcher) {
            val usdTransaction =
                TestData.sampleExpenseTransaction.copy(
                    id = 10L,
                    currencyCode = "USD",
                    amount = 12000L,
                )
            fakeRepository.transactionsToEmit = listOf(usdTransaction)

            val viewModel = createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(1, state.transactions.size)
            assertEquals("USD", state.transactions[0].currencyCode)
            assertEquals(12000L, state.transactions[0].amount)
        }

    @Test
    fun init_mixedCurrencies_preservesEachCurrencyCode() =
        runTest(mainDispatcherRule.testDispatcher) {
            val vndTransaction =
                TestData.sampleExpenseTransaction.copy(id = 1L, currencyCode = "VND", amount = 50000L)
            val usdTransaction =
                TestData.sampleExpenseTransaction.copy(id = 2L, currencyCode = "USD", amount = 12000L)
            val jpyTransaction =
                TestData.sampleIncomeTransaction.copy(id = 3L, currencyCode = "JPY", amount = 1500L)
            fakeRepository.transactionsToEmit = listOf(vndTransaction, usdTransaction, jpyTransaction)

            val viewModel = createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(3, state.transactions.size)
            assertEquals("VND", state.transactions[0].currencyCode)
            assertEquals("USD", state.transactions[1].currencyCode)
            assertEquals("JPY", state.transactions[2].currencyCode)
        }

    @Test
    fun init_usesDateRangeCalculatorForCurrentMonth() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeRepository.transactionsToEmit = emptyList()

            createViewModel()
            advanceUntilIdle()

            // March 2026 UTC: 2026-03-01T00:00:00Z to 2026-04-01T00:00:00Z
            assertEquals(1772323200000L, fakeRepository.lastObservedFrom)
            assertEquals(1775001600000L, fakeRepository.lastObservedTo)
        }

    @Test
    fun init_monthLabel_showsCurrentMonth() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeRepository.transactionsToEmit = emptyList()

            val viewModel = createViewModel()
            advanceUntilIdle()

            assertEquals("Mar 2026", viewModel.uiState.value.monthLabel)
        }

    // --- Month navigation ---

    @Test
    fun goToPreviousMonth_queriesFebruary() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeRepository.transactionsToEmit = emptyList()
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.goToPreviousMonth()
            advanceUntilIdle()

            assertEquals("Feb 2026", viewModel.uiState.value.monthLabel)
            // Feb 2026 UTC: 2026-02-01T00:00:00Z to 2026-03-01T00:00:00Z
            assertEquals(1769904000000L, fakeRepository.lastObservedFrom)
            assertEquals(1772323200000L, fakeRepository.lastObservedTo)
        }

    @Test
    fun goToNextMonth_queriesApril() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeRepository.transactionsToEmit = emptyList()
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
            fakeRepository.transactionsToEmit = emptyList()

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
            fakeRepository.transactionsToEmit = emptyList()

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.goToNextMonth()
            advanceUntilIdle()

            assertEquals("Jan 2026", viewModel.uiState.value.monthLabel)
        }

    @Test
    fun previousMonthTransactions_areRetrieable() =
        runTest(mainDispatcherRule.testDispatcher) {
            // Start with current month showing nothing
            fakeRepository.transactionsToEmit = emptyList()
            val viewModel = createViewModel()
            advanceUntilIdle()
            assertTrue(
                viewModel.uiState.value.transactions
                    .isEmpty(),
            )

            // Go to previous month where there IS data
            fakeRepository.transactionsToEmit = listOf(TestData.sampleExpenseTransaction)
            viewModel.goToPreviousMonth()
            advanceUntilIdle()

            assertEquals(1, viewModel.uiState.value.transactions.size)
            assertEquals("Feb 2026", viewModel.uiState.value.monthLabel)
        }

    // --- Shared month / setMonth ---

    @Test
    fun setMonth_jumpsToSelectedMonth() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeRepository.transactionsToEmit = emptyList()
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.setMonth(YearMonth.of(2024, 6))
            advanceUntilIdle()

            assertEquals("Jun 2024", viewModel.uiState.value.monthLabel)
        }

    @Test
    fun currentSelectedMonth_returnsSharedState() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeRepository.transactionsToEmit = emptyList()
            val viewModel = createViewModel()
            advanceUntilIdle()

            assertEquals(YearMonth.of(2026, 3), viewModel.currentSelectedMonth())

            viewModel.setMonth(YearMonth.of(2025, 1))
            assertEquals(YearMonth.of(2025, 1), viewModel.currentSelectedMonth())
        }

    @Test
    fun externalMonthChange_triggersReload() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeRepository.transactionsToEmit = emptyList()
            val viewModel = createViewModel()
            advanceUntilIdle()

            assertEquals("Mar 2026", viewModel.uiState.value.monthLabel)

            // External change (e.g., from Summary screen via shared repo)
            fakeSelectedMonth.setMonth(YearMonth.of(2025, 11))
            advanceUntilIdle()

            assertEquals("Nov 2025", viewModel.uiState.value.monthLabel)
        }

    private class FakeTransactionRepository : TransactionRepository {
        var transactionsToEmit: List<Transaction> = emptyList()
        var shouldThrowOnObserve = false
        var shouldThrowOnDelete = false
        var lastDeletedId: Long? = null
        var lastObservedFrom: Long? = null
        var lastObservedTo: Long? = null

        override fun observeTransactions(
            from: Long,
            to: Long,
            filterType: TransactionType?,
        ): Flow<List<Transaction>> {
            lastObservedFrom = from
            lastObservedTo = to
            return if (shouldThrowOnObserve) {
                flow { throw RuntimeException("Test error") }
            } else {
                flow { emit(transactionsToEmit) }
            }
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

        override suspend fun deleteTransaction(id: Long) {
            lastDeletedId = id
            if (shouldThrowOnDelete) throw RuntimeException("Delete failed")
        }

        override suspend fun getTransaction(id: Long): Transaction? = null

        override fun observeMonthlySummary(
            from: Long,
            to: Long,
        ): Flow<MonthlySummary> = flow { emit(TestData.sampleMonthlySummary) }
    }
}
