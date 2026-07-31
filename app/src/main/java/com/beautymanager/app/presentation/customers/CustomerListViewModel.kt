package com.beautymanager.app.presentation.customers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beautymanager.app.domain.model.Customer
import com.beautymanager.app.domain.repository.CustomerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CustomerListUiState(
    val query: String = "",
    val customers: List<Customer> = emptyList(),
    val isAddDialogOpen: Boolean = false
)

@HiltViewModel
class CustomerListViewModel @Inject constructor(
    private val customerRepository: CustomerRepository
) : ViewModel() {

    private val queryFlow = MutableStateFlow("")
    private val dialogOpenFlow = MutableStateFlow(false)

    val uiState: StateFlow<CustomerListUiState> = queryFlow
        .flatMapLatest { query -> customerRepository.observeAll(query.ifBlank { null }) }
        .combine(queryFlow) { customers, query -> customers to query }
        .combine(dialogOpenFlow) { (customers, query), dialogOpen ->
            CustomerListUiState(query = query, customers = customers, isAddDialogOpen = dialogOpen)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CustomerListUiState())

    fun onQueryChange(query: String) { queryFlow.value = query }
    fun onOpenAddDialog() { dialogOpenFlow.value = true }
    fun onDismissAddDialog() { dialogOpenFlow.value = false }

    fun onAddCustomer(name: String, phone: String, whatsapp: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            customerRepository.upsert(
                Customer(
                    name = name,
                    phone = phone.ifBlank { null },
                    whatsapp = whatsapp.ifBlank { phone.ifBlank { null } },
                    createdAtEpochMillis = System.currentTimeMillis()
                )
            )
            dialogOpenFlow.value = false
        }
    }
}
