package dev.tuandoan.expensetracker.domain.model

data class CurrencyMonthlySummary(
    val currencyCode: String,
    val totalExpense: Long,
    val totalIncome: Long,
    val balance: Long,
    val topExpenseCategories: List<CategoryTotal>,
)

data class MonthlySummary(
    val currencySummaries: List<CurrencyMonthlySummary>,
) {
    val isEmpty: Boolean get() = currencySummaries.isEmpty()
}

data class CategoryTotal(
    val category: Category,
    val total: Long,
)
