package dev.tuandoan.expensetracker.data.backup.mapper

import dev.tuandoan.expensetracker.data.backup.model.BackupTripDto
import dev.tuandoan.expensetracker.data.database.entity.TripEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BackupTripMapperTest {
    @Test
    fun toBackupDto_mapsAllFields_withConversionSnapshotsAndFx() {
        val entity =
            TripEntity(
                id = 1L,
                name = "Tokyo Trip",
                destination = "Tokyo, Japan",
                startDateEpochDay = 20000L,
                endDateEpochDay = 20010L,
                foreignCurrencyCode = "JPY",
                foreignToHomeRate = 165.5,
                originalCategoryId = 42L,
                originalCategoryNameSnapshot = "Japan Vacay",
                originalCategoryIconSnapshot = "flight",
                originalCategoryColorSnapshot = "#FF5722",
                createdAt = 1700000000000L,
            )

        val dto = entity.toBackupDto()

        assertEquals(1L, dto.id)
        assertEquals("Tokyo Trip", dto.name)
        assertEquals("Tokyo, Japan", dto.destination)
        assertEquals(20000L, dto.startDateEpochDay)
        assertEquals(20010L, dto.endDateEpochDay)
        assertEquals("JPY", dto.foreignCurrencyCode)
        assertEquals(165.5, dto.foreignToHomeRate!!, 0.0001)
        assertEquals(42L, dto.originalCategoryId)
        assertEquals("Japan Vacay", dto.originalCategoryNameSnapshot)
        assertEquals("flight", dto.originalCategoryIconSnapshot)
        assertEquals("#FF5722", dto.originalCategoryColorSnapshot)
        assertEquals(1700000000000L, dto.createdAt)
    }

    @Test
    fun toBackupDto_mapsNullableFieldsAsNull() {
        val entity =
            TripEntity(
                id = 2L,
                name = "Weekend Getaway",
                destination = null,
                startDateEpochDay = 20100L,
                endDateEpochDay = 20102L,
                foreignCurrencyCode = null,
                foreignToHomeRate = null,
                originalCategoryId = null,
                originalCategoryNameSnapshot = null,
                originalCategoryIconSnapshot = null,
                originalCategoryColorSnapshot = null,
                createdAt = 1700000001000L,
            )

        val dto = entity.toBackupDto()

        assertEquals(2L, dto.id)
        assertEquals("Weekend Getaway", dto.name)
        assertNull(dto.destination)
        assertNull(dto.foreignCurrencyCode)
        assertNull(dto.foreignToHomeRate)
        assertNull(dto.originalCategoryId)
        assertNull(dto.originalCategoryNameSnapshot)
        assertNull(dto.originalCategoryIconSnapshot)
        assertNull(dto.originalCategoryColorSnapshot)
        assertEquals(1700000001000L, dto.createdAt)
    }

    @Test
    fun toEntity_mapsAllFields() {
        val dto =
            BackupTripDto(
                id = 3L,
                name = "Da Nang Beach",
                destination = "Da Nang",
                startDateEpochDay = 19900L,
                endDateEpochDay = 19905L,
                foreignCurrencyCode = null,
                foreignToHomeRate = null,
                originalCategoryId = 15L,
                originalCategoryNameSnapshot = "Da Nang Old",
                originalCategoryIconSnapshot = "beach_access",
                originalCategoryColorSnapshot = "#2196F3",
                createdAt = 1700000002000L,
            )

        val entity = dto.toEntity()

        assertEquals(3L, entity.id)
        assertEquals("Da Nang Beach", entity.name)
        assertEquals("Da Nang", entity.destination)
        assertEquals(19900L, entity.startDateEpochDay)
        assertEquals(19905L, entity.endDateEpochDay)
        assertNull(entity.foreignCurrencyCode)
        assertNull(entity.foreignToHomeRate)
        assertEquals(15L, entity.originalCategoryId)
        assertEquals("Da Nang Old", entity.originalCategoryNameSnapshot)
        assertEquals("beach_access", entity.originalCategoryIconSnapshot)
        assertEquals("#2196F3", entity.originalCategoryColorSnapshot)
        assertEquals(1700000002000L, entity.createdAt)
    }

    @Test
    fun roundTrip_preservesAllFields() {
        val original =
            TripEntity(
                id = 4L,
                name = "Paris 2026",
                destination = "Paris, France",
                startDateEpochDay = 20500L,
                endDateEpochDay = 20507L,
                foreignCurrencyCode = "EUR",
                foreignToHomeRate = 27000.0,
                originalCategoryId = 88L,
                originalCategoryNameSnapshot = "Europe Trip",
                originalCategoryIconSnapshot = "euro",
                originalCategoryColorSnapshot = "#4CAF50",
                createdAt = 1700000003000L,
            )

        val backToEntity = original.toBackupDto().toEntity()

        assertEquals(original, backToEntity)
    }
}
