package dev.tuandoan.expensetracker.testutil

import dev.tuandoan.expensetracker.data.preferences.OnboardingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Test double for [OnboardingRepository].
 *
 * Single source of truth for this fixture across test modules — mirrors the
 * pattern established for `FakeAnalyticsPreferences`. Any future interface
 * change picks up through the compiler, not through manual updates scattered
 * across test files.
 *
 * Unlike the real [OnboardingRepository] which only allows setting
 * `isOnboardingComplete` to true (one-way transition on real devices), this
 * fake exposes [setOnboardingComplete] for tests that need to pin the state
 * either direction (e.g. "consent prompt should NOT show when onboarding is
 * incomplete").
 */
class FakeOnboardingRepository : OnboardingRepository {
    private val _isOnboardingComplete = MutableStateFlow(false)
    override val isOnboardingComplete: Flow<Boolean> = _isOnboardingComplete

    override suspend fun markOnboardingComplete() {
        _isOnboardingComplete.value = true
    }

    /** Test-only: pin the state either direction without the one-way semantics. */
    fun setOnboardingComplete(value: Boolean) {
        _isOnboardingComplete.value = value
    }
}
