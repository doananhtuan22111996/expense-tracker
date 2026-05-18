package dev.tuandoan.expensetracker.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Paid
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Bottom Navigation Destinations - Main app flow with persistent bottom navigation
 */
sealed class BottomNavDestination(
    val route: String,
    val title: String,
    val icon: ImageVector,
) {
    data object Home : BottomNavDestination(
        route = "main/home",
        title = "Home",
        icon = Icons.Default.Home,
    )

    data object Summary : BottomNavDestination(
        route = "main/summary",
        title = "Summary",
        icon = Icons.Outlined.BarChart,
    )

    data object Gold : BottomNavDestination(
        route = "main/gold",
        title = "Gold",
        icon = Icons.Outlined.Paid,
    )

    data object Settings : BottomNavDestination(
        route = "main/settings",
        title = "Settings",
        icon = Icons.Default.Settings,
    )

    companion object {
        val allDestinations = listOf(Home, Summary, Gold, Settings)
        const val MAIN_GRAPH_ROUTE = "main_graph"
    }
}

/**
 * Modal Destinations - Full-screen overlays that don't show bottom navigation
 */
sealed class ModalDestination(
    val route: String,
) {
    data object AddEditTransaction : ModalDestination("modal/add_edit_transaction")

    data object Categories : ModalDestination("modal/categories")

    data object Recurring : ModalDestination("modal/recurring")

    data object AddEditRecurring : ModalDestination("modal/add_edit_recurring")

    data object AddEditGoldHolding : ModalDestination("modal/add_edit_gold_holding")

    /**
     * Trips list (v3.13.0, T2.2). Active / Upcoming / Past sections + FAB
     * for Create Trip. Reached from Settings's "Trips" section row (T2.8).
     */
    data object Trips : ModalDestination("modal/trips")

    /**
     * Create / Edit Trip modal (v3.13.0, T2.4). Route arg `tripId` — `0L`
     * means create mode, any other value loads the existing trip for edit.
     * Mirrors [AddEditTransaction] semantics.
     */
    data object AddEditTrip : ModalDestination("modal/add_edit_trip")

    /**
     * Trip detail (v3.13.0, T2.6). Route arg `tripId` is required and must
     * resolve to a persisted trip; reader-only screen with KPIs, donut,
     * day-by-day chart, and transaction list. Edit/Delete/Revert live in
     * the ⋮ menu (T2.7).
     */
    data object TripDetail : ModalDestination("modal/trip_detail")

    /**
     * Convert-legacy-category wizard (v3.13.0, T4.1). Route arg
     * `categoryId` — points at the source EXPENSE category being converted
     * into a Trip. The wizard writes nothing until Commit (T4.2 state
     * machine, T4.6 atomic commit).
     */
    data object ConvertCategoryToTrip : ModalDestination("modal/convert_category_to_trip")

    /**
     * Settings → Widget Categories (v3.12.0, T6.3 / T6.4). Lets the user pick
     * up to 3 EXPENSE categories to pin as quick-add tiles on the home-screen
     * widget. Reached from Settings's "Widget" section row.
     */
    data object WidgetCategories : ModalDestination("modal/widget_categories")

    /**
     * Developer-only debug panel (v3.11.0, ADR-010). Reachable only via the
     * 7-tap Easter-egg gesture on the Settings version text; not linked from
     * any normal user surface.
     */
    data object DebugPanel : ModalDestination("modal/debug_panel")

    companion object {
        const val MODAL_GRAPH_ROUTE = "modal_graph"
    }
}
