package dev.tuandoan.expensetracker.data.database.entity

import androidx.room.ColumnInfo

data class CategoryWithCountRow(
    val id: Long,
    val name: String,
    val type: Int,
    @ColumnInfo(name = "icon_key")
    val iconKey: String?,
    @ColumnInfo(name = "color_key")
    val colorKey: String?,
    @ColumnInfo(name = "is_default")
    val isDefault: Boolean,
    @ColumnInfo(name = "transaction_count")
    val transactionCount: Int,
)
