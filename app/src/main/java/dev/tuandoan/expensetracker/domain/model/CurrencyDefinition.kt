package dev.tuandoan.expensetracker.domain.model

/**
 * Represents a currency supported by the app.
 *
 * @property code ISO 4217 currency code (e.g., "VND", "USD").
 * @property displayName Human-readable English name (e.g., "Vietnamese Dong").
 * @property symbol Currency symbol for display (e.g., "₫", "$", "S$").
 * @property minorUnitDigits Number of decimal places for this currency's minor unit.
 *   0 for VND, JPY, KRW; 2 for USD, EUR, SGD.
 */
data class CurrencyDefinition(
    val code: String,
    val displayName: String,
    val symbol: String,
    val minorUnitDigits: Int,
)

/**
 * Static, offline-only registry of all currencies supported by the app.
 *
 * This is a compile-time constant registry. No networking, no DI, no database.
 * All functions are pure and safe to call from any thread.
 */
object SupportedCurrencies {
    private val currencies: List<CurrencyDefinition> =
        listOf(
            CurrencyDefinition(code = "VND", displayName = "Vietnamese Dong", symbol = "₫", minorUnitDigits = 0),
            CurrencyDefinition(code = "USD", displayName = "US Dollar", symbol = "$", minorUnitDigits = 2),
            CurrencyDefinition(code = "EUR", displayName = "Euro", symbol = "€", minorUnitDigits = 2),
            CurrencyDefinition(code = "JPY", displayName = "Japanese Yen", symbol = "¥", minorUnitDigits = 0),
            CurrencyDefinition(code = "KRW", displayName = "South Korean Won", symbol = "₩", minorUnitDigits = 0),
            CurrencyDefinition(code = "SGD", displayName = "Singapore Dollar", symbol = "S$", minorUnitDigits = 2),
        )

    private val byCodeMap: Map<String, CurrencyDefinition> =
        currencies.associateBy { it.code }

    /** Returns all supported currencies in deterministic order (VND first, then as listed). */
    fun all(): List<CurrencyDefinition> = currencies

    /**
     * Returns the currency for the given ISO 4217 [code], or null if unsupported.
     * Lookup is case-sensitive; codes must be uppercase (e.g., "VND", not "vnd").
     */
    fun byCode(code: String): CurrencyDefinition? = byCodeMap[code]

    /**
     * Returns the currency for the given ISO 4217 [code].
     * Lookup is case-sensitive; codes must be uppercase (e.g., "VND", not "vnd").
     * @throws IllegalArgumentException if the code is not a supported currency.
     */
    fun requireByCode(code: String): CurrencyDefinition =
        byCodeMap[code]
            ?: throw IllegalArgumentException("Unsupported currency code: $code")

    /** Returns the app's default currency (VND). */
    fun default(): CurrencyDefinition = requireByCode("VND")
}
