package dev.tuandoan.expensetracker.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class GoldSaleTest {
    @Test
    fun `totalProceeds calculates sell price times sold weight`() {
        val sale =
            GoldSale(
                id = 1L,
                holdingId = 1L,
                type = GoldType.SJC,
                soldWeight = 0.5,
                weightUnit = GoldWeightUnit.TAEL,
                buyPricePerUnit = 80_000_000L,
                sellPricePerUnit = 90_000_000L,
                currencyCode = "VND",
                saleDateMillis = 1000L,
            )
        assertEquals(45_000_000L, sale.totalProceeds)
        assertEquals(40_000_000L, sale.totalCost)
        assertEquals(5_000_000L, sale.realizedPnL)
        assertEquals(12.5, sale.realizedPnLPercent, 0.01)
    }

    @Test
    fun `realizedPnL with loss`() {
        val sale =
            GoldSale(
                id = 1L,
                holdingId = 1L,
                type = GoldType.SJC,
                soldWeight = 1.0,
                weightUnit = GoldWeightUnit.TAEL,
                buyPricePerUnit = 90_000_000L,
                sellPricePerUnit = 85_000_000L,
                currencyCode = "VND",
                saleDateMillis = 1000L,
            )
        assertEquals(-5_000_000L, sale.realizedPnL)
        assertEquals(-5.555, sale.realizedPnLPercent, 0.01)
    }

    @Test
    fun `weight conversions in grams and taels`() {
        val saleTael =
            GoldSale(
                id = 1L,
                holdingId = 1L,
                type = GoldType.SJC,
                soldWeight = 2.0,
                weightUnit = GoldWeightUnit.TAEL,
                buyPricePerUnit = 80_000_000L,
                sellPricePerUnit = 90_000_000L,
                currencyCode = "VND",
                saleDateMillis = 1000L,
            )
        assertEquals(75.0, saleTael.weightInGrams(), 0.001)
        assertEquals(2.0, saleTael.weightInTaels(), 0.001)

        val saleGram =
            GoldSale(
                id = 2L,
                holdingId = null,
                type = GoldType.GOLD_24K,
                soldWeight = 37.5,
                weightUnit = GoldWeightUnit.GRAM,
                buyPricePerUnit = 2_000_000L,
                sellPricePerUnit = 2_200_000L,
                currencyCode = "VND",
                saleDateMillis = 1000L,
            )
        assertEquals(37.5, saleGram.weightInGrams(), 0.001)
        assertEquals(1.0, saleGram.weightInTaels(), 0.001)
    }

    @Test
    fun `GoldTypeAllocation properties`() {
        val allocation =
            GoldTypeAllocation(
                type = GoldType.SJC,
                totalWeightGrams = 75.0,
                totalCost = 160_000_000L,
                currentValue = 180_000_000L,
                percentageOfPortfolio = 60.0,
            )
        assertEquals(GoldType.SJC, allocation.type)
        assertEquals(75.0, allocation.totalWeightGrams, 0.001)
        assertEquals(160_000_000L, allocation.totalCost)
        assertEquals(180_000_000L, allocation.currentValue)
        assertEquals(60.0, allocation.percentageOfPortfolio, 0.001)
    }
}
