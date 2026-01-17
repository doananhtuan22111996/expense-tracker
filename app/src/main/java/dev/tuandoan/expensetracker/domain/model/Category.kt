package dev.tuandoan.expensetracker.domain.model

data class Category(
    val id: Long = 0L,
    val name: String,
    val type: TransactionType,
    val iconKey: String? = null,
    val colorKey: String? = null,
    val isDefault: Boolean = false,
)
