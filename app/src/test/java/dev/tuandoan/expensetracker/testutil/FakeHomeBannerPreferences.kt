package dev.tuandoan.expensetracker.testutil

import dev.tuandoan.expensetracker.data.preferences.HomeBannerPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Test double for [HomeBannerPreferences]. Single source of truth across
 * test modules — see [FakeAnalyticsPreferences] for the pattern.
 *
 * Default `widgetQuickAddBannerDismissed = false` matches the real impl's
 * "fresh install" default.
 */
class FakeHomeBannerPreferences : HomeBannerPreferences {
    private val _widgetQuickAddBannerDismissed = MutableStateFlow(false)
    override val widgetQuickAddBannerDismissed: Flow<Boolean> = _widgetQuickAddBannerDismissed

    /** Number of times [setWidgetQuickAddBannerDismissed] has been called. */
    var dismissCallCount: Int = 0
        private set

    override suspend fun setWidgetQuickAddBannerDismissed() {
        dismissCallCount += 1
        _widgetQuickAddBannerDismissed.value = true
    }
}
