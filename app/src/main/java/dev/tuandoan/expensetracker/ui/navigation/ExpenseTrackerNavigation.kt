package dev.tuandoan.expensetracker.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import dev.tuandoan.expensetracker.ui.screen.gold.GoldPortfolioScreen
import dev.tuandoan.expensetracker.ui.screen.home.HomeScreen
import dev.tuandoan.expensetracker.ui.screen.settings.SettingsScreen
import dev.tuandoan.expensetracker.ui.screen.summary.SummaryScreen

/**
 * Main Navigation Host with stable, single NavController architecture
 * Properly separates bottom navigation from modal navigation while maintaining stability
 */
@Composable
fun ExpenseTrackerNavigation(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    bottomContentPadding: Dp = 0.dp,
    onNavigateToAddTransaction: () -> Unit,
    onNavigateToEditTransaction: (transactionId: Long) -> Unit,
    onNavigateToCategories: () -> Unit = {},
    onNavigateToRecurring: () -> Unit = {},
    onNavigateToAddGoldHolding: () -> Unit = {},
    onNavigateToEditGoldHolding: (holdingId: Long) -> Unit = {},
) {
    NavHost(
        navController = navController,
        startDestination = BottomNavDestination.Home.route,
        modifier = modifier,
    ) {
        composable(BottomNavDestination.Home.route) {
            HomeScreen(
                onNavigateToAddTransaction = onNavigateToAddTransaction,
                onNavigateToEditTransaction = onNavigateToEditTransaction,
                bottomContentPadding = bottomContentPadding,
                viewModel = hiltViewModel(),
            )
        }

        composable(BottomNavDestination.Summary.route) {
            SummaryScreen(
                bottomContentPadding = bottomContentPadding,
                viewModel = hiltViewModel(),
            )
        }

        composable(BottomNavDestination.Gold.route) {
            GoldPortfolioScreen(
                onNavigateToAddHolding = onNavigateToAddGoldHolding,
                onNavigateToEditHolding = onNavigateToEditGoldHolding,
                bottomContentPadding = bottomContentPadding,
                viewModel = hiltViewModel(),
            )
        }

        composable(BottomNavDestination.Settings.route) {
            SettingsScreen(
                onNavigateToCategories = onNavigateToCategories,
                onNavigateToRecurring = onNavigateToRecurring,
                bottomContentPadding = bottomContentPadding,
            )
        }
    }
}

/**
 * Type-safe navigation routes with proper parameter resolution
 */
object ModalNavRoutes {
    fun addTransactionRoute(): String = "${ModalDestination.AddEditTransaction.route}/0"

    fun editTransactionRoute(transactionId: Long): String =
        "${ModalDestination.AddEditTransaction.route}/$transactionId"

    fun addGoldHoldingRoute(): String = "${ModalDestination.AddEditGoldHolding.route}/0"

    fun editGoldHoldingRoute(holdingId: Long): String = "${ModalDestination.AddEditGoldHolding.route}/$holdingId"

    fun addRecurringRoute(): String = "${ModalDestination.AddEditRecurring.route}/0"

    fun editRecurringRoute(recurringId: Long): String = "${ModalDestination.AddEditRecurring.route}/$recurringId"
}
