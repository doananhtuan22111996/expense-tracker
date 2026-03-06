package dev.tuandoan.expensetracker.domain.model

data class CategoryWithCount(
    val category: Category,
    val transactionCount: Int,
)
