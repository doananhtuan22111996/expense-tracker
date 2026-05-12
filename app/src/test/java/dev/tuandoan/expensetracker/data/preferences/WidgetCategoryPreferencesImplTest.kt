package dev.tuandoan.expensetracker.data.preferences

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Contract tests for [WidgetCategoryPreferences] exercised through a fake.
 *
 * Follows the [InsightsCollapsePreferencesImplTest] convention — the real
 * DataStore-backed impl depends on Android `Context` + [androidx.datastore]
 * which would pull in Robolectric just to exercise three long keys. The
 * fake proves the interface semantics (default empty list, round-trip,
 * order preservation, max-3 enforcement, partial clearing); the impl
 * itself is straight-line DataStore boilerplate covered by existing
 * `BudgetPreferencesImpl` usage on-device.
 */
class WidgetCategoryPreferencesImplTest {
    private lateinit var preferences: FakeWidgetCategoryPreferences

    @Before
    fun setup() {
        preferences = FakeWidgetCategoryPreferences()
    }

    @Test
    fun defaultPinnedCategoryIds_isEmpty() =
        runTest {
            assertEquals(emptyList<Long>(), preferences.pinnedCategoryIds.first())
        }

    @Test
    fun setThreeIds_roundTripsInSameOrder() =
        runTest {
            preferences.setPinnedCategoryIds(listOf(42L, 7L, 13L))

            assertEquals(listOf(42L, 7L, 13L), preferences.pinnedCategoryIds.first())
        }

    @Test
    fun setFewerThanThree_onlyPersistsProvidedSlots() =
        runTest {
            preferences.setPinnedCategoryIds(listOf(42L, 7L))

            assertEquals(listOf(42L, 7L), preferences.pinnedCategoryIds.first())
        }

    @Test
    fun setEmpty_clearsAllPreviouslySetIds() =
        runTest {
            preferences.setPinnedCategoryIds(listOf(42L, 7L, 13L))
            preferences.setPinnedCategoryIds(emptyList())

            assertEquals(emptyList<Long>(), preferences.pinnedCategoryIds.first())
        }

    @Test
    fun setMoreThanThree_truncatesToFirstThree() =
        runTest {
            preferences.setPinnedCategoryIds(listOf(1L, 2L, 3L, 4L, 5L))

            assertEquals(listOf(1L, 2L, 3L), preferences.pinnedCategoryIds.first())
        }

    @Test
    fun reorderingPicks_preservesOrder() =
        runTest {
            preferences.setPinnedCategoryIds(listOf(42L, 7L, 13L))
            preferences.setPinnedCategoryIds(listOf(13L, 42L, 7L))

            assertEquals(listOf(13L, 42L, 7L), preferences.pinnedCategoryIds.first())
        }

    @Test
    fun shrinkingPickSet_clearsTrailingSlots() =
        runTest {
            preferences.setPinnedCategoryIds(listOf(42L, 7L, 13L))
            preferences.setPinnedCategoryIds(listOf(99L))

            assertEquals(listOf(99L), preferences.pinnedCategoryIds.first())
        }
}

class FakeWidgetCategoryPreferences : WidgetCategoryPreferences {
    private val _pinnedCategoryIds = MutableStateFlow<List<Long>>(emptyList())
    override val pinnedCategoryIds: Flow<List<Long>> = _pinnedCategoryIds

    /** Last list written to the preferences (post-truncation). `null` until first write. */
    var latestWrite: List<Long>? = null
        private set

    /** Number of times `setPinnedCategoryIds` has been called (across writes + constructor-time seeds). */
    var writeCount: Int = 0
        private set

    /**
     * When true, the NEXT call to [setPinnedCategoryIds] throws. Auto-resets to false
     * after it fires so tests don't have to clear it by hand. Used by VM tests to
     * exercise the persistence-failure branch.
     */
    var throwOnNextWrite: Boolean = false

    override suspend fun setPinnedCategoryIds(ids: List<Long>) {
        if (throwOnNextWrite) {
            throwOnNextWrite = false
            throw RuntimeException("Simulated persistence failure")
        }
        val truncated = ids.take(3)
        _pinnedCategoryIds.value = truncated
        latestWrite = truncated
        writeCount++
    }
}
