package dev.tuandoan.expensetracker.testutil

import dev.tuandoan.expensetracker.data.database.entity.CategoryEntity
import dev.tuandoan.expensetracker.data.database.entity.CategorySumRow
import dev.tuandoan.expensetracker.data.database.entity.TransactionEntity
import dev.tuandoan.expensetracker.domain.model.Category
import dev.tuandoan.expensetracker.domain.model.CategoryTotal
import dev.tuandoan.expensetracker.domain.model.MonthlySummary
import dev.tuandoan.expensetracker.domain.model.Transaction
import dev.tuandoan.expensetracker.domain.model.TransactionType

object TestData {
    const val FIXED_TIME = 1700000000000L

    val expenseCategory =
        Category(
            id = 1L,
            name = "Food",
            type = TransactionType.EXPENSE,
            iconKey = "restaurant",
            colorKey = "red",
            isDefault = true,
        )

    val incomeCategory =
        Category(
            id = 2L,
            name = "Salary",
            type = TransactionType.INCOME,
            iconKey = "payments",
            colorKey = "green",
            isDefault = true,
        )

    val transportCategory =
        Category(
            id = 3L,
            name = "Transport",
            type = TransactionType.EXPENSE,
            iconKey = "directions_bus",
            colorKey = "blue",
            isDefault = false,
        )

    val expenseCategoryEntity =
        CategoryEntity(
            id = 1L,
            name = "Food",
            type = 0,
            iconKey = "restaurant",
            colorKey = "red",
            isDefault = true,
        )

    val incomeCategoryEntity =
        CategoryEntity(
            id = 2L,
            name = "Salary",
            type = 1,
            iconKey = "payments",
            colorKey = "green",
            isDefault = true,
        )

    val transportCategoryEntity =
        CategoryEntity(
            id = 3L,
            name = "Transport",
            type = 0,
            iconKey = "directions_bus",
            colorKey = "blue",
            isDefault = false,
        )

    val sampleExpenseTransaction =
        Transaction(
            id = 1L,
            type = TransactionType.EXPENSE,
            amount = 50000L,
            currencyCode = "VND",
            category = expenseCategory,
            note = "Lunch",
            timestamp = FIXED_TIME,
            createdAt = FIXED_TIME,
            updatedAt = FIXED_TIME,
        )

    val sampleIncomeTransaction =
        Transaction(
            id = 2L,
            type = TransactionType.INCOME,
            amount = 10000000L,
            currencyCode = "VND",
            category = incomeCategory,
            note = "Monthly salary",
            timestamp = FIXED_TIME,
            createdAt = FIXED_TIME,
            updatedAt = FIXED_TIME,
        )

    val sampleExpenseEntity =
        TransactionEntity(
            id = 1L,
            type = 0,
            amount = 50000L,
            currencyCode = "VND",
            categoryId = 1L,
            note = "Lunch",
            timestamp = FIXED_TIME,
            createdAt = FIXED_TIME,
            updatedAt = FIXED_TIME,
        )

    val sampleIncomeEntity =
        TransactionEntity(
            id = 2L,
            type = 1,
            amount = 10000000L,
            currencyCode = "VND",
            categoryId = 2L,
            note = "Monthly salary",
            timestamp = FIXED_TIME,
            createdAt = FIXED_TIME,
            updatedAt = FIXED_TIME,
        )

    val sampleCategorySumRows =
        listOf(
            CategorySumRow(categoryId = 1L, total = 150000L),
            CategorySumRow(categoryId = 3L, total = 50000L),
        )

    val sampleMonthlySummary =
        MonthlySummary(
            totalExpense = 200000L,
            totalIncome = 10000000L,
            balance = 9800000L,
            topExpenseCategories =
                listOf(
                    CategoryTotal(category = expenseCategory, total = 150000L),
                    CategoryTotal(category = transportCategory, total = 50000L),
                ),
        )
}
