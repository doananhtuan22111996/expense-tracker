package dev.tuandoan.expensetracker.domain.review

import dev.tuandoan.expensetracker.data.preferences.FakeReviewPreferences
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class InAppReviewManagerTest {
    private lateinit var fakePreferences: FakeReviewPreferences
    private lateinit var manager: InAppReviewManagerImpl

    @Before
    fun setup() {
        fakePreferences = FakeReviewPreferences()
        manager = InAppReviewManagerImpl(fakePreferences)
    }

    @Test
    fun isEligibleForReview_neverShown_returnsTrue() =
        runTest {
            // Default: shown count = 0, last prompt = 0
            assertTrue(manager.isEligibleForReview())
        }

    @Test
    fun isEligibleForReview_shownCountAtMax_returnsFalse() =
        runTest {
            fakePreferences.setShownCount(2)

            assertFalse(manager.isEligibleForReview())
        }

    @Test
    fun isEligibleForReview_shownCountAboveMax_returnsFalse() =
        runTest {
            fakePreferences.setShownCount(5)

            assertFalse(manager.isEligibleForReview())
        }

    @Test
    fun isEligibleForReview_withinCooldown_returnsFalse() =
        runTest {
            fakePreferences.setShownCount(1)
            // Last shown 10 days ago (within 30-day cooldown)
            val tenDaysAgo = System.currentTimeMillis() - (10L * 86_400_000L)
            fakePreferences.setLastPromptMillis(tenDaysAgo)

            assertFalse(manager.isEligibleForReview())
        }

    @Test
    fun isEligibleForReview_pastCooldown_returnsTrue() =
        runTest {
            fakePreferences.setShownCount(1)
            // Last shown 31 days ago (past 30-day cooldown)
            val thirtyOneDaysAgo = System.currentTimeMillis() - (31L * 86_400_000L)
            fakePreferences.setLastPromptMillis(thirtyOneDaysAgo)

            assertTrue(manager.isEligibleForReview())
        }

    @Test
    fun markReviewShown_incrementsCount() =
        runTest {
            manager.markReviewShown()

            // After marking, count should be 1
            // And manager should still be eligible (shown < 2, first time sets timestamp)
            fakePreferences.setShownCount(1)
            val thirtyOneDaysAgo = System.currentTimeMillis() - (31L * 86_400_000L)
            fakePreferences.setLastPromptMillis(thirtyOneDaysAgo)
            assertTrue(manager.isEligibleForReview())

            // Show again
            manager.markReviewShown()
            // Now count is 2, should not be eligible
            assertFalse(manager.isEligibleForReview())
        }
}
