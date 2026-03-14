package dev.tuandoan.expensetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import dev.tuandoan.expensetracker.domain.model.RecurrenceFrequency
import dev.tuandoan.expensetracker.domain.model.RecurringTransaction
import dev.tuandoan.expensetracker.domain.model.TransactionType
import dev.tuandoan.expensetracker.domain.repository.CategoryRepository
import dev.tuandoan.expensetracker.domain.repository.RecurringTransactionRepository
import dev.tuandoan.expensetracker.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Debug-only activity that seeds the database with demo data for screenshots.
 *
 * Launch via:
 * ```
 * adb shell am start -n dev.tuandoan.expensetracker/dev.tuandoan.expensetracker.ScreenshotSeedActivity
 * ```
 *
 * Not included in release builds.
 */
@AndroidEntryPoint
class ScreenshotSeedActivity : ComponentActivity() {
    @Inject
    lateinit var transactionRepository: TransactionRepository

    @Inject
    lateinit var categoryRepository: CategoryRepository

    @Inject
    lateinit var recurringTransactionRepository: RecurringTransactionRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            seedDemoData()
            finish()
        }
    }

    /**
     * Inserts 30 transactions across 6 categories, and 3 recurring transactions.
     * Transactions are spread over the last 3 months with varied amounts and currencies.
     */
    internal suspend fun seedDemoData() {
        val expenseCategories =
            categoryRepository.observeCategories(TransactionType.EXPENSE).first()
        val incomeCategories =
            categoryRepository.observeCategories(TransactionType.INCOME).first()

        // Use existing categories, pick the first 6 expense categories available
        val usableExpenseCategories = expenseCategories.take(CATEGORY_COUNT)

        // Use first income category for income transactions
        val incomeCategory = incomeCategories.firstOrNull() ?: return

        if (usableExpenseCategories.isEmpty()) return

        val now = System.currentTimeMillis()
        val oneDay = DAY_MILLIS

        // Insert 24 expense transactions spread over 3 months
        val expenseData =
            listOf(
                // Month 1 (current month) - 8 transactions
                Triple(0L, 50_000L, "VND"),
                Triple(1L, 120_000L, "VND"),
                Triple(2L, 35_000L, "VND"),
                Triple(3L, 200_000L, "VND"),
                Triple(5L, 15_99L, "USD"),
                Triple(7L, 80_000L, "VND"),
                Triple(10L, 45_000L, "VND"),
                Triple(12L, 9_50L, "EUR"),
                // Month 2 (last month) - 8 transactions
                Triple(31L, 150_000L, "VND"),
                Triple(33L, 75_000L, "VND"),
                Triple(35L, 22_50L, "USD"),
                Triple(37L, 300_000L, "VND"),
                Triple(40L, 60_000L, "VND"),
                Triple(42L, 18_000L, "VND"),
                Triple(45L, 12_00L, "EUR"),
                Triple(48L, 95_000L, "VND"),
                // Month 3 (two months ago) - 8 transactions
                Triple(62L, 180_000L, "VND"),
                Triple(64L, 40_000L, "VND"),
                Triple(66L, 7_99L, "USD"),
                Triple(68L, 250_000L, "VND"),
                Triple(70L, 55_000L, "VND"),
                Triple(73L, 30_000L, "VND"),
                Triple(76L, 110_000L, "VND"),
                Triple(80L, 20_00L, "EUR"),
            )

        expenseData.forEachIndexed { index, (daysAgo, amount, currency) ->
            val categoryIndex = index % usableExpenseCategories.size
            transactionRepository.addTransaction(
                type = TransactionType.EXPENSE,
                amount = amount,
                categoryId = usableExpenseCategories[categoryIndex].id,
                note = EXPENSE_NOTES[index % EXPENSE_NOTES.size],
                timestamp = now - (daysAgo * oneDay),
                currencyCode = currency,
            )
        }

        // Insert 6 income transactions spread over 3 months
        val incomeData =
            listOf(
                Triple(5L, 15_000_000L, "VND"),
                Triple(15L, 2_000_00L, "USD"),
                Triple(35L, 15_000_000L, "VND"),
                Triple(50L, 500_000L, "VND"),
                Triple(65L, 15_000_000L, "VND"),
                Triple(75L, 1_500_00L, "EUR"),
            )

        incomeData.forEachIndexed { index, (daysAgo, amount, currency) ->
            transactionRepository.addTransaction(
                type = TransactionType.INCOME,
                amount = amount,
                categoryId = incomeCategory.id,
                note = INCOME_NOTES[index % INCOME_NOTES.size],
                timestamp = now - (daysAgo * oneDay),
                currencyCode = currency,
            )
        }

        // Insert 3 recurring transactions
        val recurringItems =
            listOf(
                RecurringTransaction(
                    type = TransactionType.EXPENSE,
                    amount = 5_000_000L,
                    currencyCode = "VND",
                    categoryId = (usableExpenseCategories.getOrNull(3) ?: usableExpenseCategories.first()).id,
                    note = "Monthly rent",
                    frequency = RecurrenceFrequency.MONTHLY,
                    dayOfMonth = 1,
                    dayOfWeek = null,
                    nextDueMillis = now + (DAYS_UNTIL_RENT_DUE * oneDay),
                    isActive = true,
                ),
                RecurringTransaction(
                    type = TransactionType.EXPENSE,
                    amount = 9_99L,
                    currencyCode = "USD",
                    categoryId = (usableExpenseCategories.getOrNull(5) ?: usableExpenseCategories.first()).id,
                    note = "Streaming subscription",
                    frequency = RecurrenceFrequency.MONTHLY,
                    dayOfMonth = 15,
                    dayOfWeek = null,
                    nextDueMillis = now + (DAYS_UNTIL_SUB_DUE * oneDay),
                    isActive = true,
                ),
                RecurringTransaction(
                    type = TransactionType.EXPENSE,
                    amount = 200_000L,
                    currencyCode = "VND",
                    categoryId = usableExpenseCategories.first().id,
                    note = "Weekly groceries",
                    frequency = RecurrenceFrequency.WEEKLY,
                    dayOfMonth = null,
                    dayOfWeek = 1,
                    nextDueMillis = now + (DAYS_UNTIL_WEEKLY_DUE * oneDay),
                    isActive = true,
                ),
            )

        recurringItems.forEach { recurring ->
            recurringTransactionRepository.create(recurring)
        }
    }

    companion object {
        private const val CATEGORY_COUNT = 6
        private const val DAY_MILLIS = 86_400_000L
        private const val DAYS_UNTIL_RENT_DUE = 17L
        private const val DAYS_UNTIL_SUB_DUE = 10L
        private const val DAYS_UNTIL_WEEKLY_DUE = 3L

        private val EXPENSE_NOTES =
            listOf(
                "Lunch",
                "Grab ride",
                "New shoes",
                "Electric bill",
                "Medicine",
                "Movie tickets",
                "Coffee",
                "Dinner with friends",
                "Groceries",
                "Phone top-up",
            )

        private val INCOME_NOTES =
            listOf(
                "Monthly salary",
                "Freelance project",
                "Salary",
                "Sold old phone",
                "Monthly salary",
                "Contract payment",
            )
    }
}
