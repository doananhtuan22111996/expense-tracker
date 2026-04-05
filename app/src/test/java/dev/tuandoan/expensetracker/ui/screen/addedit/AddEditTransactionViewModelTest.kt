package dev.tuandoan.expensetracker.ui.screen.addedit

import androidx.lifecycle.SavedStateHandle
import dev.tuandoan.expensetracker.R
import dev.tuandoan.expensetracker.core.util.UiText
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
class AddEditTransactionViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeTransactionRepo: FakeTransactionRepository
    private lateinit var fakeCategoryRepo: FakeCategoryRepository
    private lateinit var fakeTimeProvider: FakeTimeProvider
    private lateinit var fakeCurrencyPreferenceRepo: FakeCurrencyPreferenceRepository
    private lateinit var fakeBudgetAlertScheduler: FakeBudgetAlertScheduler

    @Before
    fun setup() {
        fakeTransactionRepo = FakeTransactionRepository()
        fakeCategoryRepo = FakeCategoryRepository()
        fakeTimeProvider = FakeTimeProvider(currentMillis = 1700000000000L)
        fakeCurrencyPreferenceRepo = FakeCurrencyPreferenceRepository()
        fakeBudgetAlertScheduler = FakeBudgetAlertScheduler()
    }

    private fun createViewModel(transactionId: Long = 0L): AddEditTransactionViewModel {
        val savedStateHandle = SavedStateHandle(mapOf("transactionId" to transactionId))
        return AddEditTransactionViewModel(
            fakeTransactionRepo,
            fakeCategoryRepo,
            fakeTimeProvider,
            fakeCurrencyPreferenceRepo,
            fakeBudgetAlertScheduler,
            savedStateHandle,
        )
    }

    // New transaction mode tests

    @Test
    fun init_newMode_setsDefaults() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeCategoryRepo.categoriesToEmit = listOf(TestData.expenseCategory)

            val viewModel = createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(TransactionType.EXPENSE, state.type)
            assertEquals(1700000000000L, state.timestamp)
            assertNull(state.originalTransaction)
            assertFalse(state.isLoading)
        }

    @Test
    fun init_newMode_loadsExpenseCategories() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeCategoryRepo.categoriesToEmit =
                listOf(TestData.expenseCategory, TestData.transportCategory)

            val viewModel = createViewModel()
            advanceUntilIdle()

            assertEquals(2, viewModel.uiState.value.categories.size)
        }

    // Edit mode tests

    @Test
    fun init_editMode_loadsTransaction() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeTransactionRepo.transactionById = TestData.sampleExpenseTransaction
            fakeCategoryRepo.categoriesToEmit = listOf(TestData.expenseCategory)

            val viewModel = createViewModel(transactionId = 1L)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertNotNull(state.originalTransaction)
            assertEquals(TransactionType.EXPENSE, state.type)
            assertEquals(TestData.sampleExpenseTransaction.timestamp, state.timestamp)
        }

    @Test
    fun init_editMode_transactionNotFound_setsError() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeTransactionRepo.transactionById = null

            val viewModel = createViewModel(transactionId = 999L)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state.errorMessage is UiText.StringResource)
            assertEquals(
                R.string.error_transaction_not_found,
                (state.errorMessage as UiText.StringResource).resId,
            )
            assertFalse(state.isLoading)
        }

    // User action tests

    @Test
    fun onTypeChanged_updatesTypeAndResetsCategory() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeCategoryRepo.categoriesToEmit = listOf(TestData.expenseCategory)
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onCategorySelected(TestData.expenseCategory)

            fakeCategoryRepo.categoriesToEmit = listOf(TestData.incomeCategory)
            viewModel.onTypeChanged(TransactionType.INCOME)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(TransactionType.INCOME, state.type)
            assertNull(state.selectedCategory)
        }

    @Test
    fun onAmountChanged_updatesAmountText() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeCategoryRepo.categoriesToEmit = listOf(TestData.expenseCategory)
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onAmountChanged("50000")

            assertEquals("50000", viewModel.uiState.value.amountText)
        }

    @Test
    fun onCategorySelected_updatesSelectedCategory() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeCategoryRepo.categoriesToEmit = listOf(TestData.expenseCategory)
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onCategorySelected(TestData.expenseCategory)

            assertEquals(TestData.expenseCategory, viewModel.uiState.value.selectedCategory)
        }

    @Test
    fun onDateSelected_updatesTimestamp() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeCategoryRepo.categoriesToEmit = listOf(TestData.expenseCategory)
            val viewModel = createViewModel()
            advanceUntilIdle()

            val newTimestamp = 1700100000000L
            viewModel.onDateSelected(newTimestamp)

            assertEquals(newTimestamp, viewModel.uiState.value.timestamp)
        }

    @Test
    fun onNoteChanged_updatesNote() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeCategoryRepo.categoriesToEmit = listOf(TestData.expenseCategory)
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onNoteChanged("Lunch at restaurant")

            assertEquals("Lunch at restaurant", viewModel.uiState.value.note)
        }

    // Save transaction tests

    @Test
    fun saveTransaction_newMode_invalidAmount_setsError() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeCategoryRepo.categoriesToEmit = listOf(TestData.expenseCategory)
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onAmountChanged("")
            viewModel.onCategorySelected(TestData.expenseCategory)

            var successCalled = false
            viewModel.saveTransaction { successCalled = true }

            assertFalse(successCalled)
            assertTrue(viewModel.uiState.value.errorMessage is UiText.StringResource)
            assertEquals(
                R.string.error_invalid_amount,
                (viewModel.uiState.value.errorMessage as UiText.StringResource).resId,
            )
        }

    @Test
    fun saveTransaction_newMode_noCategory_setsError() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeCategoryRepo.categoriesToEmit = listOf(TestData.expenseCategory)
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onAmountChanged("50000")
            // Don't select category

            var successCalled = false
            viewModel.saveTransaction { successCalled = true }

            assertFalse(successCalled)
            assertTrue(viewModel.uiState.value.errorMessage is UiText.StringResource)
            assertEquals(
                R.string.error_select_category,
                (viewModel.uiState.value.errorMessage as UiText.StringResource).resId,
            )
        }

    @Test
    fun saveTransaction_newMode_validInput_callsAddAndSucceeds() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeCategoryRepo.categoriesToEmit = listOf(TestData.expenseCategory)
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onAmountChanged("50000")
            viewModel.onCategorySelected(TestData.expenseCategory)

            var successCalled = false
            viewModel.saveTransaction { successCalled = true }
            advanceUntilIdle()

            assertTrue(successCalled)
            assertTrue(fakeTransactionRepo.addCalled)
            assertEquals(1, fakeBudgetAlertScheduler.scheduleCount)
        }

    @Test
    fun saveTransaction_editMode_validInput_callsUpdateAndSucceeds() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeTransactionRepo.transactionById = TestData.sampleExpenseTransaction
            fakeCategoryRepo.categoriesToEmit = listOf(TestData.expenseCategory)
            val viewModel = createViewModel(transactionId = 1L)
            advanceUntilIdle()

            viewModel.onAmountChanged("100000")

            var successCalled = false
            viewModel.saveTransaction { successCalled = true }
            advanceUntilIdle()

            assertTrue(successCalled)
            assertTrue(fakeTransactionRepo.updateCalled)
            assertEquals(1, fakeBudgetAlertScheduler.scheduleCount)
        }

    @Test
    fun saveTransaction_editMode_updatedAtUsesTimeProvider() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeTimeProvider.setCurrentMillis(1700099999000L)
            fakeTransactionRepo.transactionById = TestData.sampleExpenseTransaction
            fakeCategoryRepo.categoriesToEmit = listOf(TestData.expenseCategory)
            val viewModel = createViewModel(transactionId = 1L)
            advanceUntilIdle()

            viewModel.onAmountChanged("100000")
            viewModel.saveTransaction { }
            advanceUntilIdle()

            assertEquals(1700099999000L, fakeTransactionRepo.lastUpdatedTransaction?.updatedAt)
        }

    @Test
    fun saveTransaction_error_setsErrorState() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeCategoryRepo.categoriesToEmit = listOf(TestData.expenseCategory)
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onAmountChanged("50000")
            viewModel.onCategorySelected(TestData.expenseCategory)
            fakeTransactionRepo.shouldThrowOnSave = true

            var successCalled = false
            viewModel.saveTransaction { successCalled = true }
            advanceUntilIdle()

            assertFalse(successCalled)
            assertNotNull(viewModel.uiState.value.errorMessage)
            assertFalse(viewModel.uiState.value.isLoading)
        }

    // Discard dialog tests

    @Test
    fun onBackPressed_editModeWithChanges_showsDiscardDialog() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeTransactionRepo.transactionById = TestData.sampleExpenseTransaction
            fakeCategoryRepo.categoriesToEmit = listOf(TestData.expenseCategory)
            val viewModel = createViewModel(transactionId = 1L)
            advanceUntilIdle()

            viewModel.onAmountChanged("100000")
            viewModel.onBackPressed()

            assertTrue(viewModel.uiState.value.showDiscardDialog)
        }

    @Test
    fun onBackPressed_editModeNoChanges_doesNotShowDialog() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeTransactionRepo.transactionById = TestData.sampleExpenseTransaction
            fakeCategoryRepo.categoriesToEmit = listOf(TestData.expenseCategory)
            val viewModel = createViewModel(transactionId = 1L)
            advanceUntilIdle()

            viewModel.onBackPressed()

            assertFalse(viewModel.uiState.value.showDiscardDialog)
        }

    @Test
    fun onDiscardChanges_hidesDialog() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeTransactionRepo.transactionById = TestData.sampleExpenseTransaction
            fakeCategoryRepo.categoriesToEmit = listOf(TestData.expenseCategory)
            val viewModel = createViewModel(transactionId = 1L)
            advanceUntilIdle()

            viewModel.onAmountChanged("100000")
            viewModel.onBackPressed()
            assertTrue(viewModel.uiState.value.showDiscardDialog)

            viewModel.onDiscardChanges()

            assertFalse(viewModel.uiState.value.showDiscardDialog)
        }

    @Test
    fun onCancelDiscard_hidesDialog() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeTransactionRepo.transactionById = TestData.sampleExpenseTransaction
            fakeCategoryRepo.categoriesToEmit = listOf(TestData.expenseCategory)
            val viewModel = createViewModel(transactionId = 1L)
            advanceUntilIdle()

            viewModel.onAmountChanged("100000")
            viewModel.onBackPressed()
            viewModel.onCancelDiscard()

            assertFalse(viewModel.uiState.value.showDiscardDialog)
        }

    // Clear error tests

    @Test
    fun clearError_resetsErrorMessage() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeCategoryRepo.categoriesToEmit = listOf(TestData.expenseCategory)
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onAmountChanged("")
            viewModel.saveTransaction { }

            assertNotNull(viewModel.uiState.value.errorMessage)

            viewModel.clearError()

            assertNull(viewModel.uiState.value.errorMessage)
        }

    // Currency preference tests

    @Test
    fun init_newMode_usesDefaultCurrencyFromPreference() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeCurrencyPreferenceRepo = FakeCurrencyPreferenceRepository(initialCurrency = "USD")
            fakeCategoryRepo.categoriesToEmit = listOf(TestData.expenseCategory)

            val viewModel = createViewModel()
            advanceUntilIdle()

            assertEquals("USD", viewModel.uiState.value.currencyCode)
        }

    @Test
    fun init_editMode_usesTransactionCurrencyCode() =
        runTest(mainDispatcherRule.testDispatcher) {
            val usdTransaction =
                TestData.sampleExpenseTransaction.copy(currencyCode = "USD")
            fakeTransactionRepo.transactionById = usdTransaction
            fakeCategoryRepo.categoriesToEmit = listOf(TestData.expenseCategory)
            // Default preference is VND, but edit mode should use the transaction's currency
            fakeCurrencyPreferenceRepo = FakeCurrencyPreferenceRepository(initialCurrency = "VND")

            val viewModel = createViewModel(transactionId = 1L)
            advanceUntilIdle()

            assertEquals("USD", viewModel.uiState.value.currencyCode)
        }

    @Test
    fun saveTransaction_newMode_passesCurrencyCodeToRepository() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeCurrencyPreferenceRepo = FakeCurrencyPreferenceRepository(initialCurrency = "EUR")
            fakeCategoryRepo.categoriesToEmit = listOf(TestData.expenseCategory)

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onAmountChanged("50000")
            viewModel.onCategorySelected(TestData.expenseCategory)

            var successCalled = false
            viewModel.saveTransaction { successCalled = true }
            advanceUntilIdle()

            assertTrue(successCalled)
            assertTrue(fakeTransactionRepo.addCalled)
            assertEquals("EUR", fakeTransactionRepo.lastAddedCurrencyCode)
        }

    // Currency change tests

    @Test
    fun onCurrencyChanged_updatesCurrencyCode() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeCategoryRepo.categoriesToEmit = listOf(TestData.expenseCategory)
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onCurrencyChanged("USD")

            assertEquals("USD", viewModel.uiState.value.currencyCode)
        }

    @Test
    fun onCurrencyChanged_emptyAmount_preservesEmptyText() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeCategoryRepo.categoriesToEmit = listOf(TestData.expenseCategory)
            val viewModel = createViewModel()
            advanceUntilIdle()

            // Ensure amount is empty
            viewModel.onAmountChanged("")
            viewModel.onCurrencyChanged("USD")

            assertEquals("", viewModel.uiState.value.amountText)
        }

    @Test
    fun onCurrencyChanged_sameCurrency_noChange() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeCategoryRepo.categoriesToEmit = listOf(TestData.expenseCategory)
            val viewModel = createViewModel()
            advanceUntilIdle()

            val defaultCode = viewModel.uiState.value.currencyCode
            val originalState = viewModel.uiState.value

            viewModel.onCurrencyChanged(defaultCode)

            // State should be exactly the same object (no copy triggered)
            assertEquals(originalState, viewModel.uiState.value)
        }

    @Test
    fun saveTransaction_afterCurrencyOverride_passesNewCurrency() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeCategoryRepo.categoriesToEmit = listOf(TestData.expenseCategory)
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onAmountChanged("50000")
            viewModel.onCategorySelected(TestData.expenseCategory)
            viewModel.onCurrencyChanged("JPY")

            var successCalled = false
            viewModel.saveTransaction { successCalled = true }
            advanceUntilIdle()

            assertTrue(successCalled)
            assertTrue(fakeTransactionRepo.addCalled)
            assertEquals("JPY", fakeTransactionRepo.lastAddedCurrencyCode)
        }

    @Test
    fun onCurrencyChanged_unsupportedCode_noStateChange() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeCategoryRepo.categoriesToEmit = listOf(TestData.expenseCategory)
            val viewModel = createViewModel()
            advanceUntilIdle()

            val originalState = viewModel.uiState.value
            viewModel.onCurrencyChanged("XYZ")

            assertEquals(originalState, viewModel.uiState.value)
        }

    @Test
    fun onCurrencyChanged_withAmount_keepsRawDigits() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeCategoryRepo.categoriesToEmit = listOf(TestData.expenseCategory)
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onAmountChanged("50000")
            viewModel.onCurrencyChanged("USD")

            // amountText stays as raw digits — formatting is visual only
            assertEquals("50000", viewModel.uiState.value.amountText)
        }

    @Test
    fun onCurrencyChanged_editMode_triggersHasUnsavedChanges() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeTransactionRepo.transactionById = TestData.sampleExpenseTransaction
            fakeCategoryRepo.categoriesToEmit = listOf(TestData.expenseCategory)
            val viewModel = createViewModel(transactionId = 1L)
            advanceUntilIdle()

            // Confirm no unsaved changes initially
            assertFalse(viewModel.uiState.value.hasUnsavedChanges)

            viewModel.onCurrencyChanged("USD")

            assertTrue(viewModel.uiState.value.hasUnsavedChanges)
        }

    // Fake implementations

    private class FakeTransactionRepository : TransactionRepository {
        var transactionById: Transaction? = null
        var shouldThrowOnSave = false
        var addCalled = false
        var updateCalled = false
        var lastUpdatedTransaction: Transaction? = null
        var lastAddedCurrencyCode: String? = null

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
            if (shouldThrowOnSave) throw RuntimeException("Save failed")
            addCalled = true
            lastAddedCurrencyCode = currencyCode
            return 1L
        }

        override suspend fun updateTransaction(transaction: Transaction) {
            if (shouldThrowOnSave) throw RuntimeException("Save failed")
            updateCalled = true
            lastUpdatedTransaction = transaction
        }

        override suspend fun deleteTransaction(id: Long) {}

        override suspend fun getTransaction(id: Long): Transaction? = transactionById

        override fun observeMonthlySummary(
            from: Long,
            to: Long,
        ): Flow<MonthlySummary> = flow { emit(TestData.sampleMonthlySummary) }

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
        ): List<MonthlyBarPoint> = (1..12).map { MonthlyBarPoint(month = it, totalExpense = 0L) }

        override fun searchTransactionsAdvanced(
            from: Long?,
            to: Long?,
            query: String,
            filterType: TransactionType?,
            categoryId: Long?,
        ): Flow<List<Transaction>> =
            flow {
                emit(emptyList())
            }
    }

    private class FakeCategoryRepository : CategoryRepository {
        var categoriesToEmit: List<Category> = emptyList()

        override fun observeCategories(type: TransactionType): Flow<List<Category>> = flow { emit(categoriesToEmit) }

        override suspend fun getCategory(id: Long): Category? = categoriesToEmit.find { it.id == id }

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

    private class FakeBudgetAlertScheduler : BudgetAlertScheduler {
        var scheduleCount = 0
            private set

        override fun scheduleImmediateCheck() {
            scheduleCount++
        }
    }
}
