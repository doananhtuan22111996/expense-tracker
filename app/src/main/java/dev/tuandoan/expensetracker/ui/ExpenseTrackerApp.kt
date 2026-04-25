package dev.tuandoan.expensetracker.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import dev.tuandoan.expensetracker.ui.screen.gold.AddEditGoldHoldingScreen
import dev.tuandoan.expensetracker.ui.screen.onboarding.OnboardingScreen
import dev.tuandoan.expensetracker.ui.screen.recurring.AddEditRecurringTransactionScreen
import dev.tuandoan.expensetracker.ui.screen.recurring.RecurringTransactionsScreen

/**
 * Main app composable with simplified, stable navigation architecture
 * Uses single NavHost with proper nested navigation for modal handling
 *
 * @param pendingAddTransactionTick monotonically-increasing token (nanoTime)
 * set by `MainActivity` when the home-screen widget's "+" action fires. A
 * new (non-zero, changed-since-last-composition) value causes an immediate
 * navigation to the add-transaction modal route. Ignored during onboarding.
 */
@Composable
fun ExpenseTrackerApp(
    isOnboardingComplete: Boolean = true,
    pendingAddTransactionTick: Long = 0L,
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val startDestination = if (isOnboardingComplete) "Home" else "Onboarding"

    // Widget "+" tap — navigate to add-transaction on top of whatever was
    // showing. Keyed on the tick so repeated taps re-fire after the user
    // cancels the add screen. A tap arriving during onboarding is queued:
    // the tick persists until isOnboardingComplete flips true, at which
    // point the LaunchedEffect re-keys and fires the navigation — so the
    // user's intent isn't dropped, just deferred until the welcome flow
    // completes.
    LaunchedEffect(pendingAddTransactionTick, isOnboardingComplete) {
        if (pendingAddTransactionTick != 0L && isOnboardingComplete) {
            navController.navigate(ModalNavRoutes.addTransactionRoute())
        }
    }

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
                onNavigateToAddGoldHolding = {
                    navController.navigate(ModalNavRoutes.addGoldHoldingRoute())
                },
                onNavigateToEditGoldHolding = { holdingId ->
                    navController.navigate(ModalNavRoutes.editGoldHoldingRoute(holdingId))
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
                    navController.navigate(ModalNavRoutes.addRecurringRoute())
                },
                onNavigateToEdit = { recurringId ->
                    navController.navigate(ModalNavRoutes.editRecurringRoute(recurringId))
                },
                viewModel = hiltViewModel(),
            )
        }

        composable(
            route = "${ModalDestination.AddEditRecurring.route}/{recurringId}",
            arguments =
                listOf(
                    navArgument("recurringId") {
                        type = NavType.LongType
                        defaultValue = 0L
                    },
                ),
        ) {
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
            route = "${ModalDestination.AddEditGoldHolding.route}/{holdingId}",
            arguments =
                listOf(
                    navArgument("holdingId") {
                        type = NavType.LongType
                        defaultValue = 0L
                    },
                ),
        ) {
            AddEditGoldHoldingScreen(
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
    onNavigateToAddGoldHolding: () -> Unit = {},
    onNavigateToEditGoldHolding: (holdingId: Long) -> Unit = {},
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
        // Don't apply top padding here — let each screen's Scaffold/TopAppBar handle
        // status bar insets so the TopAppBar color extends behind the status bar (M3 edge-to-edge).
        // Bottom padding is passed to each screen's scrollable content.
        ExpenseTrackerNavigation(
            navController = navController,
            modifier = Modifier.fillMaxSize(),
            bottomContentPadding = innerPadding.calculateBottomPadding(),
            onNavigateToAddTransaction = onNavigateToAddTransaction,
            onNavigateToEditTransaction = onNavigateToEditTransaction,
            onNavigateToCategories = onNavigateToCategories,
            onNavigateToRecurring = onNavigateToRecurring,
            onNavigateToAddGoldHolding = onNavigateToAddGoldHolding,
            onNavigateToEditGoldHolding = onNavigateToEditGoldHolding,
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
