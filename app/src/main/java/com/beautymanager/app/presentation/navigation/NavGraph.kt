package com.beautymanager.app.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.beautymanager.app.presentation.auth.LoginPinScreen
import com.beautymanager.app.presentation.customers.CustomerDetailScreen
import com.beautymanager.app.presentation.customers.CustomerListScreen
import com.beautymanager.app.presentation.dashboard.DashboardScreen
import com.beautymanager.app.presentation.more.MoreMenuScreen
import com.beautymanager.app.presentation.products.ProductFormScreen
import com.beautymanager.app.presentation.products.ProductListScreen
import com.beautymanager.app.presentation.reminders.RemindersScreen
import com.beautymanager.app.presentation.reports.ReportsScreen
import com.beautymanager.app.presentation.sales.PointOfSaleScreen
import com.beautymanager.app.presentation.settings.SettingsScreen
import com.beautymanager.app.presentation.stock.StockScreen

/**
 * Duas camadas: gate de autenticação (PIN/biometria) e, depois de logado, a casca do
 * app com navegação inferior para os 4 módulos mais usados no dia a dia (Dashboard,
 * Vendas, Produtos, Clientes) + um item "Mais" que abre os módulos administrativos
 * usados com menos frequência (Estoque, Relatórios, Lembretes, Configurações).
 */
sealed class Destination(val route: String) {
    data object Login : Destination("login")
    data object Dashboard : Destination("dashboard")
    data object Products : Destination("products")
    data object ProductForm : Destination("product_form?productId={productId}") {
        fun createRoute(productId: Long? = null) = "product_form?productId=${productId ?: -1}"
    }
    data object Sales : Destination("sales")
    data object Customers : Destination("customers")
    data object CustomerDetail : Destination("customer_detail/{customerId}") {
        fun createRoute(customerId: Long) = "customer_detail/$customerId"
    }
    data object More : Destination("more")
    data object Stock : Destination("stock")
    data object Reports : Destination("reports")
    data object Reminders : Destination("reminders")
    data object Settings : Destination("settings")
}

private data class BottomItem(val destination: Destination, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val bottomItems = listOf(
    BottomItem(Destination.Dashboard, "Início", Icons.Filled.Dashboard),
    BottomItem(Destination.Sales, "Vendas", Icons.Filled.PointOfSale),
    BottomItem(Destination.Products, "Produtos", Icons.Filled.Inventory2),
    BottomItem(Destination.Customers, "Clientes", Icons.Filled.People),
    BottomItem(Destination.More, "Mais", Icons.Filled.MoreHoriz)
)

@Composable
fun BeautyManagerNavHost(
    isLoggedIn: Boolean,
    navController: NavHostController = rememberNavController()
) {
    val startDestination = if (isLoggedIn) Destination.Dashboard.route else Destination.Login.route

    NavHost(navController = navController, startDestination = startDestination) {

        composable(Destination.Login.route) {
            LoginPinScreen(
                onAuthenticated = {
                    navController.navigate(Destination.Dashboard.route) {
                        popUpTo(Destination.Login.route) { inclusive = true }
                    }
                }
            )
        }

        // As telas abaixo compartilham a mesma Scaffold com bottom nav (ver AppScaffold).
        composable(Destination.Dashboard.route) { AppScaffold(navController, Destination.Dashboard) { DashboardScreen() } }
        composable(Destination.Products.route) {
            AppScaffold(navController, Destination.Products) {
                ProductListScreen(onAddProduct = { navController.navigate(Destination.ProductForm.createRoute()) },
                    onEditProduct = { id -> navController.navigate(Destination.ProductForm.createRoute(id)) })
            }
        }
        composable(Destination.ProductForm.route) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId")?.toLongOrNull()?.takeIf { it != -1L }
            ProductFormScreen(productId = productId, onDone = { navController.popBackStack() })
        }
        composable(Destination.Sales.route) { AppScaffold(navController, Destination.Sales) { PointOfSaleScreen() } }
        composable(Destination.Customers.route) {
            AppScaffold(navController, Destination.Customers) {
                CustomerListScreen(onOpenCustomer = { id -> navController.navigate(Destination.CustomerDetail.createRoute(id)) })
            }
        }
        composable(Destination.CustomerDetail.route) { backStackEntry ->
            val customerId = backStackEntry.arguments?.getString("customerId")?.toLongOrNull() ?: return@composable
            CustomerDetailScreen(customerId = customerId, onBack = { navController.popBackStack() })
        }
        composable(Destination.More.route) {
            AppScaffold(navController, Destination.More) {
                MoreMenuScreen(
                    onOpenStock = { navController.navigate(Destination.Stock.route) },
                    onOpenReports = { navController.navigate(Destination.Reports.route) },
                    onOpenReminders = { navController.navigate(Destination.Reminders.route) },
                    onOpenSettings = { navController.navigate(Destination.Settings.route) }
                )
            }
        }
        composable(Destination.Stock.route) { StockScreen(onBack = { navController.popBackStack() }) }
        composable(Destination.Reports.route) { ReportsScreen(onBack = { navController.popBackStack() }) }
        composable(Destination.Reminders.route) { RemindersScreen(onBack = { navController.popBackStack() }) }
        composable(Destination.Settings.route) { SettingsScreen(onBack = { navController.popBackStack() }) }
    }
}

@Composable
private fun AppScaffold(
    navController: NavHostController,
    current: Destination,
    content: @Composable () -> Unit
) {
    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomItems.forEach { item ->
                    NavigationBarItem(
                        selected = current == item.destination,
                        onClick = {
                            if (current != item.destination) {
                                navController.navigate(item.destination.route) {
                                    popUpTo(Destination.Dashboard.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) }
                    )
                }
            }
        }
    ) { padding ->
        androidx.compose.foundation.layout.Box(modifier = androidx.compose.ui.Modifier.padding(padding)) {
            content()
        }
    }
}
