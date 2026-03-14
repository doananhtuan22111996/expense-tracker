package dev.tuandoan.expensetracker.ui.screen.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.tuandoan.expensetracker.data.preferences.OnboardingRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel
    @Inject
    constructor(
        private val onboardingRepository: OnboardingRepository,
    ) : ViewModel() {
        fun completeOnboarding() {
            viewModelScope.launch {
                onboardingRepository.markOnboardingComplete()
            }
        }
    }
