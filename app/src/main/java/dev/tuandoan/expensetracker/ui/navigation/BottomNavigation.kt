package dev.tuandoan.expensetracker.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import dev.tuandoan.expensetracker.ui.screen.home.HomeScreen
import dev.tuandoan.expensetracker.ui.screen.settings.SettingsScreen
import dev.tuandoan.expensetracker.ui.screen.summary.SummaryScreen

/**
 * Bottom Navigation Graph - Contains Home, Summary, Settings screens
 * These screens have persistent bottom navigation and share state
 */
@Composable
fun BottomNavigation(
    navController: NavHostController,
    onNavigateToAddTransaction: () -> Unit,
    onNavigateToEditTransaction: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = BottomNavDestination.MAIN_GRAPH_ROUTE,
        modifier = modifier,
    ) {
        mainNavGraph(
            onNavigateToAddTransaction = onNavigateToAddTransaction,
            onNavigateToEditTransaction = onNavigateToEditTransaction,
        )
    }
}

/**
 * Main navigation graph containing bottom navigation destinations
 */
private fun NavGraphBuilder.mainNavGraph(
    onNavigateToAddTransaction: () -> Unit,
    onNavigateToEditTransaction: (Long) -> Unit,
) {
    navigation(
        startDestination = BottomNavDestination.Home.route,
        route = BottomNavDestination.MAIN_GRAPH_ROUTE,
    ) {
        composable(BottomNavDestination.Home.route) {
            HomeScreen(
                onNavigateToAddTransaction = onNavigateToAddTransaction,
                onNavigateToEditTransaction = onNavigateToEditTransaction,
                viewModel = hiltViewModel(),
            )
        }

        composable(BottomNavDestination.Summary.route) {
            SummaryScreen(
                viewModel = hiltViewModel(),
            )
        }

        composable(BottomNavDestination.Settings.route) {
            SettingsScreen()
        }
    }
}