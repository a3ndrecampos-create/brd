package com.beautymanager.app.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beautymanager.app.domain.model.*
import com.beautymanager.app.domain.repository.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val categories: List<Category> = emptyList(),
    val brands: List<Brand> = emptyList(),
    val suppliers: List<Supplier> = emptyList(),
    val reminderRules: List<ReminderRule> = emptyList(),
    val users: List<AppUser> = emptyList(),
    val themeMode: ThemeMode = ThemeMode.SISTEMA
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val brandRepository: BrandRepository,
    private val supplierRepository: SupplierRepository,
    private val reminderRuleRepository: ReminderRuleRepository,
    private val userRepository: UserRepository,
    private val sessionRepository: SessionRepository
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        categoryRepository.observeAll(), brandRepository.observeAll(), supplierRepository.observeAll(),
        reminderRuleRepository.observeAll(), userRepository.observeAll(), sessionRepository.observeThemeMode()
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        SettingsUiState(
            categories = values[0] as List<Category>,
            brands = values[1] as List<Brand>,
            suppliers = values[2] as List<Supplier>,
            reminderRules = values[3] as List<ReminderRule>,
            users = values[4] as List<AppUser>,
            themeMode = values[5] as ThemeMode
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun onAddCategory(name: String) { if (name.isNotBlank()) viewModelScope.launch { categoryRepository.upsert(Category(name = name)) } }
    fun onDeleteCategory(id: Long) { viewModelScope.launch { categoryRepository.delete(id) } }

    fun onAddBrand(name: String) { if (name.isNotBlank()) viewModelScope.launch { brandRepository.upsert(Brand(name = name)) } }
    fun onDeleteBrand(id: Long) { viewModelScope.launch { brandRepository.delete(id) } }

    fun onAddSupplier(name: String, phone: String) {
        if (name.isBlank()) return
        viewModelScope.launch { supplierRepository.upsert(Supplier(name = name, phone = phone.ifBlank { null })) }
    }
    fun onDeleteSupplier(id: Long) { viewModelScope.launch { supplierRepository.delete(id) } }

    fun onAddReminderRule(categoryId: Long, days: Int, template: String) {
        viewModelScope.launch {
            reminderRuleRepository.upsert(ReminderRule(categoryId = categoryId, daysThreshold = days, messageTemplate = template))
        }
    }
    fun onDeleteReminderRule(id: Long) { viewModelScope.launch { reminderRuleRepository.delete(id) } }

    fun onAddEmployee(name: String, pin: String) {
        if (name.isBlank() || pin.length < 4) return
        viewModelScope.launch {
            userRepository.upsert(
                AppUser(name = name, role = UserRole.FUNCIONARIO, canManageProducts = true, canManageSales = true, canViewReports = false, canManageUsers = false),
                pin = pin
            )
        }
    }
    fun onDeleteUser(id: Long) { viewModelScope.launch { userRepository.delete(id) } }

    fun onThemeModeChange(mode: ThemeMode) { viewModelScope.launch { sessionRepository.setThemeMode(mode) } }

    fun onLogout() { viewModelScope.launch { sessionRepository.logout() } }
}
