package dev.tuandoan.expensetracker.core.currency

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs

/**
 * Currency amount calculations for travel money tracking (ADR-002).
 *
 * Converts between foreign minor units and home minor units using HALF_EVEN rounding,
 * correctly scaling across different minor-unit precisions (e.g. 0-digit JPY/VND vs 2-digit USD/EUR).
 */
object AmountCalculator {
    /**
     * Convert foreign minor units → home minor units using HALF_EVEN rounding.
     *
     * @param foreignMinor foreign amount in minor units (e.g. 100 for $1.00, 1200 for ¥1200)
     * @param foreignDigits decimal places for foreign minor units (e.g. 2 for USD, 0 for JPY)
     * @param homeDigits decimal places for home minor units (e.g. 0 for VND, 2 for USD)
     * @param rate exchange rate as "1 <foreign> = X <home>" in major units
     * @return home currency equivalent in minor units
     */
    fun toHomeMinor(
        foreignMinor: Long,
        foreignDigits: Int,
        homeDigits: Int,
        rate: Double,
    ): Long {
        if (foreignMinor <= 0L || rate <= 0.0) return 0L
        val foreignAmount = BigDecimal.valueOf(foreignMinor)
        val rateBd = BigDecimal.valueOf(rate)
        val exponent = homeDigits - foreignDigits
        val scaleBd = BigDecimal.TEN.pow(abs(exponent))
        val scaled =
            if (exponent >= 0) {
                foreignAmount.multiply(rateBd).multiply(scaleBd)
            } else {
                foreignAmount.multiply(rateBd).divide(scaleBd, 10, RoundingMode.HALF_EVEN)
            }
        return scaled.setScale(0, RoundingMode.HALF_EVEN).toLong()
    }
}
