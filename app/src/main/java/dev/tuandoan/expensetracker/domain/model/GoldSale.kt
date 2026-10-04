package dev.tuandoan.expensetracker.domain.model

data class GoldSale(
    val id: Long = 0,
    val holdingId: Long? = null,
    val type: GoldType,
    val soldWeight: Double,
    val weightUnit: GoldWeightUnit,
    val buyPricePerUnit: Long,
    val sellPricePerUnit: Long,
    val currencyCode: String = SupportedCurrencies.default().code,
    val saleDateMillis: Long,
    val note: String? = null,
    val createdAt: Long = 0,
) {
    val totalCost: Long get() = (buyPricePerUnit * soldWeight).toLong()
    val totalProceeds: Long get() = (sellPricePerUnit * soldWeight).toLong()
    val realizedPnL: Long get() = totalProceeds - totalCost
    val realizedPnLPercent: Double
        get() = if (totalCost > 0) (realizedPnL.toDouble() / totalCost) * 100 else 0.0

    fun weightInGrams(): Double = soldWeight * weightUnit.gramsPerUnit

    fun weightInTaels(): Double = weightInGrams() / GoldWeightUnit.TAEL.gramsPerUnit
}
