package dev.tuandoan.expensetracker.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.tuandoan.expensetracker.ui.navigation.BottomNavDestination
import dev.tuandoan.expensetracker.ui.navigation.BottomNavigation
import dev.tuandoan.expensetracker.ui.navigation.ModalNavRoutes
import dev.tuandoan.expensetracker.ui.navigation.ModalNavigation

/**
 * Main app composable with separated navigation architecture
 * - Bottom navigation handles main app flow (Home, Summary, Settings)
 * - Modal navigation handles overlay screens (AddEditTransaction)
 */
@Composable
fun ExpenseTrackerApp() {
    // Separate navigation controllers for clean separation of concerns
    val bottomNavController = rememberNavController()
    val modalNavController = rememberNavController()

    // Track modal visibility state
    var isModalVisible by remember { mutableStateOf(false) }

    // Track current destination for bottom navigation selection
    val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Box(modifier = Modifier.fillMaxSize()) {
        // Main app with bottom navigation (always visible)
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                BottomNavigationBar(
                    currentDestination = currentDestination,
                    onNavigateToDestination = { destination ->
                        bottomNavController.navigate(destination.route) {
                            popUpTo(bottomNavController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            },
        ) { innerPadding ->
            BottomNavigation(
                navController = bottomNavController,
                onNavigateToAddTransaction = {
                    modalNavController.navigate(ModalNavRoutes.addTransactionRoute())
                    isModalVisible = true
                },
                onNavigateToEditTransaction = { transactionId ->
                    modalNavController.navigate(ModalNavRoutes.editTransactionRoute(transactionId))
                    isModalVisible = true
                },
                modifier = Modifier.padding(innerPadding),
            )
        }

        // Modal overlay (only visible when needed)
        if (isModalVisible) {
            ModalNavigation(
                navController = modalNavController,
                onCloseModal = {
                    modalNavController.popBackStack()
                    isModalVisible = false
                },
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(1f), // Overlay on top of main content
            )
        }
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
