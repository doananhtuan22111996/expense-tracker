package dev.tuandoan.expensetracker.ui.screen.summary

import dev.tuandoan.expensetracker.domain.model.CurrencyMonthlySummary
import dev.tuandoan.expensetracker.domain.model.MonthlySummary
import dev.tuandoan.expensetracker.domain.model.Transaction
import dev.tuandoan.expensetracker.domain.model.TransactionType
import dev.tuandoan.expensetracker.domain.repository.TransactionRepository
import dev.tuandoan.expensetracker.testutil.FakeTimeProvider
import dev.tuandoan.expensetracker.testutil.MainDispatcherRule
import dev.tuandoan.expensetracker.testutil.TestData
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

@OptIn(ExperimentalCoroutinesApi::class)
class SummaryViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeRepository: FakeTransactionRepository
    private lateinit var fakeTimeProvider: FakeTimeProvider

    @Before
    fun setup() {
        fakeRepository = FakeTransactionRepository()
        fakeTimeProvider = FakeTimeProvider()
    }

    private fun createViewModel(): SummaryViewModel = SummaryViewModel(fakeRepository, fakeTimeProvider)

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

            // Update data with higher expense
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
    fun init_usesTimeProviderMonthRange() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeTimeProvider.setMonthRange(100L to 200L)
            fakeRepository.summaryToEmit = TestData.sampleMonthlySummary

            createViewModel()
            advanceUntilIdle()

            assertEquals(100L, fakeRepository.lastFrom)
            assertEquals(200L, fakeRepository.lastTo)
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
        // Don't create ViewModel yet - just check default state
        val state = SummaryUiState()
        assertNull(state.summary)
        assertFalse(state.isLoading)
        assertFalse(state.isError)
        assertNull(state.errorMessage)
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
