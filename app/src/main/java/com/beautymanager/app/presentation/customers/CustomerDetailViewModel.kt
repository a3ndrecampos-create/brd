package com.beautymanager.app.presentation.customers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beautymanager.app.domain.model.Customer
import com.beautymanager.app.domain.model.Sale
import com.beautymanager.app.domain.repository.CustomerRepository
import com.beautymanager.app.domain.repository.SaleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CustomerDetailUiState(
    val customer: Customer? = null,
    val sales: List<Sale> = emptyList(),
    val isLoading: Boolean = true
) {
    val totalSpent: Double get() = sales.sumOf { it.totalAmount }
    val purchaseCount: Int get() = sales.size
    val averageTicket: Double get() = if (sales.isEmpty()) 0.0 else totalSpent / sales.size
    val lastPurchase: Sale? get() = sales.maxByOrNull { it.dateTimeEpochMillis }
    val firstPurchase: Sale? get() = sales.minByOrNull { it.dateTimeEpochMillis }
}

@HiltViewModel
class CustomerDetailViewModel @Inject constructor(
    private val customerRepository: CustomerRepository,
    private val saleRepository: SaleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CustomerDetailUiState())
    val uiState: StateFlow<CustomerDetailUiState> = _uiState.asStateFlow()

    fun load(customerId: Long) {
        viewModelScope.launch {
            val customer = customerRepository.getById(customerId)
            _uiState.value = _uiState.value.copy(customer = customer)
            saleRepository.observeForCustomer(customerId).collectLatest { sales ->
                _uiState.value = _uiState.value.copy(sales = sales, isLoading = false)
            }
        }
    }

    fun onUpdateCustomer(name: String, phone: String, whatsapp: String, birthDateEpochMillis: Long?, address: String) {
        val current = _uiState.value.customer ?: return
        if (name.isBlank()) return
        viewModelScope.launch {
            val updated = current.copy(
                name = name,
                phone = phone.ifBlank { null },
                whatsapp = whatsapp.ifBlank { null },
                birthDateEpochMillis = birthDateEpochMillis,
                address = address.ifBlank { null }
            )
            customerRepository.upsert(updated)
            _uiState.value = _uiState.value.copy(customer = updated)
        }
    }
}
