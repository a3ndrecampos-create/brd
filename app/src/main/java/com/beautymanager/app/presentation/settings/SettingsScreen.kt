package com.beautymanager.app.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.beautymanager.app.domain.model.ReminderRule
import com.beautymanager.app.domain.model.AppUser
import com.beautymanager.app.domain.model.Supplier
import com.beautymanager.app.domain.repository.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configurações") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Voltar") } }
            )
        }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {

            item {
                SectionHeader("Tema")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        FilterChip(selected = state.themeMode == mode, onClick = { viewModel.onThemeModeChange(mode) }, label = { Text(mode.name) })
                    }
                }
            }

            item {
                NameListSection(
                    title = "Categorias",
                    items = state.categories.map { it.id to it.name },
                    onAdd = viewModel::onAddCategory,
                    onDelete = viewModel::onDeleteCategory
                )
            }

            item {
                NameListSection(
                    title = "Marcas",
                    items = state.brands.map { it.id to it.name },
                    onAdd = viewModel::onAddBrand,
                    onDelete = viewModel::onDeleteBrand
                )
            }

            item {
                SupplierSection(suppliers = state.suppliers, onAdd = viewModel::onAddSupplier, onDelete = viewModel::onDeleteSupplier)
            }

            item {
                ReminderRuleSection(
                    rules = state.reminderRules,
                    categories = state.categories,
                    onAdd = viewModel::onAddReminderRule,
                    onDelete = viewModel::onDeleteReminderRule
                )
            }

            item {
                UsersSection(users = state.users, onAdd = viewModel::onAddEmployee, onDelete = viewModel::onDeleteUser)
            }

            item {
                OutlinedButton(onClick = viewModel::onLogout, modifier = Modifier.fillMaxWidth()) { Text("Sair") }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun NameListSection(title: String, items: List<Pair<Long, String>>, onAdd: (String) -> Unit, onDelete: (Long) -> Unit) {
    var newName by remember { mutableStateOf("") }
    Column {
        SectionHeader(title)
        items.forEach { (id, name) ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(name, style = MaterialTheme.typography.bodyLarge)
                IconButton(onClick = { onDelete(id) }) { Icon(Icons.Filled.Delete, contentDescription = "Remover") }
            }
        }
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            OutlinedTextField(value = newName, onValueChange = { newName = it }, placeholder = { Text("Adicionar novo") }, modifier = Modifier.weight(1f), singleLine = true)
            TextButton(onClick = { onAdd(newName); newName = "" }) { Text("Adicionar") }
        }
    }
}

@Composable
private fun SupplierSection(suppliers: List<Supplier>, onAdd: (String, String) -> Unit, onDelete: (Long) -> Unit) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    Column {
        SectionHeader("Fornecedores")
        suppliers.forEach { supplier ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column { Text(supplier.name); supplier.phone?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
                IconButton(onClick = { onDelete(supplier.id) }) { Icon(Icons.Filled.Delete, contentDescription = "Remover") }
            }
        }
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nome") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Spacer(Modifier.height(4.dp))
        Row {
            OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Telefone") }, modifier = Modifier.weight(1f), singleLine = true)
            TextButton(onClick = { onAdd(name, phone); name = ""; phone = "" }) { Text("Adicionar") }
        }
    }
}

@Composable
private fun ReminderRuleSection(
    rules: List<ReminderRule>,
    categories: List<com.beautymanager.app.domain.model.Category>,
    onAdd: (Long, Int, String) -> Unit,
    onDelete: (Long) -> Unit
) {
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    var days by remember { mutableStateOf("") }
    var template by remember { mutableStateOf("Olá, {cliente}! Faz um tempo desde sua última compra de {produto}. Temos novidades — quer dar uma olhada?") }
    var expanded by remember { mutableStateOf(false) }
    val categoryNameById = remember(categories) { categories.associateBy { it.id } }

    Column {
        SectionHeader("Lembretes de recompra")
        Text(
            "Configure, por categoria, depois de quantos dias sem comprar um lembrete deve aparecer.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        rules.forEach { rule ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${categoryNameById[rule.categoryId]?.name ?: "?"}: ${rule.daysThreshold} dias")
                IconButton(onClick = { onDelete(rule.id) }) { Icon(Icons.Filled.Delete, contentDescription = "Remover") }
            }
        }
        Spacer(Modifier.height(8.dp))
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = categoryNameById[selectedCategoryId]?.name ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text("Categoria") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                categories.forEach { category ->
                    DropdownMenuItem(text = { Text(category.name) }, onClick = { selectedCategoryId = category.id; expanded = false })
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = days, onValueChange = { days = it }, label = { Text("Dias") },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(value = template, onValueChange = { template = it }, label = { Text("Mensagem (use {cliente} e {produto})") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
        Spacer(Modifier.height(4.dp))
        TextButton(onClick = {
            val categoryId = selectedCategoryId ?: return@TextButton
            val d = days.toIntOrNull() ?: return@TextButton
            onAdd(categoryId, d, template)
            days = ""
        }) { Text("Adicionar regra") }
    }
}

@Composable
private fun UsersSection(users: List<AppUser>, onAdd: (String, String) -> Unit, onDelete: (Long) -> Unit) {
    var name by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    Column {
        SectionHeader("Usuários")
        users.forEach { user ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${user.name} (${user.role.name})")
                if (user.role != com.beautymanager.app.domain.model.UserRole.ADMIN) {
                    IconButton(onClick = { onDelete(user.id) }) { Icon(Icons.Filled.Delete, contentDescription = "Remover") }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nome do funcionário") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Spacer(Modifier.height(4.dp))
        Row {
            OutlinedTextField(
                value = pin, onValueChange = { pin = it }, label = { Text("PIN (4-6 dígitos)") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.weight(1f), singleLine = true
            )
            TextButton(onClick = { onAdd(name, pin); name = ""; pin = "" }) { Text("Adicionar") }
        }
    }
}
