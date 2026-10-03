package dev.tuandoan.expensetracker.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.tuandoan.expensetracker.data.preferences.SecurityPreferences
import dev.tuandoan.expensetracker.data.preferences.SecurityPreferencesImpl
import dev.tuandoan.expensetracker.domain.security.BiometricAuthHelper
import dev.tuandoan.expensetracker.domain.security.BiometricAuthHelperImpl
import dev.tuandoan.expensetracker.domain.security.ElapsedRealtimeClock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SecurityModule {
    @Binds
    abstract fun bindSecurityPreferences(impl: SecurityPreferencesImpl): SecurityPreferences

    @Binds
    abstract fun bindBiometricAuthHelper(impl: BiometricAuthHelperImpl): BiometricAuthHelper

    companion object {
        @Provides
        @Singleton
        fun provideElapsedRealtimeClock(): ElapsedRealtimeClock =
            ElapsedRealtimeClock { android.os.SystemClock.elapsedRealtime() }
    }
}
