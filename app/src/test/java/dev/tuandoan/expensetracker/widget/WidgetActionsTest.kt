package dev.tuandoan.expensetracker.widget

import android.content.Intent
import dev.tuandoan.expensetracker.MainActivity
import dev.tuandoan.expensetracker.ui.screen.quickadd.QuickAddSheetActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Contract tests for widget click routing.
 *
 * We don't use Robolectric: this suite pins the pure-Kotlin [WidgetActionSpec]
 * surface that [openAppAction] / [openAddTransactionAction] /
 * [openQuickAddAction] / [openSettingsAction] are thin adapters over. The
 * adapter itself is a 5-line translator with no branches beyond the
 * spec's sealed hierarchy, so pinning the spec pins the whole routing
 * contract. If a future change wires a spec into the wrong action factory,
 * that's the kind of bug a wire-up integration test catches (T7.1).
 *
 * Invariants this suite guards:
 * - Target Activity class (MainActivity vs QuickAddSheetActivity)
 * - Extras set exactly on the right specs (orthogonality of the three flags)
 * - Flags: NEW_TASK always; CLEAR_TOP on MainActivity specs only — NOT on
 *   QuickAdd, which is a transient Activity per T2.6's documented decision.
 * - Category id pass-through on OpenQuickAdd (including edge values that
 *   QuickAddSheetActivity.readCategoryIdExtra is expected to reject later).
 */
class WidgetActionsTest {
    // --- OpenApp ---

    @Test
    fun openApp_targetsMainActivity() {
        assertEquals(MainActivity::class.java, WidgetActionSpec.OpenApp.targetClass)
    }

    @Test
    fun openApp_hasNoExtras() {
        assertTrue(WidgetActionSpec.OpenApp.booleanExtras.isEmpty())
    }

    @Test
    fun openApp_flagsIncludeNewTaskAndClearTop() {
        val flags = WidgetActionSpec.OpenApp.flags
        assertTrue("NEW_TASK required for out-of-stack widget clicks", flags hasFlag Intent.FLAG_ACTIVITY_NEW_TASK)
        assertTrue(
            "CLEAR_TOP required so repeated taps route to existing MainActivity",
            flags hasFlag Intent.FLAG_ACTIVITY_CLEAR_TOP,
        )
    }

    // --- OpenAddTransaction ---

    @Test
    fun openAddTransaction_targetsMainActivity() {
        assertEquals(MainActivity::class.java, WidgetActionSpec.OpenAddTransaction.targetClass)
    }

    @Test
    fun openAddTransaction_setsLaunchAddFlag() {
        assertEquals(
            mapOf(MainActivity.EXTRA_LAUNCH_ADD_TRANSACTION to true),
            WidgetActionSpec.OpenAddTransaction.booleanExtras,
        )
    }

    @Test
    fun openAddTransaction_doesNotSetSettingsFlag() {
        // Orthogonality: the "+" button must not accidentally trigger the
        // Settings-navigation path in MainActivity.
        assertFalse(
            WidgetActionSpec.OpenAddTransaction.booleanExtras.containsKey(
                MainActivity.EXTRA_LAUNCH_SETTINGS,
            ),
        )
    }

    @Test
    fun openAddTransaction_flagsIncludeNewTaskAndClearTop() {
        val flags = WidgetActionSpec.OpenAddTransaction.flags
        assertTrue(flags hasFlag Intent.FLAG_ACTIVITY_NEW_TASK)
        assertTrue(flags hasFlag Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }

    // --- OpenQuickAdd ---

    @Test
    fun openQuickAdd_targetsQuickAddSheetActivity() {
        val spec = WidgetActionSpec.OpenQuickAdd(categoryId = 42L)
        assertEquals(QuickAddSheetActivity::class.java, spec.targetClass)
        assertNotEquals(
            "QuickAdd must NOT target MainActivity — routing would show the full add-transaction screen instead of the quick-add sheet",
            MainActivity::class.java,
            spec.targetClass,
        )
    }

    @Test
    fun openQuickAdd_passesCategoryIdThrough() {
        val spec = WidgetActionSpec.OpenQuickAdd(categoryId = 42L)
        assertEquals(42L, spec.categoryId)
    }

    @Test
    fun openQuickAdd_preservesInvalidCategoryIds() {
        // Builder is intentionally permissive. Validation (null/0/negative
        // rejection) is QuickAddSheetActivity.readCategoryIdExtra's job per
        // PR #130. Pinning the pass-through here so a future "helpful"
        // guard in the builder doesn't silently swallow the bug the
        // Activity-side guard is designed to surface.
        assertEquals(0L, WidgetActionSpec.OpenQuickAdd(categoryId = 0L).categoryId)
        assertEquals(-1L, WidgetActionSpec.OpenQuickAdd(categoryId = -1L).categoryId)
    }

    @Test
    fun openQuickAdd_doesNotSetMainActivityFlags() {
        // Orthogonality: none of the MainActivity-routed flags should leak
        // onto the QuickAdd spec.
        val spec = WidgetActionSpec.OpenQuickAdd(categoryId = 42L)
        assertFalse(spec.booleanExtras.containsKey(MainActivity.EXTRA_LAUNCH_ADD_TRANSACTION))
        assertFalse(spec.booleanExtras.containsKey(MainActivity.EXTRA_LAUNCH_SETTINGS))
    }

    @Test
    fun openQuickAdd_flagsIncludeNewTaskButNotClearTop() {
        // Transient-Activity invariant (T2.6 decision): QuickAddSheetActivity
        // self-finishes after save, so CLEAR_TOP would pop any modal the
        // sheet itself stacks on top. Pin this invariant so a future change
        // can't silently add CLEAR_TOP.
        val flags = WidgetActionSpec.OpenQuickAdd(categoryId = 42L).flags
        assertTrue("NEW_TASK required for out-of-stack widget clicks", flags hasFlag Intent.FLAG_ACTIVITY_NEW_TASK)
        assertFalse(
            "CLEAR_TOP must NOT be set — QuickAddSheetActivity is transient by design (T2.6)",
            flags hasFlag Intent.FLAG_ACTIVITY_CLEAR_TOP,
        )
    }

    @Test
    fun openQuickAdd_equalSpecsForEqualCategoryIds() {
        // Pinning data-class equality so the spec is safely usable in
        // collections / memoized click lookups.
        assertEquals(
            WidgetActionSpec.OpenQuickAdd(categoryId = 7L),
            WidgetActionSpec.OpenQuickAdd(categoryId = 7L),
        )
        assertNotEquals(
            WidgetActionSpec.OpenQuickAdd(categoryId = 7L),
            WidgetActionSpec.OpenQuickAdd(categoryId = 8L),
        )
    }

    // --- OpenSettings ---

    @Test
    fun openSettings_targetsMainActivity() {
        assertEquals(MainActivity::class.java, WidgetActionSpec.OpenSettings.targetClass)
    }

    @Test
    fun openSettings_setsLaunchSettingsFlag() {
        assertEquals(
            mapOf(MainActivity.EXTRA_LAUNCH_SETTINGS to true),
            WidgetActionSpec.OpenSettings.booleanExtras,
        )
    }

    @Test
    fun openSettings_doesNotSetAddTransactionFlag() {
        // Orthogonality: tapping the empty-pin placeholder must not route
        // the user to add-transaction instead of Settings.
        assertFalse(
            WidgetActionSpec.OpenSettings.booleanExtras.containsKey(
                MainActivity.EXTRA_LAUNCH_ADD_TRANSACTION,
            ),
        )
    }

    @Test
    fun openSettings_flagsIncludeNewTaskAndClearTop() {
        val flags = WidgetActionSpec.OpenSettings.flags
        assertTrue(flags hasFlag Intent.FLAG_ACTIVITY_NEW_TASK)
        assertTrue(flags hasFlag Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }

    // --- Helpers ---

    private infix fun Int.hasFlag(flag: Int): Boolean = (this and flag) == flag
}
