package dev.tuandoan.expensetracker.domain.model

data class GoldPortfolioSummary(
    val totalCost: Long,
    val totalCurrentValue: Long,
    val currencyCode: String,
) {
    val totalPnL: Long get() = totalCurrentValue - totalCost
    val pnLPercent: Double
        get() = if (totalCost > 0) (totalPnL.toDouble() / totalCost) * 100 else 0.0
}

data class GoldHoldingWithPnL(
    val holding: GoldHolding,
    val currentPricePerUnit: Long?,
) {
    val totalCost: Long get() = holding.totalCost
    val currentValue: Long?
        get() = currentPricePerUnit?.let { (it * holding.weightValue).toLong() }
    val pnL: Long? get() = currentValue?.let { it - totalCost }
    val pnLPercent: Double?
        get() = pnL?.let { if (totalCost > 0) (it.toDouble() / totalCost) * 100 else 0.0 }
}
