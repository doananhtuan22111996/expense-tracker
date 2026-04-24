package dev.tuandoan.expensetracker.widget

import dev.tuandoan.expensetracker.core.formatter.CurrencyFormatter
import dev.tuandoan.expensetracker.domain.model.Transaction
import dev.tuandoan.expensetracker.domain.model.TransactionType
import java.time.Instant
import java.time.ZoneId

/**
 * Pure mapper that projects a month's worth of transactions + the user's
 * budget into an [ExpenseWidgetState]. No Android dependencies, no repository
 * lookups — the caller is expected to pre-filter [monthExpenses] to the
 * current calendar month (that's how `TransactionRepository.observeTransactions`
 * is already called elsewhere).
 *
 * ### Scope
 *
 * - Only `EXPENSE` transactions contribute to the totals. Income is ignored.
 * - Only transactions in [defaultCurrencyCode] contribute. Mixed-currency
 *   users see totals in their default currency; other-currency transactions
 *   are silently excluded. This matches the v1 Insights constraint and keeps
 *   the widget readable on a 2×1 layout.
 *
 * ### Time
 *
 * [nowMillis] + [zoneId] determine the start-of-today boundary. A transaction
 * is "today" if its `timestamp` falls at or after local midnight today and
 * before the next emission of [nowMillis] would change the answer. Injected
 * so unit tests can fix the clock without touching `System.currentTimeMillis`.
 *
 * @param monthExpenses transactions already filtered to the current calendar
 * month by the caller. Other types / other currencies are filtered here.
 * @param defaultCurrencyCode the user's selected default currency. Used for
 * both the `CurrencyFormatter` calls and the transaction filter.
 * @param budgetAmount the monthly budget for [defaultCurrencyCode], in the
 * currency's minor units. `null` when the user has no budget set.
 * @param nowMillis current epoch ms; injected for testability.
 * @param zoneId time zone used to resolve "today" — matches the rest of the
 * app, which uses `ZoneId.systemDefault()` via `DateTimeUtil`.
 * @param formatter produces the display strings; must match what the rest of
 * the app renders so the widget can't drift from the Home/Summary tabs.
 */
fun mapExpenseWidgetState(
    monthExpenses: List<Transaction>,
    defaultCurrencyCode: String,
    budgetAmount: Long?,
    nowMillis: Long,
    zoneId: ZoneId,
    formatter: CurrencyFormatter,
): ExpenseWidgetState {
    val relevant =
        monthExpenses.filter {
            it.type == TransactionType.EXPENSE &&
                it.currencyCode == defaultCurrencyCode
        }

    val startOfTodayMillis =
        Instant
            .ofEpochMilli(nowMillis)
            .atZone(zoneId)
            .toLocalDate()
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()

    val todayTotal = relevant.sumOf { if (it.timestamp >= startOfTodayMillis) it.amount else 0L }
    val monthTotal = relevant.sumOf { it.amount }

    val budgetDisplay =
        budgetAmount?.takeIf { it > 0L }?.let { amount ->
            val rawFraction = monthTotal.toFloat() / amount.toFloat()
            BudgetDisplay(
                spentFormatted = formatter.format(monthTotal, defaultCurrencyCode),
                budgetFormatted = formatter.format(amount, defaultCurrencyCode),
                progressFraction = rawFraction.coerceIn(0f, 1f),
                isOverBudget = rawFraction > 1f,
            )
        }

    return ExpenseWidgetState(
        currencyCode = defaultCurrencyCode,
        todayFormatted = formatter.format(todayTotal, defaultCurrencyCode),
        monthFormatted = formatter.format(monthTotal, defaultCurrencyCode),
        budget = budgetDisplay,
    )
}
