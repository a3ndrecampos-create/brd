package com.beautymanager.app.presentation.more

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.beautymanager.app.presentation.common.CurrentUserViewModel

private data class MoreItem(val label: String, val subtitle: String, val icon: ImageVector, val onClick: () -> Unit)

@Composable
fun MoreMenuScreen(
    onOpenStock: () -> Unit,
    onOpenReports: () -> Unit,
    onOpenReminders: () -> Unit,
    onOpenSettings: () -> Unit,
    currentUserViewModel: CurrentUserViewModel = hiltViewModel()
) {
    val currentUser by currentUserViewModel.currentUser.collectAsState()

    val items = buildList {
        add(MoreItem("Estoque", "Entradas, saídas, ajustes e transferências", Icons.Filled.Inventory, onOpenStock))
        if (currentUser?.canViewReports != false) {
            add(MoreItem("Relatórios", "Faturamento, lucro e produtos mais vendidos", Icons.Filled.BarChart, onOpenReports))
        }
        add(MoreItem("Lembretes", "Clientes prontos para recompra", Icons.Filled.NotificationsActive, onOpenReminders))
        add(MoreItem("Configurações", "Categorias, marcas, fornecedores e usuários", Icons.Filled.Settings, onOpenSettings))
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Mais", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(items) { item ->
                ElevatedCard(onClick = item.onClick, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Icon(item.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(item.label, style = MaterialTheme.typography.titleMedium)
                            Text(item.subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}
