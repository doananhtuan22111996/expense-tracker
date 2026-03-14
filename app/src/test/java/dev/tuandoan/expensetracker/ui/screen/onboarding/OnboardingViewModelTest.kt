package dev.tuandoan.expensetracker.ui.screen.onboarding

import dev.tuandoan.expensetracker.data.preferences.FakeOnboardingRepository
import dev.tuandoan.expensetracker.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeOnboardingRepo: FakeOnboardingRepository

    @Before
    fun setup() {
        fakeOnboardingRepo = FakeOnboardingRepository()
    }

    @Test
    fun completeOnboarding_callsMarkOnboardingComplete() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = OnboardingViewModel(fakeOnboardingRepo)

            viewModel.completeOnboarding()
            advanceUntilIdle()

            assertTrue(fakeOnboardingRepo.isOnboardingComplete.first())
        }
}
