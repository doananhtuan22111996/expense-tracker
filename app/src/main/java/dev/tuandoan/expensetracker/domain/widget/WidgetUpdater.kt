package dev.tuandoan.expensetracker.domain.widget

/**
 * Domain abstraction over the home-screen widget refresh trigger. Keeps the
 * data-layer repositories free of direct Glance / Android dependencies — they
 * call [requestUpdate] after any change that would affect widget content and
 * the Glance implementation ([GlanceWidgetUpdater]) handles the actual
 * `updateAll()` call off the main thread.
 *
 * Implementations MUST be safe to call from a cancelled coroutine scope —
 * the widget refresh is always best-effort and should complete independently
 * of whatever caller triggered it. See `GlanceWidgetUpdater.requestUpdate`
 * for the `NonCancellable + Dispatchers.IO` pattern.
 */
interface WidgetUpdater {
    suspend fun requestUpdate()
}
