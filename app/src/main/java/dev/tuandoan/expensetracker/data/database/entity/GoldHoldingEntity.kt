package dev.tuandoan.expensetracker.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "gold_holdings",
    indices = [Index(value = ["buy_date_millis"])],
)
data class GoldHoldingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "type")
    val type: String, // GoldType.name: SJC, GOLD_24K, GOLD_18K, OTHER
    @ColumnInfo(name = "weight_value")
    val weightValue: Double,
    @ColumnInfo(name = "weight_unit")
    val weightUnit: String, // GoldWeightUnit.name: TAEL, GRAM, OUNCE
    @ColumnInfo(name = "buy_price_per_unit")
    val buyPricePerUnit: Long,
    @ColumnInfo(name = "currency_code", defaultValue = "VND")
    val currencyCode: String = "VND",
    @ColumnInfo(name = "buy_date_millis")
    val buyDateMillis: Long,
    @ColumnInfo(name = "note")
    val note: String?,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)
