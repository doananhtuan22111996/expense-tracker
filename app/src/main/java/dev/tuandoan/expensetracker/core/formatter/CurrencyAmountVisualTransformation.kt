package dev.tuandoan.expensetracker.core.formatter

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import dev.tuandoan.expensetracker.domain.model.SupportedCurrencies

/**
 * Formats raw digit input with thousands separators while maintaining correct cursor position.
 *
 * The underlying text field value stays as raw digits (e.g., "1000000").
 * The visual output shows formatted text (e.g., "1,000,000" or "1.000.000").
 */
class CurrencyAmountVisualTransformation(
    private val currencyCode: String,
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val originalText = text.text
        if (originalText.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        val currency = SupportedCurrencies.byCode(currencyCode)
        val separator = if (currency?.code == "VND") '.' else ','

        val formatted = addGroupingSeparator(originalText, separator)

        return TransformedText(
            AnnotatedString(formatted),
            CurrencyOffsetMapping(originalText, formatted, separator),
        )
    }

    private fun addGroupingSeparator(
        digits: String,
        separator: Char,
    ): String {
        if (digits.length <= 3) return digits
        val result = StringBuilder(digits.length + digits.length / 3)
        var count = 0
        for (i in digits.length - 1 downTo 0) {
            if (count > 0 && count % 3 == 0) {
                result.append(separator)
            }
            result.append(digits[i])
            count++
        }
        return result.reverse().toString()
    }

    private class CurrencyOffsetMapping(
        private val original: String,
        private val formatted: String,
        private val separator: Char,
    ) : OffsetMapping {
        override fun originalToTransformed(offset: Int): Int {
            // Map from raw digit position to formatted position
            // Count how many separators appear before the transformed position
            var digitsSeen = 0
            for (i in formatted.indices) {
                if (digitsSeen == offset) return i
                if (formatted[i] != separator) {
                    digitsSeen++
                }
            }
            return formatted.length
        }

        override fun transformedToOriginal(offset: Int): Int {
            // Map from formatted position to raw digit position
            // Count non-separator characters up to offset
            var digitsSeen = 0
            for (i in 0 until offset.coerceAtMost(formatted.length)) {
                if (formatted[i] != separator) {
                    digitsSeen++
                }
            }
            return digitsSeen.coerceAtMost(original.length)
        }
    }
}
