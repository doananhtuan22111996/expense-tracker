package dev.tuandoan.expensetracker.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GoldPortfolioSummaryTest {
    // --- Portfolio Summary: market P&L ---

    @Test
    fun `portfolio summary calculates market pnl correctly`() {
        val summary =
            GoldPortfolioSummary(
                totalCost = 174_000_000L,
                totalMarketValue = 186_000_000L,
                currencyCode = "VND",
            )
        assertEquals(12_000_000L, summary.marketPnL)
        assertEquals(6.896, summary.marketPnLPercent, 0.01)
    }

    @Test
    fun `portfolio summary with market loss`() {
        val summary =
            GoldPortfolioSummary(
                totalCost = 174_000_000L,
                totalMarketValue = 160_000_000L,
                currencyCode = "VND",
            )
        assertEquals(-14_000_000L, summary.marketPnL)
        assertEquals(-8.045, summary.marketPnLPercent, 0.01)
    }

    @Test
    fun `portfolio summary with zero cost returns zero percent`() {
        val summary =
            GoldPortfolioSummary(
                totalCost = 0L,
                totalMarketValue = 0L,
                currencyCode = "VND",
            )
        assertEquals(0L, summary.marketPnL)
        assertEquals(0.0, summary.marketPnLPercent, 0.001)
    }

    // --- Portfolio Summary: liquidation P&L ---

    @Test
    fun `portfolio summary calculates liquidation pnl correctly`() {
        val summary =
            GoldPortfolioSummary(
                totalCost = 174_000_000L,
                totalMarketValue = 186_000_000L,
                totalLiquidationValue = 182_000_000L,
                currencyCode = "VND",
            )
        assertEquals(8_000_000L, summary.liquidationPnL)
        assertEquals(4.597, summary.liquidationPnLPercent!!, 0.01)
    }

    @Test
    fun `portfolio summary liquidation null when no buyback`() {
        val summary =
            GoldPortfolioSummary(
                totalCost = 174_000_000L,
                totalMarketValue = 186_000_000L,
                totalLiquidationValue = null,
                currencyCode = "VND",
            )
        assertNull(summary.liquidationPnL)
        assertNull(summary.liquidationPnLPercent)
    }

    @Test
    fun `portfolio summary liquidation loss`() {
        val summary =
            GoldPortfolioSummary(
                totalCost = 186_000_000L,
                totalMarketValue = 188_000_000L,
                totalLiquidationValue = 182_000_000L,
                currencyCode = "VND",
            )
        assertEquals(-4_000_000L, summary.liquidationPnL)
    }

    // --- HoldingWithPnL: market values ---

    @Test
    fun `holdingWithPnL marketValue uses sellPrice`() {
        val holding = testHolding(buyPrice = 87_000_000L, weight = 2.0)
        val withPnL =
            GoldHoldingWithPnL(
                holding = holding,
                currentSellPricePerUnit = 93_000_000L,
                currentBuyBackPricePerUnit = 91_000_000L,
            )

        assertEquals(174_000_000L, withPnL.totalCost)
        assertEquals(186_000_000L, withPnL.marketValue)
        assertEquals(12_000_000L, withPnL.marketPnL)
        assertEquals(6.896, withPnL.marketPnLPercent!!, 0.01)
    }

    @Test
    fun `holdingWithPnL returns null when no sell price`() {
        val holding = testHolding(buyPrice = 87_000_000L, weight = 2.0)
        val withPnL =
            GoldHoldingWithPnL(
                holding = holding,
                currentSellPricePerUnit = null,
            )

        assertEquals(174_000_000L, withPnL.totalCost)
        assertNull(withPnL.marketValue)
        assertNull(withPnL.marketPnL)
        assertNull(withPnL.marketPnLPercent)
    }

    @Test
    fun `holdingWithPnL with market loss`() {
        val holding = testHolding(buyPrice = 2_500_000L, weight = 10.0)
        val withPnL =
            GoldHoldingWithPnL(
                holding = holding,
                currentSellPricePerUnit = 2_400_000L,
            )

        assertEquals(25_000_000L, withPnL.totalCost)
        assertEquals(24_000_000L, withPnL.marketValue)
        assertEquals(-1_000_000L, withPnL.marketPnL)
        assertEquals(-4.0, withPnL.marketPnLPercent!!, 0.01)
    }

    // --- HoldingWithPnL: liquidation values ---

    @Test
    fun `holdingWithPnL liquidationValue uses buyBackPrice`() {
        val holding = testHolding(buyPrice = 87_000_000L, weight = 2.0)
        val withPnL =
            GoldHoldingWithPnL(
                holding = holding,
                currentSellPricePerUnit = 93_000_000L,
                currentBuyBackPricePerUnit = 91_000_000L,
            )

        assertEquals(182_000_000L, withPnL.liquidationValue)
        assertEquals(8_000_000L, withPnL.liquidationPnL)
        assertEquals(4.597, withPnL.liquidationPnLPercent!!, 0.01)
    }

    @Test
    fun `holdingWithPnL liquidationValue null when buyBack null`() {
        val holding = testHolding(buyPrice = 87_000_000L, weight = 2.0)
        val withPnL =
            GoldHoldingWithPnL(
                holding = holding,
                currentSellPricePerUnit = 93_000_000L,
                currentBuyBackPricePerUnit = null,
            )

        assertNull(withPnL.liquidationValue)
        assertNull(withPnL.liquidationPnL)
        assertNull(withPnL.liquidationPnLPercent)
    }

    @Test
    fun `holdingWithPnL liquidation loss scenario`() {
        val holding = testHolding(buyPrice = 93_000_000L, weight = 2.0)
        val withPnL =
            GoldHoldingWithPnL(
                holding = holding,
                currentSellPricePerUnit = 94_000_000L,
                currentBuyBackPricePerUnit = 91_000_000L,
            )

        assertEquals(182_000_000L, withPnL.liquidationValue)
        assertEquals(-4_000_000L, withPnL.liquidationPnL)
    }

    // --- Helpers ---

    private fun testHolding(
        buyPrice: Long,
        weight: Double,
        type: GoldType = GoldType.SJC,
        unit: GoldWeightUnit = GoldWeightUnit.TAEL,
    ) = GoldHolding(
        type = type,
        weightValue = weight,
        weightUnit = unit,
        buyPricePerUnit = buyPrice,
        buyDateMillis = 1000L,
    )
}
