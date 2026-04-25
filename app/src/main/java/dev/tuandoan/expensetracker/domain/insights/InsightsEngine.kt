package dev.tuandoan.expensetracker.domain.insights

import dev.tuandoan.expensetracker.core.formatter.CurrencyFormatter
import dev.tuandoan.expensetracker.domain.model.BudgetStatus
import dev.tuandoan.expensetracker.domain.model.Transaction
import dev.tuandoan.expensetracker.domain.model.TransactionType
import java.time.ZoneId

/**
 * Pure-Kotlin engine that turns two months of transactions + optional budget
 * context into a structured [InsightsResult] for the Summary tab. No Android
 * dependencies, no repository access — the caller pre-loads the data.
 *
 * ### Scope of this skeleton (Task 2.1)
 *
 * This PR establishes the data model, the orchestration rules, and the
 * currency/income filter. The three insight-specific algorithms
 * ([computeBiggestMover], [computeDailyPace], [computeDayOfMonth]) are
 * intentionally stubbed to return `null`; they land in Tasks 2.2, 2.3, and
 * 2.4 respectively. Shipping the orchestration separately keeps each
 * algorithm PR surgically reviewable and lets the UI wiring (Task 2.8+)
 * start in parallel against a stable engine signature.
 *
 * ### Filter contract
 *
 * Only `EXPENSE` transactions in [defaultCurrencyCode] contribute to any
 * insight. Income is ignored (not the point of spending insights); other
 * currencies are silently excluded per PRD "Out of scope — cross-currency
 * insights". Matches the `mapExpenseWidgetState` pattern.
 *
 * ### Slot logic
 *
 * - Slot 1: [InsightRow.BiggestMover] when one exists, else slot 1 is filled
 *   by whatever would have been slot 2 ([InsightRow.DailyPace] or
 *   [InsightRow.NoBudgetFallback]).
 * - Slot 2: [InsightRow.DailyPace] when [budgetStatus] is non-null with
 *   positive budget; [InsightRow.NoBudgetFallback] otherwise.
 * - Slot 3: [InsightRow.DayOfMonth].
 *
 * Nulls from any algorithm collapse the slot. A user with insufficient
 * data receives a single [InsightRow.Empty] row with [InsightsResult.isEmpty]
 * set to `true`.
 *
 * @param currentMonthExpenses transactions already filtered to the current
 * calendar month by the caller. Other types / other currencies are filtered
 * again here.
 * @param previousMonthExpenses transactions already filtered to the prior
 * calendar month by the caller.
 * @param defaultCurrencyCode the user's selected default currency.
 * @param budgetStatus current month budget context, or `null` when no budget
 * is set for [defaultCurrencyCode].
 * @param nowMillis current epoch ms — injected for testability.
 * @param zoneId time zone used for all day/month boundary math.
 * @param formatter produces the display strings. Must match the rest of the
 * app so Insights numbers can't drift from Home/Summary totals.
 */
fun computeInsights(
    currentMonthExpenses: List<Transaction>,
    previousMonthExpenses: List<Transaction>,
    defaultCurrencyCode: String,
    budgetStatus: BudgetStatus?,
    nowMillis: Long,
    zoneId: ZoneId,
    formatter: CurrencyFormatter,
): InsightsResult {
    val currentRelevant = currentMonthExpenses.filterRelevant(defaultCurrencyCode)
    val previousRelevant = previousMonthExpenses.filterRelevant(defaultCurrencyCode)

    if (currentRelevant.isEmpty() && previousRelevant.isEmpty()) {
        return InsightsResult(rows = listOf(InsightRow.Empty), isEmpty = true)
    }

    val mover = computeBiggestMover(currentRelevant, previousRelevant, defaultCurrencyCode, formatter)
    val paceOrFallback =
        if (budgetStatus != null && budgetStatus.budgetAmount > 0L) {
            computeDailyPace(currentRelevant, budgetStatus, nowMillis, zoneId, formatter)
        } else {
            computeNoBudgetFallback(currentRelevant, defaultCurrencyCode, nowMillis, zoneId, formatter)
        }
    val dayOfMonth = computeDayOfMonth(currentRelevant, previousRelevant, nowMillis, zoneId, formatter)

    val rows =
        buildList {
            // Slot 1: biggest mover when available, else promote slot 2 into slot 1.
            if (mover != null) {
                add(mover)
                if (paceOrFallback != null) add(paceOrFallback)
            } else {
                if (paceOrFallback != null) add(paceOrFallback)
            }
            if (dayOfMonth != null) add(dayOfMonth)
        }

    return if (rows.isEmpty()) {
        InsightsResult(rows = listOf(InsightRow.Empty), isEmpty = true)
    } else {
        InsightsResult(rows = rows, isEmpty = false)
    }
}

/** Filters expense + default-currency only. Mirrors the widget mapper. */
private fun List<Transaction>.filterRelevant(defaultCurrencyCode: String): List<Transaction> =
    filter { it.type == TransactionType.EXPENSE && it.currencyCode == defaultCurrencyCode }

// --- Algorithm stubs — each lands in its own Task 2.2 / 2.3 / 2.4 PR. ---

/**
 * Task 2.2 — biggest month-over-month category mover with the ≥2-transaction
 * threshold and the 5%-of-month-total-or-¥100k-equivalent delta floor (PRD
 * FR-06, default resolved per breakdown).
 */
@Suppress("UNUSED_PARAMETER")
private fun computeBiggestMover(
    currentMonth: List<Transaction>,
    previousMonth: List<Transaction>,
    defaultCurrencyCode: String,
    formatter: CurrencyFormatter,
): InsightRow.BiggestMover? = null

/**
 * Task 2.3 — daily pace vs. active monthly budget, branching ON_PACE / OVER
 * / UNDER (PRD FR-09 through FR-11). Only called when [budgetStatus] has a
 * positive budget amount.
 */
@Suppress("UNUSED_PARAMETER")
private fun computeDailyPace(
    currentMonth: List<Transaction>,
    budgetStatus: BudgetStatus,
    nowMillis: Long,
    zoneId: ZoneId,
    formatter: CurrencyFormatter,
): InsightRow.DailyPace? = null

/**
 * Task 2.7 — informational fallback (Insight #2b) shown when no budget is
 * set. PRD FR-12: "You've spent ¥1.2M this month, about ¥40K/day".
 */
@Suppress("UNUSED_PARAMETER")
private fun computeNoBudgetFallback(
    currentMonth: List<Transaction>,
    defaultCurrencyCode: String,
    nowMillis: Long,
    zoneId: ZoneId,
    formatter: CurrencyFormatter,
): InsightRow.NoBudgetFallback? = null

/**
 * Task 2.4 — day-of-month comparison (PRD FR-13 through FR-15). Compares
 * current-month spend through today against previous-month spend through the
 * same day number.
 */
@Suppress("UNUSED_PARAMETER")
private fun computeDayOfMonth(
    currentMonth: List<Transaction>,
    previousMonth: List<Transaction>,
    nowMillis: Long,
    zoneId: ZoneId,
    formatter: CurrencyFormatter,
): InsightRow.DayOfMonth? = null
