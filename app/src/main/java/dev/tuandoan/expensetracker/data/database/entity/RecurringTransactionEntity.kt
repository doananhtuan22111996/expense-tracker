package dev.tuandoan.expensetracker.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "recurring_transactions",
    indices = [Index(value = ["next_due_millis"])],
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
)
data class RecurringTransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: Int,
    val amount: Long,
    @ColumnInfo(name = "currency_code", defaultValue = "VND")
    val currencyCode: String = "VND",
    @ColumnInfo(name = "category_id")
    val categoryId: Long?,
    val note: String?,
    val frequency: Int, // 0=DAILY, 1=WEEKLY, 2=MONTHLY, 3=YEARLY
    @ColumnInfo(name = "day_of_month")
    val dayOfMonth: Int?,
    @ColumnInfo(name = "day_of_week")
    val dayOfWeek: Int?,
    @ColumnInfo(name = "next_due_millis")
    val nextDueMillis: Long,
    @ColumnInfo(name = "is_active", defaultValue = "1")
    val isActive: Boolean = true,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)
