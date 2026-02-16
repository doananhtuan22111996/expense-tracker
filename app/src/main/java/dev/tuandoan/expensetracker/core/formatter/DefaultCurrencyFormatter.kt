package dev.tuandoan.expensetracker.core.formatter

import dev.tuandoan.expensetracker.domain.model.CurrencyDefinition
import dev.tuandoan.expensetracker.domain.model.SupportedCurrencies
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * Production implementation of [CurrencyFormatter].
 *
 * Formatting rules are hardcoded per currency using [SupportedCurrencies].
 * No java.util.Locale, no java.text.NumberFormat, no locale-dependent APIs.
 * Pure string manipulation only — stateless and thread-safe.
 */
@Singleton
class DefaultCurrencyFormatter
    @Inject
    constructor() : CurrencyFormatter {
        override fun format(
            amountMinor: Long,
            currencyCode: String,
        ): String {
            val currency = SupportedCurrencies.byCode(currencyCode)
            val isNegative = amountMinor < 0
            val absAmount = safeAbs(amountMinor)

            if (currency == null) {
                val bare = addGroupingSeparator(absAmount.toString(), ',')
                val result = "$bare $currencyCode"
                return if (isNegative) "-$result" else result
            }

            val withSymbol = formatPositiveWithSymbol(absAmount, currency)
            return if (isNegative) "-$withSymbol" else withSymbol
        }

        override fun formatWithSign(
            amountMinor: Long,
            currencyCode: String,
            isIncome: Boolean,
        ): String {
            val absAmount = safeAbs(amountMinor)
            val currency = SupportedCurrencies.byCode(currencyCode)
            val sign = if (isIncome) "+" else "-"

            if (currency == null) {
                val bare = addGroupingSeparator(absAmount.toString(), ',')
                return "$sign$bare $currencyCode"
            }

            val withSymbol = formatPositiveWithSymbol(absAmount, currency)
            return "$sign$withSymbol"
        }

        override fun formatBareAmount(
            amountMinor: Long,
            currencyCode: String,
        ): String {
            val currency = SupportedCurrencies.byCode(currencyCode)
            val isNegative = amountMinor < 0
            val absAmount = safeAbs(amountMinor)

            val bare =
                if (currency == null) {
                    addGroupingSeparator(absAmount.toString(), ',')
                } else {
                    formatPositiveBare(absAmount, currency)
                }
            return if (isNegative) "-$bare" else bare
        }

        private fun formatPositiveWithSymbol(
            absAmount: Long,
            currency: CurrencyDefinition,
        ): String {
            val bare = formatPositiveBare(absAmount, currency)
            return if (currency.code == "VND") {
                "$bare ${currency.symbol}"
            } else {
                "${currency.symbol}$bare"
            }
        }

        private fun formatPositiveBare(
            absAmount: Long,
            currency: CurrencyDefinition,
        ): String {
            val minorDigits = currency.minorUnitDigits
            val thousandsSep = if (currency.code == "VND") '.' else ','

            return if (minorDigits == 0) {
                addGroupingSeparator(absAmount.toString(), thousandsSep)
            } else {
                val divisor = pow10(minorDigits)
                val majorPart = absAmount / divisor
                val minorPart = absAmount % divisor
                val majorStr = addGroupingSeparator(majorPart.toString(), thousandsSep)
                "$majorStr.${minorPart.toString().padStart(minorDigits, '0')}"
            }
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

        private fun safeAbs(value: Long): Long = if (value == Long.MIN_VALUE) Long.MAX_VALUE else abs(value)

        private fun pow10(n: Int): Long =
            when (n) {
                0 -> 1L
                1 -> 10L
                2 -> 100L
                3 -> 1000L
                else -> error("Unsupported minor unit digits: $n")
            }
    }
