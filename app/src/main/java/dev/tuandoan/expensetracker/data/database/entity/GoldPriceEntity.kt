package dev.tuandoan.expensetracker.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(
    tableName = "gold_prices",
    primaryKeys = ["type", "unit"],
)
data class GoldPriceEntity(
    @ColumnInfo(name = "type")
    val type: String, // GoldType.name
    @ColumnInfo(name = "unit")
    val unit: String, // GoldWeightUnit.name
    @ColumnInfo(name = "price_per_unit")
    val pricePerUnit: Long,
    @ColumnInfo(name = "currency_code", defaultValue = "VND")
    val currencyCode: String = "VND",
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)
