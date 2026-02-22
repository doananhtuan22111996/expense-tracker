package dev.tuandoan.expensetracker.core.formatter

/**
 * Formats monetary amounts for display using currency-specific conventions.
 *
 * All implementations must be:
 * - Deterministic: same output on all devices regardless of locale.
 * - Thread-safe: no mutable shared state.
 * - Offline: no network calls, no locale-dependent APIs.
 *
 * Format conventions:
 *   VND:           dot thousands, no decimals, suffix symbol with space  ("120.000 ₫")
 *   USD/EUR/SGD:   comma thousands, dot decimal (2 places), prefix symbol ("$120.00")
 *   JPY/KRW:       comma thousands, no decimals, prefix symbol            ("¥1,500")
 *   Unknown:       comma thousands, no decimals, suffix code              ("1,500 GBP")
 *
 * Amount representation:
 *   For currencies with minorUnitDigits=0 (VND, JPY, KRW): amount is the whole-unit value.
 *   For currencies with minorUnitDigits=2 (USD, EUR, SGD): amount is in minor units (cents).
 */
interface CurrencyFormatter {
    /**
     * Formats [amountMinor] with the currency symbol.
     *
     * Examples:
     *   format(120000, "VND") -> "120.000 ₫"
     *   format(12000, "USD")  -> "$120.00"
     *   format(1500, "JPY")   -> "¥1,500"
     *   format(1500, "GBP")   -> "1,500 GBP"  (unknown currency fallback)
     *
     * Negative amounts are prefixed with "-": format(-120000, "VND") -> "-120.000 ₫"
     */
    fun format(
        amountMinor: Long,
        currencyCode: String,
    ): String

    /**
     * Formats [amountMinor] with a leading +/- sign and the currency symbol.
     * The sign is determined by [isIncome], not by the amount's numeric sign.
     *
     * Examples:
     *   formatWithSign(120000, "VND", isIncome = true)  -> "+120.000 ₫"
     *   formatWithSign(12000, "USD", isIncome = false)   -> "-$120.00"
     */
    fun formatWithSign(
        amountMinor: Long,
        currencyCode: String,
        isIncome: Boolean,
    ): String

    /**
     * Formats [amountMinor] as a bare number string without currency symbol.
     * Used for amount input fields.
     *
     * Examples:
     *   formatBareAmount(120000, "VND") -> "120.000"
     *   formatBareAmount(12000, "USD")  -> "120.00"
     */
    fun formatBareAmount(
        amountMinor: Long,
        currencyCode: String,
    ): String
}
