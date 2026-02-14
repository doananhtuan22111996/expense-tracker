package dev.tuandoan.expensetracker.repository.mapper

import dev.tuandoan.expensetracker.data.database.entity.TransactionEntity
import dev.tuandoan.expensetracker.domain.model.Category
import dev.tuandoan.expensetracker.domain.model.Transaction
import dev.tuandoan.expensetracker.domain.model.TransactionType
import org.junit.Assert.assertEquals
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
}
