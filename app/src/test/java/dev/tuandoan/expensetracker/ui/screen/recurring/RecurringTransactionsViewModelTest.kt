package dev.tuandoan.expensetracker.ui.screen.recurring

import dev.tuandoan.expensetracker.domain.model.RecurrenceFrequency
import dev.tuandoan.expensetracker.domain.model.RecurringTransaction
import dev.tuandoan.expensetracker.domain.model.TransactionType
import dev.tuandoan.expensetracker.domain.repository.RecurringTransactionRepository
import dev.tuandoan.expensetracker.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RecurringTransactionsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeRepo: FakeRecurringTransactionRepository

    private val sampleRecurring =
        RecurringTransaction(
            id = 1L,
            type = TransactionType.EXPENSE,
            amount = 100000L,
            currencyCode = "VND",
            categoryId = 1L,
            categoryName = "Food",
            note = null,
            frequency = RecurrenceFrequency.MONTHLY,
            dayOfMonth = 1,
            dayOfWeek = null,
            nextDueMillis = 1700000000000L,
            isActive = true,
        )

    @Before
    fun setup() {
        fakeRepo = FakeRecurringTransactionRepository()
    }

    @Test
    fun requestDelete_setsPendingDeleteId() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeRepo.items.value = listOf(sampleRecurring)

            val viewModel = RecurringTransactionsViewModel(fakeRepo)
            advanceUntilIdle()

            viewModel.requestDelete(1L)

            assertEquals(1L, viewModel.uiState.value.pendingDeleteId)
            assertEquals(0, viewModel.uiState.value.visibleRecurringTransactions.size)
        }

    @Test
    fun undoDelete_clearsPendingAndRestoresItem() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeRepo.items.value = listOf(sampleRecurring)

            val viewModel = RecurringTransactionsViewModel(fakeRepo)
            advanceUntilIdle()

            viewModel.requestDelete(1L)
            assertEquals(0, viewModel.uiState.value.visibleRecurringTransactions.size)

            viewModel.undoDelete()

            assertNull(viewModel.uiState.value.pendingDeleteId)
            assertEquals(1, viewModel.uiState.value.visibleRecurringTransactions.size)
        }

    @Test
    fun confirmDelete_callsRepositoryAndClearsPending() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeRepo.items.value = listOf(sampleRecurring)

            val viewModel = RecurringTransactionsViewModel(fakeRepo)
            advanceUntilIdle()

            viewModel.requestDelete(1L)
            viewModel.confirmDelete()
            advanceUntilIdle()

            assertNull(viewModel.uiState.value.pendingDeleteId)
            assertEquals(1L, fakeRepo.lastDeletedId)
        }

    @Test
    fun requestDelete_rapidSuccessive_autoConfirmsPrevious() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeRepo.items.value =
                listOf(
                    sampleRecurring,
                    sampleRecurring.copy(id = 2L, categoryName = "Transport"),
                )

            val viewModel = RecurringTransactionsViewModel(fakeRepo)
            advanceUntilIdle()

            viewModel.requestDelete(1L)
            viewModel.requestDelete(2L)
            advanceUntilIdle()

            assertEquals(2L, viewModel.uiState.value.pendingDeleteId)
            assertEquals(1L, fakeRepo.lastDeletedId)
        }

    private class FakeRecurringTransactionRepository : RecurringTransactionRepository {
        val items = MutableStateFlow<List<RecurringTransaction>>(emptyList())
        var lastDeletedId: Long? = null

        override fun observeAll(): Flow<List<RecurringTransaction>> = items

        override suspend fun getById(id: Long): RecurringTransaction? = items.value.find { it.id == id }

        override suspend fun create(recurring: RecurringTransaction): Long = 1L

        override suspend fun update(recurring: RecurringTransaction) {}

        override suspend fun delete(id: Long) {
            lastDeletedId = id
        }

        override suspend fun setActive(
            id: Long,
            active: Boolean,
        ) {}

        override suspend fun processDueRecurring() {}
    }
}
