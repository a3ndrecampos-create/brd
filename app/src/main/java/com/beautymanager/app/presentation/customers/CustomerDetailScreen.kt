package com.beautymanager.app.presentation.customers

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.beautymanager.app.domain.model.Sale
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailScreen(
    customerId: Long,
    onBack: () -> Unit,
    viewModel: CustomerDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val currency = remember { NumberFormat.getCurrencyInstance(Locale("pt", "BR")) }
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR")) }
    var showEditDialog by remember { mutableStateOf(false) }

    LaunchedEffect(customerId) { viewModel.load(customerId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.customer?.name ?: "Cliente") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Voltar") } },
                actions = {
                    IconButton(onClick = { showEditDialog = true }) { Icon(Icons.Filled.Edit, contentDescription = "Editar cliente") }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            state.customer?.phone?.let {
                Text(it, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            state.customer?.address?.let {
                Spacer(Modifier.height(4.dp))
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ProfileStat("Total gasto", currency.format(state.totalSpent), Modifier.weight(1f))
                ProfileStat("Compras", state.purchaseCount.toString(), Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ProfileStat("Ticket médio", currency.format(state.averageTicket), Modifier.weight(1f))
                ProfileStat(
                    "Última compra",
                    state.lastPurchase?.let { dateFormat.format(Date(it.dateTimeEpochMillis)) } ?: "—",
                    Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(24.dp))
            Text("Histórico de compras", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            if (state.sales.isEmpty()) {
                Text("Nenhuma compra registrada ainda.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.sales, key = { it.id }) { sale -> SaleRow(sale, currency, dateFormat) }
                }
            }
        }
    }

    if (showEditDialog && state.customer != null) {
        EditCustomerDialog(
            customer = state.customer!!,
            onDismiss = { showEditDialog = false },
            onConfirm = { name, phone, whatsapp, address ->
                viewModel.onUpdateCustomer(name, phone, whatsapp, address)
                showEditDialog = false
            }
        )
    }
}

@Composable
private fun EditCustomerDialog(
    customer: com.beautymanager.app.domain.model.Customer,
    onDismiss: () -> Unit,
    onConfirm: (name: String, phone: String, whatsapp: String, address: String) -> Unit
) {
    var name by remember { mutableStateOf(customer.name) }
    var phone by remember { mutableStateOf(customer.phone ?: "") }
    var whatsapp by remember { mutableStateOf(customer.whatsapp ?: "") }
    var address by remember { mutableStateOf(customer.address ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar cliente") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nome") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Telefone") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = whatsapp, onValueChange = { whatsapp = it }, label = { Text("WhatsApp") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Endereço completo") }, minLines = 3, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = { onConfirm(name, phone, whatsapp, address) }
            ) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun ProfileStat(label: String, value: String, modifier: Modifier = Modifier) {
    ElevatedCard(modifier = modifier) {
        Column(Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun SaleRow(sale: Sale, currency: NumberFormat, dateFormat: SimpleDateFormat) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(dateFormat.format(Date(sale.dateTimeEpochMillis)), style = MaterialTheme.typography.bodyMedium)
                Text(sale.paymentMethod.name, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(currency.format(sale.totalAmount), style = MaterialTheme.typography.titleMedium)
        }
    }
}
