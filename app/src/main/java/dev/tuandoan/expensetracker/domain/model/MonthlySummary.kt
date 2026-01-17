package dev.tuandoan.expensetracker.domain.model

data class MonthlySummary(
    val totalExpense: Long,
    val totalIncome: Long,
    val balance: Long,
    val topExpenseCategories: List<CategoryTotal>,
)

data class CategoryTotal(
    val category: Category,
    val total: Long,
)
