package dev.tuandoan.expensetracker.repository.mapper

import dev.tuandoan.expensetracker.data.database.entity.CategoryEntity
import dev.tuandoan.expensetracker.domain.model.Category
import dev.tuandoan.expensetracker.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryMapperTest {
    @Test
    fun toDomain_mapsAllFields() {
        val entity =
            CategoryEntity(
                id = 5L,
                name = "Food",
                type = 0,
                iconKey = "restaurant",
                colorKey = "red",
                isDefault = true,
            )

        val domain = entity.toDomain()

        assertEquals(5L, domain.id)
        assertEquals("Food", domain.name)
        assertEquals(TransactionType.EXPENSE, domain.type)
        assertEquals("restaurant", domain.iconKey)
        assertEquals("red", domain.colorKey)
        assertTrue(domain.isDefault)
    }

    @Test
    fun toDomain_incomeType() {
        val entity =
            CategoryEntity(
                id = 1L,
                name = "Salary",
                type = 1,
                iconKey = "payments",
                colorKey = "green",
                isDefault = false,
            )

        val domain = entity.toDomain()

        assertEquals(TransactionType.INCOME, domain.type)
        assertFalse(domain.isDefault)
    }

    @Test
    fun toDomain_nullOptionalFields() {
        val entity =
            CategoryEntity(
                id = 1L,
                name = "Other",
                type = 0,
                iconKey = null,
                colorKey = null,
                isDefault = false,
            )

        val domain = entity.toDomain()

        assertNull(domain.iconKey)
        assertNull(domain.colorKey)
    }

    @Test
    fun toEntity_mapsAllFields() {
        val domain =
            Category(
                id = 5L,
                name = "Food",
                type = TransactionType.EXPENSE,
                iconKey = "restaurant",
                colorKey = "red",
                isDefault = true,
            )

        val entity = domain.toEntity()

        assertEquals(5L, entity.id)
        assertEquals("Food", entity.name)
        assertEquals(0, entity.type)
        assertEquals("restaurant", entity.iconKey)
        assertEquals("red", entity.colorKey)
        assertTrue(entity.isDefault)
    }

    @Test
    fun toEntity_incomeType() {
        val domain =
            Category(
                id = 1L,
                name = "Salary",
                type = TransactionType.INCOME,
            )

        val entity = domain.toEntity()

        assertEquals(1, entity.type)
    }

    @Test
    fun roundTrip_preservesAllFields() {
        val original =
            CategoryEntity(
                id = 42L,
                name = "Entertainment",
                type = 0,
                iconKey = "movie",
                colorKey = "purple",
                isDefault = true,
            )

        val backToEntity = original.toDomain().toEntity()

        assertEquals(original.id, backToEntity.id)
        assertEquals(original.name, backToEntity.name)
        assertEquals(original.type, backToEntity.type)
        assertEquals(original.iconKey, backToEntity.iconKey)
        assertEquals(original.colorKey, backToEntity.colorKey)
        assertEquals(original.isDefault, backToEntity.isDefault)
    }
}
