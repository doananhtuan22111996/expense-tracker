package dev.tuandoan.expensetracker.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class GoldHoldingTest {
    @Test
    fun `totalCost calculates buy price times weight`() {
        val holding =
            GoldHolding(
                type = GoldType.SJC,
                weightValue = 2.0,
                weightUnit = GoldWeightUnit.TAEL,
                buyPricePerUnit = 87_000_000L,
                buyDateMillis = 1000L,
            )
        assertEquals(174_000_000L, holding.totalCost)
    }

    @Test
    fun `totalCost with fractional weight`() {
        val holding =
            GoldHolding(
                type = GoldType.GOLD_24K,
                weightValue = 0.5,
                weightUnit = GoldWeightUnit.TAEL,
                buyPricePerUnit = 87_000_000L,
                buyDateMillis = 1000L,
            )
        assertEquals(43_500_000L, holding.totalCost)
    }

    @Test
    fun `GoldType fromString returns correct type`() {
        assertEquals(GoldType.SJC, GoldType.fromString("SJC"))
        assertEquals(GoldType.GOLD_24K, GoldType.fromString("GOLD_24K"))
        assertEquals(GoldType.GOLD_18K, GoldType.fromString("GOLD_18K"))
        assertEquals(GoldType.OTHER, GoldType.fromString("OTHER"))
    }

    @Test
    fun `GoldType fromString throws for unknown type`() {
        assertThrows(IllegalArgumentException::class.java) {
            GoldType.fromString("UNKNOWN")
        }
    }

    @Test
    fun `GoldWeightUnit fromString returns correct unit`() {
        assertEquals(GoldWeightUnit.TAEL, GoldWeightUnit.fromString("TAEL"))
        assertEquals(GoldWeightUnit.GRAM, GoldWeightUnit.fromString("GRAM"))
        assertEquals(GoldWeightUnit.OUNCE, GoldWeightUnit.fromString("OUNCE"))
    }

    @Test
    fun `GoldWeightUnit fromString throws for unknown unit`() {
        assertThrows(IllegalArgumentException::class.java) {
            GoldWeightUnit.fromString("UNKNOWN")
        }
    }

    @Test
    fun `GoldWeightUnit gramsPerUnit values are correct`() {
        assertEquals(37.5, GoldWeightUnit.TAEL.gramsPerUnit, 0.001)
        assertEquals(1.0, GoldWeightUnit.GRAM.gramsPerUnit, 0.001)
        assertEquals(31.1035, GoldWeightUnit.OUNCE.gramsPerUnit, 0.001)
    }

    @Test
    fun `weightInGrams calculates grams correctly across units`() {
        val holdingTael =
            GoldHolding(
                type = GoldType.SJC,
                weightValue = 2.0,
                weightUnit = GoldWeightUnit.TAEL,
                buyPricePerUnit = 80_000_000L,
                buyDateMillis = 1000L,
            )
        assertEquals(75.0, holdingTael.weightInGrams(), 0.001)

        val holdingGram =
            GoldHolding(
                type = GoldType.GOLD_24K,
                weightValue = 10.0,
                weightUnit = GoldWeightUnit.GRAM,
                buyPricePerUnit = 2_000_000L,
                buyDateMillis = 1000L,
            )
        assertEquals(10.0, holdingGram.weightInGrams(), 0.001)

        val holdingOunce =
            GoldHolding(
                type = GoldType.OTHER,
                weightValue = 1.0,
                weightUnit = GoldWeightUnit.OUNCE,
                buyPricePerUnit = 50_000_000L,
                buyDateMillis = 1000L,
            )
        assertEquals(31.1035, holdingOunce.weightInGrams(), 0.001)
    }

    @Test
    fun `weightInTaels converts correctly to taels`() {
        val holdingTael =
            GoldHolding(
                type = GoldType.SJC,
                weightValue = 2.0,
                weightUnit = GoldWeightUnit.TAEL,
                buyPricePerUnit = 80_000_000L,
                buyDateMillis = 1000L,
            )
        assertEquals(2.0, holdingTael.weightInTaels(), 0.001)

        val holdingGram =
            GoldHolding(
                type = GoldType.GOLD_24K,
                weightValue = 37.5,
                weightUnit = GoldWeightUnit.GRAM,
                buyPricePerUnit = 2_000_000L,
                buyDateMillis = 1000L,
            )
        assertEquals(1.0, holdingGram.weightInTaels(), 0.001)
    }
}
