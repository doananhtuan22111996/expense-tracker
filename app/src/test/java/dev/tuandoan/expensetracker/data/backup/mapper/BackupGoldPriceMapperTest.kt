package dev.tuandoan.expensetracker.data.backup.mapper

import dev.tuandoan.expensetracker.data.backup.model.BackupGoldPriceDto
import dev.tuandoan.expensetracker.data.database.entity.GoldPriceEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class BackupGoldPriceMapperTest {
    @Test
    fun entityToDto_mapsAllFields() {
        val entity =
            GoldPriceEntity(
                type = "SJC",
                unit = "TAEL",
                pricePerUnit = 92_000_000L,
                buyBackPricePerUnit = 91_000_000L,
                currencyCode = "VND",
                updatedAt = 100L,
            )

        val dto = entity.toBackupDto()

        assertEquals("SJC", dto.type)
        assertEquals("TAEL", dto.unit)
        assertEquals(92_000_000L, dto.pricePerUnit)
        assertEquals(91_000_000L, dto.buyBackPricePerUnit)
        assertEquals("VND", dto.currencyCode)
        assertEquals(100L, dto.updatedAt)
    }

    @Test
    fun dtoToEntity_mapsAllFields() {
        val dto =
            BackupGoldPriceDto(
                type = "GOLD_24K",
                unit = "GRAM",
                pricePerUnit = 2_500_000L,
                buyBackPricePerUnit = 2_400_000L,
                currencyCode = "VND",
                updatedAt = 200L,
            )

        val entity = dto.toEntity()

        assertEquals("GOLD_24K", entity.type)
        assertEquals("GRAM", entity.unit)
        assertEquals(2_500_000L, entity.pricePerUnit)
        assertEquals(2_400_000L, entity.buyBackPricePerUnit)
        assertEquals("VND", entity.currencyCode)
        assertEquals(200L, entity.updatedAt)
    }

    @Test
    fun roundTrip_preservesAllFields() {
        val original =
            GoldPriceEntity(
                type = "GOLD_18K",
                unit = "OUNCE",
                pricePerUnit = 50_000_000L,
                buyBackPricePerUnit = 49_500_000L,
                currencyCode = "VND",
                updatedAt = 300L,
            )

        val roundTripped = original.toBackupDto().toEntity()

        assertEquals(original, roundTripped)
    }

    @Test
    fun dtoToEntity_nullBuyBack_preservesNull() {
        val dto =
            BackupGoldPriceDto(
                type = "SJC",
                unit = "TAEL",
                pricePerUnit = 92_000_000L,
                currencyCode = "VND",
                updatedAt = 100L,
            )

        val entity = dto.toEntity()

        assertEquals(null, entity.buyBackPricePerUnit)
    }
}
