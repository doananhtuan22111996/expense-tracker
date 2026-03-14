package dev.tuandoan.expensetracker.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.tuandoan.expensetracker.data.preferences.OnboardingRepository
import dev.tuandoan.expensetracker.data.preferences.OnboardingRepositoryImpl

@Module
@InstallIn(SingletonComponent::class)
abstract class OnboardingModule {
    @Binds
    abstract fun bindOnboardingRepository(impl: OnboardingRepositoryImpl): OnboardingRepository
}
