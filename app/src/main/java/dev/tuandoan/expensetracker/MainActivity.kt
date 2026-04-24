package dev.tuandoan.expensetracker

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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

    // Widget-action signal. We use a simple monotonically-increasing token
    // so that the Compose layer can react to *repeated* taps (e.g. user taps
    // the widget's "+" again after already cancelling the add screen). The
    // value is the tap's nanoTime at onCreate/onNewIntent — unique per tap
    // without needing a Channel or SharedFlow on the Activity boundary.
    private var pendingAddTransactionTick by mutableStateOf(0L)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        consumeWidgetExtras(intent)
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
                ExpenseTrackerApp(
                    isOnboardingComplete = isOnboardingComplete,
                    pendingAddTransactionTick = pendingAddTransactionTick,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Android delivers widget-tap intents here when the app is already
        // running (singleTop + FLAG_ACTIVITY_CLEAR_TOP). setIntent() updates
        // getIntent() so any later reader of the activity's current intent
        // sees the consumed (extra-cleared) version — must be called before
        // consumeWidgetExtras mutates the Intent.
        setIntent(intent)
        consumeWidgetExtras(intent)
    }

    /**
     * Reads widget-origin extras off [intent], converts them into the
     * [pendingAddTransactionTick] signal observed by [ExpenseTrackerApp],
     * and clears the extra so a subsequent configuration change
     * (e.g. rotation) doesn't re-trigger navigation from the same intent.
     *
     * A tap that arrives during onboarding is *queued*, not dropped: the
     * tick is set now but the `LaunchedEffect` in `ExpenseTrackerApp`
     * gates on `isOnboardingComplete`, so navigation fires as soon as the
     * user finishes the welcome flow. Acceptable v1 behavior — the tap
     * was intentional and should be honored when the app becomes usable.
     */
    private fun consumeWidgetExtras(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_LAUNCH_ADD_TRANSACTION, false) == true) {
            pendingAddTransactionTick = System.nanoTime()
            intent.removeExtra(EXTRA_LAUNCH_ADD_TRANSACTION)
        }
    }

    companion object {
        /**
         * Intent extra set by the home-screen widget's "+" action. When true,
         * MainActivity signals the Compose layer to navigate to the
         * add-transaction modal on top of whatever was showing.
         */
        const val EXTRA_LAUNCH_ADD_TRANSACTION: String = "launch_add_transaction"
    }
}
