package dev.tuandoan.expensetracker.domain.analytics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Contract tests for [AnalyticsEvent] and its [AnalyticsEventParam] enums
 * (v3.11.0, ADR-011 + PRD FR-A6 / FR-A7).
 *
 * The sealed-hierarchy shape is the **structural** privacy guarantee — a
 * contributor can't smuggle PII through a `Map<String, Any>` — but these
 * tests pin the invariants that the shape alone doesn't enforce:
 *
 * 1. **Wire values are wire-safe**: non-empty, lowercase,
 *    `[a-z0-9_]`-only, no whitespace or punctuation. GA4 event-parameter
 *    value constraints + privacy-policy disclosure consistency both
 *    depend on this.
 * 2. **Wire values are unique within each enum**: guards against a
 *    rename-then-copy-paste collision that would otherwise silently merge
 *    two semantically distinct events in analytics queries.
 * 3. **Wire values match the privacy-policy disclosure** (`privacy-policy.md`
 *    Section 2 "Optional anonymous usage analytics"): if a wire value
 *    drifts from the published list, this test fails loudly.
 */
class AnalyticsEventTest {
    @Test
    fun allEnumWireValues_matchSafeShape() {
        // Firebase event-parameter values MUST be non-empty and not contain
        // characters that would require escaping or reshape semantics in a
        // GA4 query. We lock down to [a-z0-9_] which is the conservative
        // intersection of GA4 allowance, Firebase SDK tolerance, and the
        // snake_case aesthetic chosen for the privacy-policy disclosure.
        val safe = Regex("^[a-z][a-z0-9_]*$")
        allEnumValues().forEach { param ->
            assertTrue(
                "wireValue '${param.wireValue}' (${param::class.simpleName}.$param) " +
                    "must match $safe",
                safe.matches(param.wireValue),
            )
        }
    }

    @Test
    fun wireValues_areUniqueWithinEachEnum() {
        // Rename collisions (A renamed to B's wireValue) would silently
        // merge two events in GA4 reporting. Catch at compile-adjacent time.
        listOf<List<AnalyticsEventParam>>(
            BuildType.entries.toList(),
            WidgetSize.entries.toList(),
            TransactionKind.entries.toList(),
            TransactionSource.entries.toList(),
            BackupFormat.entries.toList(),
            InsightRowType.entries.toList(),
        ).forEach { group ->
            val values = group.map { it.wireValue }
            assertEquals(
                "Duplicate wireValues within ${group.firstOrNull()?.javaClass?.enclosingClass?.simpleName}",
                values.size,
                values.toSet().size,
            )
        }
    }

    @Test
    fun buildType_hasExactlyDebugAndRelease() {
        assertEquals(setOf("debug", "release"), BuildType.entries.map { it.wireValue }.toSet())
    }

    @Test
    fun widgetSize_hasExactlySmallAndMedium() {
        assertEquals(setOf("small", "medium"), WidgetSize.entries.map { it.wireValue }.toSet())
    }

    @Test
    fun transactionKind_hasExactlyExpenseAndIncome() {
        assertEquals(
            setOf("expense", "income"),
            TransactionKind.entries.map { it.wireValue }.toSet(),
        )
    }

    @Test
    fun transactionSource_hasExactlyManualWidgetAndRecurring() {
        // Pinning the exact set so adding/removing/renaming a source
        // breaks this test loudly. Driven by privacy-policy disclosure —
        // a drift between the enum and the published list is exactly the
        // failure mode this test catches.
        assertEquals(
            setOf("manual", "widget", "recurring"),
            TransactionSource.entries.map { it.wireValue }.toSet(),
        )
    }

    @Test
    fun backupFormat_hasExactlyJsonAndEncrypted() {
        assertEquals(
            setOf("json", "encrypted"),
            BackupFormat.entries.map { it.wireValue }.toSet(),
        )
    }

    @Test
    fun insightRowType_hasExactlyFourDocumentedRows() {
        assertEquals(
            setOf("biggest_mover", "daily_pace", "no_budget_fallback", "day_of_month"),
            InsightRowType.entries.map { it.wireValue }.toSet(),
        )
    }

    @Test
    fun appOpenEvent_carriesOnlyBuildType() {
        // FR-A7 invariant, asserted structurally: AppOpen's only field is a
        // BuildType. Any future PR adding e.g. a `sessionId` parameter must
        // update this test AND the privacy-policy disclosure in lockstep.
        val event = AnalyticsEvent.AppOpen(BuildType.RELEASE)
        assertEquals(BuildType.RELEASE, event.buildType)
    }

    @Test
    fun transactionAddedEvent_carriesOnlyKindAndSource_notAmountOrCategoryOrNote() {
        // Privacy-critical structural assertion: the ONLY payload of a
        // transaction event is expense-vs-income AND the entry-point
        // source (v3.12.0 — see TransactionSource KDoc). A future
        // contributor tempted to add `amount: Long` or `categoryName:
        // String` would have to rewrite this test — which is the point.
        val event =
            AnalyticsEvent.TransactionAdded(
                type = TransactionKind.EXPENSE,
                source = TransactionSource.WIDGET,
            )
        assertEquals(TransactionKind.EXPENSE, event.type)
        assertEquals(TransactionSource.WIDGET, event.source)
    }

    @Test
    fun parameterlessEvents_stayAsDataObjects() {
        // `data object` marks a parameterless singleton. If a future
        // contributor downgrades one of these to a `data class` with a
        // field, `objectInstance` flips to null — which likely means a
        // new parameter was added that needs privacy-policy review.
        val parameterless =
            listOf(
                AnalyticsEvent.OnboardingCompleted,
                AnalyticsEvent.WidgetRemoved,
                AnalyticsEvent.TransactionUndone,
            )
        parameterless.forEach { event ->
            assertSame(
                "${event::class.simpleName} must stay a data object (no params)",
                event,
                event::class.objectInstance,
            )
        }
    }

    private fun allEnumValues(): List<AnalyticsEventParam> =
        BuildType.entries +
            WidgetSize.entries +
            TransactionKind.entries +
            TransactionSource.entries +
            BackupFormat.entries +
            InsightRowType.entries
}
