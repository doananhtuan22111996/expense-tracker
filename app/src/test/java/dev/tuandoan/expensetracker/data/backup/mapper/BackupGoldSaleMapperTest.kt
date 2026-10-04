package dev.tuandoan.expensetracker.data.backup.mapper

import dev.tuandoan.expensetracker.data.backup.model.BackupGoldSaleDto
import dev.tuandoan.expensetracker.data.database.entity.GoldSaleEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BackupGoldSaleMapperTest {
    @Test
    fun entityToDto_mapsAllFields() {
        val entity =
            GoldSaleEntity(
                id = 1L,
                holdingId = 10L,
                type = "SJC",
                soldWeight = 1.5,
                weightUnit = "TAEL",
                buyPricePerUnit = 87_000_000L,
                sellPricePerUnit = 92_000_000L,
                currencyCode = "VND",
                saleDateMillis = 1710000000000L,
                note = "partial sell",
                createdAt = 100L,
            )

        val dto = entity.toBackupDto()

        assertEquals(1L, dto.id)
        assertEquals(10L, dto.holdingId)
        assertEquals("SJC", dto.type)
        assertEquals(1.5, dto.soldWeight, 0.001)
        assertEquals("TAEL", dto.weightUnit)
        assertEquals(87_000_000L, dto.buyPricePerUnit)
        assertEquals(92_000_000L, dto.sellPricePerUnit)
        assertEquals("VND", dto.currencyCode)
        assertEquals(1710000000000L, dto.saleDateMillis)
        assertEquals("partial sell", dto.note)
        assertEquals(100L, dto.createdAt)
    }

    @Test
    fun dtoToEntity_mapsAllFields() {
        val dto =
            BackupGoldSaleDto(
                id = 2L,
                holdingId = null,
                type = "GOLD_24K",
                soldWeight = 10.0,
                weightUnit = "GRAM",
                buyPricePerUnit = 2_000_000L,
                sellPricePerUnit = 2_150_000L,
                currencyCode = "VND",
                saleDateMillis = 1710000000000L,
                note = null,
                createdAt = 300L,
            )

        val entity = dto.toEntity()

        assertEquals(2L, entity.id)
        assertNull(entity.holdingId)
        assertEquals("GOLD_24K", entity.type)
        assertEquals(10.0, entity.soldWeight, 0.001)
        assertEquals("GRAM", entity.weightUnit)
        assertEquals(2_000_000L, entity.buyPricePerUnit)
        assertEquals(2_150_000L, entity.sellPricePerUnit)
        assertEquals("VND", entity.currencyCode)
        assertEquals(1710000000000L, entity.saleDateMillis)
        assertNull(entity.note)
        assertEquals(300L, entity.createdAt)
    }

    @Test
    fun roundTrip_preservesAllFields() {
        val original =
            GoldSaleEntity(
                id = 5L,
                holdingId = 42L,
                type = "GOLD_18K",
                soldWeight = 2.0,
                weightUnit = "OUNCE",
                buyPricePerUnit = 50_000_000L,
                sellPricePerUnit = 55_000_000L,
                currencyCode = "VND",
                saleDateMillis = 1710000000000L,
                note = "round trip test",
                createdAt = 500L,
            )

        val roundTripped = original.toBackupDto().toEntity()

        assertEquals(original, roundTripped)
    }
}
