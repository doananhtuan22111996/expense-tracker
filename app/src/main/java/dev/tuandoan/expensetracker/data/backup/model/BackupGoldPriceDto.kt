package dev.tuandoan.expensetracker.data.backup.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BackupGoldPriceDto(
    @SerialName("type") val type: String,
    @SerialName("unit") val unit: String,
    @SerialName("price_per_unit") val pricePerUnit: Long,
    @SerialName("buy_back_price_per_unit") val buyBackPricePerUnit: Long? = null,
    @SerialName("currency_code") val currencyCode: String,
    @SerialName("updated_at") val updatedAt: Long,
)
