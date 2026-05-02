package dev.tuandoan.expensetracker.core.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [TapCounter]. Uses an injected clock so timing is deterministic.
 *
 * Real default is 7 taps within 3s. Tests use a smaller `targetCount = 3` +
 * shorter `windowMillis = 100` to make the logic obvious without being tied to
 * the production constants.
 */
class TapCounterTest {
    private var fakeNow: Long = 0L
    private val clock: () -> Long = { fakeNow }

    @Test
    fun firstTapAlone_returnsFalse() {
        val counter = TapCounter(targetCount = 3, windowMillis = 100L, now = clock)

        assertFalse(counter.tap())
    }

    @Test
    fun reachingThreshold_withinWindow_returnsTrue() {
        val counter = TapCounter(targetCount = 3, windowMillis = 100L, now = clock)

        fakeNow = 0L
        assertFalse(counter.tap()) // 1
        fakeNow = 30L
        assertFalse(counter.tap()) // 2
        fakeNow = 60L
        assertTrue(counter.tap()) // 3 — threshold
    }

    @Test
    fun tapOutsideWindow_restartsCounter() {
        val counter = TapCounter(targetCount = 3, windowMillis = 100L, now = clock)

        fakeNow = 0L
        counter.tap() // 1
        fakeNow = 50L
        counter.tap() // 2
        fakeNow = 200L // way past the 100ms window
        assertFalse(counter.tap()) // restart — treated as tap 1 of a new window

        // Now a fresh 3 taps within 100ms should succeed:
        fakeNow = 210L
        assertFalse(counter.tap())
        fakeNow = 220L
        assertTrue(counter.tap())
    }

    @Test
    fun afterSuccess_counterResets_requiringFullThresholdAgain() {
        val counter = TapCounter(targetCount = 3, windowMillis = 100L, now = clock)

        // First success
        fakeNow = 0L
        counter.tap()
        fakeNow = 30L
        counter.tap()
        fakeNow = 60L
        assertTrue(counter.tap())

        // Immediately tapping again should NOT re-fire on tap 1 or 2.
        fakeNow = 70L
        assertFalse(counter.tap())
        fakeNow = 80L
        assertFalse(counter.tap())
        fakeNow = 90L
        assertTrue(counter.tap()) // needs the full 3 again
    }

    @Test
    fun tapAtExactWindowBoundary_stillInsideWindow() {
        // Boundary check: a tap at exactly windowMillis after the first is on
        // the "still inside" side (`> window` is the reset predicate, not `>=`).
        val counter = TapCounter(targetCount = 3, windowMillis = 100L, now = clock)

        fakeNow = 0L
        counter.tap() // 1
        fakeNow = 50L
        assertFalse(counter.tap()) // 2 (well inside window)
        fakeNow = 100L // exactly at boundary — still inside
        assertTrue(counter.tap()) // 3 — threshold
    }

    @Test
    fun tapOneTickAfterWindow_restartsCounter() {
        // 1ms past the window boundary resets the counter. Pairs with the
        // boundary test above to pin the `>` vs `>=` semantics.
        val counter = TapCounter(targetCount = 3, windowMillis = 100L, now = clock)

        fakeNow = 0L
        counter.tap()
        fakeNow = 101L // 1ms past boundary → reset
        assertFalse(counter.tap()) // treated as tap 1
        fakeNow = 130L
        assertFalse(counter.tap()) // tap 2
        fakeNow = 160L
        assertTrue(counter.tap()) // tap 3 of the new window
    }
}
