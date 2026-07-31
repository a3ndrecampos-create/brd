package com.beautymanager.app.domain.model

/** Categoria de produto (ex.: Perfumaria, Skincare, Cabelo). Usada também para regras de lembrete. */
data class Category(
    val id: Long = 0,
    val name: String
)

data class Brand(
    val id: Long = 0,
    val name: String
)

data class Supplier(
    val id: Long = 0,
    val name: String,
    val phone: String? = null,
    val notes: String? = null
)

/**
 * Produto cadastrado na loja. Os campos "totalSold"/"totalProfit"/"lastSaleAt" são
 * atualizados automaticamente pelo RegisterSaleUseCase a cada venda, evitando
 * recalcular tudo via agregação pesada toda vez que uma tela precisa desses números.
 */
data class Product(
    val id: Long = 0,
    val barcode: String?,
    val name: String,
    val brandId: Long?,
    val categoryId: Long?,
    val supplierId: Long?,
    val photoUri: String? = null,
    val costPrice: Double,
    val salePrice: Double,
    val quantity: Int,
    val minStock: Int,
    val notes: String? = null,
    val createdAtEpochMillis: Long,
    val lastSaleAtEpochMillis: Long? = null,
    val totalSold: Int = 0,
    val totalProfit: Double = 0.0
) {
    /** Margem de lucro percentual sobre o preço de venda. */
    val marginPercent: Double
        get() = if (salePrice <= 0) 0.0 else ((salePrice - costPrice) / salePrice) * 100.0

    val isLowStock: Boolean
        get() = quantity <= minStock
}

enum class StockMovementType { ENTRADA, SAIDA, AJUSTE, TRANSFERENCIA }

data class StockMovement(
    val id: Long = 0,
    val productId: Long,
    val type: StockMovementType,
    val quantity: Int,
    val dateTimeEpochMillis: Long,
    val notes: String? = null
)

data class Customer(
    val id: Long = 0,
    val name: String,
    val phone: String? = null,
    val whatsapp: String? = null,
    val birthDateEpochMillis: Long? = null,
    val address: String? = null,
    val notes: String? = null,
    val favoriteBrandId: Long? = null,
    val createdAtEpochMillis: Long
)

enum class PaymentMethod { DINHEIRO, PIX, CARTAO, MULTIPLO }

data class Sale(
    val id: Long = 0,
    val customerId: Long?,
    val sellerUserId: Long,
    val dateTimeEpochMillis: Long,
    val discount: Double = 0.0,
    val paymentMethod: PaymentMethod,
    val totalAmount: Double,
    val totalProfit: Double
)

data class SaleItem(
    val id: Long = 0,
    val saleId: Long,
    val productId: Long,
    val quantity: Int,
    val unitPrice: Double,
    val unitCost: Double
)

/** Item de carrinho usado apenas na tela de venda (não persistido diretamente). */
data class CartItem(
    val product: Product,
    val quantity: Int
) {
    val subtotal: Double get() = product.salePrice * quantity
}

enum class ReminderStatus { PENDENTE, ENVIADO, IGNORADO }

/**
 * Regra configurável por categoria: "depois de N dias sem comprar um produto dessa
 * categoria, lembrar o cliente". Editável em Configurações > Lembretes.
 */
data class ReminderRule(
    val id: Long = 0,
    val categoryId: Long,
    val daysThreshold: Int,
    val messageTemplate: String
)

data class Reminder(
    val id: Long = 0,
    val customerId: Long,
    val productId: Long,
    val ruleId: Long,
    val daysSincePurchase: Int,
    val status: ReminderStatus,
    val generatedAtEpochMillis: Long
)

enum class UserRole { ADMIN, FUNCIONARIO }

/** Usuário do sistema (dono/administrador ou funcionário), com PIN próprio e permissões. */
data class AppUser(
    val id: Long = 0,
    val name: String,
    val role: UserRole,
    val canManageProducts: Boolean = true,
    val canManageSales: Boolean = true,
    val canViewReports: Boolean = false,
    val canManageUsers: Boolean = false
)

/** Snapshot de métricas do Dashboard, calculado sob demanda pelo DashboardMetricsUseCase. */
data class DashboardMetrics(
    val totalSoldToday: Double,
    val totalSoldThisMonth: Double,
    val salesCountToday: Int,
    val productsSoldToday: Int,
    val profitToday: Double,
    val lowStockCount: Int,
    val customersNeedingContactCount: Int,
    val birthdaysThisMonthCount: Int,
    val topProducts: List<Product>
)
