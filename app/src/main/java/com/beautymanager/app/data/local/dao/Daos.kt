package com.beautymanager.app.data.local.dao

import androidx.room.*
import com.beautymanager.app.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY name")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Upsert
    suspend fun upsert(entity: CategoryEntity): Long

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface BrandDao {
    @Query("SELECT * FROM brands ORDER BY name")
    fun observeAll(): Flow<List<BrandEntity>>

    @Upsert
    suspend fun upsert(entity: BrandEntity): Long

    @Query("DELETE FROM brands WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface SupplierDao {
    @Query("SELECT * FROM suppliers ORDER BY name")
    fun observeAll(): Flow<List<SupplierEntity>>

    @Upsert
    suspend fun upsert(entity: SupplierEntity): Long

    @Query("DELETE FROM suppliers WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface ProductDao {
    @Query("""
        SELECT * FROM products
        WHERE (:query IS NULL OR name LIKE '%' || :query || '%' OR barcode LIKE '%' || :query || '%')
        ORDER BY name
    """)
    fun observeAll(query: String?): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE quantity <= minStock ORDER BY name")
    fun observeLowStock(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getById(id: Long): ProductEntity?

    @Query("SELECT * FROM products WHERE barcode = :barcode LIMIT 1")
    suspend fun getByBarcode(barcode: String): ProductEntity?

    @Upsert
    suspend fun upsert(entity: ProductEntity): Long

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE products SET quantity = quantity + :delta WHERE id = :productId")
    suspend fun applyStockDelta(productId: Long, delta: Int)

    @Query("""
        UPDATE products SET
            totalSold = totalSold + :quantitySold,
            totalProfit = totalProfit + :profit,
            lastSaleAtEpochMillis = :saleDateEpochMillis
        WHERE id = :productId
    """)
    suspend fun registerSaleImpact(productId: Long, quantitySold: Int, profit: Double, saleDateEpochMillis: Long)

    @Query("""
        SELECT products.* FROM products
        INNER JOIN sale_items ON sale_items.productId = products.id
        INNER JOIN sales ON sales.id = sale_items.saleId
        WHERE sales.dateTimeEpochMillis >= :sinceEpochMillis
        GROUP BY products.id
        ORDER BY SUM(sale_items.quantity) DESC
        LIMIT :limit
    """)
    suspend fun getTopProducts(limit: Int, sinceEpochMillis: Long): List<ProductEntity>
}

@Dao
interface StockMovementDao {
    @Query("SELECT * FROM stock_movements WHERE productId = :productId ORDER BY dateTimeEpochMillis DESC")
    fun observeForProduct(productId: Long): Flow<List<StockMovementEntity>>

    @Query("SELECT * FROM stock_movements ORDER BY dateTimeEpochMillis DESC")
    fun observeAll(): Flow<List<StockMovementEntity>>

    @Insert
    suspend fun insert(entity: StockMovementEntity)
}

@Dao
interface CustomerDao {
    @Query("""
        SELECT * FROM customers
        WHERE (:query IS NULL OR name LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%' OR whatsapp LIKE '%' || :query || '%')
        ORDER BY name
    """)
    fun observeAll(query: String?): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun getById(id: Long): CustomerEntity?

    @Upsert
    suspend fun upsert(entity: CustomerEntity): Long

    @Query("DELETE FROM customers WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("""
        SELECT * FROM customers
        WHERE birthDateEpochMillis IS NOT NULL
        AND CAST(strftime('%m', birthDateEpochMillis / 1000, 'unixepoch') AS INTEGER) = :month
    """)
    suspend fun getBirthdaysInMonth(month: Int): List<CustomerEntity>
}

@Dao
interface SaleDao {
    @Query("SELECT * FROM sales ORDER BY dateTimeEpochMillis DESC")
    fun observeAll(): Flow<List<SaleEntity>>

    @Query("SELECT * FROM sales WHERE customerId = :customerId ORDER BY dateTimeEpochMillis DESC")
    fun observeForCustomer(customerId: Long): Flow<List<SaleEntity>>

    @Insert
    suspend fun insert(entity: SaleEntity): Long

    @Query("SELECT COALESCE(SUM(totalAmount), 0.0) FROM sales WHERE dateTimeEpochMillis >= :start AND dateTimeEpochMillis < :end")
    suspend fun getTotalSoldBetween(start: Long, end: Long): Double

    @Query("SELECT COALESCE(SUM(totalProfit), 0.0) FROM sales WHERE dateTimeEpochMillis >= :start AND dateTimeEpochMillis < :end")
    suspend fun getTotalProfitBetween(start: Long, end: Long): Double

    @Query("SELECT COUNT(*) FROM sales WHERE dateTimeEpochMillis >= :start AND dateTimeEpochMillis < :end")
    suspend fun getSalesCountBetween(start: Long, end: Long): Int
}

@Dao
interface SaleItemDao {
    @Query("SELECT * FROM sale_items WHERE saleId = :saleId")
    fun observeForSale(saleId: Long): Flow<List<SaleItemEntity>>

    @Insert
    suspend fun insertAll(items: List<SaleItemEntity>)
}

@Dao
interface ReminderRuleDao {
    @Query("SELECT * FROM reminder_rules ORDER BY daysThreshold")
    fun observeAll(): Flow<List<ReminderRuleEntity>>

    @Upsert
    suspend fun upsert(entity: ReminderRuleEntity): Long

    @Query("DELETE FROM reminder_rules WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders WHERE status = 'PENDENTE' ORDER BY daysSincePurchase DESC")
    fun observePending(): Flow<List<ReminderEntity>>

    @Upsert
    suspend fun upsert(entity: ReminderEntity): Long

    @Query("UPDATE reminders SET status = :status WHERE id = :id")
    suspend fun markStatus(id: Long, status: String)

    /**
     * Última compra de cada cliente por categoria de produto — base para decidir se um
     * novo lembrete deve ser gerado. Retorna: clienteId, categoriaId, produtoId da última
     * compra e a data dessa compra.
     */
    @Query("""
        SELECT
            sales.customerId AS customerId,
            products.categoryId AS categoryId,
            sale_items.productId AS productId,
            MAX(sales.dateTimeEpochMillis) AS lastPurchaseEpochMillis
        FROM sales
        INNER JOIN sale_items ON sale_items.saleId = sales.id
        INNER JOIN products ON products.id = sale_items.productId
        WHERE sales.customerId IS NOT NULL AND products.categoryId IS NOT NULL
        GROUP BY sales.customerId, products.categoryId
    """)
    suspend fun getLastPurchasePerCustomerCategory(): List<LastPurchaseRow>
}

data class LastPurchaseRow(
    val customerId: Long,
    val categoryId: Long,
    val productId: Long,
    val lastPurchaseEpochMillis: Long
)

@Dao
interface UserDao {
    @Query("SELECT * FROM users ORDER BY name")
    fun observeAll(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users")
    suspend fun getAll(): List<UserEntity>

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getById(id: Long): UserEntity?

    @Query("SELECT COUNT(*) FROM users")
    suspend fun count(): Int

    @Upsert
    suspend fun upsert(entity: UserEntity): Long

    @Query("DELETE FROM users WHERE id = :id")
    suspend fun delete(id: Long)
}
