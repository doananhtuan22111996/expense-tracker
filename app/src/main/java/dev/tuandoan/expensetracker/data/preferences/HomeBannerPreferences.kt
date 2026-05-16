package dev.tuandoan.expensetracker.data.preferences

import kotlinx.coroutines.flow.Flow

/**
 * One-shot dismissal flags for in-app announcement banners on the Home
 * screen. Each flag is keyed by a feature/version pair (e.g.
 * `widget_quick_add_v3_12_0`) so a banner from one release does not bleed
 * into the next: a future v3.13.0 banner gets its own flag and ignores the
 * v3.12.0 dismissal.
 *
 * Placed in `data/preferences/` alongside [InsightsCollapsePreferences] —
 * DataStore-backed prefs are an inherently data-layer concern.
 */
interface HomeBannerPreferences {
    /**
     * Emits `true` once the v3.12.0 widget-quick-add announcement banner
     * has been dismissed by the user. Defaults to `false` on fresh installs
     * and on upgrades from a pre-v3.12.0 build.
     */
    val widgetQuickAddBannerDismissed: Flow<Boolean>

    /** Persist the v3.12.0 banner as dismissed. Idempotent. */
    suspend fun setWidgetQuickAddBannerDismissed()
}
