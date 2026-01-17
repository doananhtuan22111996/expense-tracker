package dev.tuandoan.expensetracker.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import dev.tuandoan.expensetracker.ui.screen.addedit.AddEditTransactionScreen
import dev.tuandoan.expensetracker.ui.screen.home.HomeScreen
import dev.tuandoan.expensetracker.ui.screen.settings.SettingsScreen
import dev.tuandoan.expensetracker.ui.screen.summary.SummaryScreen

@Composable
fun ExpenseTrackerNavigation(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = ExpenseTrackerDestination.Home.route,
        modifier = modifier,
    ) {
        composable(ExpenseTrackerDestination.Home.route) {
            HomeScreen(
                onNavigateToAddTransaction = {
                    navController.navigate("${ExpenseTrackerDestination.AddEditTransaction.route}/0")
                },
                onNavigateToEditTransaction = { transactionId ->
                    navController.navigate("${ExpenseTrackerDestination.AddEditTransaction.route}/$transactionId")
                },
                viewModel = hiltViewModel(),
            )
        }

        composable(ExpenseTrackerDestination.Summary.route) {
            SummaryScreen(
                viewModel = hiltViewModel(),
            )
        }

        composable(ExpenseTrackerDestination.Settings.route) {
            SettingsScreen()
        }

        composable("${ExpenseTrackerDestination.AddEditTransaction.route}/{transactionId}") { backStackEntry ->
            val transactionId = backStackEntry.arguments?.getString("transactionId")?.toLongOrNull() ?: 0L
            AddEditTransactionScreen(
                transactionId = if (transactionId == 0L) null else transactionId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                viewModel = hiltViewModel(),
            )
        }
    }
}
