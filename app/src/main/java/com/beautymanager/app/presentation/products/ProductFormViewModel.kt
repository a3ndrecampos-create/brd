package com.beautymanager.app.presentation.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beautymanager.app.domain.model.Brand
import com.beautymanager.app.domain.model.Category
import com.beautymanager.app.domain.model.Product
import com.beautymanager.app.domain.model.Supplier
import com.beautymanager.app.domain.repository.BrandRepository
import com.beautymanager.app.domain.repository.CategoryRepository
import com.beautymanager.app.domain.repository.ProductRepository
import com.beautymanager.app.domain.repository.SupplierRepository
import com.beautymanager.app.domain.usecase.BarcodeLookupResult
import com.beautymanager.app.domain.usecase.LookupProductByBarcodeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProductFormState(
    val id: Long = 0,
    val barcode: String = "",
    val name: String = "",
    val brandId: Long? = null,
    val categoryId: Long? = null,
    val supplierId: Long? = null,
    val photoUri: String? = null,
    val costPrice: String = "",
    val salePrice: String = "",
    val quantity: String = "0",
    val minStock: String = "3",
    val notes: String = "",
    val isLookingUpBarcode: Boolean = false,
    val barcodeLookupMessage: String? = null,
    val isSaved: Boolean = false
) {
    val marginPercent: Double
        get() {
            val cost = costPrice.toDoubleOrNull() ?: return 0.0
            val sale = salePrice.toDoubleOrNull() ?: return 0.0
            return if (sale <= 0) 0.0 else ((sale - cost) / sale) * 100.0
        }
}

data class ProductFormOptions(
    val categories: List<Category> = emptyList(),
    val brands: List<Brand> = emptyList(),
    val suppliers: List<Supplier> = emptyList()
)

@HiltViewModel
class ProductFormViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val categoryRepository: CategoryRepository,
    private val brandRepository: BrandRepository,
    private val supplierRepository: SupplierRepository,
    private val lookupProductByBarcodeUseCase: LookupProductByBarcodeUseCase
) : ViewModel() {

    private val _formState = MutableStateFlow(ProductFormState())
    val formState: StateFlow<ProductFormState> = _formState.asStateFlow()

    val options: StateFlow<ProductFormOptions> = combine(
        categoryRepository.observeAll(), brandRepository.observeAll(), supplierRepository.observeAll()
    ) { categories, brands, suppliers -> ProductFormOptions(categories, brands, suppliers) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProductFormOptions())

    fun loadProduct(productId: Long) {
        viewModelScope.launch {
            val product = productRepository.getById(productId) ?: return@launch
            _formState.value = product.toFormState()
        }
    }

    fun onFieldChange(update: (ProductFormState) -> ProductFormState) {
        _formState.value = update(_formState.value)
    }

    /**
     * Chamado quando o scanner de câmera (CameraX + ML Kit — ver ProductFormScreen)
     * detecta um código de barras, ou quando o usuário digita manualmente.
     */
    fun onBarcodeScanned(barcode: String) {
        _formState.value = _formState.value.copy(barcode = barcode, isLookingUpBarcode = true, barcodeLookupMessage = null)
        viewModelScope.launch {
            when (val result = lookupProductByBarcodeUseCase(barcode)) {
                is BarcodeLookupResult.AlreadyRegistered -> {
                    _formState.value = result.product.toFormState().copy(
                        isLookingUpBarcode = false,
                        barcodeLookupMessage = "Este produto já está cadastrado — abrindo para edição."
                    )
                }
                is BarcodeLookupResult.SuggestionFound -> {
                    _formState.value = _formState.value.copy(
                        barcode = result.barcode,
                        name = result.info.name ?: _formState.value.name,
                        photoUri = result.info.imageUrl ?: _formState.value.photoUri,
                        isLookingUpBarcode = false,
                        barcodeLookupMessage = "Dados encontrados na base pública — confira antes de salvar."
                    )
                    // Nota: result.info.brandName vem como texto livre (ex.: "Natura, Avon");
                    // TODO: casar/criar a marca correspondente em brandRepository automaticamente.
                }
                is BarcodeLookupResult.NotFound -> {
                    _formState.value = _formState.value.copy(
                        isLookingUpBarcode = false,
                        barcodeLookupMessage = "Código novo — preencha os dados manualmente. Não será pedido de novo."
                    )
                }
            }
        }
    }

    fun onSave() {
        val state = _formState.value
        val cost = state.costPrice.toDoubleOrNull() ?: return
        val sale = state.salePrice.toDoubleOrNull() ?: return
        val quantity = state.quantity.toIntOrNull() ?: 0
        val minStock = state.minStock.toIntOrNull() ?: 0
        if (state.name.isBlank()) return

        viewModelScope.launch {
            productRepository.upsert(
                Product(
                    id = state.id,
                    barcode = state.barcode.ifBlank { null },
                    name = state.name,
                    brandId = state.brandId,
                    categoryId = state.categoryId,
                    supplierId = state.supplierId,
                    photoUri = state.photoUri,
                    costPrice = cost,
                    salePrice = sale,
                    quantity = quantity,
                    minStock = minStock,
                    notes = state.notes.ifBlank { null },
                    createdAtEpochMillis = System.currentTimeMillis()
                )
            )
            _formState.value = _formState.value.copy(isSaved = true)
        }
    }
}

private fun Product.toFormState() = ProductFormState(
    id = id,
    barcode = barcode ?: "",
    name = name,
    brandId = brandId,
    categoryId = categoryId,
    supplierId = supplierId,
    photoUri = photoUri,
    costPrice = costPrice.toString(),
    salePrice = salePrice.toString(),
    quantity = quantity.toString(),
    minStock = minStock.toString(),
    notes = notes ?: ""
)
