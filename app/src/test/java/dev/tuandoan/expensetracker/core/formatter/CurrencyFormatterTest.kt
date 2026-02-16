package dev.tuandoan.expensetracker.core.formatter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Comprehensive unit tests for [DefaultCurrencyFormatter].
 *
 * Tests are deterministic and locale-independent. They verify formatting for
 * all 6 supported currencies plus unknown-currency fallback and edge cases.
 */
class CurrencyFormatterTest {
    private lateinit var formatter: CurrencyFormatter

    @Before
    fun setUp() {
        formatter = DefaultCurrencyFormatter()
    }

    // -------------------------------------------------------------------------
    // VND: dot thousands, no decimals, suffix symbol with space
    // -------------------------------------------------------------------------

    @Test
    fun vnd_format_zero() {
        assertEquals("0 ₫", formatter.format(0L, "VND"))
    }

    @Test
    fun vnd_format_smallAmount() {
        assertEquals("500 ₫", formatter.format(500L, "VND"))
    }

    @Test
    fun vnd_format_thousands() {
        assertEquals("1.000 ₫", formatter.format(1000L, "VND"))
    }

    @Test
    fun vnd_format_120000() {
        assertEquals("120.000 ₫", formatter.format(120000L, "VND"))
    }

    @Test
    fun vnd_format_millions() {
        assertEquals("1.234.567 ₫", formatter.format(1234567L, "VND"))
    }

    @Test
    fun vnd_format_largeAmount() {
        assertEquals("1.234.567.890 ₫", formatter.format(1234567890L, "VND"))
    }

    @Test
    fun vnd_formatWithSign_income() {
        assertEquals("+120.000 ₫", formatter.formatWithSign(120000L, "VND", isIncome = true))
    }

    @Test
    fun vnd_formatWithSign_expense() {
        assertEquals("-120.000 ₫", formatter.formatWithSign(120000L, "VND", isIncome = false))
    }

    @Test
    fun vnd_formatWithSign_zeroIncome() {
        assertEquals("+0 ₫", formatter.formatWithSign(0L, "VND", isIncome = true))
    }

    @Test
    fun vnd_formatWithSign_zeroExpense() {
        assertEquals("-0 ₫", formatter.formatWithSign(0L, "VND", isIncome = false))
    }

    @Test
    fun vnd_formatBareAmount() {
        assertEquals("120.000", formatter.formatBareAmount(120000L, "VND"))
    }

    @Test
    fun vnd_formatBareAmount_zero() {
        assertEquals("0", formatter.formatBareAmount(0L, "VND"))
    }

    @Test
    fun vnd_format_negativeAmount() {
        assertEquals("-120.000 ₫", formatter.format(-120000L, "VND"))
    }

    // -------------------------------------------------------------------------
    // USD: comma thousands, dot decimal (2 places), prefix symbol
    // -------------------------------------------------------------------------

    @Test
    fun usd_format_zero() {
        assertEquals("$0.00", formatter.format(0L, "USD"))
    }

    @Test
    fun usd_format_oneCent() {
        assertEquals("$0.01", formatter.format(1L, "USD"))
    }

    @Test
    fun usd_format_fiveCents() {
        assertEquals("$0.05", formatter.format(5L, "USD"))
    }

    @Test
    fun usd_format_99cents() {
        assertEquals("$0.99", formatter.format(99L, "USD"))
    }

    @Test
    fun usd_format_oneDollar() {
        assertEquals("$1.00", formatter.format(100L, "USD"))
    }

    @Test
    fun usd_format_12000cents() {
        assertEquals("$120.00", formatter.format(12000L, "USD"))
    }

    @Test
    fun usd_format_largeAmount() {
        assertEquals("$1,234,567.89", formatter.format(123456789L, "USD"))
    }

    @Test
    fun usd_formatWithSign_income() {
        assertEquals("+$120.00", formatter.formatWithSign(12000L, "USD", isIncome = true))
    }

    @Test
    fun usd_formatWithSign_expense() {
        assertEquals("-$120.00", formatter.formatWithSign(12000L, "USD", isIncome = false))
    }

    @Test
    fun usd_formatBareAmount() {
        assertEquals("120.00", formatter.formatBareAmount(12000L, "USD"))
    }

    @Test
    fun usd_format_negativeAmount() {
        assertEquals("-$120.00", formatter.format(-12000L, "USD"))
    }

    @Test
    fun usd_format_negativeOneCent() {
        assertEquals("-$0.01", formatter.format(-1L, "USD"))
    }

    // -------------------------------------------------------------------------
    // EUR: comma thousands, dot decimal (2 places), prefix symbol
    // -------------------------------------------------------------------------

    @Test
    fun eur_format_zero() {
        assertEquals("€0.00", formatter.format(0L, "EUR"))
    }

    @Test
    fun eur_format_500cents() {
        assertEquals("€5.00", formatter.format(500L, "EUR"))
    }

    @Test
    fun eur_format_largeAmount() {
        assertEquals("€2,500.75", formatter.format(250075L, "EUR"))
    }

    @Test
    fun eur_formatWithSign_expense() {
        assertEquals("-€2,500.75", formatter.formatWithSign(250075L, "EUR", isIncome = false))
    }

    // -------------------------------------------------------------------------
    // JPY: comma thousands, no decimals, prefix symbol
    // -------------------------------------------------------------------------

    @Test
    fun jpy_format_zero() {
        assertEquals("¥0", formatter.format(0L, "JPY"))
    }

    @Test
    fun jpy_format_1500() {
        assertEquals("¥1,500", formatter.format(1500L, "JPY"))
    }

    @Test
    fun jpy_format_largeAmount() {
        assertEquals("¥100,000", formatter.format(100000L, "JPY"))
    }

    @Test
    fun jpy_formatWithSign_income() {
        assertEquals("+¥1,500", formatter.formatWithSign(1500L, "JPY", isIncome = true))
    }

    // -------------------------------------------------------------------------
    // KRW: comma thousands, no decimals, prefix symbol
    // -------------------------------------------------------------------------

    @Test
    fun krw_format_zero() {
        assertEquals("₩0", formatter.format(0L, "KRW"))
    }

    @Test
    fun krw_format_1500() {
        assertEquals("₩1,500", formatter.format(1500L, "KRW"))
    }

    @Test
    fun krw_format_largeAmount() {
        assertEquals("₩5,000,000", formatter.format(5000000L, "KRW"))
    }

    @Test
    fun krw_formatWithSign_expense() {
        assertEquals("-₩1,500", formatter.formatWithSign(1500L, "KRW", isIncome = false))
    }

    // -------------------------------------------------------------------------
    // SGD: comma thousands, dot decimal (2 places), prefix symbol "S$"
    // -------------------------------------------------------------------------

    @Test
    fun sgd_format_zero() {
        assertEquals("S$0.00", formatter.format(0L, "SGD"))
    }

    @Test
    fun sgd_format_12000cents() {
        assertEquals("S$120.00", formatter.format(12000L, "SGD"))
    }

    @Test
    fun sgd_format_150cents() {
        assertEquals("S$1.50", formatter.format(150L, "SGD"))
    }

    @Test
    fun sgd_format_largeAmount() {
        assertEquals("S$9,999.99", formatter.format(999999L, "SGD"))
    }

    @Test
    fun sgd_formatWithSign_income() {
        assertEquals("+S$120.00", formatter.formatWithSign(12000L, "SGD", isIncome = true))
    }

    // -------------------------------------------------------------------------
    // Unknown currency fallback: comma thousands, no decimals, suffix code
    // -------------------------------------------------------------------------

    @Test
    fun unknown_format_zero() {
        assertEquals("0 GBP", formatter.format(0L, "GBP"))
    }

    @Test
    fun unknown_format_1500() {
        assertEquals("1,500 GBP", formatter.format(1500L, "GBP"))
    }

    @Test
    fun unknown_format_largeAmount() {
        assertEquals("1,234,567 XYZ", formatter.format(1234567L, "XYZ"))
    }

    @Test
    fun unknown_formatWithSign_income() {
        assertEquals("+1,500 GBP", formatter.formatWithSign(1500L, "GBP", isIncome = true))
    }

    @Test
    fun unknown_formatWithSign_expense() {
        assertEquals("-1,500 GBP", formatter.formatWithSign(1500L, "GBP", isIncome = false))
    }

    @Test
    fun unknown_formatBareAmount() {
        assertEquals("1,500", formatter.formatBareAmount(1500L, "GBP"))
    }

    @Test
    fun unknown_format_negative() {
        assertEquals("-1,500 GBP", formatter.format(-1500L, "GBP"))
    }

    // -------------------------------------------------------------------------
    // Edge cases
    // -------------------------------------------------------------------------

    @Test
    fun longMaxValue_vnd_doesNotCrash() {
        val result = formatter.format(Long.MAX_VALUE, "VND")
        // Should produce a valid string without crashing
        assertTrue(result.endsWith(" ₫"))
        assertTrue(result.contains("."))
    }

    @Test
    fun longMaxValue_usd_doesNotCrash() {
        val result = formatter.format(Long.MAX_VALUE, "USD")
        // Should produce a valid string with $ prefix
        assertTrue(result.startsWith("$"))
        assertTrue(result.contains(","))
        assertTrue(result.contains("."))
    }

    @Test
    fun longMinValue_vnd_doesNotCrash() {
        val result = formatter.format(Long.MIN_VALUE, "VND")
        assertTrue(result.startsWith("-"))
        assertTrue(result.endsWith(" ₫"))
    }

    @Test
    fun longMinValue_usd_doesNotCrash() {
        val result = formatter.format(Long.MIN_VALUE, "USD")
        assertTrue(result.startsWith("-$"))
    }

    @Test
    fun negativeAmount_formatBareAmount_prefixesMinus() {
        assertEquals("-120.000", formatter.formatBareAmount(-120000L, "VND"))
        assertEquals("-120.00", formatter.formatBareAmount(-12000L, "USD"))
    }

    @Test
    fun formatWithSign_negativeAmount_usesIsIncomeNotAmountSign() {
        // formatWithSign takes absolute value, applies sign from isIncome flag
        assertEquals("+120.000 ₫", formatter.formatWithSign(-120000L, "VND", isIncome = true))
        assertEquals("-120.000 ₫", formatter.formatWithSign(-120000L, "VND", isIncome = false))
    }

    // -------------------------------------------------------------------------
    // Required test cases from spec
    // -------------------------------------------------------------------------

    @Test
    fun spec_vnd_120000() {
        assertEquals("120.000 ₫", formatter.format(120000L, "VND"))
    }

    @Test
    fun spec_vnd_0() {
        assertEquals("0 ₫", formatter.format(0L, "VND"))
    }

    @Test
    fun spec_usd_12000() {
        assertEquals("$120.00", formatter.format(12000L, "USD"))
    }

    @Test
    fun spec_usd_123456789() {
        assertEquals("$1,234,567.89", formatter.format(123456789L, "USD"))
    }

    @Test
    fun spec_eur_500() {
        assertEquals("€5.00", formatter.format(500L, "EUR"))
    }

    // -------------------------------------------------------------------------
    // formatBareAmount for all currencies
    // -------------------------------------------------------------------------

    @Test
    fun eur_formatBareAmount() {
        assertEquals("5.00", formatter.formatBareAmount(500L, "EUR"))
        assertEquals("2,500.75", formatter.formatBareAmount(250075L, "EUR"))
    }

    @Test
    fun jpy_formatBareAmount() {
        assertEquals("0", formatter.formatBareAmount(0L, "JPY"))
        assertEquals("1,500", formatter.formatBareAmount(1500L, "JPY"))
    }

    @Test
    fun krw_formatBareAmount() {
        assertEquals("0", formatter.formatBareAmount(0L, "KRW"))
        assertEquals("5,000,000", formatter.formatBareAmount(5000000L, "KRW"))
    }

    @Test
    fun sgd_formatBareAmount() {
        assertEquals("0.00", formatter.formatBareAmount(0L, "SGD"))
        assertEquals("120.00", formatter.formatBareAmount(12000L, "SGD"))
        assertEquals("9,999.99", formatter.formatBareAmount(999999L, "SGD"))
    }

    // -------------------------------------------------------------------------
    // Case-sensitive currency code behavior
    // -------------------------------------------------------------------------

    @Test
    fun lowercaseCurrencyCode_treatedAsUnknown() {
        // SupportedCurrencies.byCode is case-sensitive; lowercase falls through to unknown path
        assertEquals("1,000 vnd", formatter.format(1000L, "vnd"))
        assertEquals("1,000 usd", formatter.format(1000L, "usd"))
    }
}
