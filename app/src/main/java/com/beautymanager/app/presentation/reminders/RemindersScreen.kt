package com.beautymanager.app.presentation.reminders

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(onBack: () -> Unit, viewModel: RemindersViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var editingRow by remember { mutableStateOf<ReminderRow?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lembretes de recompra") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Voltar") } },
                actions = {
                    IconButton(onClick = viewModel::onRefresh) { Icon(Icons.Filled.Refresh, contentDescription = "Atualizar lembretes") }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            if (state.isGenerating) LinearProgressIndicator(Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))

            if (state.rows.isEmpty()) {
                Text(
                    "Nenhum lembrete pendente. Toque em atualizar para recalcular com base nas regras configuradas.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.rows, key = { it.reminder.id }) { row ->
                        ReminderCard(
                            row = row,
                            onSend = { editingRow = row },
                            onDismiss = { viewModel.onDismiss(row.reminder.id) }
                        )
                    }
                }
            }
        }
    }

    editingRow?.let { row ->
        EditMessageDialog(
            row = row,
            onDismiss = { editingRow = null },
            onSend = { message ->
                val phone = row.customer?.whatsapp?.filter { it.isDigit() }
                if (phone != null) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$phone?text=${Uri.encode(message)}"))
                    context.startActivity(intent)
                }
                viewModel.onMarkSent(row.reminder.id)
                editingRow = null
            }
        )
    }
}

@Composable
private fun ReminderCard(row: ReminderRow, onSend: () -> Unit, onDismiss: () -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(row.customer?.name ?: "Cliente #${row.reminder.customerId}", style = MaterialTheme.typography.titleMedium)
            Text(
                "${row.product?.name ?: "Produto"} • ${row.reminder.daysSincePurchase} dias sem comprar",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onSend) { Text("Enviar WhatsApp") }
                OutlinedButton(onClick = onDismiss) { Text("Ignorar") }
            }
        }
    }
}

@Composable
private fun EditMessageDialog(row: ReminderRow, onDismiss: () -> Unit, onSend: (String) -> Unit) {
    var message by remember(row) { mutableStateOf(row.defaultMessage) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mensagem para ${row.customer?.name ?: "cliente"}") },
        text = {
            OutlinedTextField(value = message, onValueChange = { message = it }, minLines = 4, modifier = Modifier.fillMaxWidth())
        },
        confirmButton = { TextButton(onClick = { onSend(message) }) { Text("Enviar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
