package dev.tuandoan.expensetracker.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import dev.tuandoan.expensetracker.domain.model.SupportedCurrencies

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "type")
    val type: Int, // 0 = EXPENSE, 1 = INCOME
    @ColumnInfo(name = "amount")
    val amount: Long, // Amount stored as Long (no decimals for VND)
    // Room @ColumnInfo defaultValue requires a literal; Kotlin default uses SupportedCurrencies
    @ColumnInfo(name = "currency_code", defaultValue = "VND")
    val currencyCode: String = SupportedCurrencies.default().code,
    @ColumnInfo(name = "category_id")
    val categoryId: Long,
    @ColumnInfo(name = "note")
    val note: String?,
    @ColumnInfo(name = "timestamp")
    val timestamp: Long, // Epoch millis
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
) {
    companion object {
        const val TYPE_EXPENSE = 0
        const val TYPE_INCOME = 1
    }
}
