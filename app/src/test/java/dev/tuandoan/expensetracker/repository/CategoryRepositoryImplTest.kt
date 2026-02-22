package dev.tuandoan.expensetracker.repository

import dev.tuandoan.expensetracker.data.database.dao.CategoryDao
import dev.tuandoan.expensetracker.data.database.entity.CategoryEntity
import dev.tuandoan.expensetracker.domain.model.TransactionType
import dev.tuandoan.expensetracker.testutil.TestData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class CategoryRepositoryImplTest {
    private lateinit var fakeCategoryDao: FakeCategoryDao
    private lateinit var repository: CategoryRepositoryImpl

    @Before
    fun setup() {
        fakeCategoryDao = FakeCategoryDao()
        repository = CategoryRepositoryImpl(fakeCategoryDao)
    }

    @Test
    fun observeCategories_expense_returnsMappedCategories() =
        runTest {
            fakeCategoryDao.expenseCategories.value =
                listOf(TestData.expenseCategoryEntity, TestData.transportCategoryEntity)

            val result = repository.observeCategories(TransactionType.EXPENSE).first()

            assertEquals(2, result.size)
            assertEquals("Food", result[0].name)
            assertEquals(TransactionType.EXPENSE, result[0].type)
            assertEquals("Transport", result[1].name)
        }

    @Test
    fun observeCategories_income_returnsMappedCategories() =
        runTest {
            fakeCategoryDao.incomeCategories.value = listOf(TestData.incomeCategoryEntity)

            val result = repository.observeCategories(TransactionType.INCOME).first()

            assertEquals(1, result.size)
            assertEquals("Salary", result[0].name)
            assertEquals(TransactionType.INCOME, result[0].type)
        }

    @Test
    fun observeCategories_empty_returnsEmptyList() =
        runTest {
            val result = repository.observeCategories(TransactionType.EXPENSE).first()

            assertEquals(0, result.size)
        }

    @Test
    fun getCategory_existing_returnsMappedCategory() =
        runTest {
            fakeCategoryDao.categoriesById[1L] = TestData.expenseCategoryEntity

            val result = repository.getCategory(1L)

            assertEquals("Food", result?.name)
            assertEquals(TransactionType.EXPENSE, result?.type)
        }

    @Test
    fun getCategory_nonExisting_returnsNull() =
        runTest {
            val result = repository.getCategory(999L)

            assertNull(result)
        }

    private class FakeCategoryDao : CategoryDao {
        val expenseCategories = MutableStateFlow<List<CategoryEntity>>(emptyList())
        val incomeCategories = MutableStateFlow<List<CategoryEntity>>(emptyList())
        val categoriesById = mutableMapOf<Long, CategoryEntity>()

        override fun getCategories(type: Int): Flow<List<CategoryEntity>> =
            when (type) {
                0 -> expenseCategories
                1 -> incomeCategories
                else -> MutableStateFlow(emptyList())
            }

        override suspend fun getById(id: Long): CategoryEntity? = categoriesById[id]

        override suspend fun getAll(): List<CategoryEntity> = categoriesById.values.toList()

        override suspend fun insertAll(list: List<CategoryEntity>) {
            list.forEach { categoriesById[it.id] = it }
        }

        override suspend fun deleteAll() {
            categoriesById.clear()
        }
    }
}
