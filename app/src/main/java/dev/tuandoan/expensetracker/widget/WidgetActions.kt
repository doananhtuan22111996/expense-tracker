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
 *
 * ### Testability seam
 * The four routing decisions (target class, extras, flags) are captured as
 * pure-Kotlin [WidgetActionSpec] values — no `Context` or `Intent` touched.
 * Contract tests assert the specs directly; the `toIntent` translator is a
 * trivial adapter that just unpacks the spec onto Android's Intent API.
 * This keeps the interesting routing logic inside a JVM-testable surface
 * without pulling in Robolectric.
 *
 * [WidgetActionSpec] is a pure-Kotlin description of a widget click target.
 * Captures everything a reader needs to understand "what intent does this
 * tap produce" without requiring an Android runtime — target Activity
 * class, extras, and flags. `booleanExtras` is typed `Map<String, Boolean>`
 * rather than `Map<String, Any>` because all of MainActivity's widget-origin
 * flags are Booleans; [WidgetActionSpec.OpenQuickAdd]'s Long `categoryId`
 * lives on the subclass as a typed parameter so the contract test can
 * assert its value without unboxing through `Any`.
 */
internal sealed interface WidgetActionSpec {
    val targetClass: Class<*>
    val flags: Int
    val booleanExtras: Map<String, Boolean>

    data object OpenApp : WidgetActionSpec {
        override val targetClass: Class<*> = MainActivity::class.java
        override val flags: Int = MAIN_ACTIVITY_FLAGS
        override val booleanExtras: Map<String, Boolean> = emptyMap()
    }

    data object OpenAddTransaction : WidgetActionSpec {
        override val targetClass: Class<*> = MainActivity::class.java
        override val flags: Int = MAIN_ACTIVITY_FLAGS
        override val booleanExtras: Map<String, Boolean> =
            mapOf(MainActivity.EXTRA_LAUNCH_ADD_TRANSACTION to true)
    }

    data class OpenQuickAdd(
        val categoryId: Long,
    ) : WidgetActionSpec {
        override val targetClass: Class<*> = QuickAddSheetActivity::class.java

        // Deliberately no CLEAR_TOP — QuickAddSheetActivity is transient by
        // design (self-finishes after save). CLEAR_TOP would pop any modal
        // the sheet itself stacks on top (future work).
        override val flags: Int = Intent.FLAG_ACTIVITY_NEW_TASK
        override val booleanExtras: Map<String, Boolean> = emptyMap()
    }

    data object OpenSettings : WidgetActionSpec {
        override val targetClass: Class<*> = MainActivity::class.java
        override val flags: Int = MAIN_ACTIVITY_FLAGS
        override val booleanExtras: Map<String, Boolean> =
            mapOf(MainActivity.EXTRA_LAUNCH_SETTINGS to true)
    }
}

/**
 * MainActivity-bound widget intents need both flags: `NEW_TASK` because
 * widget clicks fire from outside the activity stack, and `CLEAR_TOP` so
 * repeated taps route to the existing MainActivity (paired with
 * `launchMode="singleTop"` in the manifest).
 */
private const val MAIN_ACTIVITY_FLAGS: Int =
    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP

/**
 * Translates a [WidgetActionSpec] into a real Android [Intent]. Thin
 * adapter — all interesting decisions live on the spec itself.
 */
internal fun WidgetActionSpec.toIntent(context: Context): Intent {
    val intent = Intent(context, targetClass)
    intent.flags = flags
    booleanExtras.forEach { (key, value) -> intent.putExtra(key, value) }
    if (this is WidgetActionSpec.OpenQuickAdd) {
        intent.putExtra(QuickAddSheetActivity.EXTRA_CATEGORY_ID, categoryId)
    }
    return intent
}

internal fun openAppAction(context: Context): Action = actionStartActivity(WidgetActionSpec.OpenApp.toIntent(context))

internal fun openAddTransactionAction(context: Context): Action =
    actionStartActivity(WidgetActionSpec.OpenAddTransaction.toIntent(context))

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
): Action = actionStartActivity(WidgetActionSpec.OpenQuickAdd(categoryId).toIntent(context))

internal fun openSettingsAction(context: Context): Action =
    actionStartActivity(WidgetActionSpec.OpenSettings.toIntent(context))
