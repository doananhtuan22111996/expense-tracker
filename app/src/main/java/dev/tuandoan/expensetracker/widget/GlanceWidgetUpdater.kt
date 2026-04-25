package dev.tuandoan.expensetracker.widget

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.updateAll
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.tuandoan.expensetracker.di.IoDispatcher
import dev.tuandoan.expensetracker.domain.widget.WidgetUpdater
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Glance-backed [WidgetUpdater]. Triggers `updateAll<ExpenseWidget>(context)`
 * off the main thread, wrapped in [NonCancellable] so the refresh completes
 * even when the caller (typically a repository write inside a cancellable
 * scope like `viewModelScope`) is cancelled before the update finishes.
 *
 * Failures are logged and swallowed — the widget is a best-effort glance
 * into the app's state. If a refresh fails (e.g., the widget isn't currently
 * placed on any launcher), the next user interaction triggers another
 * attempt via the same refresh hook.
 */
@Singleton
class GlanceWidgetUpdater
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : WidgetUpdater {
        override suspend fun requestUpdate() {
            withContext(NonCancellable + ioDispatcher) {
                @Suppress("TooGenericExceptionCaught")
                try {
                    ExpenseWidget().updateAll(context)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to update widget", e)
                }
            }
        }

        companion object {
            private const val TAG = "GlanceWidgetUpdater"
        }
    }
