package com.beautymanager.app.presentation.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beautymanager.app.domain.model.Product
import com.beautymanager.app.domain.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProductListUiState(
    val query: String = "",
    val products: List<Product> = emptyList()
)

@HiltViewModel
class ProductListViewModel @Inject constructor(
    private val productRepository: ProductRepository
) : ViewModel() {

    private val queryFlow = MutableStateFlow("")

    val uiState: StateFlow<ProductListUiState> = queryFlow
        .flatMapLatest { query ->
            productRepository.observeAll(query.ifBlank { null }).combine(queryFlow) { products, q ->
                ProductListUiState(query = q, products = products)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProductListUiState())

    fun onQueryChange(query: String) {
        queryFlow.value = query
    }

    fun onDelete(productId: Long) {
        viewModelScope.launch { productRepository.delete(productId) }
    }
}
