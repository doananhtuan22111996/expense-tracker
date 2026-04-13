package dev.tuandoan.expensetracker.domain.model

data class GoldPrice(
    val type: GoldType,
    val unit: GoldWeightUnit,
    val sellPricePerUnit: Long,
    val buyBackPricePerUnit: Long? = null,
    val currencyCode: String = SupportedCurrencies.default().code,
    val updatedAt: Long = 0,
)
