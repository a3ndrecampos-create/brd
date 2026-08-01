package com.beautymanager.app.presentation.products

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductFormScreen(
    productId: Long?,
    onDone: () -> Unit,
    viewModel: ProductFormViewModel = hiltViewModel()
) {
    val state by viewModel.formState.collectAsState()
    val options by viewModel.options.collectAsState()
    var showScanner by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.onFieldChange { s -> s.copy(photoUri = uri.toString()) }
    }

    LaunchedEffect(productId) {
        if (productId != null) viewModel.loadProduct(productId)
    }
    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onDone()
    }

    if (showScanner) {
        BarcodeScannerScreen(
            onBarcodeDetected = { code ->
                showScanner = false
                viewModel.onBarcodeScanned(code)
            },
            onClose = { showScanner = false }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (productId == null) "Novo produto" else "Editar produto") },
                navigationIcon = { IconButton(onClick = onDone) { Icon(Icons.Filled.ArrowBack, contentDescription = "Voltar") } }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            OutlinedTextField(
                value = state.barcode,
                onValueChange = { viewModel.onFieldChange { s -> s.copy(barcode = it) } },
                label = { Text("Código de barras") },
                trailingIcon = {
                    IconButton(onClick = { showScanner = true }) {
                        Icon(Icons.Filled.QrCodeScanner, contentDescription = "Escanear código de barras")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            if (state.isLookingUpBarcode) {
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            state.barcodeLookupMessage?.let {
                Spacer(Modifier.height(4.dp))
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }

            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (state.photoUri.isNullOrBlank()) {
                        Icon(Icons.Filled.AddAPhoto, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        AsyncImage(
                            model = state.photoUri,
                            contentDescription = "Foto do produto",
                            modifier = Modifier.size(72.dp).clip(RoundedCornerShape(12.dp))
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        if (state.photoUri.isNullOrBlank()) "Nenhuma foto ainda" else "Foto selecionada",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    TextButton(
                        onClick = {
                            photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }
                    ) { Text(if (state.photoUri.isNullOrBlank()) "Escolher foto" else "Trocar foto") }
                }
            }

            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = state.name,
                onValueChange = { viewModel.onFieldChange { s -> s.copy(name = it) } },
                label = { Text("Nome do produto") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))
            DropdownField(
                label = "Categoria",
                selectedLabel = options.categories.firstOrNull { it.id == state.categoryId }?.name,
                items = options.categories.map { it.id to it.name },
                onSelected = { id -> viewModel.onFieldChange { s -> s.copy(categoryId = id) } }
            )

            Spacer(Modifier.height(12.dp))
            DropdownField(
                label = "Marca",
                selectedLabel = options.brands.firstOrNull { it.id == state.brandId }?.name,
                items = options.brands.map { it.id to it.name },
                onSelected = { id -> viewModel.onFieldChange { s -> s.copy(brandId = id) } }
            )

            Spacer(Modifier.height(12.dp))
            DropdownField(
                label = "Fornecedor",
                selectedLabel = options.suppliers.firstOrNull { it.id == state.supplierId }?.name,
                items = options.suppliers.map { it.id to it.name },
                onSelected = { id -> viewModel.onFieldChange { s -> s.copy(supplierId = id) } }
            )

            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = state.costPrice,
                    onValueChange = { viewModel.onFieldChange { s -> s.copy(costPrice = it) } },
                    label = { Text("Preço de custo") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = state.salePrice,
                    onValueChange = { viewModel.onFieldChange { s -> s.copy(salePrice = it) } },
                    label = { Text("Preço de venda") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "Margem estimada: ${"%.0f".format(state.marginPercent)}%",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = state.quantity,
                    onValueChange = { viewModel.onFieldChange { s -> s.copy(quantity = it) } },
                    label = { Text("Quantidade") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = state.minStock,
                    onValueChange = { viewModel.onFieldChange { s -> s.copy(minStock = it) } },
                    label = { Text("Estoque mínimo") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = state.notes,
                onValueChange = { viewModel.onFieldChange { s -> s.copy(notes = it) } },
                label = { Text("Observações") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = viewModel::onSave,
                enabled = state.name.isNotBlank() && state.costPrice.isNotBlank() && state.salePrice.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) { Text("Salvar produto") }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownField(
    label: String,
    selectedLabel: String?,
    items: List<Pair<Long, String>>,
    onSelected: (Long?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selectedLabel ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("Nenhum") }, onClick = { onSelected(null); expanded = false })
            items.forEach { (id, name) ->
                DropdownMenuItem(text = { Text(name) }, onClick = { onSelected(id); expanded = false })
            }
        }
    }
}
