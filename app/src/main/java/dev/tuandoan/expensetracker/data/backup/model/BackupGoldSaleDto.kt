package dev.tuandoan.expensetracker.data.backup.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BackupGoldSaleDto(
    @SerialName("id") val id: Long,
    @SerialName("holding_id") val holdingId: Long? = null,
    @SerialName("type") val type: String,
    @SerialName("sold_weight") val soldWeight: Double,
    @SerialName("weight_unit") val weightUnit: String,
    @SerialName("buy_price_per_unit") val buyPricePerUnit: Long,
    @SerialName("sell_price_per_unit") val sellPricePerUnit: Long,
    @SerialName("currency_code") val currencyCode: String = "VND",
    @SerialName("sale_date_millis") val saleDateMillis: Long,
    @SerialName("note") val note: String? = null,
    @SerialName("created_at") val createdAt: Long,
)
