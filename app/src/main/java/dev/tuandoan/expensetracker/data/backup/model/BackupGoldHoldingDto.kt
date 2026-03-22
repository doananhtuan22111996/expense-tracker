package dev.tuandoan.expensetracker.data.backup.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BackupGoldHoldingDto(
    @SerialName("id") val id: Long,
    @SerialName("type") val type: String,
    @SerialName("weight_value") val weightValue: Double,
    @SerialName("weight_unit") val weightUnit: String,
    @SerialName("buy_price_per_unit") val buyPricePerUnit: Long,
    @SerialName("currency_code") val currencyCode: String,
    @SerialName("buy_date_millis") val buyDateMillis: Long,
    @SerialName("note") val note: String? = null,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long,
)
