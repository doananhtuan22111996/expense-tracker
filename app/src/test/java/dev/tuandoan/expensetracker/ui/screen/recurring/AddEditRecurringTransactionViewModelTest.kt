package dev.tuandoan.expensetracker.ui.screen.recurring

import androidx.lifecycle.SavedStateHandle
import dev.tuandoan.expensetracker.domain.model.Category
import dev.tuandoan.expensetracker.domain.model.CategoryWithCount
import dev.tuandoan.expensetracker.domain.model.RecurrenceFrequency
import dev.tuandoan.expensetracker.domain.model.RecurringTransaction
import dev.tuandoan.expensetracker.domain.model.TransactionType
import dev.tuandoan.expensetracker.domain.repository.CategoryRepository
import dev.tuandoan.expensetracker.domain.repository.RecurringTransactionRepository
import dev.tuandoan.expensetracker.testutil.FakeCurrencyPreferenceRepository
import dev.tuandoan.expensetracker.testutil.FakeTimeProvider
import dev.tuandoan.expensetracker.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class AddEditRecurringTransactionViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeRecurringRepo: FakeRecurringTransactionRepository
    private lateinit var fakeCategoryRepo: FakeCategoryRepository
    private lateinit var fakeCurrencyRepo: FakeCurrencyPreferenceRepository
    private lateinit var fakeTimeProvider: FakeTimeProvider
    private val zoneId = ZoneId.of("UTC")

    @Before
    fun setup() {
        fakeRecurringRepo = FakeRecurringTransactionRepository()
        fakeCategoryRepo = FakeCategoryRepository()
        fakeCurrencyRepo = FakeCurrencyPreferenceRepository("VND")
        fakeTimeProvider = FakeTimeProvider()
    }

    private fun createViewModel(recurringId: Long = 0L): AddEditRecurringTransactionViewModel {
        val savedStateHandle = SavedStateHandle(mapOf("recurringId" to recurringId))
        return AddEditRecurringTransactionViewModel(
            savedStateHandle = savedStateHandle,
            recurringTransactionRepository = fakeRecurringRepo,
            categoryRepository = fakeCategoryRepo,
            currencyPreferenceRepository = fakeCurrencyRepo,
            timeProvider = fakeTimeProvider,
            zoneId = zoneId,
        )
    }

    // --- Add mode ---

    @Test
    fun addMode_loadsDefaults() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(viewModel.isEditMode)
            assertFalse(state.isLoading)
            assertEquals("VND", state.currencyCode)
            assertEquals(TransactionType.EXPENSE, state.type)
            assertEquals(RecurrenceFrequency.MONTHLY, state.frequency)
            assertEquals(fakeTimeProvider.currentTimeMillis(), state.startDate)
        }

    @Test
    fun addMode_loadsCategories() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeCategoryRepo.expenseCategories.value = listOf(testCategory(1, "Food"), testCategory(2, "Transport"))

            val viewModel = createViewModel()
            advanceUntilIdle()

            assertEquals(2, viewModel.uiState.value.categories.size)
        }

    @Test
    fun addMode_formValidation_emptyAmount() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isFormValid)
        }

    @Test
    fun addMode_formValidation_validForm() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeCategoryRepo.expenseCategories.value = listOf(testCategory(1, "Food"))

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onAmountChanged("50000")
            viewModel.onCategorySelected(testCategory(1, "Food"))

            assertTrue(viewModel.uiState.value.isFormValid)
        }

    @Test
    fun addMode_save_callsRepository() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeCategoryRepo.expenseCategories.value = listOf(testCategory(1, "Food"))

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onAmountChanged("50000")
            viewModel.onCategorySelected(testCategory(1, "Food"))
            viewModel.onFrequencyChanged(RecurrenceFrequency.WEEKLY)
            viewModel.onNoteChanged("Groceries")

            var successCalled = false
            viewModel.save { successCalled = true }
            advanceUntilIdle()

            assertTrue(successCalled)
            assertEquals(1, fakeRecurringRepo.createdItems.size)
            val created = fakeRecurringRepo.createdItems[0]
            assertEquals(50000L, created.amount)
            assertEquals(1L, created.categoryId)
            assertEquals(RecurrenceFrequency.WEEKLY, created.frequency)
            assertEquals("Groceries", created.note)
            assertEquals("VND", created.currencyCode)
            assertTrue(created.isActive)
        }

    @Test
    fun addMode_save_invalidAmount_showsError() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.save {}
            advanceUntilIdle()

            assertNotNull(viewModel.uiState.value.errorMessage)
            assertFalse(viewModel.uiState.value.isSaving)
        }

    @Test
    fun addMode_save_noCategory_showsError() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onAmountChanged("50000")
            viewModel.save {}
            advanceUntilIdle()

            assertNotNull(viewModel.uiState.value.errorMessage)
        }

    @Test
    fun addMode_save_repositoryThrows_showsError() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeCategoryRepo.expenseCategories.value = listOf(testCategory(1, "Food"))
            fakeRecurringRepo.createShouldThrow = true

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onAmountChanged("50000")
            viewModel.onCategorySelected(testCategory(1, "Food"))

            var successCalled = false
            viewModel.save { successCalled = true }
            advanceUntilIdle()

            assertFalse(successCalled)
            assertNotNull(viewModel.uiState.value.errorMessage)
            assertFalse(viewModel.uiState.value.isSaving)
        }

    @Test
    fun addMode_hasUnsavedChanges_detectsInput() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.hasUnsavedChanges)

            viewModel.onAmountChanged("100")
            assertTrue(viewModel.uiState.value.hasUnsavedChanges)
        }

    @Test
    fun addMode_hasUnsavedChanges_note() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onNoteChanged("something")
            assertTrue(viewModel.uiState.value.hasUnsavedChanges)
        }

    @Test
    fun addMode_hasUnsavedChanges_category() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onCategorySelected(testCategory(1, "Food"))
            assertTrue(viewModel.uiState.value.hasUnsavedChanges)
        }

    // --- Edit mode ---

    @Test
    fun editMode_loadsExistingRecurring() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeRecurringRepo.itemToReturn = testRecurring()
            fakeCategoryRepo.expenseCategories.value = listOf(testCategory(1, "Food"))

            val viewModel = createViewModel(recurringId = 1L)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(viewModel.isEditMode)
            assertFalse(state.isLoading)
            assertEquals(TransactionType.EXPENSE, state.type)
            assertEquals("50000", state.amountText)
            assertEquals("VND", state.currencyCode)
            assertEquals(RecurrenceFrequency.MONTHLY, state.frequency)
            assertEquals("Rent", state.note)
            assertTrue(state.isActive)
            assertNotNull(state.selectedCategory)
            assertEquals(1L, state.selectedCategory?.id)
        }

    @Test
    fun editMode_notFound_showsError() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeRecurringRepo.itemToReturn = null

            val viewModel = createViewModel(recurringId = 999L)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertNotNull(state.errorMessage)
            assertFalse(state.isLoading)
        }

    @Test
    fun editMode_hasUnsavedChanges_noChanges() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeRecurringRepo.itemToReturn = testRecurring()
            fakeCategoryRepo.expenseCategories.value = listOf(testCategory(1, "Food"))

            val viewModel = createViewModel(recurringId = 1L)
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.hasUnsavedChanges)
        }

    @Test
    fun editMode_hasUnsavedChanges_amountChanged() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeRecurringRepo.itemToReturn = testRecurring()
            fakeCategoryRepo.expenseCategories.value = listOf(testCategory(1, "Food"))

            val viewModel = createViewModel(recurringId = 1L)
            advanceUntilIdle()

            viewModel.onAmountChanged("99999")
            assertTrue(viewModel.uiState.value.hasUnsavedChanges)

            // Revert
            viewModel.onAmountChanged("50000")
            assertFalse(viewModel.uiState.value.hasUnsavedChanges)
        }

    @Test
    fun editMode_hasUnsavedChanges_frequencyChanged() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeRecurringRepo.itemToReturn = testRecurring()
            fakeCategoryRepo.expenseCategories.value = listOf(testCategory(1, "Food"))

            val viewModel = createViewModel(recurringId = 1L)
            advanceUntilIdle()

            viewModel.onFrequencyChanged(RecurrenceFrequency.DAILY)
            assertTrue(viewModel.uiState.value.hasUnsavedChanges)

            viewModel.onFrequencyChanged(RecurrenceFrequency.MONTHLY)
            assertFalse(viewModel.uiState.value.hasUnsavedChanges)
        }

    @Test
    fun editMode_hasUnsavedChanges_noteChanged() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeRecurringRepo.itemToReturn = testRecurring()
            fakeCategoryRepo.expenseCategories.value = listOf(testCategory(1, "Food"))

            val viewModel = createViewModel(recurringId = 1L)
            advanceUntilIdle()

            viewModel.onNoteChanged("Updated note")
            assertTrue(viewModel.uiState.value.hasUnsavedChanges)

            viewModel.onNoteChanged("Rent")
            assertFalse(viewModel.uiState.value.hasUnsavedChanges)
        }

    @Test
    fun editMode_hasUnsavedChanges_typeChanged() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeRecurringRepo.itemToReturn = testRecurring()
            fakeCategoryRepo.expenseCategories.value = listOf(testCategory(1, "Food"))
            fakeCategoryRepo.incomeCategories.value = listOf(testCategory(3, "Salary"))

            val viewModel = createViewModel(recurringId = 1L)
            advanceUntilIdle()

            viewModel.onTypeChanged(TransactionType.INCOME)
            assertTrue(viewModel.uiState.value.hasUnsavedChanges)

            viewModel.onTypeChanged(TransactionType.EXPENSE)
            advanceUntilIdle()
            // Category gets cleared on type change, so still has changes
            assertTrue(viewModel.uiState.value.hasUnsavedChanges)
        }

    @Test
    fun editMode_hasUnsavedChanges_currencyChanged() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeRecurringRepo.itemToReturn = testRecurring()
            fakeCategoryRepo.expenseCategories.value = listOf(testCategory(1, "Food"))

            val viewModel = createViewModel(recurringId = 1L)
            advanceUntilIdle()

            viewModel.onCurrencyChanged("USD")
            assertTrue(viewModel.uiState.value.hasUnsavedChanges)

            viewModel.onCurrencyChanged("VND")
            assertFalse(viewModel.uiState.value.hasUnsavedChanges)
        }

    @Test
    fun editMode_save_callsUpdate() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeRecurringRepo.itemToReturn = testRecurring()
            fakeCategoryRepo.expenseCategories.value = listOf(testCategory(1, "Food"))

            val viewModel = createViewModel(recurringId = 1L)
            advanceUntilIdle()

            viewModel.onAmountChanged("75000")

            var successCalled = false
            viewModel.save { successCalled = true }
            advanceUntilIdle()

            assertTrue(successCalled)
            assertEquals(1, fakeRecurringRepo.updatedItems.size)
            assertEquals(75000L, fakeRecurringRepo.updatedItems[0].amount)
            assertEquals(1L, fakeRecurringRepo.updatedItems[0].id)
        }

    @Test
    fun editMode_save_preservesIsActive() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeRecurringRepo.itemToReturn = testRecurring(isActive = false)
            fakeCategoryRepo.expenseCategories.value = listOf(testCategory(1, "Food"))

            val viewModel = createViewModel(recurringId = 1L)
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isActive)

            viewModel.onAmountChanged("75000")
            viewModel.save {}
            advanceUntilIdle()

            assertEquals(1, fakeRecurringRepo.updatedItems.size)
            assertFalse(fakeRecurringRepo.updatedItems[0].isActive)
        }

    @Test
    fun editMode_loadExisting_repositoryThrows_showsError() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeRecurringRepo.getByIdShouldThrow = true

            val viewModel = createViewModel(recurringId = 1L)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertNotNull(state.errorMessage)
        }

    // --- Field change tests ---

    @Test
    fun onTypeChanged_clearsCategoryAndReloadsCategories() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeCategoryRepo.expenseCategories.value = listOf(testCategory(1, "Food"))
            fakeCategoryRepo.incomeCategories.value = listOf(testCategory(3, "Salary"))

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onCategorySelected(testCategory(1, "Food"))
            assertNotNull(viewModel.uiState.value.selectedCategory)

            viewModel.onTypeChanged(TransactionType.INCOME)
            advanceUntilIdle()

            assertNull(viewModel.uiState.value.selectedCategory)
            assertEquals(1, viewModel.uiState.value.categories.size)
            assertEquals(
                "Salary",
                viewModel.uiState.value.categories[0]
                    .name,
            )
        }

    @Test
    fun onCurrencyChanged_invalidCode_ignored() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onCurrencyChanged("INVALID")
            assertEquals("VND", viewModel.uiState.value.currencyCode)
        }

    @Test
    fun onCurrencyChanged_sameCurrency_ignored() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onCurrencyChanged("VND")
            assertEquals("VND", viewModel.uiState.value.currencyCode)
        }

    @Test
    fun clearError_clearsErrorMessage() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.save {} // will fail with empty form
            advanceUntilIdle()
            assertNotNull(viewModel.uiState.value.errorMessage)

            viewModel.clearError()
            assertNull(viewModel.uiState.value.errorMessage)
        }

    @Test
    fun addMode_save_blankNote_becomesNull() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeCategoryRepo.expenseCategories.value = listOf(testCategory(1, "Food"))

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onAmountChanged("50000")
            viewModel.onCategorySelected(testCategory(1, "Food"))
            viewModel.onNoteChanged("   ")

            viewModel.save {}
            advanceUntilIdle()

            assertEquals(1, fakeRecurringRepo.createdItems.size)
            assertNull(fakeRecurringRepo.createdItems[0].note)
        }

    // --- Helpers ---

    private fun testCategory(
        id: Long = 1L,
        name: String = "Food",
    ) = Category(
        id = id,
        name = name,
        type = if (name == "Salary") TransactionType.INCOME else TransactionType.EXPENSE,
        iconKey = null,
        colorKey = null,
        isDefault = false,
    )

    private fun testRecurring(
        id: Long = 1L,
        isActive: Boolean = true,
    ) = RecurringTransaction(
        id = id,
        type = TransactionType.EXPENSE,
        amount = 50000L,
        currencyCode = "VND",
        categoryId = 1L,
        categoryName = "Food",
        note = "Rent",
        frequency = RecurrenceFrequency.MONTHLY,
        dayOfMonth = 15,
        dayOfWeek = 3,
        nextDueMillis = 1700000000000L,
        isActive = isActive,
    )

    // --- Fakes ---

    private class FakeRecurringTransactionRepository : RecurringTransactionRepository {
        var itemToReturn: RecurringTransaction? = null
        var getByIdShouldThrow = false
        var createShouldThrow = false
        val createdItems = mutableListOf<RecurringTransaction>()
        val updatedItems = mutableListOf<RecurringTransaction>()

        override fun observeAll(): Flow<List<RecurringTransaction>> = MutableStateFlow(emptyList())

        override suspend fun getById(id: Long): RecurringTransaction? {
            if (getByIdShouldThrow) throw RuntimeException("Fake getById error")
            return itemToReturn
        }

        override suspend fun create(recurring: RecurringTransaction): Long {
            if (createShouldThrow) throw RuntimeException("Fake create error")
            createdItems.add(recurring)
            return recurring.id
        }

        override suspend fun update(recurring: RecurringTransaction) {
            updatedItems.add(recurring)
        }

        override suspend fun delete(id: Long) {}

        override suspend fun setActive(
            id: Long,
            active: Boolean,
        ) {}

        override suspend fun processDueRecurring() {}
    }

    private class FakeCategoryRepository : CategoryRepository {
        val expenseCategories = MutableStateFlow<List<Category>>(emptyList())
        val incomeCategories = MutableStateFlow<List<Category>>(emptyList())

        override fun observeCategories(type: TransactionType): Flow<List<Category>> =
            when (type) {
                TransactionType.EXPENSE -> expenseCategories
                TransactionType.INCOME -> incomeCategories
            }

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

        override fun getCategoriesWithTransactionCount(): Flow<List<CategoryWithCount>> = MutableStateFlow(emptyList())
    }
}
