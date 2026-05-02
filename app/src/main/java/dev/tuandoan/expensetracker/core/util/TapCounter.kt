package dev.tuandoan.expensetracker.core.util

/**
 * Tracks consecutive taps within a rolling time window. Used for Easter-egg
 * gestures (v3.11.0: 7 taps on Settings version text within 3s unlocks the
 * debug panel per ADR-010).
 *
 * Behavior:
 * - The first tap starts the window.
 * - Each subsequent tap within [windowMillis] of the first one increments the
 *   counter. A tap outside the window restarts with a fresh window.
 * - On reaching [targetCount], [tap] returns `true` and resets — the next tap
 *   begins a brand-new window.
 *
 * Not thread-safe by design: the only caller is a Compose click handler
 * dispatched on the main thread.
 *
 * @param targetCount number of taps required to trigger (default 7)
 * @param windowMillis rolling window starting at the first tap (default 3s)
 * @param now clock source — injected for deterministic unit tests
 */
class TapCounter(
    private val targetCount: Int = DEFAULT_TARGET_COUNT,
    private val windowMillis: Long = DEFAULT_WINDOW_MILLIS,
    private val now: () -> Long = { System.currentTimeMillis() },
) {
    private var count: Int = 0
    private var firstTapAt: Long = 0L

    /** Records a tap. Returns `true` exactly when the threshold is reached. */
    fun tap(): Boolean {
        val currentTime = now()
        if (count == 0 || currentTime - firstTapAt > windowMillis) {
            // Start (or restart) a fresh window.
            count = 1
            firstTapAt = currentTime
            return false
        }
        count++
        if (count >= targetCount) {
            count = 0
            firstTapAt = 0L
            return true
        }
        return false
    }

    private companion object {
        const val DEFAULT_TARGET_COUNT = 7
        const val DEFAULT_WINDOW_MILLIS = 3_000L
    }
}
