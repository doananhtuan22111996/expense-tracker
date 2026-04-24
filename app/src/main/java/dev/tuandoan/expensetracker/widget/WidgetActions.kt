package dev.tuandoan.expensetracker.widget

import android.content.Context
import android.content.Intent
import androidx.glance.action.Action
import androidx.glance.appwidget.action.actionStartActivity
import dev.tuandoan.expensetracker.MainActivity

/**
 * Action factories for [ExpenseWidget] click targets.
 *
 * Two destinations per the widget PRD:
 * - "+" button → [openAddTransactionAction] launches MainActivity with
 *   [MainActivity.EXTRA_LAUNCH_ADD_TRANSACTION] = true. MainActivity reads
 *   the extra on `onCreate` / `onNewIntent` and navigates the outer
 *   NavController to the add-transaction modal route.
 * - Elsewhere on the widget → [openAppAction] launches MainActivity with no
 *   extras, landing on whatever the app's startDestination is (Home).
 *
 * Using plain Intent + extras rather than deep-link URIs because:
 * - Only two destinations to support.
 * - No intent-filter churn in AndroidManifest.
 * - Simpler Compose integration — a single Boolean flag read at the top
 *   of the navigation hierarchy vs. deep-link argument routing.
 *
 * `Intent.FLAG_ACTIVITY_NEW_TASK` is required because widget actions fire
 * from outside the app's activity stack. `FLAG_ACTIVITY_CLEAR_TOP` ensures
 * tapping the widget while the app is already running delivers the intent
 * to the existing MainActivity (paired with `launchMode="singleTop"`).
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

private fun buildMainActivityIntent(context: Context): Intent =
    Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
