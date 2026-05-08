package dev.tuandoan.expensetracker.widget

import dev.tuandoan.expensetracker.core.formatter.CurrencyFormatter
import dev.tuandoan.expensetracker.domain.model.Category
import dev.tuandoan.expensetracker.domain.model.Transaction
import dev.tuandoan.expensetracker.domain.model.TransactionType
import dev.tuandoan.expensetracker.domain.widget.PinnedCategorySlot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class ExpenseWidgetStateMapperTest {
    // Fix time zone + "now" to a deterministic local midnight so every test
    // can reason about today vs. earlier-this-month without drift.
    private val zone = ZoneId.of("UTC")
    private val today: LocalDate = LocalDate.of(2026, 4, 15)
    private val nowMillis: Long =
        today
            .atTime(14, 30)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()
    private val startOfToday: Long =
        today.atStartOfDay(zone).toInstant().toEpochMilli()

    // Tests assert exact strings, so the fake formatter encodes inputs in a
    // stable form rather than mimicking the real currency conventions.
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

    private val foodCategory =
        Category(
            id = 1L,
            name = "Food",
            type = TransactionType.EXPENSE,
            iconKey = "restaurant",
            colorKey = "red",
            isDefault = true,
        )
    private val salaryCategory =
        Category(
            id = 2L,
            name = "Salary",
            type = TransactionType.INCOME,
            iconKey = "payments",
            colorKey = "green",
            isDefault = true,
        )

    private fun expense(
        id: Long,
        amount: Long,
        timestamp: Long,
        currency: String = "VND",
    ): Transaction =
        Transaction(
            id = id,
            type = TransactionType.EXPENSE,
            amount = amount,
            currencyCode = currency,
            category = foodCategory,
            note = null,
            timestamp = timestamp,
            createdAt = timestamp,
            updatedAt = timestamp,
        )

    // --- Today vs. month partitioning ---

    @Test
    fun mapState_todayTxnsCountInTodayTotal_olderTxnsCountOnlyInMonth() {
        val twoDaysAgo = startOfToday - 2 * 24 * 60 * 60 * 1000L
        val transactions =
            listOf(
                expense(id = 1, amount = 50_000, timestamp = twoDaysAgo),
                expense(id = 2, amount = 30_000, timestamp = startOfToday + 60_000),
                expense(id = 3, amount = 20_000, timestamp = nowMillis),
            )

        val state =
            mapExpenseWidgetState(
                monthExpenses = transactions,
                defaultCurrencyCode = "VND",
                budgetAmount = null,
                nowMillis = nowMillis,
                zoneId = zone,
                formatter = fakeFormatter,
            )

        assertEquals("50000 VND", state.todayFormatted) // 30_000 + 20_000
        assertEquals("100000 VND", state.monthFormatted) // all three
    }

    @Test
    fun mapState_transactionExactlyAtMidnight_countsAsToday() {
        val transactions = listOf(expense(id = 1, amount = 10_000, timestamp = startOfToday))

        val state =
            mapExpenseWidgetState(
                monthExpenses = transactions,
                defaultCurrencyCode = "VND",
                budgetAmount = null,
                nowMillis = nowMillis,
                zoneId = zone,
                formatter = fakeFormatter,
            )

        assertEquals("10000 VND", state.todayFormatted)
        assertEquals("10000 VND", state.monthFormatted)
    }

    @Test
    fun mapState_transactionOneMillisBeforeMidnight_isNotToday() {
        val transactions =
            listOf(expense(id = 1, amount = 10_000, timestamp = startOfToday - 1))

        val state =
            mapExpenseWidgetState(
                monthExpenses = transactions,
                defaultCurrencyCode = "VND",
                budgetAmount = null,
                nowMillis = nowMillis,
                zoneId = zone,
                formatter = fakeFormatter,
            )

        assertEquals("0 VND", state.todayFormatted)
        assertEquals("10000 VND", state.monthFormatted)
    }

    @Test
    fun mapState_noTransactions_emitsZeroedFormattedStrings() {
        val state =
            mapExpenseWidgetState(
                monthExpenses = emptyList(),
                defaultCurrencyCode = "VND",
                budgetAmount = null,
                nowMillis = nowMillis,
                zoneId = zone,
                formatter = fakeFormatter,
            )

        assertEquals("0 VND", state.todayFormatted)
        assertEquals("0 VND", state.monthFormatted)
        assertNull(state.budget)
    }

    // --- Income + other-currency filtering ---

    @Test
    fun mapState_incomeTransactions_areIgnored() {
        val transactions =
            listOf(
                expense(id = 1, amount = 50_000, timestamp = nowMillis),
                Transaction(
                    id = 2,
                    type = TransactionType.INCOME,
                    amount = 10_000_000L,
                    currencyCode = "VND",
                    category = salaryCategory,
                    note = null,
                    timestamp = nowMillis,
                    createdAt = nowMillis,
                    updatedAt = nowMillis,
                ),
            )

        val state =
            mapExpenseWidgetState(
                monthExpenses = transactions,
                defaultCurrencyCode = "VND",
                budgetAmount = null,
                nowMillis = nowMillis,
                zoneId = zone,
                formatter = fakeFormatter,
            )

        assertEquals("50000 VND", state.todayFormatted)
        assertEquals("50000 VND", state.monthFormatted)
    }

    @Test
    fun mapState_otherCurrencyTransactions_areSilentlyExcluded() {
        val transactions =
            listOf(
                expense(id = 1, amount = 50_000, timestamp = nowMillis, currency = "VND"),
                expense(id = 2, amount = 120_00, timestamp = nowMillis, currency = "USD"),
            )

        val state =
            mapExpenseWidgetState(
                monthExpenses = transactions,
                defaultCurrencyCode = "VND",
                budgetAmount = null,
                nowMillis = nowMillis,
                zoneId = zone,
                formatter = fakeFormatter,
            )

        assertEquals("50000 VND", state.todayFormatted)
        assertEquals("50000 VND", state.monthFormatted)
    }

    // --- Budget display ---

    @Test
    fun mapState_withinBudget_emitsBudgetBlockWithCoercedFraction() {
        val transactions = listOf(expense(id = 1, amount = 200_000, timestamp = nowMillis))

        val state =
            mapExpenseWidgetState(
                monthExpenses = transactions,
                defaultCurrencyCode = "VND",
                budgetAmount = 1_000_000,
                nowMillis = nowMillis,
                zoneId = zone,
                formatter = fakeFormatter,
            )

        val budget = state.budget
        assertNotNull(budget)
        assertEquals("200000 VND", budget!!.spentFormatted)
        assertEquals("1000000 VND", budget.budgetFormatted)
        assertEquals(0.2f, budget.progressFraction, 0.0001f)
        assertFalse(budget.isOverBudget)
        assertNull(budget.overByFormatted)
    }

    @Test
    fun mapState_overBudget_clampsFractionAtOneAndFlagsOverState() {
        val transactions = listOf(expense(id = 1, amount = 1_500_000, timestamp = nowMillis))

        val state =
            mapExpenseWidgetState(
                monthExpenses = transactions,
                defaultCurrencyCode = "VND",
                budgetAmount = 1_000_000,
                nowMillis = nowMillis,
                zoneId = zone,
                formatter = fakeFormatter,
            )

        val budget = state.budget
        assertNotNull(budget)
        assertEquals(1f, budget!!.progressFraction, 0.0001f)
        assertTrue(budget.isOverBudget)
        // TalkBack announces "Over budget by X" — must be the raw spend-minus-budget
        // difference, pre-formatted via the injected formatter.
        assertEquals("500000 VND", budget.overByFormatted)
    }

    @Test
    fun mapState_exactlyAtBudget_isNotOverAndOverByIsNull() {
        // Edge: spent == budget → rawFraction == 1.0 → isOverBudget = false.
        // overByFormatted must be null because spend does not exceed budget.
        val transactions = listOf(expense(id = 1, amount = 1_000_000, timestamp = nowMillis))

        val state =
            mapExpenseWidgetState(
                monthExpenses = transactions,
                defaultCurrencyCode = "VND",
                budgetAmount = 1_000_000,
                nowMillis = nowMillis,
                zoneId = zone,
                formatter = fakeFormatter,
            )

        val budget = state.budget
        assertNotNull(budget)
        assertEquals(1f, budget!!.progressFraction, 0.0001f)
        assertFalse(budget.isOverBudget)
        assertNull(budget.overByFormatted)
    }

    @Test
    fun mapState_nullBudget_emitsNullBudgetBlock() {
        val transactions = listOf(expense(id = 1, amount = 50_000, timestamp = nowMillis))

        val state =
            mapExpenseWidgetState(
                monthExpenses = transactions,
                defaultCurrencyCode = "VND",
                budgetAmount = null,
                nowMillis = nowMillis,
                zoneId = zone,
                formatter = fakeFormatter,
            )

        assertNull(state.budget)
    }

    @Test
    fun mapState_zeroBudget_emitsNullBudgetBlock() {
        // A zero budget means "no budget set" per BudgetPreferences semantics;
        // don't render a degenerate 0/0 row.
        val transactions = listOf(expense(id = 1, amount = 50_000, timestamp = nowMillis))

        val state =
            mapExpenseWidgetState(
                monthExpenses = transactions,
                defaultCurrencyCode = "VND",
                budgetAmount = 0L,
                nowMillis = nowMillis,
                zoneId = zone,
                formatter = fakeFormatter,
            )

        assertNull(state.budget)
    }

    // --- LOADING state ---

    @Test
    fun loading_isEmptyPlaceholder() {
        val loading = ExpenseWidgetState.LOADING
        assertEquals("", loading.currencyCode)
        assertEquals("", loading.todayFormatted)
        assertEquals("", loading.monthFormatted)
        assertNull(loading.budget)
        // Pinned slots pre-populated so the medium layout never reflows
        // between the loading tile strip and the first real emission.
        assertEquals(3, loading.pinnedCategories.size)
        assertTrue(loading.pinnedCategories.all { it is PinnedCategorySlot.Empty })
        assertEquals(listOf(1, 2, 3), loading.pinnedCategories.map { it.index })
    }

    // --- Currency plumbing ---

    @Test
    fun mapState_usdMinorUnits_passThroughToFormatter() {
        val transactions = listOf(expense(id = 1, amount = 4200, timestamp = nowMillis, currency = "USD"))

        val state =
            mapExpenseWidgetState(
                monthExpenses = transactions,
                defaultCurrencyCode = "USD",
                budgetAmount = 100_000,
                nowMillis = nowMillis,
                zoneId = zone,
                formatter = fakeFormatter,
            )

        assertEquals("USD", state.currencyCode)
        assertEquals("4200 USD", state.todayFormatted)
        assertEquals("4200 USD", state.monthFormatted)
        assertEquals("4200 USD", state.budget!!.spentFormatted)
        assertEquals("100000 USD", state.budget!!.budgetFormatted)
    }

    @Test
    fun mapState_todayDatedOtherCurrency_doesNotInflateTodayBucket() {
        // A non-default-currency expense dated for "today" must be silently
        // filtered — it must NOT bleed into the default-currency today total.
        // Guards against a regression where the today-vs-older partitioning
        // runs before the currency filter.
        val transactions =
            listOf(
                expense(id = 1, amount = 120_000, timestamp = nowMillis, currency = "USD"),
            )

        val state =
            mapExpenseWidgetState(
                monthExpenses = transactions,
                defaultCurrencyCode = "VND",
                budgetAmount = null,
                nowMillis = nowMillis,
                zoneId = zone,
                formatter = fakeFormatter,
            )

        assertEquals("0 VND", state.todayFormatted)
        assertEquals("0 VND", state.monthFormatted)
    }

    @Test
    fun mapState_spentExceedsBudgetByOne_isOver() {
        // Boundary: rawFraction = (budget + 1) / budget is > 1.0f, so isOverBudget
        // must be true. Guards against an accidental change from ">" to ">=" in
        // the mapper that would treat spent == budget as over-budget.
        val transactions = listOf(expense(id = 1, amount = 1_000_001, timestamp = nowMillis))

        val state =
            mapExpenseWidgetState(
                monthExpenses = transactions,
                defaultCurrencyCode = "VND",
                budgetAmount = 1_000_000,
                nowMillis = nowMillis,
                zoneId = zone,
                formatter = fakeFormatter,
            )

        val budget = state.budget
        assertNotNull(budget)
        assertTrue(budget!!.isOverBudget)
        assertEquals("1 VND", budget.overByFormatted)
    }

    // --- Time zone handling ---

    @Test
    fun mapState_nonUtcZone_partitionsUsingInjectedZone() {
        // Verify the mapper reads the injected zoneId — not ZoneId.systemDefault().
        // For America/New_York, local midnight on 2026-04-15 is 04:00Z (UTC),
        // so a transaction stamped at 03:00Z belongs to the previous day
        // (2026-04-14 in NY time). Must count in month total but NOT in today.
        val nyZone = ZoneId.of("America/New_York")
        val nyToday = LocalDate.of(2026, 4, 15)
        val nyNow =
            nyToday
                .atTime(10, 0) // 10:00 NY = 14:00Z
                .atZone(nyZone)
                .toInstant()
                .toEpochMilli()
        val nyStartOfToday =
            nyToday.atStartOfDay(nyZone).toInstant().toEpochMilli() // 04:00Z

        // One millisecond before local midnight in NY → belongs to April 14 in NY time.
        val yesterdayLateNight = nyStartOfToday - 1
        val transactions =
            listOf(
                expense(id = 1, amount = 50_000, timestamp = yesterdayLateNight),
                expense(id = 2, amount = 30_000, timestamp = nyNow),
            )

        val state =
            mapExpenseWidgetState(
                monthExpenses = transactions,
                defaultCurrencyCode = "VND",
                budgetAmount = null,
                nowMillis = nyNow,
                zoneId = nyZone,
                formatter = fakeFormatter,
            )

        assertEquals("30000 VND", state.todayFormatted)
        assertEquals("80000 VND", state.monthFormatted)
    }

    @Test
    fun mapState_dstSpringForward_dayBoundaryStillCorrect() {
        // 2026-03-08 is the US DST spring-forward date. NY local clocks skip
        // from 02:00 to 03:00 EST→EDT; the day still starts at 00:00 local
        // (which is 05:00Z, not 04:00Z as on non-DST days).
        // A transaction at 05:30Z on 2026-03-08 is 00:30 local — "today".
        // A transaction at 04:30Z on 2026-03-08 is 23:30 local on 2026-03-07 — NOT today.
        val nyZone = ZoneId.of("America/New_York")
        val dstDay = LocalDate.of(2026, 3, 8)
        val nyNoon =
            dstDay
                .atTime(12, 0)
                .atZone(nyZone)
                .toInstant()
                .toEpochMilli()

        val localStartOfDstDay =
            dstDay.atStartOfDay(nyZone).toInstant().toEpochMilli()

        val justAfterDstMidnight = localStartOfDstDay + 30 * 60_000L // +30 min in the new day
        val justBeforeDstMidnight = localStartOfDstDay - 30 * 60_000L // -30 min, previous day

        val transactions =
            listOf(
                expense(id = 1, amount = 10_000, timestamp = justBeforeDstMidnight),
                expense(id = 2, amount = 20_000, timestamp = justAfterDstMidnight),
            )

        val state =
            mapExpenseWidgetState(
                monthExpenses = transactions,
                defaultCurrencyCode = "VND",
                budgetAmount = null,
                nowMillis = nyNoon,
                zoneId = nyZone,
                formatter = fakeFormatter,
            )

        assertEquals("20000 VND", state.todayFormatted)
        assertEquals("30000 VND", state.monthFormatted)
    }

    // --- Pinned categories pass-through (T2.2) ---
    //
    // The mapper doesn't compute pins — `PinnedCategoriesUseCase` already
    // padded the list to 3 slots + filtered deleted IDs. These cases pin
    // the pass-through contract so a future refactor can't silently drop
    // the list or mutate slot order.

    private val groceriesCategory =
        Category(
            id = 10L,
            name = "Groceries",
            type = TransactionType.EXPENSE,
            iconKey = "restaurant",
            colorKey = "green",
            isDefault = false,
        )
    private val coffeeCategory =
        Category(
            id = 11L,
            name = "Coffee",
            type = TransactionType.EXPENSE,
            iconKey = "coffee",
            colorKey = "blue",
            isDefault = false,
        )
    private val transitCategory =
        Category(
            id = 12L,
            name = "Transit",
            type = TransactionType.EXPENSE,
            iconKey = "directions_bus",
            colorKey = "yellow",
            isDefault = false,
        )

    @Test
    fun mapState_threeFilledPins_passesThroughUnchanged() {
        val pins =
            listOf(
                PinnedCategorySlot.Filled(index = 1, category = groceriesCategory),
                PinnedCategorySlot.Filled(index = 2, category = coffeeCategory),
                PinnedCategorySlot.Filled(index = 3, category = transitCategory),
            )

        val state =
            mapExpenseWidgetState(
                monthExpenses = emptyList(),
                defaultCurrencyCode = "VND",
                budgetAmount = null,
                nowMillis = nowMillis,
                zoneId = zone,
                formatter = fakeFormatter,
                pinnedCategories = pins,
            )

        assertEquals(pins, state.pinnedCategories)
    }

    @Test
    fun mapState_partialPins_preservesEmptySlotAtCorrectIndex() {
        // Upstream use case guarantees: two filled + one empty in slot 3.
        // Mapper must not reorder or drop the empty.
        val pins =
            listOf(
                PinnedCategorySlot.Filled(index = 1, category = groceriesCategory),
                PinnedCategorySlot.Filled(index = 2, category = coffeeCategory),
                PinnedCategorySlot.Empty(index = 3),
            )

        val state =
            mapExpenseWidgetState(
                monthExpenses = emptyList(),
                defaultCurrencyCode = "VND",
                budgetAmount = null,
                nowMillis = nowMillis,
                zoneId = zone,
                formatter = fakeFormatter,
                pinnedCategories = pins,
            )

        assertEquals(pins, state.pinnedCategories)
        assertTrue(state.pinnedCategories[2] is PinnedCategorySlot.Empty)
    }

    @Test
    fun mapState_allEmptyPins_passesThroughAsThreeEmpty() {
        val pins =
            listOf(
                PinnedCategorySlot.Empty(index = 1),
                PinnedCategorySlot.Empty(index = 2),
                PinnedCategorySlot.Empty(index = 3),
            )

        val state =
            mapExpenseWidgetState(
                monthExpenses = emptyList(),
                defaultCurrencyCode = "VND",
                budgetAmount = null,
                nowMillis = nowMillis,
                zoneId = zone,
                formatter = fakeFormatter,
                pinnedCategories = pins,
            )

        assertEquals(pins, state.pinnedCategories)
        assertTrue(state.pinnedCategories.all { it is PinnedCategorySlot.Empty })
    }

    @Test
    fun mapState_deletedPinFallback_keepsEmptyAtOriginalIndex() {
        // Simulates FR-07: use case emitted Empty(2) because the pinned ID
        // for slot 2 no longer resolves to a category. Mapper must not
        // "compact" the list or treat the empty as an error.
        val pins =
            listOf(
                PinnedCategorySlot.Filled(index = 1, category = groceriesCategory),
                PinnedCategorySlot.Empty(index = 2),
                PinnedCategorySlot.Filled(index = 3, category = transitCategory),
            )

        val state =
            mapExpenseWidgetState(
                monthExpenses = emptyList(),
                defaultCurrencyCode = "VND",
                budgetAmount = null,
                nowMillis = nowMillis,
                zoneId = zone,
                formatter = fakeFormatter,
                pinnedCategories = pins,
            )

        assertEquals(pins, state.pinnedCategories)
        assertEquals(
            PinnedCategorySlot.Filled(index = 3, category = transitCategory),
            state.pinnedCategories[2],
        )
    }

    @Test
    fun mapState_pinnedCategoriesOmitted_defaultsToEmptyList() {
        // Callers that don't care about tiles (e.g. existing small-widget
        // tests) can skip the parameter — the mapper uses an empty list
        // rather than throwing.
        val state =
            mapExpenseWidgetState(
                monthExpenses = emptyList(),
                defaultCurrencyCode = "VND",
                budgetAmount = null,
                nowMillis = nowMillis,
                zoneId = zone,
                formatter = fakeFormatter,
            )

        assertEquals(emptyList<PinnedCategorySlot>(), state.pinnedCategories)
    }
}
