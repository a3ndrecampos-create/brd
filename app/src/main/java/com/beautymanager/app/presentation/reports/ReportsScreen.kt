package com.beautymanager.app.presentation.reports

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.beautymanager.app.presentation.dashboard.BarChartEntry
import com.beautymanager.app.presentation.dashboard.SimpleBarChart
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(onBack: () -> Unit, viewModel: ReportsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val currency = remember { NumberFormat.getCurrencyInstance(Locale("pt", "BR")) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Relatórios") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Voltar") } },
                actions = {
                    IconButton(onClick = viewModel::onExportPdf) { Icon(Icons.Filled.Description, contentDescription = "Exportar PDF") }
                    IconButton(onClick = viewModel::onExportExcel) { Icon(Icons.Filled.TableChart, contentDescription = "Exportar Excel") }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ReportPeriod.entries.forEach { period ->
                    FilterChip(selected = state.period == period, onClick = { viewModel.load(period) }, label = { Text(period.label) })
                }
            }
            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ReportMetric("Faturamento", currency.format(state.totalSold), Modifier.weight(1f))
                ReportMetric("Lucro", currency.format(state.totalProfit), Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            ReportMetric("Número de vendas", state.salesCount.toString(), Modifier.fillMaxWidth())

            Spacer(Modifier.height(24.dp))
            Text("Produtos mais vendidos", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            if (state.topProducts.isEmpty()) {
                Text("Sem vendas no período selecionado.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                SimpleBarChart(entries = state.topProducts.map { BarChartEntry(it.name.take(8), it.totalSold.toFloat()) })
            }

            Spacer(Modifier.height(16.dp))
            Text(
                "Exportação em PDF/Excel ainda não implementada nesta versão — os botões no topo já estão " +
                    "no lugar certo, faltando só ligar a geração do arquivo.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ReportMetric(label: String, value: String, modifier: Modifier = Modifier) {
    ElevatedCard(modifier = modifier) {
        Column(Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleLarge)
        }
    }
}
