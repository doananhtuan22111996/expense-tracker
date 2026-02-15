package dev.tuandoan.expensetracker.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class CurrencyDefinitionTest {
    // --- SupportedCurrencies.all() ---

    @Test
    fun all_returnsExactly6Currencies() {
        assertEquals(6, SupportedCurrencies.all().size)
    }

    @Test
    fun all_hasCorrectOrderVndFirst() {
        val codes = SupportedCurrencies.all().map { it.code }
        assertEquals(listOf("VND", "USD", "EUR", "JPY", "KRW", "SGD"), codes)
    }

    @Test
    fun all_noDuplicateCodes() {
        val codes = SupportedCurrencies.all().map { it.code }
        assertEquals(codes.size, codes.distinct().size)
    }

    @Test
    fun all_returnsStableOrder() {
        assertEquals(SupportedCurrencies.all(), SupportedCurrencies.all())
    }

    @Test
    fun all_firstElementIsDefault() {
        assertEquals(SupportedCurrencies.default(), SupportedCurrencies.all().first())
    }

    // --- SupportedCurrencies.byCode() ---

    @Test
    fun byCode_knownCodes_returnCorrectCurrency() {
        listOf("VND", "USD", "EUR", "JPY", "KRW", "SGD").forEach { code ->
            val currency = SupportedCurrencies.byCode(code)
            assertNotNull("byCode($code) should not be null", currency)
            assertEquals(code, currency!!.code)
        }
    }

    @Test
    fun byCode_unknownCode_returnsNull() {
        assertNull(SupportedCurrencies.byCode("GBP"))
        assertNull(SupportedCurrencies.byCode("XXX"))
        assertNull(SupportedCurrencies.byCode(""))
    }

    @Test
    fun byCode_lowercaseCode_returnsNull() {
        assertNull(SupportedCurrencies.byCode("vnd"))
        assertNull(SupportedCurrencies.byCode("usd"))
    }

    // --- SupportedCurrencies.requireByCode() ---

    @Test
    fun requireByCode_knownCode_returnsCurrency() {
        val eur = SupportedCurrencies.requireByCode("EUR")
        assertEquals("EUR", eur.code)
        assertEquals("Euro", eur.displayName)
    }

    @Test
    fun requireByCode_unknownCode_throwsIllegalArgumentException() {
        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                SupportedCurrencies.requireByCode("GBP")
            }
        assertTrue(exception.message!!.contains("GBP"))
    }

    @Test
    fun requireByCode_emptyString_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            SupportedCurrencies.requireByCode("")
        }
    }

    @Test
    fun requireByCode_lowercaseCode_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            SupportedCurrencies.requireByCode("vnd")
        }
    }

    // --- SupportedCurrencies.default() ---

    @Test
    fun default_returnsVnd() {
        val default = SupportedCurrencies.default()
        assertEquals("VND", default.code)
        assertEquals("Vietnamese Dong", default.displayName)
        assertEquals("₫", default.symbol)
        assertEquals(0, default.minorUnitDigits)
    }

    @Test
    fun default_matchesByCodeVnd() {
        assertEquals(SupportedCurrencies.default(), SupportedCurrencies.byCode("VND"))
    }

    // --- minorUnitDigits correctness ---

    @Test
    fun minorUnitDigits_zeroDecimalCurrencies() {
        listOf("VND", "JPY", "KRW").forEach { code ->
            assertEquals(
                "Expected minorUnitDigits=0 for $code",
                0,
                SupportedCurrencies.requireByCode(code).minorUnitDigits,
            )
        }
    }

    @Test
    fun minorUnitDigits_twoDecimalCurrencies() {
        listOf("USD", "EUR", "SGD").forEach { code ->
            assertEquals(
                "Expected minorUnitDigits=2 for $code",
                2,
                SupportedCurrencies.requireByCode(code).minorUnitDigits,
            )
        }
    }

    // --- Symbol correctness ---

    @Test
    fun symbols_matchSpecification() {
        assertEquals("₫", SupportedCurrencies.requireByCode("VND").symbol)
        assertEquals("$", SupportedCurrencies.requireByCode("USD").symbol)
        assertEquals("€", SupportedCurrencies.requireByCode("EUR").symbol)
        assertEquals("¥", SupportedCurrencies.requireByCode("JPY").symbol)
        assertEquals("₩", SupportedCurrencies.requireByCode("KRW").symbol)
        assertEquals("S$", SupportedCurrencies.requireByCode("SGD").symbol)
    }

    // --- Display names ---

    @Test
    fun displayNames_matchSpecification() {
        assertEquals("Vietnamese Dong", SupportedCurrencies.requireByCode("VND").displayName)
        assertEquals("US Dollar", SupportedCurrencies.requireByCode("USD").displayName)
        assertEquals("Euro", SupportedCurrencies.requireByCode("EUR").displayName)
        assertEquals("Japanese Yen", SupportedCurrencies.requireByCode("JPY").displayName)
        assertEquals("South Korean Won", SupportedCurrencies.requireByCode("KRW").displayName)
        assertEquals("Singapore Dollar", SupportedCurrencies.requireByCode("SGD").displayName)
    }

    // --- Every currency in all() is retrievable via byCode ---

    @Test
    fun all_everyCurrencyRetrievableByCode() {
        SupportedCurrencies.all().forEach { currency ->
            assertEquals(currency, SupportedCurrencies.byCode(currency.code))
        }
    }

    // --- CurrencyDefinition data class behavior ---

    @Test
    fun dataClass_structuralEquality() {
        val a = CurrencyDefinition("TST", "Test Currency", "T", 0)
        val b = CurrencyDefinition("TST", "Test Currency", "T", 0)
        assertEquals(a, b)
    }

    @Test
    fun dataClass_copy() {
        val vnd = SupportedCurrencies.default()
        val modified = vnd.copy(displayName = "Modified")
        assertEquals("Modified", modified.displayName)
        assertEquals("VND", modified.code)
    }

    // --- Integration with existing defaults ---

    @Test
    fun default_codeIsVnd() {
        assertEquals("VND", SupportedCurrencies.default().code)
    }
}
