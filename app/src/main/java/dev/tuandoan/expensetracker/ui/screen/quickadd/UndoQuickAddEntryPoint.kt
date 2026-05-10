package dev.tuandoan.expensetracker.ui.screen.quickadd

import android.content.Context
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import dev.tuandoan.expensetracker.domain.analytics.Analytics
import dev.tuandoan.expensetracker.domain.crash.CrashReporter
import dev.tuandoan.expensetracker.domain.notification.QuickAddNotificationSurface
import dev.tuandoan.expensetracker.domain.notification.QuickAddUndoWorkScheduler
import dev.tuandoan.expensetracker.domain.repository.TransactionRepository
import dev.tuandoan.expensetracker.domain.widget.WidgetUpdater

/**
 * Hilt `@EntryPoint` for [UndoQuickAddReceiver] (v3.12.0 T4.3).
 *
 * BroadcastReceivers can't be `@AndroidEntryPoint`-annotated the way
 * Activities/Fragments can, and the receiver's lifecycle is too short
 * for a scoped component to buy anything over Singleton-scope pull. This
 * entry point pulls the six dependencies the Undo flow needs directly
 * off the application graph.
 *
 * Process-death-safe: `fromApplication` resolves the
 * `Hilt_ApplicationComponent` whether or not the app process was cold-
 * started by the broadcast — which is exactly the case we have to
 * worry about (user taps Undo from the shade after the app was
 * memory-killed).
 *
 * Pattern mirrors [dev.tuandoan.expensetracker.widget.WidgetEntryPoint].
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface UndoQuickAddEntryPoint {
    fun transactionRepository(): TransactionRepository

    fun quickAddNotificationSurface(): QuickAddNotificationSurface

    fun quickAddUndoWorkScheduler(): QuickAddUndoWorkScheduler

    fun analytics(): Analytics

    fun widgetUpdater(): WidgetUpdater

    fun crashReporter(): CrashReporter

    companion object {
        fun get(context: Context): UndoQuickAddEntryPoint =
            EntryPointAccessors.fromApplication(context, UndoQuickAddEntryPoint::class.java)
    }
}
