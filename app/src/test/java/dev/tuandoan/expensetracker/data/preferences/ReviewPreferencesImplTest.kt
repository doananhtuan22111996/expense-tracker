package dev.tuandoan.expensetracker.data.preferences

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ReviewPreferencesImplTest {
    private lateinit var preferences: FakeReviewPreferences

    @Before
    fun setup() {
        preferences = FakeReviewPreferences()
    }

    @Test
    fun defaultReviewShownCount_isZero() =
        runTest {
            assertEquals(0, preferences.reviewShownCount.first())
        }

    @Test
    fun defaultLastReviewPromptMillis_isZero() =
        runTest {
            assertEquals(0L, preferences.lastReviewPromptMillis.first())
        }

    @Test
    fun markReviewShown_incrementsCountAndUpdatesTimestamp() =
        runTest {
            preferences.markReviewShown()

            val count = preferences.reviewShownCount.first()
            val timestamp = preferences.lastReviewPromptMillis.first()

            assertEquals(1, count)
            assert(timestamp > 0L) { "Timestamp should be set to current time" }
        }

    @Test
    fun markReviewShown_calledTwice_incrementsCountToTwo() =
        runTest {
            preferences.markReviewShown()
            preferences.markReviewShown()

            assertEquals(2, preferences.reviewShownCount.first())
        }
}

/**
 * In-memory fake of [ReviewPreferences] for unit testing.
 */
class FakeReviewPreferences : ReviewPreferences {
    private val promptMillisState = MutableStateFlow(0L)
    private val shownCountState = MutableStateFlow(0)

    override val lastReviewPromptMillis: Flow<Long> = promptMillisState
    override val reviewShownCount: Flow<Int> = shownCountState

    override suspend fun markReviewShown() {
        shownCountState.value += 1
        promptMillisState.value = System.currentTimeMillis()
    }

    /** Test helper: set the last prompt timestamp directly. */
    fun setLastPromptMillis(millis: Long) {
        promptMillisState.value = millis
    }

    /** Test helper: set the shown count directly. */
    fun setShownCount(count: Int) {
        shownCountState.value = count
    }
}
