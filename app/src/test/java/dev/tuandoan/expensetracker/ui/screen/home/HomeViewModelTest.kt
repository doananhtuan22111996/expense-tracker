package dev.tuandoan.expensetracker.ui.screen.home

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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeRepository: FakeTransactionRepository
    private lateinit var fakeTimeProvider: FakeTimeProvider

    @Before
    fun setup() {
        fakeRepository = FakeTransactionRepository()
        fakeTimeProvider = FakeTimeProvider()
    }

    private fun createViewModel(): HomeViewModel = HomeViewModel(fakeRepository, fakeTimeProvider)

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

    // Home list currency visibility – Phase 2.2 Item 6
    // Guards that currencyCode survives the ViewModel pipeline to HomeUiState

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
    fun init_usesTimeProviderMonthRange() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeTimeProvider.setMonthRange(100L to 200L)
            fakeRepository.transactionsToEmit = emptyList()

            createViewModel()
            advanceUntilIdle()

            assertEquals(100L, fakeRepository.lastObservedFrom)
            assertEquals(200L, fakeRepository.lastObservedTo)
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
