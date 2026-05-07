package dev.tuandoan.expensetracker.domain.analytics

import dev.tuandoan.expensetracker.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pins the [TransactionType] → [TransactionKind] mapping for the
 * `transaction_added` event (v3.11.0, PRD FR-A6 / FR-A7).
 *
 * Sibling to [InsightRowAnalyticsMappingTest]. The mapping's `when` over
 * [TransactionType] is compiler-checked exhaustive; these tests are the
 * runtime companion that guards wire-value drift — if `EXPENSE` or
 * `INCOME` ever got renamed on the enum side and the mapping wasn't
 * updated in lockstep, these assertions would flag it loudly.
 *
 * The two-case coverage today looks trivial. The real value is that
 * when [TransactionType] grows a future variant (e.g. a hypothetical
 * `TRANSFER` for multi-wallet support), the sealed-enum exhaustive
 * `when` fails compile *in the mapping file*, forcing a decision here
 * — should the new type ship to Firebase? If yes, the mapping grows
 * and this test grows with it; if no, the `when` adds a `null` return
 * and [AnalyticsEvent.TransactionAdded] stops being fired for that
 * variant. Either way, the change is visible to review.
 */
class TransactionTypeAnalyticsMappingTest {
    @Test
    fun expense_mapsToExpenseKind() {
        assertEquals(TransactionKind.EXPENSE, TransactionType.EXPENSE.toAnalyticsKind())
    }

    @Test
    fun income_mapsToIncomeKind() {
        assertEquals(TransactionKind.INCOME, TransactionType.INCOME.toAnalyticsKind())
    }
}
