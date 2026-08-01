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

    fun onAddCustomer(
        name: String,
        phone: String,
        whatsapp: String,
        zipCode: String,
        street: String,
        number: String,
        complement: String,
        neighborhood: String,
        city: String,
        state: String
    ) {
        if (name.isBlank()) return
        viewModelScope.launch {
            customerRepository.upsert(
                Customer(
                    name = name,
                    phone = phone.ifBlank { null },
                    whatsapp = whatsapp.ifBlank { phone.ifBlank { null } },
                    address = buildFullAddress(zipCode, street, number, complement, neighborhood, city, state),
                    createdAtEpochMillis = System.currentTimeMillis()
                )
            )
            dialogOpenFlow.value = false
        }
    }
}

/** Monta um endereço completo e legível a partir dos campos separados do formulário. */
fun buildFullAddress(
    zipCode: String, street: String, number: String, complement: String,
    neighborhood: String, city: String, state: String
): String? {
    val streetLine = street.ifBlank { null }?.let { if (number.isNotBlank()) "$it, $number" else it }
    val cityStateLine = listOfNotNull(city.ifBlank { null }, state.ifBlank { null }).joinToString(" - ").ifBlank { null }
    val parts = listOfNotNull(
        streetLine,
        complement.ifBlank { null },
        neighborhood.ifBlank { null },
        cityStateLine,
        zipCode.ifBlank { null }?.let { "CEP $it" }
    )
    return parts.joinToString(", ").ifBlank { null }
}
