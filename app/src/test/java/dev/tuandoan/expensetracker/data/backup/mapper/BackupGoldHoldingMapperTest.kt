package dev.tuandoan.expensetracker.data.backup.mapper

import dev.tuandoan.expensetracker.data.backup.model.BackupGoldHoldingDto
import dev.tuandoan.expensetracker.data.database.entity.GoldHoldingEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BackupGoldHoldingMapperTest {
    @Test
    fun entityToDto_mapsAllFields() {
        val entity =
            GoldHoldingEntity(
                id = 1L,
                type = "SJC",
                weightValue = 2.5,
                weightUnit = "TAEL",
                buyPricePerUnit = 87_000_000L,
                currencyCode = "VND",
                buyDateMillis = 1710000000000L,
                note = "test note",
                createdAt = 100L,
                updatedAt = 200L,
            )

        val dto = entity.toBackupDto()

        assertEquals(1L, dto.id)
        assertEquals("SJC", dto.type)
        assertEquals(2.5, dto.weightValue, 0.001)
        assertEquals("TAEL", dto.weightUnit)
        assertEquals(87_000_000L, dto.buyPricePerUnit)
        assertEquals("VND", dto.currencyCode)
        assertEquals(1710000000000L, dto.buyDateMillis)
        assertEquals("test note", dto.note)
        assertEquals(100L, dto.createdAt)
        assertEquals(200L, dto.updatedAt)
    }

    @Test
    fun dtoToEntity_mapsAllFields() {
        val dto =
            BackupGoldHoldingDto(
                id = 2L,
                type = "GOLD_24K",
                weightValue = 1.0,
                weightUnit = "GRAM",
                buyPricePerUnit = 2_000_000L,
                currencyCode = "VND",
                buyDateMillis = 1710000000000L,
                note = null,
                createdAt = 300L,
                updatedAt = 400L,
            )

        val entity = dto.toEntity()

        assertEquals(2L, entity.id)
        assertEquals("GOLD_24K", entity.type)
        assertEquals(1.0, entity.weightValue, 0.001)
        assertEquals("GRAM", entity.weightUnit)
        assertEquals(2_000_000L, entity.buyPricePerUnit)
        assertEquals("VND", entity.currencyCode)
        assertEquals(1710000000000L, entity.buyDateMillis)
        assertNull(entity.note)
        assertEquals(300L, entity.createdAt)
        assertEquals(400L, entity.updatedAt)
    }

    @Test
    fun roundTrip_preservesAllFields() {
        val original =
            GoldHoldingEntity(
                id = 5L,
                type = "GOLD_18K",
                weightValue = 3.14,
                weightUnit = "OUNCE",
                buyPricePerUnit = 50_000_000L,
                currencyCode = "VND",
                buyDateMillis = 1710000000000L,
                note = "round trip",
                createdAt = 500L,
                updatedAt = 600L,
            )

        val roundTripped = original.toBackupDto().toEntity()

        assertEquals(original, roundTripped)
    }
}
