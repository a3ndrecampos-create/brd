package com.beautymanager.app.presentation.sales

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beautymanager.app.domain.model.CartItem
import com.beautymanager.app.domain.model.PaymentMethod
import com.beautymanager.app.domain.model.Product
import com.beautymanager.app.domain.repository.CustomerRepository
import com.beautymanager.app.domain.repository.ProductRepository
import com.beautymanager.app.domain.repository.SessionRepository
import com.beautymanager.app.domain.usecase.RegisterSaleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CompletedSaleSummary(
    val saleId: Long,
    val items: List<CartItem>,
    val total: Double,
    val discount: Double,
    val paymentMethod: PaymentMethod,
    val customerName: String?,
    val dateTimeEpochMillis: Long
)

data class PointOfSaleUiState(
    val searchQuery: String = "",
    val searchResults: List<Product> = emptyList(),
    val cartItems: List<CartItem> = emptyList(),
    val discount: String = "0",
    val paymentMethod: PaymentMethod = PaymentMethod.PIX,
    val selectedCustomerId: Long? = null,
    val selectedCustomerName: String? = null,
    val completedSale: CompletedSaleSummary? = null,
    val errorMessage: String? = null
) {
    val subtotal: Double get() = cartItems.sumOf { it.subtotal }
    val total: Double get() = (subtotal - (discount.toDoubleOrNull() ?: 0.0)).coerceAtLeast(0.0)
}

@HiltViewModel
class PointOfSaleViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val customerRepository: CustomerRepository,
    private val sessionRepository: SessionRepository,
    private val registerSaleUseCase: RegisterSaleUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(PointOfSaleUiState())
    val uiState: StateFlow<PointOfSaleUiState> = _uiState.asStateFlow()

    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        viewModelScope.launch {
            if (query.isBlank()) {
                _uiState.value = _uiState.value.copy(searchResults = emptyList())
                return@launch
            }
            // Primeira emissão do Flow já basta para uma busca pontual (evita manter
            // uma segunda coleta contínua só para a barra de busca do PDV).
            val first = kotlinx.coroutines.flow.firstOrNull(productRepository.observeAll(query))
            _uiState.value = _uiState.value.copy(searchResults = first ?: emptyList())
        }
    }

    fun onAddToCart(product: Product) {
        val current = _uiState.value.cartItems
        val existing = current.find { it.product.id == product.id }
        val updated = if (existing != null) {
            current.map { if (it.product.id == product.id) it.copy(quantity = it.quantity + 1) else it }
        } else {
            current + CartItem(product, 1)
        }
        _uiState.value = _uiState.value.copy(cartItems = updated, searchQuery = "", searchResults = emptyList())
    }

    fun onChangeQuantity(productId: Long, quantity: Int) {
        if (quantity <= 0) {
            onRemoveFromCart(productId)
            return
        }
        _uiState.value = _uiState.value.copy(
            cartItems = _uiState.value.cartItems.map { if (it.product.id == productId) it.copy(quantity = quantity) else it }
        )
    }

    fun onRemoveFromCart(productId: Long) {
        _uiState.value = _uiState.value.copy(cartItems = _uiState.value.cartItems.filterNot { it.product.id == productId })
    }

    fun onDiscountChange(discount: String) {
        _uiState.value = _uiState.value.copy(discount = discount)
    }

    fun onPaymentMethodChange(method: PaymentMethod) {
        _uiState.value = _uiState.value.copy(paymentMethod = method)
    }

    fun onSelectCustomer(id: Long?, name: String?) {
        _uiState.value = _uiState.value.copy(selectedCustomerId = id, selectedCustomerName = name)
    }

    fun onCheckout() {
        val state = _uiState.value
        if (state.cartItems.isEmpty()) return
        viewModelScope.launch {
            val seller = kotlinx.coroutines.flow.firstOrNull(sessionRepository.observeCurrentUser())
            if (seller == null) {
                _uiState.value = state.copy(errorMessage = "Sessão expirada — faça login novamente.")
                return@launch
            }
            val result = registerSaleUseCase(
                cartItems = state.cartItems,
                customerId = state.selectedCustomerId,
                sellerUserId = seller.id,
                discount = state.discount.toDoubleOrNull() ?: 0.0,
                paymentMethod = state.paymentMethod
            )
            result.fold(
                onSuccess = { saleId ->
                    _uiState.value = PointOfSaleUiState(
                        completedSale = CompletedSaleSummary(
                            saleId = saleId,
                            items = state.cartItems,
                            total = state.total,
                            discount = state.discount.toDoubleOrNull() ?: 0.0,
                            paymentMethod = state.paymentMethod,
                            customerName = state.selectedCustomerName,
                            dateTimeEpochMillis = System.currentTimeMillis()
                        )
                    )
                },
                onFailure = { e -> _uiState.value = state.copy(errorMessage = e.message) }
            )
        }
    }

    fun onReceiptDismissed() {
        _uiState.value = _uiState.value.copy(completedSale = null)
    }
}
