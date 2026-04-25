package dev.tuandoan.expensetracker.domain.insights

import dev.tuandoan.expensetracker.core.formatter.CurrencyFormatter
import dev.tuandoan.expensetracker.domain.model.BudgetStatus
import dev.tuandoan.expensetracker.domain.model.SupportedCurrencies
import dev.tuandoan.expensetracker.domain.model.Transaction
import dev.tuandoan.expensetracker.domain.model.TransactionType
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

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
 * Biggest month-over-month category mover (PRD FR-05 through FR-08, Task 2.2).
 *
 * Picks the category with the largest absolute percentage change in spend
 * between [currentMonth] and [previousMonth]. Filters to prevent noise:
 *
 * 1. **≥2 transactions in BOTH months** — single-txn categories would let
 *    outliers (one big restaurant bill) dominate the insight.
 * 2. **Delta floor** — category's absolute spend change must satisfy at least
 *    one of: `|delta| ≥ 5% of current month total` OR `|delta| ≥ 100k minor
 *    units in a zero-decimal currency` / `≥ 10k minor units (≈ $100) in a
 *    two-decimal currency`. The floor keeps a `¥10k → ¥30k` (+200%) category
 *    from beating a `¥400k → ¥500k` (+25%) category when the latter is the
 *    meaningful narrative.
 *
 * Ranking: by `|percentChange|` descending, tie-break by category id ascending
 * for determinism.
 *
 * Returns `null` when no category clears both filters — the orchestrator then
 * promotes slot 2's insight into slot 1 per PRD Open Question 1 resolution.
 */
private fun computeBiggestMover(
    currentMonth: List<Transaction>,
    previousMonth: List<Transaction>,
    defaultCurrencyCode: String,
    formatter: CurrencyFormatter,
): InsightRow.BiggestMover? {
    val currentByCategory = currentMonth.groupBy { it.category.id }
    val previousByCategory = previousMonth.groupBy { it.category.id }

    val currentMonthTotal = currentMonth.sumOf { it.amount }
    val deltaFloorMinorUnits = deltaFloorMinorUnits(defaultCurrencyCode)

    data class Candidate(
        val categoryId: Long,
        val categoryName: String,
        val prevSum: Long,
        val currSum: Long,
        val percentChange: Int,
    )

    val candidates =
        currentByCategory.keys
            .intersect(previousByCategory.keys)
            .mapNotNull { categoryId ->
                val currTxns = currentByCategory.getValue(categoryId)
                val prevTxns = previousByCategory.getValue(categoryId)
                if (currTxns.size < MIN_TRANSACTIONS_PER_MONTH ||
                    prevTxns.size < MIN_TRANSACTIONS_PER_MONTH
                ) {
                    return@mapNotNull null
                }

                val currSum = currTxns.sumOf { it.amount }
                val prevSum = prevTxns.sumOf { it.amount }
                if (prevSum <= 0L || currSum <= 0L) return@mapNotNull null

                val delta = currSum - prevSum
                if (!passesDeltaFloor(
                        delta = delta,
                        currentMonthTotal = currentMonthTotal,
                        floorMinorUnits = deltaFloorMinorUnits,
                    )
                ) {
                    return@mapNotNull null
                }

                val percentChange = (delta.toFloat() / prevSum.toFloat() * 100f).roundToInt()
                Candidate(
                    categoryId = categoryId,
                    categoryName = currTxns.first().category.name,
                    prevSum = prevSum,
                    currSum = currSum,
                    percentChange = percentChange,
                )
            }

    val winner =
        candidates
            .sortedWith(
                compareByDescending<Candidate> { it.percentChange.absoluteValue }
                    .thenBy { it.categoryId },
            ).firstOrNull()
            ?: return null

    return InsightRow.BiggestMover(
        categoryName = winner.categoryName,
        previousFormatted = formatter.format(winner.prevSum, defaultCurrencyCode),
        currentFormatted = formatter.format(winner.currSum, defaultCurrencyCode),
        percentChange = winner.percentChange,
        direction = if (winner.percentChange >= 0) InsightRow.Direction.UP else InsightRow.Direction.DOWN,
    )
}

/**
 * `|delta| ≥ 5% of currentMonthTotal` OR `|delta| ≥ floorMinorUnits`.
 * Uses integer-only arithmetic to avoid float rounding on the percentage check.
 */
private fun passesDeltaFloor(
    delta: Long,
    currentMonthTotal: Long,
    floorMinorUnits: Long,
): Boolean {
    val absDelta = delta.absoluteValue
    if (absDelta >= floorMinorUnits) return true
    // absDelta / currentMonthTotal ≥ 0.05  ⟺  absDelta * 100 ≥ currentMonthTotal * 5
    return absDelta * 100L >= currentMonthTotal * DELTA_FLOOR_PERCENT
}

/**
 * "¥100k equivalent" — the breakdown's delta floor. Expressed in the default
 * currency's minor units so a two-decimal currency uses `10_000` (~$100) while
 * a zero-decimal currency uses `100_000` (~¥100k / ₫100k). Falls back to the
 * loose zero-decimal value if the code is unknown.
 */
private fun deltaFloorMinorUnits(currencyCode: String): Long {
    val digits = SupportedCurrencies.byCode(currencyCode)?.minorUnitDigits ?: 0
    return if (digits >= 2) 10_000L else 100_000L
}

private const val MIN_TRANSACTIONS_PER_MONTH = 2
private const val DELTA_FLOOR_PERCENT = 5L

/**
 * Daily pace vs. active monthly budget (PRD FR-09 through FR-11, Task 2.3).
 *
 * Projects current spend to month-end assuming a constant daily rate:
 * `projected = spend * daysInMonth / daysElapsed`. Integer arithmetic
 * throughout — no float rounding at the ±5% slack boundary.
 *
 * The three status branches are driven by the signed difference between the
 * projected total and the budget:
 *
 * - **ON_PACE** — `|projected - budget| ≤ 5% of budget`. Within tolerance; the
 *   UI copy is "pacing to spend X — right on budget", no difference amount.
 * - **OVER** — `projected > budget * 1.05`. Copy: "pacing to exceed your X
 *   budget by Y" with [differenceFormatted] = formatted(projected − budget).
 * - **UNDER** — `projected < budget * 0.95`. Copy: "pacing Y under budget"
 *   with [differenceFormatted] = formatted(budget − projected).
 *
 * Returns `null` only in defensive edge cases (daysInMonth == 0 which is
 * unreachable in a valid calendar; kept as a guard against timestamp corruption).
 *
 * Only called by the orchestrator when [budgetStatus] has a positive budget —
 * that guard lives in [computeInsights], not repeated here.
 */
private fun computeDailyPace(
    currentMonth: List<Transaction>,
    budgetStatus: BudgetStatus,
    nowMillis: Long,
    zoneId: ZoneId,
    formatter: CurrencyFormatter,
): InsightRow.DailyPace? {
    val today = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
    val yearMonth = YearMonth.from(today)
    val daysInMonth = yearMonth.lengthOfMonth()
    if (daysInMonth <= 0) return null

    // Clamp to handle any drift where "today" reads outside the current month
    // after cross-month timestamp corruption. Worst case: daysElapsed == daysInMonth
    // means projected == spend (no extrapolation).
    val daysElapsed = today.dayOfMonth.coerceIn(1, daysInMonth)

    val spend = currentMonth.sumOf { it.amount }
    val budget = budgetStatus.budgetAmount
    val currencyCode = budgetStatus.currency.code

    // Integer projection: spend * daysInMonth / daysElapsed.
    // For 5,000-txn month with amounts in minor units, spend ≤ ~10^10, × 31
    // stays well under Long.MAX_VALUE (~9.2×10^18).
    val projected = spend * daysInMonth / daysElapsed
    val difference = projected - budget

    // Slack = 5% of budget. Integer-only; no float rounding at the boundary.
    val slack = budget * PACE_SLACK_PERCENT / 100L
    val status =
        when {
            difference.absoluteValue <= slack -> InsightRow.PaceStatus.ON_PACE
            difference > 0 -> InsightRow.PaceStatus.OVER
            else -> InsightRow.PaceStatus.UNDER
        }

    val projectedFormatted = formatter.format(projected, currencyCode)
    val budgetFormatted = formatter.format(budget, currencyCode)
    val differenceFormatted =
        when (status) {
            InsightRow.PaceStatus.ON_PACE -> null
            InsightRow.PaceStatus.OVER,
            InsightRow.PaceStatus.UNDER,
            -> formatter.format(difference.absoluteValue, currencyCode)
        }

    return InsightRow.DailyPace(
        status = status,
        projectedFormatted = projectedFormatted,
        budgetFormatted = budgetFormatted,
        differenceFormatted = differenceFormatted,
    )
}

private const val PACE_SLACK_PERCENT = 5L

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
