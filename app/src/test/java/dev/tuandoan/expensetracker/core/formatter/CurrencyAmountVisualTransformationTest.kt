package dev.tuandoan.expensetracker.core.formatter

import androidx.compose.ui.text.AnnotatedString
import org.junit.Assert.assertEquals
import org.junit.Test

class CurrencyAmountVisualTransformationTest {
    @Test
    fun emptyInput_returnsEmpty() {
        val transformation = CurrencyAmountVisualTransformation("VND")
        val result = transformation.filter(AnnotatedString(""))
        assertEquals("", result.text.text)
    }

    @Test
    fun shortInput_noSeparator() {
        val transformation = CurrencyAmountVisualTransformation("VND")
        val result = transformation.filter(AnnotatedString("123"))
        assertEquals("123", result.text.text)
    }

    @Test
    fun vnd_usesDotSeparator() {
        val transformation = CurrencyAmountVisualTransformation("VND")
        val result = transformation.filter(AnnotatedString("1000000"))
        assertEquals("1.000.000", result.text.text)
    }

    @Test
    fun usd_usesCommaSeparator() {
        val transformation = CurrencyAmountVisualTransformation("USD")
        val result = transformation.filter(AnnotatedString("1000000"))
        assertEquals("1,000,000", result.text.text)
    }

    @Test
    fun fourDigits_oneGroup() {
        val transformation = CurrencyAmountVisualTransformation("USD")
        val result = transformation.filter(AnnotatedString("5000"))
        assertEquals("5,000", result.text.text)
    }

    @Test
    fun offsetMapping_originalToTransformed_atEnd() {
        val transformation = CurrencyAmountVisualTransformation("USD")
        val result = transformation.filter(AnnotatedString("50000"))
        // "50000" -> "50,000"
        assertEquals("50,000", result.text.text)
        // Cursor after all 5 digits -> position 6 (after "50,000")
        assertEquals(6, result.offsetMapping.originalToTransformed(5))
    }

    @Test
    fun offsetMapping_originalToTransformed_beforeSeparator() {
        val transformation = CurrencyAmountVisualTransformation("USD")
        val result = transformation.filter(AnnotatedString("50000"))
        // "50000" -> "50,000"
        // Cursor after 2 digits (after "50") -> position 2 (before comma)
        assertEquals(2, result.offsetMapping.originalToTransformed(2))
    }

    @Test
    fun offsetMapping_transformedToOriginal_afterSeparator() {
        val transformation = CurrencyAmountVisualTransformation("USD")
        val result = transformation.filter(AnnotatedString("50000"))
        // "50,000" -> position 3 (after comma) maps to original position 2
        assertEquals(2, result.offsetMapping.transformedToOriginal(3))
    }

    @Test
    fun offsetMapping_transformedToOriginal_atEnd() {
        val transformation = CurrencyAmountVisualTransformation("USD")
        val result = transformation.filter(AnnotatedString("50000"))
        // "50,000" -> position 6 (end) maps to original position 5 (end)
        assertEquals(5, result.offsetMapping.transformedToOriginal(6))
    }

    @Test
    fun offsetMapping_transformedToOriginal_atStart() {
        val transformation = CurrencyAmountVisualTransformation("USD")
        val result = transformation.filter(AnnotatedString("50000"))
        assertEquals(0, result.offsetMapping.transformedToOriginal(0))
    }

    @Test
    fun singleDigit_noFormatting() {
        val transformation = CurrencyAmountVisualTransformation("USD")
        val result = transformation.filter(AnnotatedString("5"))
        assertEquals("5", result.text.text)
    }

    @Test
    fun largeNumber_multipleGroups() {
        val transformation = CurrencyAmountVisualTransformation("VND")
        val result = transformation.filter(AnnotatedString("1234567890"))
        assertEquals("1.234.567.890", result.text.text)
    }
}
