package dev.tuandoan.expensetracker.data.analytics

import dev.tuandoan.expensetracker.domain.analytics.AnalyticsEvent
import dev.tuandoan.expensetracker.domain.analytics.BackupFormat
import dev.tuandoan.expensetracker.domain.analytics.BuildType
import dev.tuandoan.expensetracker.domain.analytics.InsightRowType
import dev.tuandoan.expensetracker.domain.analytics.TransactionKind
import dev.tuandoan.expensetracker.domain.analytics.TransactionSource
import dev.tuandoan.expensetracker.domain.analytics.WidgetSize
import org.junit.Test
import org.mockito.Mockito.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

/**
 * Unit tests for [FirebaseAnalyticsImpl] (PRD FR-A6 / FR-A9 / FR-A10).
 *
 * Verifies:
 * 1. Impl is a pure pass-through to [FirebaseAnalyticsWrapper] — no
 *    branching on consent inside the impl, no silent swallowing.
 * 2. Each [AnalyticsEvent] subtype maps to the exact event name AND
 *    parameter map shape documented in the PRD event table + the
 *    `privacy-policy.md` Section 2 disclosure. Drift here is a loud
 *    failure, not a silent reporting bug.
 *
 * Runs under the `release` source set so Hilt + variant wiring is
 * exercised the same way a real release build would exercise it. Mirrors
 * the [dev.tuandoan.expensetracker.data.crash.FirebaseCrashReporterImplTest]
 * pattern from PR #103.
 */
class FirebaseAnalyticsImplTest {
    @Test
    fun logEvent_appOpen_release_mapsToAppOpenNameAndBuildTypeParam() {
        val wrapper: FirebaseAnalyticsWrapper = mock()
        val impl = FirebaseAnalyticsImpl(wrapper)

        impl.logEvent(AnalyticsEvent.AppOpen(BuildType.RELEASE))

        verify(wrapper).logEvent(
            name = "app_open",
            params = mapOf("build_type" to "release"),
        )
    }

    @Test
    fun logEvent_appOpen_debug_mapsToAppOpenNameAndDebugBuildType() {
        val wrapper: FirebaseAnalyticsWrapper = mock()
        val impl = FirebaseAnalyticsImpl(wrapper)

        impl.logEvent(AnalyticsEvent.AppOpen(BuildType.DEBUG))

        verify(wrapper).logEvent(
            name = "app_open",
            params = mapOf("build_type" to "debug"),
        )
    }

    @Test
    fun logEvent_onboardingCompleted_mapsToNameWithNoParams() {
        // Parameterless events must send an empty map, not a map with a
        // placeholder key. FR-A7 defence: fewer parameter slots = less
        // surface for future PII smuggling.
        val wrapper: FirebaseAnalyticsWrapper = mock()
        val impl = FirebaseAnalyticsImpl(wrapper)

        impl.logEvent(AnalyticsEvent.OnboardingCompleted)

        verify(wrapper).logEvent(
            name = "onboarding_completed",
            params = emptyMap(),
        )
    }

    @Test
    fun logEvent_widgetAdded_small_mapsToWidgetSizeParam() {
        val wrapper: FirebaseAnalyticsWrapper = mock()
        val impl = FirebaseAnalyticsImpl(wrapper)

        impl.logEvent(AnalyticsEvent.WidgetAdded(WidgetSize.SMALL))

        verify(wrapper).logEvent(
            name = "widget_added",
            params = mapOf("widget_size" to "small"),
        )
    }

    @Test
    fun logEvent_widgetAdded_medium_mapsToWidgetSizeParam() {
        val wrapper: FirebaseAnalyticsWrapper = mock()
        val impl = FirebaseAnalyticsImpl(wrapper)

        impl.logEvent(AnalyticsEvent.WidgetAdded(WidgetSize.MEDIUM))

        verify(wrapper).logEvent(
            name = "widget_added",
            params = mapOf("widget_size" to "medium"),
        )
    }

    @Test
    fun logEvent_widgetRemoved_mapsToNameWithNoParams() {
        val wrapper: FirebaseAnalyticsWrapper = mock()
        val impl = FirebaseAnalyticsImpl(wrapper)

        impl.logEvent(AnalyticsEvent.WidgetRemoved)

        verify(wrapper).logEvent(
            name = "widget_removed",
            params = emptyMap(),
        )
    }

    @Test
    fun logEvent_transactionAdded_expenseManual_mapsToTypeAndSourceParams_noAmountOrCategoryName() {
        // Privacy-critical regression guard: TransactionAdded must emit
        // ONLY `type` + `source` (v3.12.0), never leak into an `amount`,
        // `currency`, or `category_name` parameter. The sealed hierarchy
        // makes this structural; this test makes it visible at the wire
        // boundary.
        val wrapper: FirebaseAnalyticsWrapper = mock()
        val impl = FirebaseAnalyticsImpl(wrapper)

        impl.logEvent(AnalyticsEvent.TransactionAdded(TransactionKind.EXPENSE, TransactionSource.MANUAL))

        verify(wrapper).logEvent(
            name = "transaction_added",
            params = mapOf("type" to "expense", "source" to "manual"),
        )
    }

    @Test
    fun logEvent_transactionAdded_incomeWidget_mapsToTypeAndSourceParams() {
        val wrapper: FirebaseAnalyticsWrapper = mock()
        val impl = FirebaseAnalyticsImpl(wrapper)

        impl.logEvent(AnalyticsEvent.TransactionAdded(TransactionKind.INCOME, TransactionSource.WIDGET))

        verify(wrapper).logEvent(
            name = "transaction_added",
            params = mapOf("type" to "income", "source" to "widget"),
        )
    }

    @Test
    fun logEvent_transactionAdded_recurringSource_emitsRecurringWireValue() {
        // RECURRING has no existing call site today (RecurringTransactionWorker
        // doesn't fire analytics); this test pins the mapping so a future
        // wire-up lands cleanly without a surprise at the wire boundary.
        val wrapper: FirebaseAnalyticsWrapper = mock()
        val impl = FirebaseAnalyticsImpl(wrapper)

        impl.logEvent(AnalyticsEvent.TransactionAdded(TransactionKind.EXPENSE, TransactionSource.RECURRING))

        verify(wrapper).logEvent(
            name = "transaction_added",
            params = mapOf("type" to "expense", "source" to "recurring"),
        )
    }

    @Test
    fun logEvent_transactionUndone_emitsNamedEventWithNoParams() {
        // Parameterless event — the sealed `data object` shape is what
        // guarantees we never accidentally smuggle a parameter here. This
        // test pins the wire-boundary shape (event name + empty params map).
        val wrapper: FirebaseAnalyticsWrapper = mock()
        val impl = FirebaseAnalyticsImpl(wrapper)

        impl.logEvent(AnalyticsEvent.TransactionUndone)

        verify(wrapper).logEvent(
            name = "transaction_undone",
            params = emptyMap(),
        )
    }

    @Test
    fun logEvent_backupExported_json_mapsToFormatParam() {
        val wrapper: FirebaseAnalyticsWrapper = mock()
        val impl = FirebaseAnalyticsImpl(wrapper)

        impl.logEvent(AnalyticsEvent.BackupExported(BackupFormat.JSON))

        verify(wrapper).logEvent(
            name = "backup_exported",
            params = mapOf("format" to "json"),
        )
    }

    @Test
    fun logEvent_backupExported_encrypted_mapsToFormatParam() {
        val wrapper: FirebaseAnalyticsWrapper = mock()
        val impl = FirebaseAnalyticsImpl(wrapper)

        impl.logEvent(AnalyticsEvent.BackupExported(BackupFormat.ENCRYPTED))

        verify(wrapper).logEvent(
            name = "backup_exported",
            params = mapOf("format" to "encrypted"),
        )
    }

    @Test
    fun logEvent_backupImported_bothFormats_mapToImportedNameAndFormatParam() {
        val wrapper: FirebaseAnalyticsWrapper = mock()
        val impl = FirebaseAnalyticsImpl(wrapper)

        impl.logEvent(AnalyticsEvent.BackupImported(BackupFormat.JSON))
        impl.logEvent(AnalyticsEvent.BackupImported(BackupFormat.ENCRYPTED))

        verify(wrapper).logEvent("backup_imported", mapOf("format" to "json"))
        verify(wrapper).logEvent("backup_imported", mapOf("format" to "encrypted"))
    }

    @Test
    fun logEvent_insightShown_allFourRowTypes_mapToInsightNameAndRowTypeParam() {
        val wrapper: FirebaseAnalyticsWrapper = mock()
        val impl = FirebaseAnalyticsImpl(wrapper)

        InsightRowType.entries.forEach { rowType ->
            impl.logEvent(AnalyticsEvent.InsightShown(rowType))
            verify(wrapper).logEvent(
                name = "insight_shown",
                params = mapOf("row_type" to rowType.wireValue),
            )
        }
    }

    @Test
    fun setCollectionEnabled_true_forwardsToWrapper() {
        val wrapper: FirebaseAnalyticsWrapper = mock()
        val impl = FirebaseAnalyticsImpl(wrapper)

        impl.setCollectionEnabled(true)

        verify(wrapper).setAnalyticsCollectionEnabled(true)
    }

    @Test
    fun setCollectionEnabled_false_forwardsToWrapper() {
        // PRD FR-A5: flipping consent false MUST invoke
        // setAnalyticsCollectionEnabled(false) on the underlying SDK.
        val wrapper: FirebaseAnalyticsWrapper = mock()
        val impl = FirebaseAnalyticsImpl(wrapper)

        impl.setCollectionEnabled(false)

        verify(wrapper).setAnalyticsCollectionEnabled(false)
    }

    @Test
    fun setCollectionEnabled_ordering_trueThenFalse_forwardsBothInOrder() {
        // Regression guard: rapid toggle in Settings must preserve ordering.
        val wrapper: FirebaseAnalyticsWrapper = mock()
        val impl = FirebaseAnalyticsImpl(wrapper)

        impl.setCollectionEnabled(true)
        impl.setCollectionEnabled(false)

        val inOrder = inOrder(wrapper)
        inOrder.verify(wrapper).setAnalyticsCollectionEnabled(true)
        inOrder.verify(wrapper).setAnalyticsCollectionEnabled(false)
    }
}
