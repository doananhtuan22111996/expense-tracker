package dev.tuandoan.expensetracker.data.backup.mapper

import dev.tuandoan.expensetracker.data.backup.model.BackupCategoryDto
import dev.tuandoan.expensetracker.data.database.entity.CategoryEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupCategoryMapperTest {
    @Test
    fun toBackupDto_mapsAllFields() {
        val entity =
            CategoryEntity(
                id = 5L,
                name = "Food",
                type = 0,
                iconKey = "restaurant",
                colorKey = "red",
                isDefault = true,
            )

        val dto = entity.toBackupDto()

        assertEquals(5L, dto.id)
        assertEquals("Food", dto.name)
        assertEquals(0, dto.type)
        assertEquals("restaurant", dto.iconKey)
        assertEquals("red", dto.colorKey)
        assertTrue(dto.isDefault)
    }

    @Test
    fun toBackupDto_nullOptionalFields() {
        val entity =
            CategoryEntity(
                id = 1L,
                name = "Other",
                type = 1,
                iconKey = null,
                colorKey = null,
                isDefault = false,
            )

        val dto = entity.toBackupDto()

        assertNull(dto.iconKey)
        assertNull(dto.colorKey)
    }

    @Test
    fun toEntity_mapsAllFields() {
        val dto =
            BackupCategoryDto(
                id = 5L,
                name = "Food",
                type = 0,
                iconKey = "restaurant",
                colorKey = "red",
                isDefault = true,
            )

        val entity = dto.toEntity()

        assertEquals(5L, entity.id)
        assertEquals("Food", entity.name)
        assertEquals(0, entity.type)
        assertEquals("restaurant", entity.iconKey)
        assertEquals("red", entity.colorKey)
        assertTrue(entity.isDefault)
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

        val backToEntity = original.toBackupDto().toEntity()

        assertEquals(original.id, backToEntity.id)
        assertEquals(original.name, backToEntity.name)
        assertEquals(original.type, backToEntity.type)
        assertEquals(original.iconKey, backToEntity.iconKey)
        assertEquals(original.colorKey, backToEntity.colorKey)
        assertEquals(original.isDefault, backToEntity.isDefault)
    }

    @Test
    fun roundTrip_preservesNullOptionalFields() {
        val original =
            CategoryEntity(
                id = 1L,
                name = "Other",
                type = 1,
                iconKey = null,
                colorKey = null,
                isDefault = false,
            )

        val backToEntity = original.toBackupDto().toEntity()

        assertEquals(original.id, backToEntity.id)
        assertEquals(original.name, backToEntity.name)
        assertEquals(original.type, backToEntity.type)
        assertNull(backToEntity.iconKey)
        assertNull(backToEntity.colorKey)
        assertEquals(original.isDefault, backToEntity.isDefault)
    }
}
