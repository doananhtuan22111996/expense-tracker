package dev.tuandoan.expensetracker.ui.screen.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.tuandoan.expensetracker.data.preferences.OnboardingRepository
import dev.tuandoan.expensetracker.domain.analytics.Analytics
import dev.tuandoan.expensetracker.domain.analytics.AnalyticsEvent
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel
    @Inject
    constructor(
        private val onboardingRepository: OnboardingRepository,
        private val analytics: Analytics,
    ) : ViewModel() {
        fun completeOnboarding() {
            viewModelScope.launch {
                onboardingRepository.markOnboardingComplete()
                // PRD FR-A6: log onboarding_completed after persistence
                // succeeds so we don't record a completion for a flow the
                // user didn't actually finish. No parameters.
                analytics.logEvent(AnalyticsEvent.OnboardingCompleted)
            }
        }
    }
