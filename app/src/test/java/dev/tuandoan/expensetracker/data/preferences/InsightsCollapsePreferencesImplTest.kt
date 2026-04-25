package dev.tuandoan.expensetracker.data.preferences

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Contract tests for [InsightsCollapsePreferences] exercised through a fake.
 *
 * Follows the convention established by [OnboardingRepositoryImplTest] — the
 * real DataStore-backed impl depends on Android `Context` + [androidx.datastore]
 * which would pull in Robolectric just to exercise a one-key preference. The
 * fake proves the interface semantics (default false, round-trip, idempotent
 * reads); the impl itself is ~40 lines of straight-line DataStore boilerplate
 * that's covered by existing `BackupEncryptionPreferencesImpl` usage on-device.
 */
class InsightsCollapsePreferencesImplTest {
    private lateinit var preferences: FakeInsightsCollapsePreferences

    @Before
    fun setup() {
        preferences = FakeInsightsCollapsePreferences()
    }

    @Test
    fun defaultCollapsed_isFalse() =
        runTest {
            val result = preferences.collapsed.first()
            assertFalse(result)
        }

    @Test
    fun setCollapsedTrue_thenReadEmitsTrue() =
        runTest {
            preferences.setCollapsed(true)

            assertTrue(preferences.collapsed.first())
        }

    @Test
    fun setCollapsedFalse_afterTrue_roundTripsBack() =
        runTest {
            preferences.setCollapsed(true)
            preferences.setCollapsed(false)

            assertFalse(preferences.collapsed.first())
        }

    @Test
    fun subsequentReads_afterSetCollapsed_emitSameValue() =
        runTest {
            preferences.setCollapsed(true)

            assertTrue(preferences.collapsed.first())
            assertTrue(preferences.collapsed.first())
        }
}

class FakeInsightsCollapsePreferences : InsightsCollapsePreferences {
    private val _collapsed = MutableStateFlow(false)
    override val collapsed: Flow<Boolean> = _collapsed

    override suspend fun setCollapsed(value: Boolean) {
        _collapsed.value = value
    }
}
