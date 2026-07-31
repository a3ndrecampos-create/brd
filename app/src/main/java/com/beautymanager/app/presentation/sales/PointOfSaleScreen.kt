package com.beautymanager.app.presentation.sales

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.beautymanager.app.domain.model.CartItem
import com.beautymanager.app.domain.model.PaymentMethod
import com.beautymanager.app.domain.model.Product
import java.text.NumberFormat
import java.util.Locale

@Composable
fun PointOfSaleScreen(viewModel: PointOfSaleViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val currency = remember { NumberFormat.getCurrencyInstance(Locale("pt", "BR")) }

    LaunchedEffect(state.lastSaleId) {
        if (state.lastSaleId != null) {
            // TODO: disparar impressão/exibição do comprovante aqui antes de reconhecer.
            viewModel.onSaleAcknowledged()
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Venda", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = viewModel::onSearchQueryChange,
            placeholder = { Text("Buscar produto ou ler código de barras") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        if (state.searchResults.isNotEmpty()) {
            LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                items(state.searchResults, key = { it.id }) { product ->
                    SearchResultRow(product, currency) { viewModel.onAddToCart(product) }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        HorizontalDivider()
        Spacer(Modifier.height(12.dp))

        if (state.cartItems.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                Text("Carrinho vazio — busque um produto acima", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.cartItems, key = { it.product.id }) { item ->
                    CartRow(item, currency, viewModel)
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = state.discount,
            onValueChange = viewModel::onDiscountChange,
            label = { Text("Desconto (R$)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))
        Text("Forma de pagamento", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PaymentMethod.entries.forEach { method ->
                FilterChip(
                    selected = state.paymentMethod == method,
                    onClick = { viewModel.onPaymentMethodChange(method) },
                    label = { Text(method.name) }
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Total", style = MaterialTheme.typography.titleLarge)
            Text(currency.format(state.total), style = MaterialTheme.typography.titleLarge)
        }

        state.errorMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(Modifier.height(12.dp))
        Button(
            onClick = { viewModel.onCheckout() },
            enabled = state.cartItems.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) { Text("Finalizar venda") }
    }
}

@Composable
private fun SearchResultRow(product: Product, currency: NumberFormat, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(product.name) },
        supportingContent = { Text("Estoque: ${product.quantity}") },
        trailingContent = { Text(currency.format(product.salePrice)) },
        modifier = Modifier.clickable(onClick)
    )
}

@Composable
private fun CartRow(item: CartItem, currency: NumberFormat, viewModel: PointOfSaleViewModel) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(item.product.name, style = MaterialTheme.typography.titleMedium)
                Text(currency.format(item.product.salePrice) + " un.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { viewModel.onChangeQuantity(item.product.id, item.quantity - 1) }) {
                Icon(Icons.Filled.Remove, contentDescription = "Diminuir")
            }
            Text(item.quantity.toString(), style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = { viewModel.onChangeQuantity(item.product.id, item.quantity + 1) }) {
                Icon(Icons.Filled.Add, contentDescription = "Aumentar")
            }
            IconButton(onClick = { viewModel.onRemoveFromCart(item.product.id) }) {
                Icon(Icons.Filled.Delete, contentDescription = "Remover", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
