package dev.tuandoan.expensetracker.core.formatter

/**
 * VND-default static formatting facade.
 *
 * Delegates all formatting to [DefaultCurrencyFormatter]. Provides a static entry
 * point for contexts that cannot receive Hilt-injected dependencies (e.g., data-class
 * computed properties and Composable functions).
 *
 * For DI-capable classes (ViewModels, repositories), prefer injecting [CurrencyFormatter].
 */
object AmountFormatter {
    private val delegate: CurrencyFormatter = DefaultCurrencyFormatter()
    private const val DEFAULT_CURRENCY = "VND"

    fun formatAmount(
        amount: Long,
        currencyCode: String = DEFAULT_CURRENCY,
    ): String = delegate.formatBareAmount(amount, currencyCode)

    fun formatAmountWithCurrency(
        amount: Long,
        currencyCode: String = DEFAULT_CURRENCY,
    ): String = delegate.format(amount, currencyCode)

    fun formatAmountWithSign(
        amount: Long,
        isIncome: Boolean,
        currencyCode: String = DEFAULT_CURRENCY,
    ): String = delegate.formatWithSign(amount, currencyCode, isIncome)

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
