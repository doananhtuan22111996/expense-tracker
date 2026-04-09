package dev.tuandoan.expensetracker.data.database.dao

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.tuandoan.expensetracker.data.database.AppDatabase
import dev.tuandoan.expensetracker.data.database.entity.CategoryEntity
import dev.tuandoan.expensetracker.data.database.entity.TransactionEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TransactionDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var transactionDao: TransactionDao
    private lateinit var categoryDao: CategoryDao

    // Categories
    private val foodCategory = CategoryEntity(id = 1, name = "Food", type = CategoryEntity.TYPE_EXPENSE)
    private val transportCategory = CategoryEntity(id = 2, name = "Transport", type = CategoryEntity.TYPE_EXPENSE)
    private val salaryCategory = CategoryEntity(id = 3, name = "Salary", type = CategoryEntity.TYPE_INCOME)

    // Timestamps: Jan 15, Feb 15, Mar 15 2026
    private val jan15 = 1_736_899_200_000L // 2025-01-15 00:00 UTC
    private val feb15 = 1_739_577_600_000L // 2025-02-15 00:00 UTC
    private val mar15 = 1_742_169_600_000L // 2025-03-15 00:00 UTC
    private val janStart = 1_735_689_600_000L // 2025-01-01 00:00 UTC
    private val febStart = 1_738_368_000_000L // 2025-02-01 00:00 UTC
    private val marStart = 1_740_787_200_000L // 2025-03-01 00:00 UTC
    private val aprStart = 1_743_465_600_000L // 2025-04-01 00:00 UTC

    // Transactions
    private val lunchExpense =
        TransactionEntity(
            id = 1,
            type = TransactionEntity.TYPE_EXPENSE,
            amount = 50_000L,
            categoryId = 1,
            note = "Lunch at cafe",
            timestamp = jan15,
            createdAt = jan15,
            updatedAt = jan15,
        )
    private val busExpense =
        TransactionEntity(
            id = 2,
            type = TransactionEntity.TYPE_EXPENSE,
            amount = 10_000L,
            categoryId = 2,
            note = "Bus ticket",
            timestamp = feb15,
            createdAt = feb15,
            updatedAt = feb15,
        )
    private val salaryIncome =
        TransactionEntity(
            id = 3,
            type = TransactionEntity.TYPE_INCOME,
            amount = 5_000_000L,
            categoryId = 3,
            note = "Monthly salary",
            timestamp = mar15,
            createdAt = mar15,
            updatedAt = mar15,
        )
    private val dinnerExpense =
        TransactionEntity(
            id = 4,
            type = TransactionEntity.TYPE_EXPENSE,
            amount = 80_000L,
            categoryId = 1,
            note = "Dinner with friends",
            timestamp = mar15 + 1000,
            createdAt = mar15,
            updatedAt = mar15,
        )

    @Before
    fun setUp() =
        runTest {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            db =
                Room
                    .inMemoryDatabaseBuilder(context, AppDatabase::class.java)
                    .allowMainThreadQueries()
                    .build()
            transactionDao = db.transactionDao()
            categoryDao = db.categoryDao()

            // Insert categories first (FK constraint)
            categoryDao.insert(foodCategory)
            categoryDao.insert(transportCategory)
            categoryDao.insert(salaryCategory)

            // Insert transactions
            transactionDao.insert(lunchExpense)
            transactionDao.insert(busExpense)
            transactionDao.insert(salaryIncome)
            transactionDao.insert(dinnerExpense)
        }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun noFilters_returnsAllTransactionsOrderedByTimestampDesc() =
        runTest {
            val result =
                transactionDao
                    .searchTransactionsAdvanced(
                        from = null,
                        to = null,
                        query = "",
                    ).first()

            assertEquals(4, result.size)
            // Most recent first (dinnerExpense has highest timestamp)
            assertEquals(4L, result[0].id)
            assertEquals(3L, result[1].id)
            assertEquals(2L, result[2].id)
            assertEquals(1L, result[3].id)
        }

    @Test
    fun filterByTypeOnly_returnsMatchingType() =
        runTest {
            val expenses =
                transactionDao
                    .searchTransactionsAdvanced(
                        from = null,
                        to = null,
                        query = "",
                        type = TransactionEntity.TYPE_EXPENSE,
                    ).first()

            assertEquals(3, expenses.size)
            assertTrue(expenses.all { it.type == TransactionEntity.TYPE_EXPENSE })

            val incomes =
                transactionDao
                    .searchTransactionsAdvanced(
                        from = null,
                        to = null,
                        query = "",
                        type = TransactionEntity.TYPE_INCOME,
                    ).first()

            assertEquals(1, incomes.size)
            assertEquals(salaryIncome.id, incomes[0].id)
        }

    @Test
    fun filterByCategoryOnly_returnsMatchingCategory() =
        runTest {
            val result =
                transactionDao
                    .searchTransactionsAdvanced(
                        from = null,
                        to = null,
                        query = "",
                        categoryId = 1L,
                    ).first()

            assertEquals(2, result.size)
            assertTrue(result.all { it.categoryId == 1L })
        }

    @Test
    fun filterByDateRangeOnly_returnsTransactionsInRange() =
        runTest {
            // Feb only
            val result =
                transactionDao
                    .searchTransactionsAdvanced(
                        from = febStart,
                        to = marStart,
                        query = "",
                    ).first()

            assertEquals(1, result.size)
            assertEquals(busExpense.id, result[0].id)
        }

    @Test
    fun filterByDateRange_nullFrom_returnsAllBeforeTo() =
        runTest {
            // Everything before March
            val result =
                transactionDao
                    .searchTransactionsAdvanced(
                        from = null,
                        to = marStart,
                        query = "",
                    ).first()

            assertEquals(2, result.size)
            assertTrue(result.all { it.timestamp < marStart })
        }

    @Test
    fun filterByDateRange_nullTo_returnsAllFromStart() =
        runTest {
            // Everything from March onward
            val result =
                transactionDao
                    .searchTransactionsAdvanced(
                        from = marStart,
                        to = null,
                        query = "",
                    ).first()

            assertEquals(2, result.size)
            assertTrue(result.all { it.timestamp >= marStart })
        }

    @Test
    fun filterByTextQueryOnly_returnsMatchingNotes() =
        runTest {
            val result =
                transactionDao
                    .searchTransactionsAdvanced(
                        from = null,
                        to = null,
                        query = "lunch",
                    ).first()

            assertEquals(1, result.size)
            assertEquals(lunchExpense.id, result[0].id)
        }

    @Test
    fun filterByTextQuery_isCaseInsensitive() =
        runTest {
            val result =
                transactionDao
                    .searchTransactionsAdvanced(
                        from = null,
                        to = null,
                        query = "SALARY",
                    ).first()

            assertEquals(1, result.size)
            assertEquals(salaryIncome.id, result[0].id)
        }

    @Test
    fun filterByTextQueryAndCategory_returnsIntersection() =
        runTest {
            // "dinner" in Food category (id=1)
            val result =
                transactionDao
                    .searchTransactionsAdvanced(
                        from = null,
                        to = null,
                        query = "dinner",
                        categoryId = 1L,
                    ).first()

            assertEquals(1, result.size)
            assertEquals(dinnerExpense.id, result[0].id)

            // "bus" in Food category (id=1) — no match
            val noMatch =
                transactionDao
                    .searchTransactionsAdvanced(
                        from = null,
                        to = null,
                        query = "bus",
                        categoryId = 1L,
                    ).first()

            assertTrue(noMatch.isEmpty())
        }

    @Test
    fun filterByAllParameters_returnsNarrowestResult() =
        runTest {
            // Food expenses in March containing "dinner"
            val result =
                transactionDao
                    .searchTransactionsAdvanced(
                        from = marStart,
                        to = aprStart,
                        query = "dinner",
                        type = TransactionEntity.TYPE_EXPENSE,
                        categoryId = 1L,
                    ).first()

            assertEquals(1, result.size)
            assertEquals(dinnerExpense.id, result[0].id)
        }

    @Test
    fun noMatches_returnsEmptyList() =
        runTest {
            val result =
                transactionDao
                    .searchTransactionsAdvanced(
                        from = null,
                        to = null,
                        query = "nonexistent",
                    ).first()

            assertTrue(result.isEmpty())
        }

    @Test
    fun filterByTypeAndDateRange_returnsCombinedFilter() =
        runTest {
            // Expenses in March
            val result =
                transactionDao
                    .searchTransactionsAdvanced(
                        from = marStart,
                        to = aprStart,
                        query = "",
                        type = TransactionEntity.TYPE_EXPENSE,
                    ).first()

            assertEquals(1, result.size)
            assertEquals(dinnerExpense.id, result[0].id)
        }
}
