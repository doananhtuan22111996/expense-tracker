package dev.tuandoan.expensetracker.data.notification

import dev.tuandoan.expensetracker.core.formatter.CurrencyFormatter
import dev.tuandoan.expensetracker.testutil.FakeQuickAddNotificationSurface
import dev.tuandoan.expensetracker.testutil.FakeQuickAddUndoWorkScheduler
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [QuickAddConfirmationNotifierImpl]. Verifies the post
 * orchestration:
 * 1. Amount is formatted via the injected `CurrencyFormatter`.
 * 2. Notification surface is called FIRST to post the confirmation.
 * 3. Work scheduler then schedules the expiry + dismiss workers, using
 *    the notification id returned by the surface.
 */
class QuickAddConfirmationNotifierImplTest {
    private val notificationSurface = FakeQuickAddNotificationSurface()
    private val workScheduler = FakeQuickAddUndoWorkScheduler()
    private val currencyFormatter =
        object : CurrencyFormatter {
            override fun format(
                amountMinor: Long,
                currencyCode: String,
            ): String = "$amountMinor $currencyCode"

            override fun formatWithSign(
                amountMinor: Long,
                currencyCode: String,
                isIncome: Boolean,
            ): String = (if (isIncome) "+" else "-") + format(amountMinor, currencyCode)

            override fun formatBareAmount(
                amountMinor: Long,
                currencyCode: String,
            ): String = amountMinor.toString()
        }

    private val notifier =
        QuickAddConfirmationNotifierImpl(
            notificationSurface = notificationSurface,
            workScheduler = workScheduler,
            currencyFormatter = currencyFormatter,
        )

    @Test
    fun post_formatsAmountViaCurrencyFormatter() =
        runTest {
            notifier.post(
                transactionId = 100L,
                amountMinor = 15_000L,
                currencyCode = "VND",
                categoryName = "Groceries",
            )
            // Fake formatter renders as "{amount} {code}"; both downstream
            // calls must receive the same formatted string so the T+10s
            // re-post matches the original post visually.
            assertEquals("15000 VND", notificationSurface.showCalls.single().amountFormatted)
            assertEquals("15000 VND", workScheduler.scheduleExpiryAndDismissCalls.single().amountFormatted)
        }

    @Test
    fun post_passesThroughTransactionIdAndCategory() =
        runTest {
            notifier.post(
                transactionId = 42L,
                amountMinor = 15_000L,
                currencyCode = "VND",
                categoryName = "Groceries",
            )
            val showCall = notificationSurface.showCalls.single()
            assertEquals(42L, showCall.transactionId)
            assertEquals("Groceries", showCall.categoryName)

            val scheduleCall = workScheduler.scheduleExpiryAndDismissCalls.single()
            assertEquals(42L, scheduleCall.transactionId)
            assertEquals("Groceries", scheduleCall.categoryName)
        }

    @Test
    fun post_schedulerReceivesNotificationIdFromSurface() =
        runTest {
            // The scheduler needs the SAME notification id that the surface
            // returned, so that the T+10s re-post and T+30s cancel target
            // the same shade entry the user is looking at. If the two ever
            // drift, the Undo button wouldn't disappear.
            notificationSurface.nextNotificationId = 999
            notifier.post(
                transactionId = 42L,
                amountMinor = 15_000L,
                currencyCode = "VND",
                categoryName = "Groceries",
            )
            assertEquals(999, workScheduler.scheduleExpiryAndDismissCalls.single().notificationId)
        }

    @Test
    fun post_doesNotScheduleUndoneDismiss() =
        runTest {
            // scheduleUndoneDismiss is T4.3's territory (called from the undo
            // receiver after Undo tap). Verify the post path doesn't
            // accidentally trigger it.
            notifier.post(
                transactionId = 42L,
                amountMinor = 15_000L,
                currencyCode = "VND",
                categoryName = "Groceries",
            )
            assertTrue(workScheduler.scheduleUndoneDismissCalls.isEmpty())
            assertTrue(workScheduler.cancelExpiryAndDismissCalls.isEmpty())
        }
}
