package dev.tuandoan.expensetracker.ui.screen.categories

import dev.tuandoan.expensetracker.domain.model.Category
import dev.tuandoan.expensetracker.domain.model.CategoryWithCount
import dev.tuandoan.expensetracker.domain.model.TransactionType
import dev.tuandoan.expensetracker.domain.repository.CategoryRepository
import dev.tuandoan.expensetracker.testutil.MainDispatcherRule
import dev.tuandoan.expensetracker.testutil.TestData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CategoriesViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeCategoryRepo: FakeCategoryRepository

    @Before
    fun setup() {
        fakeCategoryRepo = FakeCategoryRepository()
    }

    @Test
    fun init_loadsCategoriesSplitByType() =
        runTest(mainDispatcherRule.testDispatcher) {
            val expenseCat =
                CategoryWithCount(
                    category = TestData.expenseCategory,
                    transactionCount = 5,
                )
            val incomeCat =
                CategoryWithCount(
                    category = TestData.incomeCategory,
                    transactionCount = 3,
                )
            fakeCategoryRepo.categoriesWithCount.value = listOf(expenseCat, incomeCat)

            val viewModel = CategoriesViewModel(fakeCategoryRepo)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertEquals(1, state.expenseCategories.size)
            assertEquals(1, state.incomeCategories.size)
            assertEquals("Food", state.expenseCategories[0].category.name)
            assertEquals("Salary", state.incomeCategories[0].category.name)
        }

    @Test
    fun onTabSelected_updatesSelectedTab() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = CategoriesViewModel(fakeCategoryRepo)
            advanceUntilIdle()

            assertEquals(TransactionType.EXPENSE, viewModel.uiState.value.selectedTab)

            viewModel.onTabSelected(TransactionType.INCOME)

            assertEquals(TransactionType.INCOME, viewModel.uiState.value.selectedTab)
        }

    @Test
    fun requestDelete_setsPendingDeleteId() =
        runTest(mainDispatcherRule.testDispatcher) {
            val expenseCat =
                CategoryWithCount(
                    category = TestData.expenseCategory,
                    transactionCount = 0,
                )
            fakeCategoryRepo.categoriesWithCount.value = listOf(expenseCat)

            val viewModel = CategoriesViewModel(fakeCategoryRepo)
            advanceUntilIdle()

            viewModel.requestDelete(TestData.expenseCategory.id)

            assertEquals(TestData.expenseCategory.id, viewModel.uiState.value.pendingDeleteId)
            assertEquals(0, viewModel.uiState.value.visibleExpenseCategories.size)
        }

    @Test
    fun undoDelete_clearsPendingAndRestoresItem() =
        runTest(mainDispatcherRule.testDispatcher) {
            val expenseCat =
                CategoryWithCount(
                    category = TestData.expenseCategory,
                    transactionCount = 0,
                )
            fakeCategoryRepo.categoriesWithCount.value = listOf(expenseCat)

            val viewModel = CategoriesViewModel(fakeCategoryRepo)
            advanceUntilIdle()

            viewModel.requestDelete(TestData.expenseCategory.id)
            assertEquals(0, viewModel.uiState.value.visibleExpenseCategories.size)

            viewModel.undoDelete()

            assertNull(viewModel.uiState.value.pendingDeleteId)
            assertEquals(1, viewModel.uiState.value.visibleExpenseCategories.size)
        }

    @Test
    fun confirmDelete_callsRepositoryAndClearsPending() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = CategoriesViewModel(fakeCategoryRepo)
            advanceUntilIdle()

            viewModel.requestDelete(1L)
            viewModel.confirmDelete()
            advanceUntilIdle()

            assertNull(viewModel.uiState.value.pendingDeleteId)
            assertEquals(1L, fakeCategoryRepo.lastDeletedId)
        }

    @Test
    fun confirmDelete_error_surfacesInState() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeCategoryRepo.shouldThrowOnDelete = true

            val viewModel = CategoriesViewModel(fakeCategoryRepo)
            advanceUntilIdle()

            viewModel.requestDelete(1L)
            viewModel.confirmDelete()
            advanceUntilIdle()

            assertNotNull(viewModel.uiState.value.error)
        }

    @Test
    fun onErrorDismissed_clearsError() =
        runTest(mainDispatcherRule.testDispatcher) {
            fakeCategoryRepo.shouldThrowOnDelete = true

            val viewModel = CategoriesViewModel(fakeCategoryRepo)
            advanceUntilIdle()

            viewModel.requestDelete(1L)
            viewModel.confirmDelete()
            advanceUntilIdle()
            assertNotNull(viewModel.uiState.value.error)

            viewModel.onErrorDismissed()

            assertNull(viewModel.uiState.value.error)
        }

    @Test
    fun createCategory_callsRepository() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = CategoriesViewModel(fakeCategoryRepo)
            advanceUntilIdle()

            viewModel.createCategory("Test", TransactionType.EXPENSE, null, "red")
            advanceUntilIdle()

            assertEquals("Test", fakeCategoryRepo.lastCreatedName)
        }

    private class FakeCategoryRepository : CategoryRepository {
        val categoriesWithCount = MutableStateFlow<List<CategoryWithCount>>(emptyList())
        var shouldThrowOnDelete = false
        var lastCreatedName: String? = null
        var lastDeletedId: Long? = null

        override fun observeCategories(type: TransactionType): Flow<List<Category>> = flow { emit(emptyList()) }

        override suspend fun getCategory(id: Long): Category? = null

        override suspend fun createCategory(
            name: String,
            type: TransactionType,
            iconKey: String?,
            colorKey: String?,
        ): Long {
            lastCreatedName = name
            return 1L
        }

        override suspend fun updateCategory(
            id: Long,
            name: String,
            iconKey: String?,
            colorKey: String?,
        ) {}

        override suspend fun deleteCategory(id: Long) {
            if (shouldThrowOnDelete) {
                throw RuntimeException("Cannot delete default category")
            }
            lastDeletedId = id
        }

        override fun getCategoriesWithTransactionCount(): Flow<List<CategoryWithCount>> = categoriesWithCount
    }
}
