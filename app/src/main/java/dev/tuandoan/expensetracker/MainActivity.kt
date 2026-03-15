package dev.tuandoan.expensetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import dev.tuandoan.expensetracker.data.preferences.OnboardingRepository
import dev.tuandoan.expensetracker.data.preferences.ThemePreference
import dev.tuandoan.expensetracker.data.preferences.ThemePreferencesRepository
import dev.tuandoan.expensetracker.ui.ExpenseTrackerApp
import dev.tuandoan.expensetracker.ui.theme.ExpenseTrackerTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var themePreferencesRepository: ThemePreferencesRepository

    @Inject
    lateinit var onboardingRepository: OnboardingRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themePreference by themePreferencesRepository.themePreference
                .collectAsStateWithLifecycle(initialValue = ThemePreference.SYSTEM)

            val isOnboardingComplete by onboardingRepository.isOnboardingComplete
                .collectAsStateWithLifecycle(initialValue = true)

            val darkTheme =
                when (themePreference) {
                    ThemePreference.LIGHT -> false
                    ThemePreference.DARK -> true
                    ThemePreference.SYSTEM -> isSystemInDarkTheme()
                }

            ExpenseTrackerTheme(darkTheme = darkTheme) {
                ExpenseTrackerApp(isOnboardingComplete = isOnboardingComplete)
            }
        }
    }
}
