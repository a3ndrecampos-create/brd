package com.beautymanager.app.domain.repository

import com.beautymanager.app.domain.model.*
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun observeAll(): Flow<List<Category>>
    suspend fun upsert(category: Category): Long
    suspend fun delete(id: Long)
}

interface BrandRepository {
    fun observeAll(): Flow<List<Brand>>
    suspend fun upsert(brand: Brand): Long
    suspend fun delete(id: Long)
}

interface SupplierRepository {
    fun observeAll(): Flow<List<Supplier>>
    suspend fun upsert(supplier: Supplier): Long
    suspend fun delete(id: Long)
}

interface ProductRepository {
    fun observeAll(query: String? = null): Flow<List<Product>>
    fun observeLowStock(): Flow<List<Product>>
    suspend fun getById(id: Long): Product?
    suspend fun getByBarcode(barcode: String): Product?
    suspend fun upsert(product: Product): Long
    suspend fun delete(id: Long)
    suspend fun applyStockDelta(productId: Long, delta: Int)
    suspend fun registerSaleImpact(productId: Long, quantitySold: Int, profit: Double, saleDateEpochMillis: Long)
}

interface StockMovementRepository {
    fun observeForProduct(productId: Long): Flow<List<StockMovement>>
    fun observeAll(): Flow<List<StockMovement>>
    suspend fun record(movement: StockMovement)
}

interface CustomerRepository {
    fun observeAll(query: String? = null): Flow<List<Customer>>
    suspend fun getById(id: Long): Customer?
    suspend fun upsert(customer: Customer): Long
    suspend fun delete(id: Long)
    suspend fun getBirthdaysInMonth(month: Int): List<Customer>
}

interface SaleRepository {
    fun observeAll(): Flow<List<Sale>>
    fun observeForCustomer(customerId: Long): Flow<List<Sale>>
    fun observeItemsForSale(saleId: Long): Flow<List<SaleItem>>
    suspend fun registerSale(sale: Sale, items: List<SaleItem>): Long
    suspend fun getTotalSoldBetween(startEpochMillis: Long, endEpochMillis: Long): Double
    suspend fun getTotalProfitBetween(startEpochMillis: Long, endEpochMillis: Long): Double
    suspend fun getSalesCountBetween(startEpochMillis: Long, endEpochMillis: Long): Int
    suspend fun getTopProducts(limit: Int, sinceEpochMillis: Long): List<Product>
}

interface ReminderRuleRepository {
    fun observeAll(): Flow<List<ReminderRule>>
    suspend fun upsert(rule: ReminderRule): Long
    suspend fun delete(id: Long)
}

interface ReminderRepository {
    fun observePending(): Flow<List<Reminder>>
    suspend fun upsert(reminder: Reminder): Long
    suspend fun markStatus(id: Long, status: ReminderStatus)
    suspend fun regenerateFromRules(rules: List<ReminderRule>, now: Long)
}

interface UserRepository {
    fun observeAll(): Flow<List<AppUser>>
    suspend fun getById(id: Long): AppUser?
    suspend fun upsert(user: AppUser, pin: String? = null): Long
    suspend fun delete(id: Long)
    suspend fun verifyPin(userId: Long, pin: String): Boolean
}

/** Autenticação da sessão atual (quem está logado agora + preferências de biometria/tema). */
interface SessionRepository {
    suspend fun isAnyUserConfigured(): Boolean
    suspend fun loginWithPin(pin: String): AppUser?
    fun observeCurrentUser(): Flow<AppUser?>
    suspend fun logout()
    suspend fun isBiometricEnabled(): Boolean
    suspend fun setBiometricEnabled(enabled: Boolean)
    fun observeThemeMode(): Flow<ThemeMode>
    suspend fun setThemeMode(mode: ThemeMode)
}

enum class ThemeMode { CLARO, ESCURO, SISTEMA }

/** Consulta pública de metadados de produto por código de barras (Open Food Facts). */
interface BarcodeLookupRepository {
    suspend fun lookup(barcode: String): BarcodeProductInfo?
}

data class BarcodeProductInfo(
    val name: String?,
    val brandName: String?,
    val imageUrl: String?
)
