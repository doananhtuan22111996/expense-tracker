package dev.tuandoan.expensetracker.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.tuandoan.expensetracker.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private val Context.onboardingDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "onboarding_preferences",
)

@Singleton
class OnboardingRepositoryImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : OnboardingRepository {
        private val onboardingCompleteKey = booleanPreferencesKey("onboarding_complete")

        override val isOnboardingComplete: Flow<Boolean> =
            context.onboardingDataStore.data.map { preferences ->
                preferences[onboardingCompleteKey] ?: false
            }

        override suspend fun markOnboardingComplete() {
            withContext(ioDispatcher) {
                context.onboardingDataStore.edit { preferences ->
                    preferences[onboardingCompleteKey] = true
                }
            }
        }
    }
