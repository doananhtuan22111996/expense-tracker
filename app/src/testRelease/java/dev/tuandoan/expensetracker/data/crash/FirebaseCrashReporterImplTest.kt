package dev.tuandoan.expensetracker.data.crash

import org.junit.Test
import org.mockito.Mockito.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

/**
 * Unit tests for [FirebaseCrashReporterImpl] (PRD FR-15).
 *
 * Verifies that the impl is a pure pass-through to [FirebaseCrashlyticsWrapper] —
 * no branching on consent inside the impl (that lives in
 * [dev.tuandoan.expensetracker.ExpenseTrackerApplication]), no silent swallowing,
 * no custom key manipulation.
 *
 * Runs under the `release` source set so Hilt / Gradle variant wiring is
 * exercised the same way a real release build would exercise it.
 */
class FirebaseCrashReporterImplTest {
    @Test
    fun recordException_forwardsToWrapperUnchanged() {
        val wrapper: FirebaseCrashlyticsWrapper = mock()
        val reporter = FirebaseCrashReporterImpl(wrapper)
        val exception = IllegalStateException("synthetic test")

        reporter.recordException(exception)

        verify(wrapper).recordException(exception)
    }

    @Test
    fun setCollectionEnabled_true_forwardsToWrapper() {
        val wrapper: FirebaseCrashlyticsWrapper = mock()
        val reporter = FirebaseCrashReporterImpl(wrapper)

        reporter.setCollectionEnabled(true)

        verify(wrapper).setCrashlyticsCollectionEnabled(true)
    }

    @Test
    fun setCollectionEnabled_false_forwardsToWrapper() {
        // PRD FR-15: flipping consent false MUST invoke setCollectionEnabled(false)
        // on the underlying SDK. The Application layer observes analyticsConsent
        // and drives this call; here we pin the pass-through contract.
        val wrapper: FirebaseCrashlyticsWrapper = mock()
        val reporter = FirebaseCrashReporterImpl(wrapper)

        reporter.setCollectionEnabled(false)

        verify(wrapper).setCrashlyticsCollectionEnabled(false)
    }

    @Test
    fun setCollectionEnabled_ordering_trueThenFalse_forwardsBothInOrder() {
        // Regression guard: when the user toggles the Settings switch rapidly,
        // the observer fires both states; impl must preserve ordering.
        val wrapper: FirebaseCrashlyticsWrapper = mock()
        val reporter = FirebaseCrashReporterImpl(wrapper)

        reporter.setCollectionEnabled(true)
        reporter.setCollectionEnabled(false)

        val inOrder = inOrder(wrapper)
        inOrder.verify(wrapper).setCrashlyticsCollectionEnabled(true)
        inOrder.verify(wrapper).setCrashlyticsCollectionEnabled(false)
    }
}
