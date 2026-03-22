package dev.tuandoan.expensetracker.domain.model

data class GoldHolding(
    val id: Long = 0,
    val type: GoldType,
    val weightValue: Double,
    val weightUnit: GoldWeightUnit,
    val buyPricePerUnit: Long,
    val currencyCode: String = SupportedCurrencies.default().code,
    val buyDateMillis: Long,
    val note: String? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
) {
    val totalCost: Long get() = (buyPricePerUnit * weightValue).toLong()
}

enum class GoldType {
    SJC,
    GOLD_24K,
    GOLD_18K,
    OTHER,
    ;

    companion object {
        fun fromString(value: String): GoldType =
            entries.firstOrNull { it.name == value }
                ?: throw IllegalArgumentException("Unknown gold type: $value")
    }
}

enum class GoldWeightUnit(
    val gramsPerUnit: Double,
) {
    TAEL(37.5),
    GRAM(1.0),
    OUNCE(31.1035),
    ;

    companion object {
        fun fromString(value: String): GoldWeightUnit =
            entries.firstOrNull { it.name == value }
                ?: throw IllegalArgumentException("Unknown weight unit: $value")
    }
}
