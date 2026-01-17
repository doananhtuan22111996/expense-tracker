package dev.tuandoan.expensetracker.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.ui.graphics.vector.ImageVector

sealed class ExpenseTrackerDestination(
    val route: String,
    val title: String,
    val icon: ImageVector,
) {
    data object Home : ExpenseTrackerDestination(
        route = "home",
        title = "Home",
        icon = Icons.Default.Home,
    )

    data object Summary : ExpenseTrackerDestination(
        route = "summary",
        title = "Summary",
        icon = Icons.Outlined.BarChart,
    )

    data object Settings : ExpenseTrackerDestination(
        route = "settings",
        title = "Settings",
        icon = Icons.Default.Settings,
    )

    data object AddEditTransaction : ExpenseTrackerDestination(
        route = "add_edit_transaction",
        title = "Transaction",
        icon = Icons.Default.Home, // Not used for bottom nav
    )

    companion object {
        val bottomNavDestinations = listOf(Home, Summary, Settings)
    }
}
