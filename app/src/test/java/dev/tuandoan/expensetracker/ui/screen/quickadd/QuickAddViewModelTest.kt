package dev.tuandoan.expensetracker.ui.screen.quickadd

import androidx.lifecycle.SavedStateHandle
import dev.tuandoan.expensetracker.R
import dev.tuandoan.expensetracker.core.util.UiText
import dev.tuandoan.expensetracker.domain.analytics.Analytics
import dev.tuandoan.expensetracker.domain.analytics.AnalyticsEvent
import dev.tuandoan.expensetracker.domain.model.Category
import dev.tuandoan.expensetracker.domain.model.CategoryWithCount
import dev.tuandoan.expensetracker.domain.model.MonthlyBarPoint
import dev.tuandoan.expensetracker.domain.model.MonthlySummary
import dev.tuandoan.expensetracker.domain.model.Transaction
import dev.tuandoan.expensetracker.domain.model.TransactionType
import dev.tuandoan.expensetracker.domain.repository.BudgetAlertScheduler
import dev.tuandoan.expensetracker.domain.repository.CategoryRepository
import dev.tuandoan.expensetracker.domain.repository.TransactionRepository
import dev.tuandoan.expensetracker.testutil.FakeCurrencyPreferenceRepository
import dev.tuandoan.expensetracker.testutil.FakeTimeProvider
import dev.tuandoan.expensetracker.testutil.MainDispatcherRule
import dev.tuandoan.expensetracker.testutil.TestData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
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
class QuickAddViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var transactionRepo: FakeTransactionRepository
    private lateinit var categoryRepo: FakeCategoryRepository
    private lateinit var currencyRepo: FakeCurrencyPreferenceRepository
    private lateinit var scheduler: FakeBudgetAlertScheduler
    private lateinit var timeProvider: FakeTimeProvider
    private lateinit var analytics: RecordingAnalytics

    @Before
    fun setup() {
        transactionRepo = FakeTransactionRepository()
        categoryRepo = FakeCategoryRepository()
        currencyRepo = FakeCurrencyPreferenceRepository(initialCurrency = "VND")
        scheduler = FakeBudgetAlertScheduler()
        timeProvider = FakeTimeProvider(currentMillis = TestData.FIXED_TIME)
        analytics = RecordingAnalytics()
    }

    private fun createViewModel(categoryId: Long = TestData.expenseCategory.id): QuickAddViewModel {
        val savedState = SavedStateHandle(mapOf(QuickAddViewModel.KEY_CATEGORY_ID to categoryId))
        return QuickAddViewModel(
            transactionRepo,
            categoryRepo,
            currencyRepo,
            scheduler,
            timeProvider,
            analytics,
            savedState,
        )
    }

    // --- init ---

    @Test
    fun init_loadsCategoryAndResolvesDefaultCurrency() =
        runTest(mainDispatcherRule.testDispatcher) {
            categoryRepo.categoriesById[TestData.expenseCategory.id] = TestData.expenseCategory

            val vm = createViewModel()
            advanceUntilIdle()

            val state = vm.uiState.value
            assertEquals(TestData.expenseCategory, state.category)
            assertEquals("VND", state.currencyCode)
            assertFalse(state.categoryMissing)
        }

    @Test
    fun init_missingCategoryId_surfacesCategoryMissing() =
        runTest(mainDispatcherRule.testDispatcher) {
            val vm = createViewModel(categoryId = 0L)
            advanceUntilIdle()

            assertTrue(vm.uiState.value.categoryMissing)
            assertNull(vm.uiState.value.category)
        }

    @Test
    fun init_categoryIdDoesNotResolve_surfacesCategoryMissing() =
        runTest(mainDispatcherRule.testDispatcher) {
            // categoryId is non-zero but the repo has no matching category (FR-07 race).
            val vm = createViewModel(categoryId = 999L)
            advanceUntilIdle()

            val state = vm.uiState.value
            assertTrue(state.categoryMissing)
            assertNull(state.category)
            // Currency is still resolved so the UI can show a helpful fallback.
            assertEquals("VND", state.currencyCode)
        }

    // --- amount validation ---

    @Test
    fun save_emptyAmount_setsInvalidAmountError() =
        runTest(mainDispatcherRule.testDispatcher) {
            categoryRepo.categoriesById[TestData.expenseCategory.id] = TestData.expenseCategory
            val vm = createViewModel()
            advanceUntilIdle()

            vm.saveTransaction()
            advanceUntilIdle()

            assertEquals(
                UiText.StringResource(R.string.error_invalid_amount),
                vm.uiState.value.errorMessage,
            )
            assertFalse(vm.uiState.value.saved)
            assertEquals(0, transactionRepo.addCount)
        }

    @Test
    fun save_zeroAmount_setsInvalidAmountError() =
        runTest(mainDispatcherRule.testDispatcher) {
            categoryRepo.categoriesById[TestData.expenseCategory.id] = TestData.expenseCategory
            val vm = createViewModel()
            advanceUntilIdle()

            vm.onAmountChanged("0")
            vm.saveTransaction()
            advanceUntilIdle()

            assertEquals(
                UiText.StringResource(R.string.error_invalid_amount),
                vm.uiState.value.errorMessage,
            )
            assertEquals(0, transactionRepo.addCount)
        }

    @Test
    fun save_nonDigitsOnly_setsInvalidAmountError() =
        runTest(mainDispatcherRule.testDispatcher) {
            categoryRepo.categoriesById[TestData.expenseCategory.id] = TestData.expenseCategory
            val vm = createViewModel()
            advanceUntilIdle()

            vm.onAmountChanged("abc")
            vm.saveTransaction()
            advanceUntilIdle()

            assertNotNull(vm.uiState.value.errorMessage)
            assertEquals(0, transactionRepo.addCount)
        }

    @Test
    fun onAmountChanged_clearsExistingError() =
        runTest(mainDispatcherRule.testDispatcher) {
            categoryRepo.categoriesById[TestData.expenseCategory.id] = TestData.expenseCategory
            val vm = createViewModel()
            advanceUntilIdle()

            vm.saveTransaction() // triggers empty-amount error
            advanceUntilIdle()
            assertNotNull(vm.uiState.value.errorMessage)

            vm.onAmountChanged("1")
            assertNull(vm.uiState.value.errorMessage)
        }

    // --- save happy path ---

    @Test
    fun save_validAmount_callsAddTransactionWithCorrectArgs() =
        runTest(mainDispatcherRule.testDispatcher) {
            categoryRepo.categoriesById[TestData.expenseCategory.id] = TestData.expenseCategory
            val vm = createViewModel()
            advanceUntilIdle()

            vm.onAmountChanged("50000")
            vm.saveTransaction()
            advanceUntilIdle()

            assertEquals(1, transactionRepo.addCount)
            val args = transactionRepo.lastAddArgs!!
            assertEquals(TransactionType.EXPENSE, args.type)
            assertEquals(50_000L, args.amount)
            assertEquals(TestData.expenseCategory.id, args.categoryId)
            assertEquals("VND", args.currencyCode)
            assertNull(args.note)
            assertEquals(TestData.FIXED_TIME, args.timestamp)
        }

    @Test
    fun save_success_flipsSavedAndClearsSaving() =
        runTest(mainDispatcherRule.testDispatcher) {
            categoryRepo.categoriesById[TestData.expenseCategory.id] = TestData.expenseCategory
            val vm = createViewModel()
            advanceUntilIdle()

            vm.onAmountChanged("100")
            vm.saveTransaction()
            advanceUntilIdle()

            val state = vm.uiState.value
            assertTrue(state.saved)
            assertFalse(state.isSaving)
            assertNull(state.errorMessage)
        }

    @Test
    fun save_success_logsTransactionAddedAnalyticsEvent() =
        runTest(mainDispatcherRule.testDispatcher) {
            categoryRepo.categoriesById[TestData.expenseCategory.id] = TestData.expenseCategory
            val vm = createViewModel()
            advanceUntilIdle()

            vm.onAmountChanged("100")
            vm.saveTransaction()
            advanceUntilIdle()

            val expense = analytics.events.filterIsInstance<AnalyticsEvent.TransactionAdded>()
            assertEquals(1, expense.size)
        }

    @Test
    fun save_success_kicksBudgetAlertScheduler() =
        runTest(mainDispatcherRule.testDispatcher) {
            categoryRepo.categoriesById[TestData.expenseCategory.id] = TestData.expenseCategory
            val vm = createViewModel()
            advanceUntilIdle()

            vm.onAmountChanged("100")
            vm.saveTransaction()
            advanceUntilIdle()

            assertEquals(1, scheduler.scheduleCount)
        }

    // --- save failure ---

    @Test
    fun save_repositoryThrows_setsErrorAndClearsSaving() =
        runTest(mainDispatcherRule.testDispatcher) {
            categoryRepo.categoriesById[TestData.expenseCategory.id] = TestData.expenseCategory
            transactionRepo.throwOnAdd = RuntimeException("db down")
            val vm = createViewModel()
            advanceUntilIdle()

            vm.onAmountChanged("100")
            vm.saveTransaction()
            advanceUntilIdle()

            val state = vm.uiState.value
            assertFalse(state.saved)
            assertFalse(state.isSaving)
            assertNotNull(state.errorMessage)
            // Analytics must NOT fire on a failed save — defense-in-depth for
            // the "no phantom events" property.
            assertTrue(analytics.events.filterIsInstance<AnalyticsEvent.TransactionAdded>().isEmpty())
        }

    @Test
    fun save_whenAlreadySaving_isIgnored() =
        runTest(mainDispatcherRule.testDispatcher) {
            categoryRepo.categoriesById[TestData.expenseCategory.id] = TestData.expenseCategory
            // Prevent the first save coroutine from completing so isSaving stays true.
            transactionRepo.suspendAdd = true
            val vm = createViewModel()
            advanceUntilIdle()

            vm.onAmountChanged("100")
            vm.saveTransaction()
            // Dispatch the first save so it hits the repo and starts hanging.
            // We can't advanceUntilIdle here because the repo never returns;
            // runCurrent drains currently-dispatched work without waiting on
            // the suspended continuation.
            runCurrent()
            assertEquals(1, transactionRepo.addCount)
            assertTrue(vm.uiState.value.isSaving)

            // Subsequent taps while isSaving=true must short-circuit.
            vm.saveTransaction()
            vm.saveTransaction()
            runCurrent()
            assertEquals(1, transactionRepo.addCount)
        }

    @Test
    fun save_withoutCategoryYet_setsErrorAndDoesNotCallRepo() =
        runTest(mainDispatcherRule.testDispatcher) {
            // Category resolution never lands: categoryId is valid but the
            // repo lookup returns null → categoryMissing. A Save tap in that
            // race window should short-circuit, not write a bogus transaction.
            val vm = createViewModel(categoryId = 999L)
            advanceUntilIdle()
            assertTrue(vm.uiState.value.categoryMissing)

            vm.onAmountChanged("100")
            vm.saveTransaction()
            advanceUntilIdle()

            assertEquals(0, transactionRepo.addCount)
            assertNotNull(vm.uiState.value.errorMessage)
        }
}

// --- Fakes ---

private data class AddArgs(
    val type: TransactionType,
    val amount: Long,
    val categoryId: Long,
    val note: String?,
    val timestamp: Long,
    val currencyCode: String,
)

private class FakeTransactionRepository : TransactionRepository {
    var addCount: Int = 0
    var lastAddArgs: AddArgs? = null
    var throwOnAdd: Throwable? = null
    var suspendAdd: Boolean = false

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
    ): Long {
        if (suspendAdd) {
            // Record the call synchronously then hang — simulates a long-running
            // DB write so the "ignore duplicate taps while isSaving" guard test
            // can observe the guard without also tripping on a second repo call.
            addCount++
            lastAddArgs = AddArgs(type, amount, categoryId, note, timestamp, currencyCode)
            kotlinx.coroutines.awaitCancellation()
        }
        throwOnAdd?.let { throw it }
        addCount++
        lastAddArgs = AddArgs(type, amount, categoryId, note, timestamp, currencyCode)
        return 1L
    }

    override suspend fun updateTransaction(transaction: Transaction) = Unit

    override suspend fun deleteTransaction(id: Long) = Unit

    override suspend fun getTransaction(id: Long): Transaction? = null

    override fun observeMonthlySummary(
        from: Long,
        to: Long,
    ): Flow<MonthlySummary> = flow { emit(MonthlySummary(currencySummaries = emptyList())) }

    override fun searchTransactions(
        from: Long,
        to: Long,
        query: String,
        filterType: TransactionType?,
    ): Flow<List<Transaction>> = flow { emit(emptyList()) }

    override fun searchTransactionsAdvanced(
        from: Long?,
        to: Long?,
        query: String,
        filterType: TransactionType?,
        categoryId: Long?,
    ): Flow<List<Transaction>> = flow { emit(emptyList()) }

    override suspend fun getMonthlyExpenseTotals(
        from: Long,
        to: Long,
        currencyCode: String,
    ): List<MonthlyBarPoint> = emptyList()
}

private class FakeCategoryRepository : CategoryRepository {
    val categoriesById: MutableMap<Long, Category> = mutableMapOf()

    override fun observeCategories(type: TransactionType): Flow<List<Category>> =
        flow { emit(categoriesById.values.filter { it.type == type }) }

    override suspend fun getCategory(id: Long): Category? = categoriesById[id]

    override suspend fun createCategory(
        name: String,
        type: TransactionType,
        iconKey: String?,
        colorKey: String?,
    ): Long = throw UnsupportedOperationException()

    override suspend fun updateCategory(
        id: Long,
        name: String,
        iconKey: String?,
        colorKey: String?,
    ) = throw UnsupportedOperationException()

    override suspend fun deleteCategory(id: Long) = throw UnsupportedOperationException()

    override fun getCategoriesWithTransactionCount(): Flow<List<CategoryWithCount>> =
        throw UnsupportedOperationException()
}

private class FakeBudgetAlertScheduler : BudgetAlertScheduler {
    var scheduleCount = 0
        private set

    override fun scheduleImmediateCheck() {
        scheduleCount++
    }
}

private class RecordingAnalytics : Analytics {
    val events: MutableList<AnalyticsEvent> = mutableListOf()

    override fun logEvent(event: AnalyticsEvent) {
        events += event
    }

    override fun setCollectionEnabled(enabled: Boolean) = Unit
}
