package dev.tuandoan.expensetracker.ui.screen.summary

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
            assertEquals(200000L, state.summary!!.totalExpense)
            assertEquals(10000000L, state.summary!!.totalIncome)
            assertEquals(9800000L, state.summary!!.balance)
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

            // Update data
            val updatedSummary = TestData.sampleMonthlySummary.copy(totalExpense = 500000L)
            fakeRepository.summaryToEmit = updatedSummary

            viewModel.refresh()
            advanceUntilIdle()

            assertEquals(
                500000L,
                viewModel.uiState.value.summary!!
                    .totalExpense,
            )
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
    fun init_summaryWithZeroValues() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeRepository.summaryToEmit =
                MonthlySummary(
                    totalExpense = 0L,
                    totalIncome = 0L,
                    balance = 0L,
                    topExpenseCategories = emptyList(),
                )

            val viewModel = createViewModel()
            advanceUntilIdle()

            val summary = viewModel.uiState.value.summary!!
            assertEquals(0L, summary.totalExpense)
            assertEquals(0L, summary.totalIncome)
            assertEquals(0L, summary.balance)
            assertTrue(summary.topExpenseCategories.isEmpty())
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
