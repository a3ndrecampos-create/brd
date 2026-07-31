package com.beautymanager.app.presentation.stock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beautymanager.app.domain.model.Product
import com.beautymanager.app.domain.model.StockMovement
import com.beautymanager.app.domain.model.StockMovementType
import com.beautymanager.app.domain.repository.ProductRepository
import com.beautymanager.app.domain.repository.StockMovementRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StockUiState(
    val movements: List<StockMovement> = emptyList(),
    val products: List<Product> = emptyList(),
    val isFormOpen: Boolean = false
)

@HiltViewModel
class StockViewModel @Inject constructor(
    private val stockMovementRepository: StockMovementRepository,
    private val productRepository: ProductRepository
) : ViewModel() {

    private val formOpenFlow = MutableStateFlow(false)

    val uiState: StateFlow<StockUiState> = combine(
        stockMovementRepository.observeAll(), productRepository.observeAll(), formOpenFlow
    ) { movements, products, formOpen -> StockUiState(movements, products, formOpen) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StockUiState())

    fun onOpenForm() { formOpenFlow.value = true }
    fun onDismissForm() { formOpenFlow.value = false }

    fun onRegisterMovement(productId: Long, type: StockMovementType, quantity: Int, notes: String) {
        if (quantity <= 0) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val delta = when (type) {
                StockMovementType.ENTRADA -> quantity
                StockMovementType.SAIDA, StockMovementType.TRANSFERENCIA -> -quantity
                StockMovementType.AJUSTE -> quantity // ajuste pode ser positivo ou negativo; UI envia o sinal já aplicado
            }
            productRepository.applyStockDelta(productId, delta)
            stockMovementRepository.record(
                StockMovement(productId = productId, type = type, quantity = quantity, dateTimeEpochMillis = now, notes = notes.ifBlank { null })
            )
            formOpenFlow.value = false
        }
    }
}
