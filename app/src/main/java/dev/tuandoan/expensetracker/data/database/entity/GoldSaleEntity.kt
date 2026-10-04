package dev.tuandoan.expensetracker.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "gold_sales",
    indices = [
        Index(value = ["sale_date_millis"]),
        Index(value = ["holding_id"]),
    ],
)
data class GoldSaleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "holding_id")
    val holdingId: Long? = null,
    @ColumnInfo(name = "type")
    val type: String, // GoldType.name: SJC, GOLD_24K, GOLD_18K, OTHER
    @ColumnInfo(name = "sold_weight")
    val soldWeight: Double,
    @ColumnInfo(name = "weight_unit")
    val weightUnit: String, // GoldWeightUnit.name: TAEL, GRAM, OUNCE
    @ColumnInfo(name = "buy_price_per_unit")
    val buyPricePerUnit: Long,
    @ColumnInfo(name = "sell_price_per_unit")
    val sellPricePerUnit: Long,
    @ColumnInfo(name = "currency_code", defaultValue = "VND")
    val currencyCode: String = "VND",
    @ColumnInfo(name = "sale_date_millis")
    val saleDateMillis: Long,
    @ColumnInfo(name = "note")
    val note: String? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = 0,
)
