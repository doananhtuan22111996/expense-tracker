package dev.tuandoan.expensetracker.core.formatter

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object AmountFormatter {
    private val formatter = DecimalFormat("#,###", DecimalFormatSymbols(Locale.getDefault()))

    fun formatAmount(amount: Long): String = formatter.format(amount)

    fun formatAmountWithCurrency(amount: Long): String = "${formatAmount(amount)} ₫"

    fun formatAmountWithSign(
        amount: Long,
        isIncome: Boolean,
    ): String {
        val formattedAmount = formatAmountWithCurrency(amount)
        return if (isIncome) "+$formattedAmount" else "-$formattedAmount"
    }

    fun parseAmount(text: String): Long? =
        try {
            val cleanText = text.replace("[^0-9]".toRegex(), "")
            if (cleanText.isBlank()) {
                null
            } else {
                // Prevent parsing strings that are too long to be valid Long values
                // Long.MAX_VALUE is 19 digits, so anything longer than 19 digits will overflow
                if (cleanText.length > 19) {
                    null
                } else {
                    val amount = cleanText.toLong()
                    // Accept any positive value within Long range, including 0
                    if (amount >= 0) amount else null
                }
            }
        } catch (e: NumberFormatException) {
            null
        }
}
