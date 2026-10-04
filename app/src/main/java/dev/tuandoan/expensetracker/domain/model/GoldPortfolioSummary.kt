package dev.tuandoan.expensetracker.domain.model

data class GoldPortfolioSummary(
    val totalCost: Long,
    val totalMarketValue: Long,
    val totalLiquidationValue: Long? = null,
    val totalRealizedPnL: Long = 0L,
    val totalRealizedProceeds: Long = 0L,
    val totalWeightGrams: Double = 0.0,
    val currencyCode: String,
) {
    val marketPnL: Long get() = totalMarketValue - totalCost
    val marketPnLPercent: Double
        get() = if (totalCost > 0) (marketPnL.toDouble() / totalCost) * 100 else 0.0

    val liquidationPnL: Long? get() = totalLiquidationValue?.let { it - totalCost }
    val liquidationPnLPercent: Double?
        get() = liquidationPnL?.let { if (totalCost > 0) (it.toDouble() / totalCost) * 100 else 0.0 }

    val totalNetPnL: Long get() = (liquidationPnL ?: marketPnL) + totalRealizedPnL

    val totalWeightTaels: Double get() = totalWeightGrams / GoldWeightUnit.TAEL.gramsPerUnit
}

data class GoldTypeAllocation(
    val type: GoldType,
    val totalWeightGrams: Double,
    val totalCost: Long,
    val currentValue: Long,
    val percentageOfPortfolio: Double,
)

data class GoldHoldingWithPnL(
    val holding: GoldHolding,
    val currentSellPricePerUnit: Long?,
    val currentBuyBackPricePerUnit: Long? = null,
) {
    val totalCost: Long get() = holding.totalCost

    val marketValue: Long?
        get() = currentSellPricePerUnit?.let { (it * holding.weightValue).toLong() }
    val marketPnL: Long? get() = marketValue?.let { it - totalCost }
    val marketPnLPercent: Double?
        get() = marketPnL?.let { if (totalCost > 0) (it.toDouble() / totalCost) * 100 else 0.0 }

    val liquidationValue: Long?
        get() = currentBuyBackPricePerUnit?.let { (it * holding.weightValue).toLong() }
    val liquidationPnL: Long? get() = liquidationValue?.let { it - totalCost }
    val liquidationPnLPercent: Double?
        get() = liquidationPnL?.let { if (totalCost > 0) (it.toDouble() / totalCost) * 100 else 0.0 }
}
