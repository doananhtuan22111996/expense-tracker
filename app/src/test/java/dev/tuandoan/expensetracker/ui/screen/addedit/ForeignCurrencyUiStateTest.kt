package dev.tuandoan.expensetracker.ui.screen.addedit

import dev.tuandoan.expensetracker.domain.model.Trip
import dev.tuandoan.expensetracker.testutil.TestData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [AddEditTransactionUiState] foreign-currency mode:
 * [isForeignCurrencyMode], [isFormValid] with fx fields, and [parseRate].
 *
 * Covers the minor-unit pair invariants: currencies with minorUnitDigits=0
 * (VND, JPY, KRW) use whole units; currencies with minorUnitDigits=2
 * (USD, EUR, SGD) use cents. [amountForeignText] follows the same raw-digit
 * convention as [amountText] — no decimal input.
 */
class ForeignCurrencyUiStateTest {
    // --- isForeignCurrencyMode ---

    @Test
    fun isForeignCurrencyMode_noTrip_false() {
        val state = AddEditTransactionUiState(selectedTrip = null)
        assertFalse(state.isForeignCurrencyMode)
    }

    @Test
    fun isForeignCurrencyMode_tripWithoutFxCode_false() {
        val state = AddEditTransactionUiState(selectedTrip = trip(foreignCode = null))
        assertFalse(state.isForeignCurrencyMode)
    }

    @Test
    fun isForeignCurrencyMode_tripWithBlankFxCode_false() {
        val state = AddEditTransactionUiState(selectedTrip = trip(foreignCode = ""))
        assertFalse(state.isForeignCurrencyMode)
    }

    @Test
    fun isForeignCurrencyMode_tripWithFxCode_true() {
        val state = AddEditTransactionUiState(selectedTrip = trip(foreignCode = "USD"))
        assertTrue(state.isForeignCurrencyMode)
    }

    // --- isFormValid: home-currency only (no fx) ---

    @Test
    fun isFormValid_homeCurrencyOnly_missingForeignFields_trueWhenBaseValid() {
        val state =
            AddEditTransactionUiState(
                amountText = "50000",
                selectedCategory = TestData.expenseCategory,
                selectedTrip = trip(foreignCode = null),
            )
        assertTrue(state.isFormValid)
    }

    // --- isFormValid: foreign-currency mode ---

    @Test
    fun isFormValid_fxMode_missingForeignAmount_false() {
        val state =
            AddEditTransactionUiState(
                amountText = "50000",
                selectedCategory = TestData.expenseCategory,
                selectedTrip = trip(foreignCode = "USD"),
                amountForeignText = "",
                rateOverrideText = "165",
            )
        assertFalse(state.isFormValid)
    }

    @Test
    fun isFormValid_fxMode_zeroForeignAmount_false() {
        val state =
            AddEditTransactionUiState(
                amountText = "50000",
                selectedCategory = TestData.expenseCategory,
                selectedTrip = trip(foreignCode = "USD"),
                amountForeignText = "0",
                rateOverrideText = "165",
            )
        assertFalse(state.isFormValid)
    }

    @Test
    fun isFormValid_fxMode_missingRate_false() {
        val state =
            AddEditTransactionUiState(
                amountText = "50000",
                selectedCategory = TestData.expenseCategory,
                selectedTrip = trip(foreignCode = "USD"),
                amountForeignText = "100",
                rateOverrideText = "",
            )
        assertFalse(state.isFormValid)
    }

    @Test
    fun isFormValid_fxMode_zeroRate_false() {
        val state =
            AddEditTransactionUiState(
                amountText = "50000",
                selectedCategory = TestData.expenseCategory,
                selectedTrip = trip(foreignCode = "USD"),
                amountForeignText = "100",
                rateOverrideText = "0",
            )
        assertFalse(state.isFormValid)
    }

    @Test
    fun isFormValid_fxMode_negativeRate_false() {
        val state =
            AddEditTransactionUiState(
                amountText = "50000",
                selectedCategory = TestData.expenseCategory,
                selectedTrip = trip(foreignCode = "USD"),
                amountForeignText = "100",
                rateOverrideText = "-5",
            )
        assertFalse(state.isFormValid)
    }

    @Test
    fun isFormValid_fxMode_allValid_true() {
        val state =
            AddEditTransactionUiState(
                amountText = "50000",
                selectedCategory = TestData.expenseCategory,
                selectedTrip = trip(foreignCode = "USD"),
                amountForeignText = "100",
                rateOverrideText = "165",
            )
        assertTrue(state.isFormValid)
    }

    // Minor-unit pairs: VND home, USD foreign (minorUnitDigits 0 vs 2)
    @Test
    fun isFormValid_fxMode_vndHomeUsdForeign_rawCentsInput_valid() {
        // 1 USD = 165 VND; user enters "100" meaning 100 cents = 1.00 USD
        val state =
            AddEditTransactionUiState(
                amountText = "165", // 165 VND home
                currencyCode = "VND",
                selectedCategory = TestData.expenseCategory,
                selectedTrip = trip(foreignCode = "USD"),
                amountForeignText = "100", // 100 cents = 1.00 USD
                rateOverrideText = "165",
            )
        assertTrue(state.isFormValid)
    }

    @Test
    fun isFormValid_fxMode_vndHomeJpyForeign_bothZeroDecimal_valid() {
        // Both VND and JPY have minorUnitDigits=0 — user enters whole-unit amounts
        val state =
            AddEditTransactionUiState(
                amountText = "165000", // 165000 VND
                currencyCode = "VND",
                selectedCategory = TestData.expenseCategory,
                selectedTrip = trip(foreignCode = "JPY"),
                amountForeignText = "1000", // 1000 JPY
                rateOverrideText = "165",
            )
        assertTrue(state.isFormValid)
    }

    // --- parseRate ---

    @Test
    fun parseRate_blank_null() {
        assertNull(parseRate(""))
    }

    @Test
    fun parseRate_whitespace_null() {
        assertNull(parseRate("   "))
    }

    @Test
    fun parseRate_zero_null() {
        assertNull(parseRate("0"))
    }

    @Test
    fun parseRate_negative_null() {
        assertNull(parseRate("-5"))
    }

    @Test
    fun parseRate_nonNumeric_null() {
        assertNull(parseRate("abc"))
    }

    @Test
    fun parseRate_integerString_parsesCorrectly() {
        assertEquals(165.0, parseRate("165")!!, 0.0001)
    }

    @Test
    fun parseRate_dotDecimal_parsesCorrectly() {
        assertEquals(165.5, parseRate("165.5")!!, 0.0001)
    }

    @Test
    fun parseRate_commaDecimal_parsesCorrectly() {
        assertEquals(165.5, parseRate("165,5")!!, 0.0001)
    }

    @Test
    fun parseRate_smallRate_parsesCorrectly() {
        assertEquals(0.006, parseRate("0.006")!!, 0.000001)
    }

    // --- formatRate ---

    @Test
    fun formatRate_wholeNumber_noTrailingDot() {
        assertEquals("165", AddEditTransactionViewModel.formatRate(165.0))
    }

    @Test
    fun formatRate_decimal_preservesDecimal() {
        assertEquals("165.5", AddEditTransactionViewModel.formatRate(165.5))
    }

    private fun trip(foreignCode: String?) =
        Trip(
            id = 1L,
            name = "Test",
            destination = null,
            startDateEpochDay = 0L,
            endDateEpochDay = 5L,
            foreignCurrencyCode = foreignCode,
            foreignToHomeRate = if (foreignCode != null) 165.0 else null,
            originalCategoryId = null,
            originalCategoryNameSnapshot = null,
            originalCategoryIconSnapshot = null,
            originalCategoryColorSnapshot = null,
            createdAt = 0L,
        )
}
