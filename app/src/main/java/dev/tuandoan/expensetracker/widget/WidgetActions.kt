package dev.tuandoan.expensetracker.widget

import android.content.Context
import android.content.Intent
import androidx.glance.action.Action
import androidx.glance.appwidget.action.actionStartActivity
import dev.tuandoan.expensetracker.MainActivity
import dev.tuandoan.expensetracker.ui.screen.quickadd.QuickAddSheetActivity

/**
 * Action factories for [ExpenseWidget] click targets.
 *
 * Four destinations:
 * - Background of widget → [openAppAction] (MainActivity, no extras).
 * - "+" button → [openAddTransactionAction] (MainActivity with
 *   [MainActivity.EXTRA_LAUNCH_ADD_TRANSACTION] = true; navigates to the
 *   full add-transaction modal).
 * - Pinned category tile → [openQuickAddAction] (QuickAddSheetActivity with
 *   [QuickAddSheetActivity.EXTRA_CATEGORY_ID] = categoryId). v3.12.0's
 *   one-tap expense-logging entry point.
 * - Empty-pin placeholder → [openSettingsAction] (MainActivity with
 *   [MainActivity.EXTRA_LAUNCH_SETTINGS] = true). Once T6.4 lands the
 *   Widget Categories settings row this routes the user to the right spot
 *   to configure pins; until then it just lands on Settings.
 *
 * Using plain Intent + extras rather than deep-link URIs because:
 * - Small, fixed set of destinations.
 * - No intent-filter churn in AndroidManifest.
 * - Simpler Compose integration — single flags read at the top of the
 *   navigation hierarchy vs. deep-link argument routing.
 *
 * `Intent.FLAG_ACTIVITY_NEW_TASK` is required because widget actions fire
 * from outside the app's activity stack. `FLAG_ACTIVITY_CLEAR_TOP` ensures
 * tapping the widget while the app is already running delivers the intent
 * to the existing MainActivity (paired with `launchMode="singleTop"`).
 * `QuickAddSheetActivity` is its own task target — we don't `CLEAR_TOP`
 * there because it's transient by design (it self-finishes after save).
 */
internal fun openAppAction(context: Context): Action {
    val intent = buildMainActivityIntent(context)
    return actionStartActivity(intent)
}

internal fun openAddTransactionAction(context: Context): Action {
    val intent =
        buildMainActivityIntent(context).apply {
            putExtra(MainActivity.EXTRA_LAUNCH_ADD_TRANSACTION, true)
        }
    return actionStartActivity(intent)
}

/**
 * Launches the quick-add bottom sheet for the given [categoryId]. The
 * Activity is `exported=false` and reachable only via this in-process
 * PendingIntent. `NEW_TASK` is required because widget actions fire
 * from outside the activity stack; `singleTop` on the Activity handles
 * repeated taps by routing to `onNewIntent`.
 */
internal fun openQuickAddAction(
    context: Context,
    categoryId: Long,
): Action {
    val intent =
        Intent(context, QuickAddSheetActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra(QuickAddSheetActivity.EXTRA_CATEGORY_ID, categoryId)
        }
    return actionStartActivity(intent)
}

internal fun openSettingsAction(context: Context): Action {
    val intent =
        buildMainActivityIntent(context).apply {
            putExtra(MainActivity.EXTRA_LAUNCH_SETTINGS, true)
        }
    return actionStartActivity(intent)
}

private fun buildMainActivityIntent(context: Context): Intent =
    Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
