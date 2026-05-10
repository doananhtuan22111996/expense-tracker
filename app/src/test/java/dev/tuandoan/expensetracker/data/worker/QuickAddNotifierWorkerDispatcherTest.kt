package dev.tuandoan.expensetracker.data.worker

import dev.tuandoan.expensetracker.testutil.FakeQuickAddNotificationSurface
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [QuickAddNotifierWorker.Dispatcher] — the pure-Kotlin
 * branching logic extracted from the Worker so it's JVM-testable without
 * instantiating `WorkManager` or Robolectric.
 *
 * Validates:
 * - EXPIRE mode calls `updateQuickAddConfirmationWithoutUndo` with exact args.
 * - DISMISS mode calls `cancelQuickAddConfirmation` with the notification id.
 * - Invalid notification id (the `-1` sentinel) is dropped silently.
 * - Unknown mode string is dropped silently via `Mode.parse` returning null.
 * - Blank/missing amount or category in EXPIRE mode is dropped silently.
 *
 * Plus the `Mode.parse` helper in its own test block.
 */
class QuickAddNotifierWorkerDispatcherTest {
    private val notificationSurface = FakeQuickAddNotificationSurface()
    private val dispatcher = QuickAddNotifierWorker.Dispatcher(notificationSurface)

    // --- EXPIRE mode ---

    @Test
    fun dispatch_expire_callsUpdateWithoutUndoWithCorrectArgs() {
        dispatcher.dispatch(
            notificationId = 42,
            rawMode = QuickAddNotifierWorker.Mode.EXPIRE.name,
            amountFormatted = "₫15,000",
            categoryName = "Groceries",
        )

        assertEquals(1, notificationSurface.updateWithoutUndoCalls.size)
        val call = notificationSurface.updateWithoutUndoCalls.single()
        assertEquals(42, call.notificationId)
        assertEquals("₫15,000", call.amountFormatted)
        assertEquals("Groceries", call.categoryName)
        // EXPIRE must NOT cancel the notification — that's DISMISS's job.
        assertTrue(notificationSurface.cancelCalls.isEmpty())
    }

    @Test
    fun dispatch_expire_blankAmount_noOps() {
        dispatcher.dispatch(
            notificationId = 42,
            rawMode = QuickAddNotifierWorker.Mode.EXPIRE.name,
            amountFormatted = "  ",
            categoryName = "Groceries",
        )
        assertTrue(notificationSurface.updateWithoutUndoCalls.isEmpty())
        assertTrue(notificationSurface.cancelCalls.isEmpty())
    }

    @Test
    fun dispatch_expire_nullAmount_noOps() {
        dispatcher.dispatch(
            notificationId = 42,
            rawMode = QuickAddNotifierWorker.Mode.EXPIRE.name,
            amountFormatted = null,
            categoryName = "Groceries",
        )
        assertTrue(notificationSurface.updateWithoutUndoCalls.isEmpty())
    }

    @Test
    fun dispatch_expire_blankCategory_noOps() {
        dispatcher.dispatch(
            notificationId = 42,
            rawMode = QuickAddNotifierWorker.Mode.EXPIRE.name,
            amountFormatted = "₫15,000",
            categoryName = "",
        )
        assertTrue(notificationSurface.updateWithoutUndoCalls.isEmpty())
    }

    // --- DISMISS mode ---

    @Test
    fun dispatch_dismiss_callsCancelWithNotificationId() {
        dispatcher.dispatch(
            notificationId = 42,
            rawMode = QuickAddNotifierWorker.Mode.DISMISS.name,
            amountFormatted = null,
            categoryName = null,
        )
        assertEquals(listOf(42), notificationSurface.cancelCalls)
        // DISMISS must NOT post an update — that's EXPIRE's job.
        assertTrue(notificationSurface.updateWithoutUndoCalls.isEmpty())
    }

    // --- Input validation ---

    @Test
    fun dispatch_invalidNotificationId_noOps() {
        dispatcher.dispatch(
            notificationId = QuickAddNotifierWorker.INVALID_ID,
            rawMode = QuickAddNotifierWorker.Mode.DISMISS.name,
            amountFormatted = null,
            categoryName = null,
        )
        assertTrue(notificationSurface.updateWithoutUndoCalls.isEmpty())
        assertTrue(notificationSurface.cancelCalls.isEmpty())
    }

    @Test
    fun dispatch_unknownMode_noOps() {
        dispatcher.dispatch(
            notificationId = 42,
            rawMode = "UNKNOWN_FUTURE_MODE",
            amountFormatted = "₫15,000",
            categoryName = "Groceries",
        )
        assertTrue(notificationSurface.updateWithoutUndoCalls.isEmpty())
        assertTrue(notificationSurface.cancelCalls.isEmpty())
    }

    @Test
    fun dispatch_nullMode_noOps() {
        dispatcher.dispatch(
            notificationId = 42,
            rawMode = null,
            amountFormatted = "₫15,000",
            categoryName = "Groceries",
        )
        assertTrue(notificationSurface.updateWithoutUndoCalls.isEmpty())
        assertTrue(notificationSurface.cancelCalls.isEmpty())
    }

    // --- Mode.parse ---

    @Test
    fun modeParse_validExpire_returnsMode() {
        assertEquals(QuickAddNotifierWorker.Mode.EXPIRE, QuickAddNotifierWorker.Mode.parse("EXPIRE"))
    }

    @Test
    fun modeParse_validDismiss_returnsMode() {
        assertEquals(QuickAddNotifierWorker.Mode.DISMISS, QuickAddNotifierWorker.Mode.parse("DISMISS"))
    }

    @Test
    fun modeParse_unknown_returnsNull() {
        assertNull(QuickAddNotifierWorker.Mode.parse("expire_lowercase"))
        assertNull(QuickAddNotifierWorker.Mode.parse("future_mode"))
        assertNull(QuickAddNotifierWorker.Mode.parse(""))
    }

    @Test
    fun modeParse_null_returnsNull() {
        assertNull(QuickAddNotifierWorker.Mode.parse(null))
    }
}
