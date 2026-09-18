package dev.tuandoan.expensetracker.core.currency

import org.junit.Assert.assertEquals
import org.junit.Test

class AmountCalculatorTest {
    @Test
    fun toHomeMinor_zeroMinorDigits_to_zeroMinorDigits() {
        // ¥1,200 (JPY, 0 minor digits) @ 165.0 VND/JPY -> ₫198,000 (VND, 0 minor digits)
        val result =
            AmountCalculator.toHomeMinor(
                foreignMinor = 1200L,
                foreignDigits = 0,
                homeDigits = 0,
                rate = 165.0,
            )
        assertEquals(198000L, result)
    }

    @Test
    fun toHomeMinor_zeroMinorDigits_to_twoMinorDigits() {
        // ¥1,200 (JPY, 0 digits) @ 0.0068 USD/JPY -> $8.16 (USD, 2 digits -> 816 cents)
        val result =
            AmountCalculator.toHomeMinor(
                foreignMinor = 1200L,
                foreignDigits = 0,
                homeDigits = 2,
                rate = 0.0068,
            )
        assertEquals(816L, result)
    }

    @Test
    fun toHomeMinor_twoMinorDigits_to_zeroMinorDigits() {
        // $1.00 (USD, 2 digits -> 100 cents) @ 25,400 VND/USD -> ₫25,400 (VND, 0 digits)
        val result =
            AmountCalculator.toHomeMinor(
                foreignMinor = 100L,
                foreignDigits = 2,
                homeDigits = 0,
                rate = 25400.0,
            )
        assertEquals(25400L, result)
    }

    @Test
    fun toHomeMinor_twoMinorDigits_to_twoMinorDigits() {
        // $1.00 (USD, 2 digits -> 100 cents) @ 0.92 EUR/USD -> €0.92 (EUR, 2 digits -> 92 cents)
        val result =
            AmountCalculator.toHomeMinor(
                foreignMinor = 100L,
                foreignDigits = 2,
                homeDigits = 2,
                rate = 0.92,
            )
        assertEquals(92L, result)
    }

    @Test
    fun toHomeMinor_roundingHalfEven() {
        // $1.25 (125 cents) @ 1.5 rate, both 2 digits -> 125 * 1.5 = 187.5 -> round to even: 188
        val result =
            AmountCalculator.toHomeMinor(
                foreignMinor = 125L,
                foreignDigits = 2,
                homeDigits = 2,
                rate = 1.5,
            )
        assertEquals(188L, result)
    }

    @Test
    fun toHomeMinor_zeroOrNegative_returnsZero() {
        assertEquals(0L, AmountCalculator.toHomeMinor(0L, 2, 2, 1.5))
        assertEquals(0L, AmountCalculator.toHomeMinor(-10L, 2, 2, 1.5))
        assertEquals(0L, AmountCalculator.toHomeMinor(100L, 2, 2, 0.0))
        assertEquals(0L, AmountCalculator.toHomeMinor(100L, 2, 2, -1.0))
    }
}
