package dev.tuandoan.expensetracker.ui.screen.quickadd

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import dev.tuandoan.expensetracker.MainActivity
import dev.tuandoan.expensetracker.data.preferences.OnboardingRepository
import dev.tuandoan.expensetracker.data.preferences.ThemePreference
import dev.tuandoan.expensetracker.data.preferences.ThemePreferencesRepository
import dev.tuandoan.expensetracker.ui.theme.ExpenseTrackerTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Transparent-theme Activity that hosts the quick-add bottom sheet launched
 * from a pinned widget tile. Scaffold only in this PR (T3.1); the real
 * amount-entry UI + save path arrive in T3.2/T3.4.
 *
 * ### Flow
 *
 * 1. Widget tile tap → `PendingIntent` with `EXTRA_CATEGORY_ID` → this Activity.
 * 2. `onCreate` reads `categoryId` off the intent; if missing or invalid,
 *    finishes immediately (defensive — shouldn't happen via the widget path
 *    but cheap insurance if a future contributor forgets to wire the extra).
 * 3. **Onboarding gate** (PRD FR-19): if onboarding is incomplete, route to
 *    `MainActivity` (which renders the welcome flow) and finish. Prevents
 *    writing a transaction before the user has even set a default currency.
 * 4. Otherwise, render the quick-add sheet over the launcher wallpaper.
 *
 * ### Security
 *
 * `exported="false"` in the manifest — this Activity is only reachable from
 * the widget's in-process `PendingIntent`, never discoverable to other apps.
 * The `categoryId` intent extra is treated as untrusted input (Long parse +
 * `0L` sentinel rejection) per the "treat all Intent extras as untrusted"
 * convention.
 */
@AndroidEntryPoint
class QuickAddSheetActivity : ComponentActivity() {
    @Inject
    lateinit var themePreferencesRepository: ThemePreferencesRepository

    @Inject
    lateinit var onboardingRepository: OnboardingRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val categoryId = readCategoryIdExtra(intent)
        if (categoryId == null) {
            finish()
            return
        }

        // FR-19: Onboarding gate is evaluated once here — a widget tap during
        // onboarding routes the user to the welcome flow rather than silently
        // writing a transaction in the wrong currency. Reading the flow
        // inside `lifecycleScope` keeps this off the main-thread critical
        // path; we finish the Activity before any Compose content renders.
        lifecycleScope.launch {
            val onboardingComplete = onboardingRepository.isOnboardingComplete.first()
            if (!onboardingComplete) {
                startActivity(
                    Intent(this@QuickAddSheetActivity, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    },
                )
                finish()
                return@launch
            }
            renderSheet(categoryId = categoryId)
        }
    }

    private fun renderSheet(categoryId: Long) {
        setContent {
            val themePreference by themePreferencesRepository.themePreference
                .collectAsStateWithLifecycle(initialValue = ThemePreference.SYSTEM)
            val darkTheme =
                when (themePreference) {
                    ThemePreference.LIGHT -> false
                    ThemePreference.DARK -> true
                    ThemePreference.SYSTEM -> isSystemInDarkTheme()
                }
            ExpenseTrackerTheme(darkTheme = darkTheme) {
                QuickAddSheetContent(
                    categoryId = categoryId,
                    onDismiss = { finish() },
                )
            }
        }
    }

    companion object {
        /**
         * Intent extra carrying the pinned category ID. Read as a `Long`; any
         * missing extra or default-value sentinel (`0L`) is treated as an
         * invalid request and the Activity finishes without rendering.
         */
        const val EXTRA_CATEGORY_ID: String = "category_id"
    }
}

/**
 * Pure-Kotlin helper extracted from [QuickAddSheetActivity.onCreate] so the
 * "parse + validate intent extra" contract can be unit-tested without
 * Robolectric. Returns `null` for a null intent, a missing extra, or the
 * `0L` default sentinel (a bogus caller that forgot to set the extra would
 * otherwise let a non-existent category ID through).
 */
internal fun readCategoryIdExtra(intent: Intent?): Long? {
    if (intent == null) return null
    if (!intent.hasExtra(QuickAddSheetActivity.EXTRA_CATEGORY_ID)) return null
    val id = intent.getLongExtra(QuickAddSheetActivity.EXTRA_CATEGORY_ID, 0L)
    return if (id > 0L) id else null
}
