package dev.tuandoan.expensetracker.domain.widget

import dev.tuandoan.expensetracker.data.preferences.FakeWidgetCategoryPreferences
import dev.tuandoan.expensetracker.domain.model.Category
import dev.tuandoan.expensetracker.domain.model.CategoryWithCount
import dev.tuandoan.expensetracker.domain.model.TransactionType
import dev.tuandoan.expensetracker.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PinnedCategoriesUseCaseTest {
    private lateinit var preferences: FakeWidgetCategoryPreferences
    private lateinit var categoryRepository: FakeCategoryRepository
    private lateinit var useCase: PinnedCategoriesUseCase

    private val groceries = Category(id = 1L, name = "Groceries", type = TransactionType.EXPENSE)
    private val coffee = Category(id = 2L, name = "Coffee", type = TransactionType.EXPENSE)
    private val transit = Category(id = 3L, name = "Transit", type = TransactionType.EXPENSE)
    private val dining = Category(id = 4L, name = "Dining", type = TransactionType.EXPENSE)

    @Before
    fun setup() {
        preferences = FakeWidgetCategoryPreferences()
        categoryRepository = FakeCategoryRepository()
        useCase = PinnedCategoriesUseCase(preferences, categoryRepository)
    }

    @Test
    fun noPinsAndNoCategories_emitsThreeEmptySlots() =
        runTest {
            val slots = useCase().first()

            assertEquals(3, slots.size)
            assertEquals(PinnedCategorySlot.Empty(1), slots[0])
            assertEquals(PinnedCategorySlot.Empty(2), slots[1])
            assertEquals(PinnedCategorySlot.Empty(3), slots[2])
        }

    @Test
    fun noPinsWithCategoriesAvailable_stillEmitsThreeEmptySlots() =
        runTest {
            categoryRepository.setCategories(listOf(groceries, coffee, transit))

            val slots = useCase().first()

            assertTrue(slots.all { it is PinnedCategorySlot.Empty })
        }

    @Test
    fun threePinsAllResolvable_emitsThreeFilledSlotsInOrder() =
        runTest {
            categoryRepository.setCategories(listOf(groceries, coffee, transit, dining))
            preferences.setPinnedCategoryIds(listOf(groceries.id, coffee.id, transit.id))

            val slots = useCase().first()

            assertEquals(PinnedCategorySlot.Filled(index = 1, category = groceries), slots[0])
            assertEquals(PinnedCategorySlot.Filled(index = 2, category = coffee), slots[1])
            assertEquals(PinnedCategorySlot.Filled(index = 3, category = transit), slots[2])
        }

    @Test
    fun twoPins_fillsSlotsOneAndTwo_slotThreeEmpty() =
        runTest {
            categoryRepository.setCategories(listOf(groceries, coffee))
            preferences.setPinnedCategoryIds(listOf(groceries.id, coffee.id))

            val slots = useCase().first()

            assertEquals(PinnedCategorySlot.Filled(index = 1, category = groceries), slots[0])
            assertEquals(PinnedCategorySlot.Filled(index = 2, category = coffee), slots[1])
            assertEquals(PinnedCategorySlot.Empty(3), slots[2])
        }

    @Test
    fun pinnedCategoryDeleted_collapsesToEmptySlotAtSamePosition() =
        runTest {
            categoryRepository.setCategories(listOf(groceries, transit))
            preferences.setPinnedCategoryIds(listOf(groceries.id, coffee.id, transit.id))

            val slots = useCase().first()

            assertEquals(PinnedCategorySlot.Filled(index = 1, category = groceries), slots[0])
            assertEquals(PinnedCategorySlot.Empty(2), slots[1])
            assertEquals(PinnedCategorySlot.Filled(index = 3, category = transit), slots[2])
        }

    @Test
    fun allPinnedCategoriesDeleted_emitsThreeEmptySlots() =
        runTest {
            categoryRepository.setCategories(emptyList())
            preferences.setPinnedCategoryIds(listOf(groceries.id, coffee.id, transit.id))

            val slots = useCase().first()

            assertTrue(slots.all { it is PinnedCategorySlot.Empty })
            assertEquals(listOf(1, 2, 3), slots.map { it.index })
        }

    @Test
    fun reorderingPins_reordersFilledSlots() =
        runTest {
            categoryRepository.setCategories(listOf(groceries, coffee, transit))
            preferences.setPinnedCategoryIds(listOf(groceries.id, coffee.id, transit.id))
            preferences.setPinnedCategoryIds(listOf(transit.id, groceries.id, coffee.id))

            val slots = useCase().first()

            assertEquals(PinnedCategorySlot.Filled(index = 1, category = transit), slots[0])
            assertEquals(PinnedCategorySlot.Filled(index = 2, category = groceries), slots[1])
            assertEquals(PinnedCategorySlot.Filled(index = 3, category = coffee), slots[2])
        }

    @Test
    fun emittedSlotsAlwaysNumberedOneTwoThree() =
        runTest {
            categoryRepository.setCategories(listOf(groceries))
            preferences.setPinnedCategoryIds(listOf(groceries.id))

            val slots = useCase().first()

            assertEquals(listOf(1, 2, 3), slots.map { it.index })
        }
}

private class FakeCategoryRepository : CategoryRepository {
    private val expenseCategories = MutableStateFlow<List<Category>>(emptyList())

    fun setCategories(categories: List<Category>) {
        expenseCategories.value = categories.filter { it.type == TransactionType.EXPENSE }
    }

    override fun observeCategories(type: TransactionType): Flow<List<Category>> = expenseCategories

    override suspend fun getCategory(id: Long): Category? = expenseCategories.value.firstOrNull { it.id == id }

    override suspend fun createCategory(
        name: String,
        type: TransactionType,
        iconKey: String?,
        colorKey: String?,
    ): Long = throw UnsupportedOperationException("Not needed for this test")

    override suspend fun updateCategory(
        id: Long,
        name: String,
        iconKey: String?,
        colorKey: String?,
    ) = throw UnsupportedOperationException("Not needed for this test")

    override suspend fun deleteCategory(id: Long) = throw UnsupportedOperationException("Not needed for this test")

    override fun getCategoriesWithTransactionCount(): Flow<List<CategoryWithCount>> =
        throw UnsupportedOperationException("Not needed for this test")
}
