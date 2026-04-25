package dev.tuandoan.expensetracker.data.preferences

import kotlinx.coroutines.flow.Flow

/**
 * Preferences for the Summary-tab Insights section collapse state. When
 * collapsed, the section's rows are hidden but the header remains visible
 * so users can restore it. Defaults to `false` (expanded) — the PRD
 * prioritizes discoverability for new users over conservation of space.
 *
 * Placed in `data/preferences/` alongside [BackupEncryptionPreferences] and
 * friends rather than under `domain/repository/` because DataStore-backed
 * preferences are an inherently data-layer concern — matches the pattern
 * already established for analytics / review / backup-encryption prefs.
 */
interface InsightsCollapsePreferences {
    /** Emits the current collapse state; `false` means the section is expanded. */
    val collapsed: Flow<Boolean>

    /** Persist [value] as the new collapse state. Runs on the injected IO dispatcher. */
    suspend fun setCollapsed(value: Boolean)
}
