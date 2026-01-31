package dev.tuandoan.expensetracker.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BarChart
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

    data object Settings : BottomNavDestination(
        route = "main/settings",
        title = "Settings",
        icon = Icons.Default.Settings,
    )

    companion object {
        val allDestinations = listOf(Home, Summary, Settings)
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

    companion object {
        const val MODAL_GRAPH_ROUTE = "modal_graph"
    }
}
