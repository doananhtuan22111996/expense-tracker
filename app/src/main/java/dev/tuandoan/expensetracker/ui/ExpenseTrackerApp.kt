package dev.tuandoan.expensetracker.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.tuandoan.expensetracker.ui.navigation.BottomNavDestination
import dev.tuandoan.expensetracker.ui.navigation.ExpenseTrackerNavigation
import dev.tuandoan.expensetracker.ui.navigation.ModalDestination
import dev.tuandoan.expensetracker.ui.navigation.ModalNavRoutes
import dev.tuandoan.expensetracker.ui.screen.addedit.AddEditTransactionScreen

/**
 * Main app composable with simplified, stable navigation architecture
 * Uses single NavHost with proper nested navigation for modal handling
 */
@Composable
fun ExpenseTrackerApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    NavHost(
        navController = navController,
        startDestination = "Home",
        modifier = Modifier.fillMaxSize(),
    ) {
        composable("Home") {
            Home(
                onNavigateToAddTransaction = {
                    navController.navigate(ModalNavRoutes.addTransactionRoute())
                },
                onNavigateToEditTransaction = { transactionId ->
                    navController.navigate(ModalNavRoutes.editTransactionRoute(transactionId))
                },
            )
        }

        composable(
            route = "${ModalDestination.AddEditTransaction.route}/{transactionId}",
            arguments =
                listOf(
                    navArgument("transactionId") {
                        type = NavType.LongType
                        defaultValue = 0L
                    },
                ),
        ) { backStackEntry ->
            val transactionId = backStackEntry.arguments?.getLong("transactionId") ?: 0L
            AddEditTransactionScreen(
                transactionId = if (transactionId == 0L) null else transactionId,
                onNavigateBack = {
                    // Safe navigation back to Home
                    if (!navController.popBackStack()) {
                        // Fallback: navigate to home if back stack is empty
                        navController.navigate("Home") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                },
                viewModel = hiltViewModel(),
            )
        }
    }
}

@Composable
private fun Home(
    onNavigateToAddTransaction: () -> Unit,
    onNavigateToEditTransaction: (transactionId: Long) -> Unit,
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            BottomNavigationBar(
                currentDestination = currentDestination,
                onNavigateToDestination = { destination ->
                    navController.navigate(destination.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
            )
        },
    ) { innerPadding ->
        ExpenseTrackerNavigation(
            navController = navController,
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            onNavigateToAddTransaction = onNavigateToAddTransaction,
            onNavigateToEditTransaction = onNavigateToEditTransaction,
        )
    }
}

/**
 * Bottom Navigation Bar Component
 * Separated for clarity and reusability
 */
@Composable
private fun BottomNavigationBar(
    currentDestination: androidx.navigation.NavDestination?,
    onNavigateToDestination: (BottomNavDestination) -> Unit,
) {
    NavigationBar {
        BottomNavDestination.allDestinations.forEach { destination ->
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = destination.title,
                    )
                },
                label = { Text(destination.title) },
                selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true,
                onClick = { onNavigateToDestination(destination) },
            )
        }
    }
}
