package dev.tuandoan.expensetracker.domain.analytics

import org.junit.Test

/**
 * Trivial contract test for [NoOpAnalytics] — mirrors the shape of
 * [dev.tuandoan.expensetracker.domain.crash.NoOpCrashReporter]'s coverage.
 *
 * The implementation is literally empty-body; these tests just pin the
 * "must not throw" contract so a future accidental `throw` or `require`
 * inside the debug binding can't silently crash a debug build.
 */
class NoOpAnalyticsTest {
    @Test
    fun logEvent_doesNotThrow_forEveryEventSubtype() {
        val noOp = NoOpAnalytics()
        listOf(
            AnalyticsEvent.AppOpen(BuildType.RELEASE),
            AnalyticsEvent.AppOpen(BuildType.DEBUG),
            AnalyticsEvent.OnboardingCompleted,
            AnalyticsEvent.WidgetAdded(WidgetSize.SMALL),
            AnalyticsEvent.WidgetAdded(WidgetSize.MEDIUM),
            AnalyticsEvent.WidgetRemoved,
            AnalyticsEvent.TransactionAdded(TransactionKind.EXPENSE, TransactionSource.MANUAL, tripAttached = false),
            AnalyticsEvent.TransactionAdded(TransactionKind.INCOME, TransactionSource.WIDGET, tripAttached = true),
            AnalyticsEvent.TransactionAdded(TransactionKind.EXPENSE, TransactionSource.RECURRING, tripAttached = false),
            AnalyticsEvent.TransactionUndone,
            AnalyticsEvent.BackupExported(BackupFormat.JSON),
            AnalyticsEvent.BackupExported(BackupFormat.ENCRYPTED),
            AnalyticsEvent.BackupImported(BackupFormat.JSON),
            AnalyticsEvent.BackupImported(BackupFormat.ENCRYPTED),
            AnalyticsEvent.InsightShown(InsightRowType.BIGGEST_MOVER),
            AnalyticsEvent.InsightShown(InsightRowType.DAILY_PACE),
            AnalyticsEvent.InsightShown(InsightRowType.NO_BUDGET_FALLBACK),
            AnalyticsEvent.InsightShown(InsightRowType.DAY_OF_MONTH),
            AnalyticsEvent.TripCreated(foreignCurrency = true),
            AnalyticsEvent.TripConvertedFromCategory(TransactionCountBucket.ONE_TO_NINE, foreignCurrency = false),
        ).forEach { noOp.logEvent(it) }
    }

    @Test
    fun setCollectionEnabled_doesNotThrow_inBothDirections() {
        val noOp = NoOpAnalytics()
        noOp.setCollectionEnabled(true)
        noOp.setCollectionEnabled(false)
        noOp.setCollectionEnabled(true)
    }
}
