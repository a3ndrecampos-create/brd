package com.beautymanager.app.presentation.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.beautymanager.app.domain.model.DashboardMetrics
import java.text.NumberFormat
import java.util.Locale

@Composable
fun DashboardScreen(viewModel: DashboardViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Olá! 👋", style = MaterialTheme.typography.headlineMedium)
        Text("Aqui está o resumo da sua loja hoje", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(20.dp))

        when (val s = state) {
            DashboardUiState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            is DashboardUiState.Ready -> DashboardContent(s.metrics)
        }
    }
}

@Composable
private fun ColumnScope.DashboardContent(metrics: DashboardMetrics) {
    val currency = remember(metrics) { NumberFormat.getCurrencyInstance(Locale("pt", "BR")) }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.weight(1f)
    ) {
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
            MetricCard(
                title = "Vendido hoje",
                value = currency.format(metrics.totalSoldToday),
                icon = Icons.Filled.TrendingUp,
                accent = MaterialTheme.colorScheme.primary,
                emphasized = true
            )
        }
        item { MetricCard("Vendido no mês", currency.format(metrics.totalSoldThisMonth), Icons.Filled.CalendarMonth) }
        item { MetricCard("Lucro do dia", currency.format(metrics.profitToday), Icons.Filled.Savings) }
        item { MetricCard("Vendas hoje", metrics.salesCountToday.toString(), Icons.Filled.ShoppingCart) }
        item { MetricCard("Produtos vendidos", metrics.productsSoldToday.toString(), Icons.Filled.Inventory2) }
        item {
            MetricCard(
                "Estoque baixo",
                metrics.lowStockCount.toString(),
                Icons.Filled.WarningAmber,
                accent = if (metrics.lowStockCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        }
        item { MetricCard("P/ contato (recompra)", metrics.customersNeedingContactCount.toString(), Icons.Filled.NotificationsActive) }
        item { MetricCard("Aniversariantes do mês", metrics.birthdaysThisMonthCount.toString(), Icons.Filled.Cake) }

        if (metrics.topProducts.isNotEmpty()) {
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Produtos mais vendidos (mês)", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(12.dp))
                        SimpleBarChart(
                            entries = metrics.topProducts.map { BarChartEntry(it.name.take(8), it.totalSold.toFloat()) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    accent: Color = MaterialTheme.colorScheme.primary,
    emphasized: Boolean = false
) {
    ElevatedCard(
        colors = if (emphasized) CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer) else CardDefaults.elevatedCardColors()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(8.dp))
            Text(value, style = if (emphasized) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge)
        }
    }
}
