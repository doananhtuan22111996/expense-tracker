package dev.tuandoan.expensetracker.ui.screen.widgetcategories

import dev.tuandoan.expensetracker.core.util.UiText
import dev.tuandoan.expensetracker.data.preferences.FakeWidgetCategoryPreferences
import dev.tuandoan.expensetracker.domain.model.Category
import dev.tuandoan.expensetracker.domain.model.CategoryWithCount
import dev.tuandoan.expensetracker.domain.model.TransactionType
import dev.tuandoan.expensetracker.domain.repository.CategoryRepository
import dev.tuandoan.expensetracker.domain.widget.PinnedCategoriesUseCase
import dev.tuandoan.expensetracker.domain.widget.PinnedCategorySlot
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

/**
 * Covers the T6.1 WidgetCategoriesViewModel contract:
 * - Initial state merges pinned slots + available categories + pin count.
 * - `onTogglePin` adds under cap, removes when already pinned, and emits
 *   the over-limit hint when the cap is hit.
 * - `onMove` reorders the raw pin list atomically via a single prefs write.
 * - Persistence failures surface as one-shot errors, cleared via
 *   `onErrorShown` / `onOverLimitMessageShown`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WidgetCategoriesViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var preferences: FakeWidgetCategoryPreferences
    private lateinit var categoryRepository: FakeCategoryRepository
    private lateinit var useCase: PinnedCategoriesUseCase

    private val food = Category(id = 1L, name = "Food", type = TransactionType.EXPENSE)
    private val transport = Category(id = 2L, name = "Transport", type = TransactionType.EXPENSE)
    private val groceries = Category(id = 3L, name = "Groceries", type = TransactionType.EXPENSE)
    private val coffee = Category(id = 4L, name = "Coffee", type = TransactionType.EXPENSE)

    @Before
    fun setup() {
        preferences = FakeWidgetCategoryPreferences()
        categoryRepository = FakeCategoryRepository()
        useCase = PinnedCategoriesUseCase(preferences, categoryRepository)
        categoryRepository.expenseCategories.value = listOf(food, transport, groceries, coffee)
    }

    private fun newViewModel() =
        WidgetCategoriesViewModel(
            widgetCategoryPreferences = preferences,
            categoryRepository = categoryRepository,
            pinnedCategoriesUseCase = useCase,
        )

    @Test
    fun init_loadsStateWithResolvedSlotsAndAllExpenseCategories() =
        runTest(mainDispatcherRule.testDispatcher) {
            preferences.setPinnedCategoryIds(listOf(food.id, transport.id))
            val vm = newViewModel()

            advanceUntilIdle()

            val state = vm.uiState.value
            assertFalse(state.isLoading)
            assertEquals(3, state.pinnedSlots.size)
            val slot0 = state.pinnedSlots[0] as PinnedCategorySlot.Filled
            val slot1 = state.pinnedSlots[1] as PinnedCategorySlot.Filled
            assertEquals(food, slot0.category)
            assertEquals(transport, slot1.category)
            assertTrue(state.pinnedSlots[2] is PinnedCategorySlot.Empty)
            assertEquals(4, state.availableCategories.size)
            assertEquals(2, state.pinnedCount)
            assertFalse(state.isAtMaxPins)
        }

    @Test
    fun onTogglePin_addsCategoryWhenUnderCap() =
        runTest(mainDispatcherRule.testDispatcher) {
            preferences.setPinnedCategoryIds(listOf(food.id))
            val vm = newViewModel()
            advanceUntilIdle()

            vm.onTogglePin(transport.id)
            advanceUntilIdle()

            assertEquals(listOf(food.id, transport.id), preferences.latestWrite)
            assertEquals(2, vm.uiState.value.pinnedCount)
        }

    @Test
    fun onTogglePin_removesCategoryWhenAlreadyPinned() =
        runTest(mainDispatcherRule.testDispatcher) {
            preferences.setPinnedCategoryIds(listOf(food.id, transport.id, groceries.id))
            val vm = newViewModel()
            advanceUntilIdle()

            vm.onTogglePin(transport.id)
            advanceUntilIdle()

            assertEquals(listOf(food.id, groceries.id), preferences.latestWrite)
            assertEquals(2, vm.uiState.value.pinnedCount)
            assertFalse(vm.uiState.value.isAtMaxPins)
        }

    @Test
    fun onTogglePin_atMaxCap_emitsOverLimitMessageAndDoesNotWrite() =
        runTest(mainDispatcherRule.testDispatcher) {
            preferences.setPinnedCategoryIds(listOf(food.id, transport.id, groceries.id))
            val vm = newViewModel()
            advanceUntilIdle()
            val writeCountBefore = preferences.writeCount

            vm.onTogglePin(coffee.id)
            advanceUntilIdle()

            assertNotNull(vm.uiState.value.overLimitMessage)
            assertTrue(vm.uiState.value.overLimitMessage is UiText.StringResource)
            assertEquals(writeCountBefore, preferences.writeCount)
            assertEquals(3, vm.uiState.value.pinnedCount)
        }

    @Test
    fun onTogglePin_unpinOneWhileAtMax_clearsAtMaxAndAllowsSubsequentAdd() =
        runTest(mainDispatcherRule.testDispatcher) {
            preferences.setPinnedCategoryIds(listOf(food.id, transport.id, groceries.id))
            val vm = newViewModel()
            advanceUntilIdle()

            vm.onTogglePin(transport.id) // unpin
            advanceUntilIdle()
            vm.onTogglePin(coffee.id) // now under cap, add
            advanceUntilIdle()

            assertEquals(listOf(food.id, groceries.id, coffee.id), preferences.latestWrite)
            assertNull(vm.uiState.value.overLimitMessage)
        }

    @Test
    fun onMove_reordersPinListAtomically() =
        runTest(mainDispatcherRule.testDispatcher) {
            preferences.setPinnedCategoryIds(listOf(food.id, transport.id, groceries.id))
            val vm = newViewModel()
            advanceUntilIdle()

            vm.onMove(fromIndex = 0, toIndex = 2) // food to end
            advanceUntilIdle()

            assertEquals(listOf(transport.id, groceries.id, food.id), preferences.latestWrite)
        }

    @Test
    fun onMove_withOrphanPresent_reordersLiveListAndStripsOrphan() =
        runTest(mainDispatcherRule.testDispatcher) {
            // Raw persisted: [food, groceries-orphan, transport]
            // Live view: [food, transport] (orphan hidden)
            preferences.setPinnedCategoryIds(listOf(food.id, groceries.id, transport.id))
            categoryRepository.expenseCategories.value = listOf(food, transport, coffee)
            val vm = newViewModel()
            advanceUntilIdle()

            // Reorder fromIndex=0 toIndex=1 operates on the LIVE view: [food,transport] → [transport,food]
            vm.onMove(fromIndex = 0, toIndex = 1)
            advanceUntilIdle()

            assertEquals(listOf(transport.id, food.id), preferences.latestWrite)
        }

    @Test
    fun onMove_sameIndex_isNoOp() =
        runTest(mainDispatcherRule.testDispatcher) {
            preferences.setPinnedCategoryIds(listOf(food.id, transport.id))
            val vm = newViewModel()
            advanceUntilIdle()
            val writeCountBefore = preferences.writeCount

            vm.onMove(fromIndex = 1, toIndex = 1)
            advanceUntilIdle()

            assertEquals(writeCountBefore, preferences.writeCount)
        }

    @Test
    fun onMove_outOfRange_isNoOp() =
        runTest(mainDispatcherRule.testDispatcher) {
            preferences.setPinnedCategoryIds(listOf(food.id))
            val vm = newViewModel()
            advanceUntilIdle()
            val writeCountBefore = preferences.writeCount

            vm.onMove(fromIndex = 0, toIndex = 5)
            advanceUntilIdle()
            vm.onMove(fromIndex = -1, toIndex = 0)
            advanceUntilIdle()

            assertEquals(writeCountBefore, preferences.writeCount)
        }

    @Test
    fun orphanedPin_isExcludedFromLivePinCount_andCapStaysUnderMax() =
        runTest(mainDispatcherRule.testDispatcher) {
            // 3 pins, then delete one category upstream — orphan remains in raw list.
            preferences.setPinnedCategoryIds(listOf(food.id, transport.id, groceries.id))
            categoryRepository.expenseCategories.value = listOf(food, transport, coffee) // groceries deleted
            val vm = newViewModel()
            advanceUntilIdle()

            // Live pin count should exclude the orphan (groceries), so UI is NOT at cap.
            assertEquals(2, vm.uiState.value.pinnedCount)
            assertFalse(vm.uiState.value.isAtMaxPins)
        }

    @Test
    fun onTogglePin_unknownCategoryId_isSilentNoOp_doesNotCreateOrphan() =
        runTest(mainDispatcherRule.testDispatcher) {
            preferences.setPinnedCategoryIds(listOf(food.id))
            val vm = newViewModel()
            advanceUntilIdle()
            val writeCountBefore = preferences.writeCount

            // 9999 isn't in availableCategories — defence against stale/leaked IDs.
            vm.onTogglePin(categoryId = 9999L)
            advanceUntilIdle()

            assertEquals(writeCountBefore, preferences.writeCount)
            assertNull(vm.uiState.value.overLimitMessage)
        }

    @Test
    fun onTogglePin_addingWhenOrphanExists_compactsOrphanOutOfPersistedList() =
        runTest(mainDispatcherRule.testDispatcher) {
            // Orphan present: groceries is in raw pin list but not in live category list.
            preferences.setPinnedCategoryIds(listOf(food.id, transport.id, groceries.id))
            categoryRepository.expenseCategories.value = listOf(food, transport, coffee)
            val vm = newViewModel()
            advanceUntilIdle()

            vm.onTogglePin(coffee.id)
            advanceUntilIdle()

            // Persisted list should now be 3 live entries — orphan silently stripped.
            assertEquals(listOf(food.id, transport.id, coffee.id), preferences.latestWrite)
            assertNull(vm.uiState.value.overLimitMessage)
        }

    @Test
    fun onOverLimitMessageShown_clearsMessage() =
        runTest(mainDispatcherRule.testDispatcher) {
            preferences.setPinnedCategoryIds(listOf(food.id, transport.id, groceries.id))
            val vm = newViewModel()
            advanceUntilIdle()
            vm.onTogglePin(coffee.id) // trips over-limit
            advanceUntilIdle()
            assertNotNull(vm.uiState.value.overLimitMessage)

            vm.onOverLimitMessageShown()

            assertNull(vm.uiState.value.overLimitMessage)
        }

    @Test
    fun persistenceFailure_surfacesErrorOneShot() =
        runTest(mainDispatcherRule.testDispatcher) {
            val vm = newViewModel()
            advanceUntilIdle()
            preferences.throwOnNextWrite = true

            vm.onTogglePin(food.id)
            advanceUntilIdle()

            assertNotNull(vm.uiState.value.error)

            vm.onErrorShown()

            assertNull(vm.uiState.value.error)
        }

    private class FakeCategoryRepository : CategoryRepository {
        val expenseCategories = MutableStateFlow<List<Category>>(emptyList())

        override fun observeCategories(type: TransactionType): Flow<List<Category>> =
            when (type) {
                TransactionType.EXPENSE -> expenseCategories
                TransactionType.INCOME -> MutableStateFlow(emptyList())
            }

        override suspend fun getCategory(id: Long): Category? = expenseCategories.value.firstOrNull { it.id == id }

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
        ) = Unit

        override suspend fun deleteCategory(id: Long) = Unit

        override fun getCategoriesWithTransactionCount(): Flow<List<CategoryWithCount>> = MutableStateFlow(emptyList())
    }
}
