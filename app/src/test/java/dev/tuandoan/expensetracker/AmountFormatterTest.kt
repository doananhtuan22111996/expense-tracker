package dev.tuandoan.expensetracker

import dev.tuandoan.expensetracker.core.formatter.AmountFormatter
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for AmountFormatter (VND-default static facade).
 * Verifies backward compatibility: all format methods delegate to DefaultCurrencyFormatter.
 * VND uses dot as thousands separator per Vietnamese convention.
 */
class AmountFormatterTest {
    @Test
    fun formatAmount_withSmallNumbers_returnsCorrectFormat() {
        assertEquals("0", AmountFormatter.formatAmount(0L))
        assertEquals("1", AmountFormatter.formatAmount(1L))
        assertEquals("999", AmountFormatter.formatAmount(999L))
    }

    @Test
    fun formatAmount_withThousands_returnsWithDots() {
        assertEquals("1.000", AmountFormatter.formatAmount(1000L))
        assertEquals("1.234", AmountFormatter.formatAmount(1234L))
        assertEquals("12.345", AmountFormatter.formatAmount(12345L))
        assertEquals("123.456", AmountFormatter.formatAmount(123456L))
    }

    @Test
    fun formatAmount_withMillions_returnsWithDots() {
        assertEquals("1.000.000", AmountFormatter.formatAmount(1000000L))
        assertEquals("1.234.567", AmountFormatter.formatAmount(1234567L))
        assertEquals("12.345.678", AmountFormatter.formatAmount(12345678L))
    }

    @Test
    fun formatAmountWithCurrency_addsVNDSymbol() {
        assertEquals("0 ₫", AmountFormatter.formatAmountWithCurrency(0L))
        assertEquals("1.000 ₫", AmountFormatter.formatAmountWithCurrency(1000L))
        assertEquals("1.234.567 ₫", AmountFormatter.formatAmountWithCurrency(1234567L))
    }

    @Test
    fun formatAmountWithSign_income_returnsPlusSign() {
        assertEquals("+0 ₫", AmountFormatter.formatAmountWithSign(0L, true))
        assertEquals("+1.000 ₫", AmountFormatter.formatAmountWithSign(1000L, true))
        assertEquals("+1.234.567 ₫", AmountFormatter.formatAmountWithSign(1234567L, true))
    }

    @Test
    fun formatAmountWithSign_expense_returnsMinusSign() {
        assertEquals("-0 ₫", AmountFormatter.formatAmountWithSign(0L, false))
        assertEquals("-1.000 ₫", AmountFormatter.formatAmountWithSign(1000L, false))
        assertEquals("-1.234.567 ₫", AmountFormatter.formatAmountWithSign(1234567L, false))
    }

    @Test
    fun formatAmount_withCurrencyCode_formatsCorrectly() {
        assertEquals("120.00", AmountFormatter.formatAmount(12000L, "USD"))
        assertEquals("1,500", AmountFormatter.formatAmount(1500L, "JPY"))
    }

    @Test
    fun formatAmountWithCurrency_withCurrencyCode_formatsCorrectly() {
        assertEquals("$120.00", AmountFormatter.formatAmountWithCurrency(12000L, "USD"))
        assertEquals("¥1,500", AmountFormatter.formatAmountWithCurrency(1500L, "JPY"))
    }

    @Test
    fun parseAmount_validInput_returnsLong() {
        assertEquals(0L, AmountFormatter.parseAmount("0"))
        assertEquals(1000L, AmountFormatter.parseAmount("1000"))
        assertEquals(1234L, AmountFormatter.parseAmount("1234"))
        assertEquals(1234567L, AmountFormatter.parseAmount("1234567"))
    }

    @Test
    fun parseAmount_withSeparators_stripsAndParses() {
        assertEquals(1000L, AmountFormatter.parseAmount("1,000"))
        assertEquals(1000L, AmountFormatter.parseAmount("1.000"))
        assertEquals(1234567L, AmountFormatter.parseAmount("1,234,567"))
        assertEquals(1234567L, AmountFormatter.parseAmount("1.234.567"))
        assertEquals(12345678L, AmountFormatter.parseAmount("12,345,678"))
    }

    @Test
    fun parseAmount_withMixedCharacters_extractsNumbers() {
        assertEquals(1000L, AmountFormatter.parseAmount("1,000 ₫"))
        assertEquals(1000L, AmountFormatter.parseAmount("1.000 ₫"))
        assertEquals(1234L, AmountFormatter.parseAmount("abc1234def"))
        assertEquals(567L, AmountFormatter.parseAmount("VND 567 currency"))
        assertEquals(0L, AmountFormatter.parseAmount("0000"))
    }

    @Test
    fun parseAmount_emptyOrInvalid_returnsNull() {
        assertNull(AmountFormatter.parseAmount(""))
        assertNull(AmountFormatter.parseAmount("   "))
        assertNull(AmountFormatter.parseAmount("abc"))
        assertNull(AmountFormatter.parseAmount("₫"))
        assertNull(AmountFormatter.parseAmount("..."))
    }

    @Test
    fun parseAmount_leadingZeros_handlesCorrectly() {
        assertEquals(123L, AmountFormatter.parseAmount("000123"))
        assertEquals(0L, AmountFormatter.parseAmount("0000"))
        assertEquals(1L, AmountFormatter.parseAmount("0001"))
    }

    @Test
    fun parseAmount_maxLongValue_handlesCorrectly() {
        val maxLongStr = Long.MAX_VALUE.toString()
        assertEquals(Long.MAX_VALUE, AmountFormatter.parseAmount(maxLongStr))
    }

    // Home list currency visibility – Phase 2.2 Item 6
    // Guards the exact call path used by TransactionItem: AmountText(showSign=true) -> formatAmountWithSign

    @Test
    fun formatAmountWithSign_usd_income_includesSymbolAndSign() {
        val result = AmountFormatter.formatAmountWithSign(12000L, isIncome = true, currencyCode = "USD")
        assertEquals("+$120.00", result)
        assertTrue(result.contains("$"))
    }

    @Test
    fun formatAmountWithSign_usd_expense_includesSymbolAndSign() {
        val result = AmountFormatter.formatAmountWithSign(12000L, isIncome = false, currencyCode = "USD")
        assertEquals("-$120.00", result)
        assertTrue(result.contains("$"))
    }

    @Test
    fun formatAmountWithSign_eur_income_includesSymbolAndSign() {
        val result = AmountFormatter.formatAmountWithSign(250075L, isIncome = true, currencyCode = "EUR")
        assertEquals("+€2,500.75", result)
        assertTrue(result.contains("€"))
    }

    @Test
    fun formatAmountWithSign_jpy_income_includesSymbolAndSign() {
        val result = AmountFormatter.formatAmountWithSign(1500L, isIncome = true, currencyCode = "JPY")
        assertEquals("+¥1,500", result)
        assertTrue(result.contains("¥"))
    }

    @Test
    fun formatAmountWithSign_unknownCurrency_includesCodeAsFallback() {
        val result = AmountFormatter.formatAmountWithSign(1500L, isIncome = false, currencyCode = "GBP")
        assertEquals("-1,500 GBP", result)
        assertTrue(result.contains("GBP"))
        assertFalse(result.contains("₫"))
        assertFalse(result.contains("$"))
    }

    @Test
    fun formatAmountWithSign_perTransactionIsolation_eachUsesOwnCurrency() {
        val vndResult = AmountFormatter.formatAmountWithSign(50000L, isIncome = false, currencyCode = "VND")
        val usdResult = AmountFormatter.formatAmountWithSign(12000L, isIncome = true, currencyCode = "USD")

        assertTrue(vndResult.contains("₫"))
        assertFalse(vndResult.contains("$"))
        assertTrue(usdResult.contains("$"))
        assertFalse(usdResult.contains("₫"))
    }

    @Test
    fun formatAndParse_roundTrip_maintainsValue() {
        val testValues = listOf(0L, 1L, 1000L, 1234567L, 999999999L)
        testValues.forEach { value ->
            val formatted = AmountFormatter.formatAmount(value)
            val parsed = AmountFormatter.parseAmount(formatted)
            assertEquals("Round trip failed for $value", value, parsed)
        }
    }
}
