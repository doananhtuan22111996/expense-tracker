package dev.tuandoan.expensetracker.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GoldPortfolioSummaryTest {
    @Test
    fun `portfolio summary calculates pnl correctly`() {
        val summary =
            GoldPortfolioSummary(
                totalCost = 174_000_000L,
                totalCurrentValue = 186_000_000L,
                currencyCode = "VND",
            )
        assertEquals(12_000_000L, summary.totalPnL)
        assertEquals(6.896, summary.pnLPercent, 0.01)
    }

    @Test
    fun `portfolio summary with loss`() {
        val summary =
            GoldPortfolioSummary(
                totalCost = 174_000_000L,
                totalCurrentValue = 160_000_000L,
                currencyCode = "VND",
            )
        assertEquals(-14_000_000L, summary.totalPnL)
        assertEquals(-8.045, summary.pnLPercent, 0.01)
    }

    @Test
    fun `portfolio summary with zero cost returns zero percent`() {
        val summary =
            GoldPortfolioSummary(
                totalCost = 0L,
                totalCurrentValue = 0L,
                currencyCode = "VND",
            )
        assertEquals(0L, summary.totalPnL)
        assertEquals(0.0, summary.pnLPercent, 0.001)
    }

    @Test
    fun `holdingWithPnL calculates values when current price set`() {
        val holding =
            GoldHolding(
                type = GoldType.SJC,
                weightValue = 2.0,
                weightUnit = GoldWeightUnit.TAEL,
                buyPricePerUnit = 87_000_000L,
                buyDateMillis = 1000L,
            )
        val withPnL = GoldHoldingWithPnL(holding = holding, currentPricePerUnit = 93_000_000L)

        assertEquals(174_000_000L, withPnL.totalCost)
        assertEquals(186_000_000L, withPnL.currentValue)
        assertEquals(12_000_000L, withPnL.pnL)
        assertEquals(6.896, withPnL.pnLPercent!!, 0.01)
    }

    @Test
    fun `holdingWithPnL returns null when no current price`() {
        val holding =
            GoldHolding(
                type = GoldType.SJC,
                weightValue = 2.0,
                weightUnit = GoldWeightUnit.TAEL,
                buyPricePerUnit = 87_000_000L,
                buyDateMillis = 1000L,
            )
        val withPnL = GoldHoldingWithPnL(holding = holding, currentPricePerUnit = null)

        assertEquals(174_000_000L, withPnL.totalCost)
        assertNull(withPnL.currentValue)
        assertNull(withPnL.pnL)
        assertNull(withPnL.pnLPercent)
    }

    @Test
    fun `holdingWithPnL with loss`() {
        val holding =
            GoldHolding(
                type = GoldType.GOLD_24K,
                weightValue = 10.0,
                weightUnit = GoldWeightUnit.GRAM,
                buyPricePerUnit = 2_500_000L,
                buyDateMillis = 1000L,
            )
        val withPnL = GoldHoldingWithPnL(holding = holding, currentPricePerUnit = 2_400_000L)

        assertEquals(25_000_000L, withPnL.totalCost)
        assertEquals(24_000_000L, withPnL.currentValue)
        assertEquals(-1_000_000L, withPnL.pnL)
        assertEquals(-4.0, withPnL.pnLPercent!!, 0.01)
    }
}
