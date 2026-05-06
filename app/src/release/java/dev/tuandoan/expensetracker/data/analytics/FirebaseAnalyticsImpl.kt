package dev.tuandoan.expensetracker.data.analytics

import dev.tuandoan.expensetracker.domain.analytics.Analytics
import dev.tuandoan.expensetracker.domain.analytics.AnalyticsEvent
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real [Analytics] implementation bound in release builds only (ADR-010,
 * PRD FR-A10). Pure pass-through to [FirebaseAnalyticsWrapper] — all
 * policy (consent observation, FR-A7 parameter filtering) lives upstack.
 *
 * ### Event + parameter names
 *
 * Event names are the snake_case form of the subtype (`AppOpen` →
 * `app_open`) and parameter names match the PRD FR-A6 table exactly
 * (`build_type`, `widget_size`, `type`, `format`, `row_type`).
 *
 * ### Why the `when` is exhaustive
 *
 * [AnalyticsEvent] is a sealed interface, so the Kotlin compiler verifies
 * every subtype is handled — adding a new event (a PR that also updates
 * `privacy-policy.md` and the PRD) forces a compile error here until the
 * mapping is added. That's the design.
 *
 * ### FR-A7 invariant
 *
 * Because the event hierarchy has no `String` or numeric fields outside
 * the shared `wireValue` pattern, this impl CANNOT construct a parameter
 * value containing user-typed text. The only values that reach
 * [FirebaseAnalyticsWrapper.logEvent] are enum `wireValue` strings
 * authored in the codebase. A runtime parameter-validation test lives
 * separately under Task 6.4 per PRD FR-A11.
 */
@Singleton
class FirebaseAnalyticsImpl
    @Inject
    constructor(
        private val analytics: FirebaseAnalyticsWrapper,
    ) : Analytics {
        override fun logEvent(event: AnalyticsEvent) {
            val (name, params) = event.toWire()
            analytics.logEvent(name, params)
        }

        override fun setCollectionEnabled(enabled: Boolean) {
            analytics.setAnalyticsCollectionEnabled(enabled)
        }

        private fun AnalyticsEvent.toWire(): Pair<String, Map<String, String>> =
            when (this) {
                is AnalyticsEvent.AppOpen ->
                    EVENT_APP_OPEN to mapOf(PARAM_BUILD_TYPE to buildType.wireValue)
                AnalyticsEvent.OnboardingCompleted ->
                    EVENT_ONBOARDING_COMPLETED to emptyMap()
                is AnalyticsEvent.WidgetAdded ->
                    EVENT_WIDGET_ADDED to mapOf(PARAM_WIDGET_SIZE to size.wireValue)
                AnalyticsEvent.WidgetRemoved ->
                    EVENT_WIDGET_REMOVED to emptyMap()
                is AnalyticsEvent.TransactionAdded ->
                    EVENT_TRANSACTION_ADDED to mapOf(PARAM_TYPE to type.wireValue)
                is AnalyticsEvent.BackupExported ->
                    EVENT_BACKUP_EXPORTED to mapOf(PARAM_FORMAT to format.wireValue)
                is AnalyticsEvent.BackupImported ->
                    EVENT_BACKUP_IMPORTED to mapOf(PARAM_FORMAT to format.wireValue)
                is AnalyticsEvent.InsightShown ->
                    EVENT_INSIGHT_SHOWN to mapOf(PARAM_ROW_TYPE to rowType.wireValue)
            }

        companion object {
            // Event names — snake_case mirror of AnalyticsEvent subtype names,
            // matching the PRD FR-A6 event table and the privacy-policy
            // disclosure (privacy-policy.md §2) verbatim.
            const val EVENT_APP_OPEN = "app_open"
            const val EVENT_ONBOARDING_COMPLETED = "onboarding_completed"
            const val EVENT_WIDGET_ADDED = "widget_added"
            const val EVENT_WIDGET_REMOVED = "widget_removed"
            const val EVENT_TRANSACTION_ADDED = "transaction_added"
            const val EVENT_BACKUP_EXPORTED = "backup_exported"
            const val EVENT_BACKUP_IMPORTED = "backup_imported"
            const val EVENT_INSIGHT_SHOWN = "insight_shown"

            // Parameter names — match the PRD FR-A6 table column headers.
            const val PARAM_BUILD_TYPE = "build_type"
            const val PARAM_WIDGET_SIZE = "widget_size"
            const val PARAM_TYPE = "type"
            const val PARAM_FORMAT = "format"
            const val PARAM_ROW_TYPE = "row_type"
        }
    }
