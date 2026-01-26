package dev.tuandoan.expensetracker.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import dev.tuandoan.expensetracker.ui.screen.addedit.AddEditTransactionScreen

/**
 * Modal Navigation Graph - Contains full-screen overlay screens
 * These screens don't show bottom navigation and operate independently
 */
@Composable
fun ModalNavigation(
    navController: NavHostController,
    onCloseModal: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = ModalDestination.MODAL_GRAPH_ROUTE,
        modifier = modifier,
    ) {
        modalNavGraph(
            onCloseModal = onCloseModal,
        )
    }
}

/**
 * Modal navigation graph containing overlay destinations
 */
private fun NavGraphBuilder.modalNavGraph(
    onCloseModal: () -> Unit,
) {
    navigation(
        startDestination = "${ModalDestination.AddEditTransaction.route}/{transactionId}",
        route = ModalDestination.MODAL_GRAPH_ROUTE,
    ) {
        composable("${ModalDestination.AddEditTransaction.route}/{transactionId}") { backStackEntry ->
            val transactionId = backStackEntry.arguments?.getString("transactionId")?.toLongOrNull() ?: 0L
            AddEditTransactionScreen(
                transactionId = if (transactionId == 0L) null else transactionId,
                onNavigateBack = onCloseModal,
                viewModel = hiltViewModel(),
            )
        }
    }
}

/**
 * Modal Navigation Routes - Type-safe route builders
 */
object ModalNavRoutes {
    fun addTransactionRoute(): String {
        return "${ModalDestination.AddEditTransaction.route}/0"
    }

    fun editTransactionRoute(transactionId: Long): String {
        return "${ModalDestination.AddEditTransaction.route}/$transactionId"
    }
}