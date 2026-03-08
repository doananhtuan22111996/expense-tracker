package dev.tuandoan.expensetracker.repository

import dev.tuandoan.expensetracker.data.database.TransactionRunner
import dev.tuandoan.expensetracker.data.database.dao.CategoryDao
import dev.tuandoan.expensetracker.data.database.dao.TransactionDao
import dev.tuandoan.expensetracker.data.database.entity.CategoryEntity
import dev.tuandoan.expensetracker.data.database.entity.CategoryWithCountRow
import dev.tuandoan.expensetracker.data.database.entity.CurrencyCategorySumRow
import dev.tuandoan.expensetracker.data.database.entity.CurrencySumRow
import dev.tuandoan.expensetracker.data.database.entity.MonthlyTotalRow
import dev.tuandoan.expensetracker.data.database.entity.TransactionEntity
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
    private lateinit var fakeTransactionDao: FakeTransactionDao
    private lateinit var fakeTransactionRunner: FakeTransactionRunner
    private lateinit var repository: CategoryRepositoryImpl

    @Before
    fun setup() {
        fakeCategoryDao = FakeCategoryDao()
        fakeTransactionDao = FakeTransactionDao()
        fakeTransactionRunner = FakeTransactionRunner()
        repository = CategoryRepositoryImpl(fakeCategoryDao, fakeTransactionDao, fakeTransactionRunner)
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

    @Test
    fun createCategory_insertsNonDefaultEntity() =
        runTest {
            val id = repository.createCategory("Custom", TransactionType.EXPENSE, "work", "blue")

            assertEquals(100L, id)
            val inserted = fakeCategoryDao.lastInsertedEntity
            assertEquals("Custom", inserted?.name)
            assertEquals(0, inserted?.type) // EXPENSE
            assertEquals("work", inserted?.iconKey)
            assertEquals("blue", inserted?.colorKey)
            assertEquals(false, inserted?.isDefault)
        }

    @Test(expected = IllegalStateException::class)
    fun deleteCategory_defaultCategory_throws() =
        runTest {
            fakeCategoryDao.categoriesById[1L] = TestData.expenseCategoryEntity // isDefault = true
            repository.deleteCategory(1L)
        }

    @Test
    fun deleteCategory_migratesTransactionsBeforeDelete() =
        runTest {
            val customCategory =
                CategoryEntity(
                    id = 10L,
                    name = "Custom",
                    type = 0,
                    isDefault = false,
                )
            val uncategorized =
                CategoryEntity(
                    id = 99L,
                    name = "Uncategorized",
                    type = 0,
                    iconKey = "help_outline",
                    colorKey = "gray",
                    isDefault = true,
                )
            fakeCategoryDao.categoriesById[10L] = customCategory
            fakeCategoryDao.categoriesById[99L] = uncategorized
            fakeCategoryDao.allCategoriesList = listOf(customCategory, uncategorized)

            repository.deleteCategory(10L)

            assertEquals(10L, fakeTransactionDao.lastReassignFromId)
            assertEquals(99L, fakeTransactionDao.lastReassignToId)
            assertEquals(10L, fakeCategoryDao.lastDeletedNonDefaultId)
            assertEquals(1, fakeTransactionRunner.transactionCount)
        }

    @Test(expected = IllegalStateException::class)
    fun deleteCategory_notFound_throws() =
        runTest {
            repository.deleteCategory(999L)
        }

    private class FakeCategoryDao : CategoryDao {
        val expenseCategories = MutableStateFlow<List<CategoryEntity>>(emptyList())
        val incomeCategories = MutableStateFlow<List<CategoryEntity>>(emptyList())
        val categoriesById = mutableMapOf<Long, CategoryEntity>()
        var allCategoriesList: List<CategoryEntity> = emptyList()
        var lastInsertedEntity: CategoryEntity? = null
        var lastDeletedNonDefaultId: Long? = null

        override fun getCategories(type: Int): Flow<List<CategoryEntity>> =
            when (type) {
                0 -> expenseCategories
                1 -> incomeCategories
                else -> MutableStateFlow(emptyList())
            }

        override suspend fun getById(id: Long): CategoryEntity? = categoriesById[id]

        override suspend fun getAll(): List<CategoryEntity> =
            if (allCategoriesList.isNotEmpty()) {
                allCategoriesList
            } else {
                categoriesById.values.toList()
            }

        override suspend fun count(): Int = categoriesById.size

        override suspend fun insertAll(list: List<CategoryEntity>) {
            list.forEach { categoriesById[it.id] = it }
        }

        override suspend fun insert(entity: CategoryEntity): Long {
            lastInsertedEntity = entity
            return 100L
        }

        override suspend fun update(entity: CategoryEntity) {
            categoriesById[entity.id] = entity
        }

        override suspend fun getByNameAndType(
            name: String,
            type: Int,
        ): CategoryEntity? = categoriesById.values.firstOrNull { it.name == name && it.type == type }

        override suspend fun deleteNonDefault(id: Long): Int {
            lastDeletedNonDefaultId = id
            val entity = categoriesById[id]
            return if (entity != null && !entity.isDefault) {
                categoriesById.remove(id)
                1
            } else {
                0
            }
        }

        override fun getCategoriesWithCount(): Flow<List<CategoryWithCountRow>> = MutableStateFlow(emptyList())

        override suspend fun deleteAll() {
            categoriesById.clear()
        }
    }

    private class FakeTransactionDao : TransactionDao {
        var lastReassignFromId: Long? = null
        var lastReassignToId: Long? = null

        override fun getTransactions(
            from: Long,
            to: Long,
            type: Int?,
        ): Flow<List<TransactionEntity>> = MutableStateFlow(emptyList())

        override fun searchTransactions(
            from: Long,
            to: Long,
            query: String,
            type: Int?,
        ): Flow<List<TransactionEntity>> = MutableStateFlow(emptyList())

        override suspend fun insert(entity: TransactionEntity): Long = 0L

        override suspend fun update(entity: TransactionEntity) {}

        override suspend fun deleteById(id: Long) {}

        override suspend fun getById(id: Long): TransactionEntity? = null

        override suspend fun getAll(): List<TransactionEntity> = emptyList()

        override suspend fun getAllOrdered(): List<TransactionEntity> = emptyList()

        override suspend fun insertAll(list: List<TransactionEntity>) {}

        override suspend fun reassignCategory(
            fromId: Long,
            toId: Long,
        ) {
            lastReassignFromId = fromId
            lastReassignToId = toId
        }

        override suspend fun deleteAll() {}

        override fun sumExpenseByCurrency(
            from: Long,
            to: Long,
        ): Flow<List<CurrencySumRow>> = MutableStateFlow(emptyList())

        override fun sumIncomeByCurrency(
            from: Long,
            to: Long,
        ): Flow<List<CurrencySumRow>> = MutableStateFlow(emptyList())

        override suspend fun getMonthlyExpenseTotals(
            from: Long,
            to: Long,
            currencyCode: String,
        ): List<MonthlyTotalRow> = emptyList()

        override fun sumByCurrencyAndCategory(
            from: Long,
            to: Long,
            type: Int,
        ): Flow<List<CurrencyCategorySumRow>> = MutableStateFlow(emptyList())
    }

    private class FakeTransactionRunner : TransactionRunner {
        var transactionCount = 0

        override suspend fun <R> runInTransaction(block: suspend () -> R): R {
            transactionCount++
            return block()
        }
    }
}
