package dev.tuandoan.expensetracker.domain.insights

import dev.tuandoan.expensetracker.core.formatter.CurrencyFormatter
import dev.tuandoan.expensetracker.domain.model.BudgetStatus
import dev.tuandoan.expensetracker.domain.model.Category
import dev.tuandoan.expensetracker.domain.model.SupportedCurrencies
import dev.tuandoan.expensetracker.domain.model.Transaction
import dev.tuandoan.expensetracker.domain.model.TransactionType
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import kotlin.random.Random
import kotlin.system.measureNanoTime

/**
 * Performance guard for [computeInsights] (v3.10.0 Task 2.13).
 *
 * PRD target: ≤ 50 ms on a Pixel 7a for a 5,000-transaction month. This is a
 * JUnit-timed JVM test rather than an `androidx.benchmark` microbenchmark —
 * the project doesn't have instrumented-benchmark infrastructure, and the
 * target here is "don't regress algorithmically," not "prove exact device
 * latency." A CI JVM is typically harsher than a Pixel 7a (cold classloader,
 * interpreted fallback on the first iteration), so the test threshold carries
 * a 4× headroom — any regression that would actually breach 50 ms on-device
 * will blow past 200 ms in CI long before a user notices.
 *
 * Device-latency verification remains the manual Pixel 7a pass.
 */
class InsightsEngineBenchmarkTest {
    private val zone = ZoneId.of("UTC")
    private val today: LocalDate = LocalDate.of(2026, 4, 15)
    private val nowMillis: Long =
        today
            .atTime(10, 0)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()

    /** Stable formatter — no real formatting cost, isolates the engine's own work. */
    private val fakeFormatter =
        object : CurrencyFormatter {
            override fun format(
                amountMinor: Long,
                currencyCode: String,
            ): String = "$amountMinor $currencyCode"

            override fun formatWithSign(
                amountMinor: Long,
                currencyCode: String,
                isIncome: Boolean,
            ): String = (if (isIncome) "+" else "-") + format(amountMinor, currencyCode)

            override fun formatBareAmount(
                amountMinor: Long,
                currencyCode: String,
            ): String = amountMinor.toString()
        }

    @Test
    fun computeInsights_fiveThousandTransactionsPerMonth_stayWithinBudget() {
        val categories = buildCategories(count = 12)
        val currentMonthStart = LocalDate.of(2026, 4, 1)
        val previousMonthStart = LocalDate.of(2026, 3, 1)

        // Two unrelated seeds (not seed + 1) — sequential seeds can produce
        // correlated RNG streams on some generators; deterministic but
        // independent is the target.
        val currentMonth =
            buildMonth(
                size = 5_000,
                monthStart = currentMonthStart,
                categories = categories,
                idOffset = 0L,
                seed = 42L,
            )
        val previousMonth =
            buildMonth(
                size = 5_000,
                monthStart = previousMonthStart,
                categories = categories,
                idOffset = 1_000_000L,
                seed = 8675309L,
            )

        val budget =
            BudgetStatus(
                currency =
                    checkNotNull(SupportedCurrencies.byCode("VND")) {
                        "VND must exist in SupportedCurrencies — benchmark fixture invariant"
                    },
                budgetAmount = 50_000_000L,
                spentAmount = currentMonth.sumOf { it.amount },
            )

        // Warmup — first few runs pay the JIT / classloader cost. Not counted.
        repeat(WARMUP_ITERATIONS) {
            computeInsights(
                currentMonthExpenses = currentMonth,
                previousMonthExpenses = previousMonth,
                defaultCurrencyCode = "VND",
                budgetStatus = budget,
                nowMillis = nowMillis,
                zoneId = zone,
                formatter = fakeFormatter,
            )
        }

        // Measured — average across N runs smooths over single-run GC pauses.
        val totalNanos =
            measureNanoTime {
                repeat(MEASURED_ITERATIONS) {
                    computeInsights(
                        currentMonthExpenses = currentMonth,
                        previousMonthExpenses = previousMonth,
                        defaultCurrencyCode = "VND",
                        budgetStatus = budget,
                        nowMillis = nowMillis,
                        zoneId = zone,
                        formatter = fakeFormatter,
                    )
                }
            }
        val averageMs = totalNanos / MEASURED_ITERATIONS / 1_000_000.0

        assertTrue(
            "computeInsights averaged ${String.format(Locale.ROOT, "%.2f", averageMs)}ms " +
                "over $MEASURED_ITERATIONS runs; CI budget is ${CI_BUDGET_MS}ms " +
                "(PRD target 50ms on-device, 4× headroom for CI)",
            averageMs <= CI_BUDGET_MS,
        )
    }

    /** 12 distinct expense categories — enough variety to exercise the biggest-mover grouping. */
    private fun buildCategories(count: Int): List<Category> =
        (1..count).map { id ->
            Category(
                id = id.toLong(),
                name = "Category $id",
                type = TransactionType.EXPENSE,
                iconKey = "restaurant",
                colorKey = "red",
                isDefault = true,
            )
        }

    /**
     * Spreads [size] expenses across the month's days and categories. Amounts
     * are in VND minor units (whole units, since VND has 0 decimal digits),
     * uniformly distributed to 10k–510k per transaction — realistic mid-market
     * spend pattern.
     */
    private fun buildMonth(
        size: Int,
        monthStart: LocalDate,
        categories: List<Category>,
        idOffset: Long,
        seed: Long,
    ): List<Transaction> {
        val rng = Random(seed)
        val daysInMonth = monthStart.lengthOfMonth()
        return List(size) { index ->
            val dayOfMonth = (index % daysInMonth) + 1
            val timestamp =
                monthStart
                    .withDayOfMonth(dayOfMonth)
                    .atTime(12, 0)
                    .atZone(zone)
                    .toInstant()
                    .toEpochMilli()
            Transaction(
                id = idOffset + index,
                type = TransactionType.EXPENSE,
                amount = rng.nextLong(10_000L, 510_000L),
                currencyCode = "VND",
                category = categories[index % categories.size],
                note = null,
                timestamp = timestamp,
                createdAt = timestamp,
                updatedAt = timestamp,
            )
        }
    }

    private companion object {
        const val WARMUP_ITERATIONS = 3
        const val MEASURED_ITERATIONS = 10

        /**
         * 4× headroom over the 50ms PRD target. A real algorithmic regression
         * (e.g. someone changes biggest-mover from O(n) to O(n²)) would blow
         * past 200ms at 5,000 txns — but a cold CI runner spiking a single
         * measurement by 2–3× won't flake.
         */
        const val CI_BUDGET_MS = 200.0
    }
}
