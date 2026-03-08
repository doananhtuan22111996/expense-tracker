package dev.tuandoan.expensetracker.core.util

import dev.tuandoan.expensetracker.data.database.TransactionRunner
import dev.tuandoan.expensetracker.data.database.dao.RecurringTransactionDao
import dev.tuandoan.expensetracker.data.database.dao.TransactionDao
import dev.tuandoan.expensetracker.data.database.entity.CurrencyCategorySumRow
import dev.tuandoan.expensetracker.data.database.entity.CurrencySumRow
import dev.tuandoan.expensetracker.data.database.entity.MonthlyTotalRow
import dev.tuandoan.expensetracker.data.database.entity.RecurringTransactionEntity
import dev.tuandoan.expensetracker.data.database.entity.TransactionEntity
import dev.tuandoan.expensetracker.domain.model.RecurrenceFrequency
import dev.tuandoan.expensetracker.testutil.FakeTimeProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class RecurrenceSchedulerTest {
    private lateinit var scheduler: RecurrenceScheduler
    private lateinit var fakeTimeProvider: FakeTimeProvider
    private val zoneId = ZoneId.of("UTC")

    @Before
    fun setup() {
        fakeTimeProvider = FakeTimeProvider()
        scheduler = RecurrenceScheduler(fakeTimeProvider)
    }

    // -- calculateNextDue tests --

    @Test
    fun calculateNextDue_daily_addsOneDay() {
        val base =
            ZonedDateTime
                .of(2026, 3, 1, 12, 0, 0, 0, zoneId)
                .toInstant()
                .toEpochMilli()
        val next = scheduler.calculateNextDue(RecurrenceFrequency.DAILY, base, zoneId)
        val nextDate = Instant.ofEpochMilli(next).atZone(zoneId).toLocalDate()
        assertEquals(LocalDate.of(2026, 3, 2), nextDate)
    }

    @Test
    fun calculateNextDue_weekly_addsSevenDays() {
        val base =
            ZonedDateTime
                .of(2026, 3, 1, 12, 0, 0, 0, zoneId)
                .toInstant()
                .toEpochMilli()
        val next = scheduler.calculateNextDue(RecurrenceFrequency.WEEKLY, base, zoneId)
        val nextDate = Instant.ofEpochMilli(next).atZone(zoneId).toLocalDate()
        assertEquals(LocalDate.of(2026, 3, 8), nextDate)
    }

    @Test
    fun calculateNextDue_monthly_addsOneMonth() {
        val base =
            ZonedDateTime
                .of(2026, 1, 15, 12, 0, 0, 0, zoneId)
                .toInstant()
                .toEpochMilli()
        val next = scheduler.calculateNextDue(RecurrenceFrequency.MONTHLY, base, zoneId)
        val nextDate = Instant.ofEpochMilli(next).atZone(zoneId).toLocalDate()
        assertEquals(LocalDate.of(2026, 2, 15), nextDate)
    }

    @Test
    fun calculateNextDue_monthly_jan31ToFeb_capsAtEndOfMonth() {
        val base =
            ZonedDateTime
                .of(2026, 1, 31, 12, 0, 0, 0, zoneId)
                .toInstant()
                .toEpochMilli()
        val next = scheduler.calculateNextDue(RecurrenceFrequency.MONTHLY, base, zoneId)
        val nextDate = Instant.ofEpochMilli(next).atZone(zoneId).toLocalDate()
        // Java's plusMonths(1) from Jan 31 goes to Feb 28 (non-leap year)
        assertEquals(LocalDate.of(2026, 2, 28), nextDate)
    }

    @Test
    fun calculateNextDue_yearly_addsOneYear() {
        val base =
            ZonedDateTime
                .of(2026, 6, 15, 12, 0, 0, 0, zoneId)
                .toInstant()
                .toEpochMilli()
        val next = scheduler.calculateNextDue(RecurrenceFrequency.YEARLY, base, zoneId)
        val nextDate = Instant.ofEpochMilli(next).atZone(zoneId).toLocalDate()
        assertEquals(LocalDate.of(2027, 6, 15), nextDate)
    }

    @Test
    fun calculateNextDue_yearly_feb29LeapYear() {
        // 2024 is a leap year, Feb 29 -> next year Feb 28
        val base =
            ZonedDateTime
                .of(2024, 2, 29, 12, 0, 0, 0, zoneId)
                .toInstant()
                .toEpochMilli()
        val next = scheduler.calculateNextDue(RecurrenceFrequency.YEARLY, base, zoneId)
        val nextDate = Instant.ofEpochMilli(next).atZone(zoneId).toLocalDate()
        assertEquals(LocalDate.of(2025, 2, 28), nextDate)
    }

    // -- processDueRecurring tests --

    @Test
    fun processDueRecurring_insertsTransactionsForDueItems() =
        runTest {
            val now = 1700000000000L
            fakeTimeProvider.setCurrentMillis(now)

            val fakeRecurringDao = FakeRecurringTransactionDao()
            val fakeTransactionDao = FakeTransactionDao()
            val fakeRunner = FakeTransactionRunner()

            // Add a due recurring item (nextDueMillis < now)
            fakeRecurringDao.dueItems.add(
                RecurringTransactionEntity(
                    id = 1L,
                    type = 0,
                    amount = 50000L,
                    currencyCode = "VND",
                    categoryId = 1L,
                    note = "Rent",
                    frequency = RecurrenceFrequency.MONTHLY.toInt(),
                    dayOfMonth = 1,
                    dayOfWeek = null,
                    nextDueMillis = now - 1000L,
                    isActive = true,
                    createdAt = now - 100000L,
                    updatedAt = now - 100000L,
                ),
            )

            scheduler.processDueRecurring(fakeRecurringDao, fakeTransactionDao, fakeRunner, zoneId)

            assertEquals(1, fakeTransactionDao.inserted.size)
            assertEquals(50000L, fakeTransactionDao.inserted[0].amount)
            assertEquals("VND", fakeTransactionDao.inserted[0].currencyCode)
            assertEquals("Rent", fakeTransactionDao.inserted[0].note)
            assertTrue(fakeRecurringDao.updatedNextDues.isNotEmpty())
        }

    @Test
    fun processDueRecurring_noDueItems_doesNothing() =
        runTest {
            fakeTimeProvider.setCurrentMillis(1700000000000L)

            val fakeRecurringDao = FakeRecurringTransactionDao()
            val fakeTransactionDao = FakeTransactionDao()
            val fakeRunner = FakeTransactionRunner()

            scheduler.processDueRecurring(fakeRecurringDao, fakeTransactionDao, fakeRunner, zoneId)

            assertEquals(0, fakeTransactionDao.inserted.size)
        }

    @Test
    fun processDueRecurring_idempotent_secondRunNoDuplicates() =
        runTest {
            val now = 1700000000000L
            fakeTimeProvider.setCurrentMillis(now)

            val fakeRecurringDao = FakeRecurringTransactionDao()
            val fakeTransactionDao = FakeTransactionDao()
            val fakeRunner = FakeTransactionRunner()

            fakeRecurringDao.dueItems.add(
                RecurringTransactionEntity(
                    id = 1L,
                    type = 0,
                    amount = 50000L,
                    currencyCode = "VND",
                    categoryId = 1L,
                    note = "Test",
                    frequency = RecurrenceFrequency.DAILY.toInt(),
                    dayOfMonth = null,
                    dayOfWeek = null,
                    nextDueMillis = now - 1000L,
                    isActive = true,
                    createdAt = now,
                    updatedAt = now,
                ),
            )

            // First run
            scheduler.processDueRecurring(fakeRecurringDao, fakeTransactionDao, fakeRunner, zoneId)
            assertEquals(1, fakeTransactionDao.inserted.size)

            // Second run - no due items (cleared after first)
            fakeRecurringDao.dueItems.clear()
            scheduler.processDueRecurring(fakeRecurringDao, fakeTransactionDao, fakeRunner, zoneId)
            assertEquals(1, fakeTransactionDao.inserted.size)
        }

    @Test
    fun processDueRecurring_multipleDueItems_insertsAll() =
        runTest {
            val now = 1700000000000L
            fakeTimeProvider.setCurrentMillis(now)

            val fakeRecurringDao = FakeRecurringTransactionDao()
            val fakeTransactionDao = FakeTransactionDao()
            val fakeRunner = FakeTransactionRunner()

            fakeRecurringDao.dueItems.addAll(
                listOf(
                    RecurringTransactionEntity(
                        id = 1L,
                        type = 0,
                        amount = 50000L,
                        currencyCode = "VND",
                        categoryId = 1L,
                        note = "Item1",
                        frequency = RecurrenceFrequency.MONTHLY.toInt(),
                        dayOfMonth = 1,
                        dayOfWeek = null,
                        nextDueMillis = now - 2000L,
                        isActive = true,
                        createdAt = now,
                        updatedAt = now,
                    ),
                    RecurringTransactionEntity(
                        id = 2L,
                        type = 1,
                        amount = 100000L,
                        currencyCode = "USD",
                        categoryId = 2L,
                        note = "Item2",
                        frequency = RecurrenceFrequency.WEEKLY.toInt(),
                        dayOfMonth = null,
                        dayOfWeek = 1,
                        nextDueMillis = now - 1000L,
                        isActive = true,
                        createdAt = now,
                        updatedAt = now,
                    ),
                ),
            )

            scheduler.processDueRecurring(fakeRecurringDao, fakeTransactionDao, fakeRunner, zoneId)

            assertEquals(2, fakeTransactionDao.inserted.size)
            assertEquals(2, fakeRecurringDao.updatedNextDues.size)
        }

    // -- Fakes --

    private class FakeTransactionRunner : TransactionRunner {
        override suspend fun <R> runInTransaction(block: suspend () -> R): R = block()
    }

    private class FakeRecurringTransactionDao : RecurringTransactionDao {
        val dueItems = mutableListOf<RecurringTransactionEntity>()
        val updatedNextDues = mutableListOf<Triple<Long, Long, Long>>()

        override fun getAll(): Flow<List<RecurringTransactionEntity>> = MutableStateFlow(emptyList())

        override suspend fun getDue(nowMillis: Long): List<RecurringTransactionEntity> = dueItems.toList()

        override suspend fun insert(entity: RecurringTransactionEntity): Long = entity.id

        override suspend fun update(entity: RecurringTransactionEntity) {}

        override suspend fun deleteById(id: Long) {}

        override suspend fun updateNextDue(
            id: Long,
            nextDue: Long,
            now: Long,
        ) {
            updatedNextDues.add(Triple(id, nextDue, now))
        }

        override suspend fun setActive(
            id: Long,
            active: Boolean,
            now: Long,
        ) {}

        override suspend fun getAllList(): List<RecurringTransactionEntity> = emptyList()

        override suspend fun insertAll(list: List<RecurringTransactionEntity>) {}

        override suspend fun deleteAll() {}
    }

    private class FakeTransactionDao : TransactionDao {
        val inserted = mutableListOf<TransactionEntity>()

        override fun getTransactions(
            from: Long,
            to: Long,
            type: Int?,
        ): Flow<List<TransactionEntity>> = MutableStateFlow(emptyList())

        override suspend fun insert(entity: TransactionEntity): Long {
            inserted.add(entity)
            return entity.id
        }

        override suspend fun update(entity: TransactionEntity) {}

        override suspend fun deleteById(id: Long) {}

        override suspend fun getById(id: Long): TransactionEntity? = null

        override suspend fun getAll(): List<TransactionEntity> = emptyList()

        override suspend fun getAllOrdered(): List<TransactionEntity> = emptyList()

        override suspend fun insertAll(list: List<TransactionEntity>) {}

        override suspend fun deleteAll() {}

        override fun sumExpenseByCurrency(
            from: Long,
            to: Long,
        ): Flow<List<CurrencySumRow>> = MutableStateFlow(emptyList())

        override fun sumIncomeByCurrency(
            from: Long,
            to: Long,
        ): Flow<List<CurrencySumRow>> = MutableStateFlow(emptyList())

        override fun sumByCurrencyAndCategory(
            from: Long,
            to: Long,
            type: Int,
        ): Flow<List<CurrencyCategorySumRow>> = MutableStateFlow(emptyList())

        override fun searchTransactions(
            from: Long,
            to: Long,
            query: String,
            type: Int?,
        ): Flow<List<TransactionEntity>> = MutableStateFlow(emptyList())

        override suspend fun getMonthlyExpenseTotals(
            from: Long,
            to: Long,
            currencyCode: String,
        ): List<MonthlyTotalRow> = emptyList()

        override suspend fun reassignCategory(
            fromId: Long,
            toId: Long,
        ) {}
    }
}
