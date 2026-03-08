package dev.tuandoan.expensetracker.domain.model

enum class BudgetStatusLevel {
    OK,
    WARNING,
    OVER_BUDGET,
}

data class BudgetStatus(
    val currency: CurrencyDefinition,
    val budgetAmount: Long,
    val spentAmount: Long,
) {
    val remainingAmount: Long get() = budgetAmount - spentAmount

    val progressFraction: Float
        get() =
            if (budgetAmount == 0L) 0f else spentAmount.toFloat() / budgetAmount

    val status: BudgetStatusLevel
        get() =
            when {
                progressFraction >= 1.0f -> BudgetStatusLevel.OVER_BUDGET
                progressFraction >= 0.8f -> BudgetStatusLevel.WARNING
                else -> BudgetStatusLevel.OK
            }
}
