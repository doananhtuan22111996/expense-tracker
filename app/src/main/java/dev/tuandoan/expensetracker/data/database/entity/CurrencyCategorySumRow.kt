package dev.tuandoan.expensetracker.data.database.entity

data class CurrencyCategorySumRow(
    val currencyCode: String,
    val categoryId: Long,
    val total: Long,
)
