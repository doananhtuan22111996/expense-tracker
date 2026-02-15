package dev.tuandoan.expensetracker.repository.mapper

import dev.tuandoan.expensetracker.data.database.entity.TransactionEntity
import dev.tuandoan.expensetracker.domain.model.Category
import dev.tuandoan.expensetracker.domain.model.Transaction
import dev.tuandoan.expensetracker.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TransactionMapperTest {
    private val testCategory =
        Category(
            id = 1L,
            name = "Food",
            type = TransactionType.EXPENSE,
        )

    @Test
    fun toDomain_mapsCurrencyCode() {
        val entity =
            TransactionEntity(
                id = 1L,
                type = 0,
                amount = 50000L,
                currencyCode = "USD",
                categoryId = 1L,
                note = "Test",
                timestamp = 1700000000000L,
                createdAt = 1700000000000L,
                updatedAt = 1700000000000L,
            )

        val domain = entity.toDomain(testCategory)

        assertEquals("USD", domain.currencyCode)
    }

    @Test
    fun toDomain_defaultCurrencyCodeIsVND() {
        val entity =
            TransactionEntity(
                id = 1L,
                type = 0,
                amount = 50000L,
                categoryId = 1L,
                note = "Test",
                timestamp = 1700000000000L,
                createdAt = 1700000000000L,
                updatedAt = 1700000000000L,
            )

        val domain = entity.toDomain(testCategory)

        assertEquals("VND", domain.currencyCode)
    }

    @Test
    fun toEntity_mapsCurrencyCode() {
        val domain =
            Transaction(
                id = 1L,
                type = TransactionType.EXPENSE,
                amount = 50000L,
                currencyCode = "EUR",
                category = testCategory,
                note = "Test",
                timestamp = 1700000000000L,
                createdAt = 1700000000000L,
                updatedAt = 1700000000000L,
            )

        val entity = domain.toEntity()

        assertEquals("EUR", entity.currencyCode)
    }

    @Test
    fun toEntity_defaultCurrencyCodeIsVND() {
        val domain =
            Transaction(
                id = 1L,
                type = TransactionType.EXPENSE,
                amount = 50000L,
                category = testCategory,
                note = "Test",
                timestamp = 1700000000000L,
                createdAt = 1700000000000L,
                updatedAt = 1700000000000L,
            )

        val entity = domain.toEntity()

        assertEquals("VND", entity.currencyCode)
    }

    @Test
    fun roundTrip_preservesCurrencyCode() {
        val original =
            TransactionEntity(
                id = 42L,
                type = 1,
                amount = 1000000L,
                currencyCode = "JPY",
                categoryId = 1L,
                note = null,
                timestamp = 1700000000000L,
                createdAt = 1700000000000L,
                updatedAt = 1700000000000L,
            )

        val domain = original.toDomain(testCategory)
        val backToEntity = domain.toEntity()

        assertEquals(original.currencyCode, backToEntity.currencyCode)
        assertEquals(original.amount, backToEntity.amount)
        assertEquals(original.type, backToEntity.type)
    }

    // Additional field mapping tests

    @Test
    fun toDomain_mapsId() {
        val entity =
            TransactionEntity(
                id = 99L,
                type = 0,
                amount = 1000L,
                categoryId = 1L,
                note = null,
                timestamp = 1700000000000L,
                createdAt = 1700000000000L,
                updatedAt = 1700000000000L,
            )

        val domain = entity.toDomain(testCategory)

        assertEquals(99L, domain.id)
    }

    @Test
    fun toDomain_mapsExpenseType() {
        val entity =
            TransactionEntity(
                id = 1L,
                type = 0,
                amount = 1000L,
                categoryId = 1L,
                note = null,
                timestamp = 1700000000000L,
                createdAt = 1700000000000L,
                updatedAt = 1700000000000L,
            )

        val domain = entity.toDomain(testCategory)

        assertEquals(TransactionType.EXPENSE, domain.type)
    }

    @Test
    fun toDomain_mapsIncomeType() {
        val entity =
            TransactionEntity(
                id = 1L,
                type = 1,
                amount = 1000L,
                categoryId = 1L,
                note = null,
                timestamp = 1700000000000L,
                createdAt = 1700000000000L,
                updatedAt = 1700000000000L,
            )

        val incomeCategory =
            Category(
                id = 2L,
                name = "Salary",
                type = TransactionType.INCOME,
            )

        val domain = entity.toDomain(incomeCategory)

        assertEquals(TransactionType.INCOME, domain.type)
    }

    @Test
    fun toDomain_mapsAmount() {
        val entity =
            TransactionEntity(
                id = 1L,
                type = 0,
                amount = 999999L,
                categoryId = 1L,
                note = null,
                timestamp = 1700000000000L,
                createdAt = 1700000000000L,
                updatedAt = 1700000000000L,
            )

        val domain = entity.toDomain(testCategory)

        assertEquals(999999L, domain.amount)
    }

    @Test
    fun toDomain_mapsCategory() {
        val entity =
            TransactionEntity(
                id = 1L,
                type = 0,
                amount = 1000L,
                categoryId = 1L,
                note = null,
                timestamp = 1700000000000L,
                createdAt = 1700000000000L,
                updatedAt = 1700000000000L,
            )

        val domain = entity.toDomain(testCategory)

        assertEquals(testCategory, domain.category)
    }

    @Test
    fun toDomain_mapsNoteWhenPresent() {
        val entity =
            TransactionEntity(
                id = 1L,
                type = 0,
                amount = 1000L,
                categoryId = 1L,
                note = "Coffee at cafe",
                timestamp = 1700000000000L,
                createdAt = 1700000000000L,
                updatedAt = 1700000000000L,
            )

        val domain = entity.toDomain(testCategory)

        assertEquals("Coffee at cafe", domain.note)
    }

    @Test
    fun toDomain_mapsNullNote() {
        val entity =
            TransactionEntity(
                id = 1L,
                type = 0,
                amount = 1000L,
                categoryId = 1L,
                note = null,
                timestamp = 1700000000000L,
                createdAt = 1700000000000L,
                updatedAt = 1700000000000L,
            )

        val domain = entity.toDomain(testCategory)

        assertNull(domain.note)
    }

    @Test
    fun toDomain_mapsTimestamps() {
        val entity =
            TransactionEntity(
                id = 1L,
                type = 0,
                amount = 1000L,
                categoryId = 1L,
                note = null,
                timestamp = 1700000000000L,
                createdAt = 1700000001000L,
                updatedAt = 1700000002000L,
            )

        val domain = entity.toDomain(testCategory)

        assertEquals(1700000000000L, domain.timestamp)
        assertEquals(1700000001000L, domain.createdAt)
        assertEquals(1700000002000L, domain.updatedAt)
    }

    @Test
    fun toEntity_mapsCategoryId() {
        val domain =
            Transaction(
                id = 1L,
                type = TransactionType.EXPENSE,
                amount = 1000L,
                category = testCategory,
                note = null,
                timestamp = 1700000000000L,
                createdAt = 1700000000000L,
                updatedAt = 1700000000000L,
            )

        val entity = domain.toEntity()

        assertEquals(testCategory.id, entity.categoryId)
    }

    @Test
    fun roundTrip_preservesAllFields() {
        val original =
            TransactionEntity(
                id = 10L,
                type = 0,
                amount = 75000L,
                currencyCode = "VND",
                categoryId = 1L,
                note = "Dinner",
                timestamp = 1700000000000L,
                createdAt = 1700000001000L,
                updatedAt = 1700000002000L,
            )

        val domain = original.toDomain(testCategory)
        val backToEntity = domain.toEntity()

        assertEquals(original.id, backToEntity.id)
        assertEquals(original.type, backToEntity.type)
        assertEquals(original.amount, backToEntity.amount)
        assertEquals(original.currencyCode, backToEntity.currencyCode)
        assertEquals(original.categoryId, backToEntity.categoryId)
        assertEquals(original.note, backToEntity.note)
        assertEquals(original.timestamp, backToEntity.timestamp)
        assertEquals(original.createdAt, backToEntity.createdAt)
        assertEquals(original.updatedAt, backToEntity.updatedAt)
    }
}
