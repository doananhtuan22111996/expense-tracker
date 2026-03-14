package dev.tuandoan.expensetracker.data.preferences

import kotlinx.coroutines.flow.Flow

interface OnboardingRepository {
    val isOnboardingComplete: Flow<Boolean>

    suspend fun markOnboardingComplete()
}
