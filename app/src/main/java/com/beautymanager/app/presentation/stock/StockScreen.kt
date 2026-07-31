package com.beautymanager.app.presentation.stock

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.beautymanager.app.domain.model.Product
import com.beautymanager.app.domain.model.StockMovement
import com.beautymanager.app.domain.model.StockMovementType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockScreen(onBack: () -> Unit, viewModel: StockViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val dateFormat = remember { SimpleDateFormat("dd/MM HH:mm", Locale("pt", "BR")) }
    val productNameById = remember(state.products) { state.products.associateBy { it.id } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Estoque") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Voltar") } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::onOpenForm) { Icon(Icons.Filled.Add, contentDescription = "Nova movimentação") }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            if (state.movements.isEmpty()) {
                Text("Nenhuma movimentação registrada ainda.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.movements, key = { it.id }) { movement ->
                        MovementRow(movement, productNameById[movement.productId], dateFormat)
                    }
                }
            }
        }
    }

    if (state.isFormOpen) {
        NewMovementDialog(products = state.products, onDismiss = viewModel::onDismissForm, onConfirm = viewModel::onRegisterMovement)
    }
}

@Composable
private fun MovementRow(movement: StockMovement, product: Product?, dateFormat: SimpleDateFormat) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(product?.name ?: "Produto #${movement.productId}", style = MaterialTheme.typography.titleMedium)
                Text(
                    "${movement.type.name} • ${dateFormat.format(Date(movement.dateTimeEpochMillis))}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                (if (movement.type == StockMovementType.ENTRADA) "+" else "-") + movement.quantity,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewMovementDialog(
    products: List<Product>,
    onDismiss: () -> Unit,
    onConfirm: (productId: Long, type: StockMovementType, quantity: Int, notes: String) -> Unit
) {
    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    var type by remember { mutableStateOf(StockMovementType.ENTRADA) }
    var quantity by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nova movimentação") },
        text = {
            Column {
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = selectedProduct?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Produto") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        products.forEach { product ->
                            DropdownMenuItem(text = { Text(product.name) }, onClick = { selectedProduct = product; expanded = false })
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StockMovementType.entries.forEach { option ->
                        FilterChip(selected = type == option, onClick = { type = option }, label = { Text(option.name) })
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Quantidade") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Observação") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val product = selectedProduct ?: return@TextButton
                    val qty = quantity.toIntOrNull() ?: return@TextButton
                    onConfirm(product.id, type, qty, notes)
                }
            ) { Text("Registrar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
