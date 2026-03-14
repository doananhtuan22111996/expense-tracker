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
import dev.tuandoan.expensetracker.ui.screen.categories.CategoriesScreen
import dev.tuandoan.expensetracker.ui.screen.onboarding.OnboardingScreen
import dev.tuandoan.expensetracker.ui.screen.recurring.AddEditRecurringTransactionScreen
import dev.tuandoan.expensetracker.ui.screen.recurring.RecurringTransactionsScreen

/**
 * Main app composable with simplified, stable navigation architecture
 * Uses single NavHost with proper nested navigation for modal handling
 */
@Composable
fun ExpenseTrackerApp(isOnboardingComplete: Boolean = true) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val startDestination = if (isOnboardingComplete) "Home" else "Onboarding"
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = Modifier.fillMaxSize(),
    ) {
        composable("Onboarding") {
            OnboardingScreen(
                onComplete = {
                    navController.navigate("Home") {
                        popUpTo("Onboarding") { inclusive = true }
                    }
                },
            )
        }

        composable("Home") {
            Home(
                onNavigateToAddTransaction = {
                    navController.navigate(ModalNavRoutes.addTransactionRoute())
                },
                onNavigateToEditTransaction = { transactionId ->
                    navController.navigate(ModalNavRoutes.editTransactionRoute(transactionId))
                },
                onNavigateToCategories = {
                    navController.navigate(ModalDestination.Categories.route)
                },
                onNavigateToRecurring = {
                    navController.navigate(ModalDestination.Recurring.route)
                },
            )
        }

        composable(ModalDestination.Recurring.route) {
            RecurringTransactionsScreen(
                onNavigateBack = {
                    if (!navController.popBackStack()) {
                        navController.navigate("Home") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                },
                onNavigateToAdd = {
                    navController.navigate(ModalDestination.AddRecurring.route)
                },
                viewModel = hiltViewModel(),
            )
        }

        composable(ModalDestination.AddRecurring.route) {
            AddEditRecurringTransactionScreen(
                onNavigateBack = {
                    if (!navController.popBackStack()) {
                        navController.navigate("Home") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                },
                viewModel = hiltViewModel(),
            )
        }

        composable(ModalDestination.Categories.route) {
            CategoriesScreen(
                onNavigateBack = {
                    if (!navController.popBackStack()) {
                        navController.navigate("Home") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                },
                viewModel = hiltViewModel(),
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
    onNavigateToCategories: () -> Unit,
    onNavigateToRecurring: () -> Unit = {},
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
            onNavigateToCategories = onNavigateToCategories,
            onNavigateToRecurring = onNavigateToRecurring,
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
