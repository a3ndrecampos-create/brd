package com.beautymanager.app.data.repository

import com.beautymanager.app.data.local.dao.*
import com.beautymanager.app.data.local.entity.*
import com.beautymanager.app.domain.model.*
import com.beautymanager.app.domain.repository.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CategoryRepositoryImpl @Inject constructor(
    private val dao: CategoryDao
) : CategoryRepository {
    override fun observeAll(): Flow<List<Category>> = dao.observeAll().map { list -> list.map { it.toDomain() } }
    override suspend fun upsert(category: Category): Long = dao.upsert(category.toEntity())
    override suspend fun delete(id: Long) = dao.delete(id)
}

class BrandRepositoryImpl @Inject constructor(
    private val dao: BrandDao
) : BrandRepository {
    override fun observeAll(): Flow<List<Brand>> = dao.observeAll().map { list -> list.map { it.toDomain() } }
    override suspend fun upsert(brand: Brand): Long = dao.upsert(brand.toEntity())
    override suspend fun delete(id: Long) = dao.delete(id)
}

class SupplierRepositoryImpl @Inject constructor(
    private val dao: SupplierDao
) : SupplierRepository {
    override fun observeAll(): Flow<List<Supplier>> = dao.observeAll().map { list -> list.map { it.toDomain() } }
    override suspend fun upsert(supplier: Supplier): Long = dao.upsert(supplier.toEntity())
    override suspend fun delete(id: Long) = dao.delete(id)
}

class ProductRepositoryImpl @Inject constructor(
    private val dao: ProductDao
) : ProductRepository {
    override fun observeAll(query: String?): Flow<List<Product>> =
        dao.observeAll(query).map { list -> list.map { it.toDomain() } }

    override fun observeLowStock(): Flow<List<Product>> =
        dao.observeLowStock().map { list -> list.map { it.toDomain() } }

    override suspend fun getById(id: Long): Product? = dao.getById(id)?.toDomain()

    override suspend fun getByBarcode(barcode: String): Product? = dao.getByBarcode(barcode)?.toDomain()

    override suspend fun upsert(product: Product): Long = dao.upsert(product.toEntity())

    override suspend fun delete(id: Long) = dao.delete(id)

    override suspend fun applyStockDelta(productId: Long, delta: Int) = dao.applyStockDelta(productId, delta)

    override suspend fun registerSaleImpact(productId: Long, quantitySold: Int, profit: Double, saleDateEpochMillis: Long) =
        dao.registerSaleImpact(productId, quantitySold, profit, saleDateEpochMillis)
}

class StockMovementRepositoryImpl @Inject constructor(
    private val dao: StockMovementDao
) : StockMovementRepository {
    override fun observeForProduct(productId: Long): Flow<List<StockMovement>> =
        dao.observeForProduct(productId).map { list -> list.map { it.toDomain() } }

    override fun observeAll(): Flow<List<StockMovement>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun record(movement: StockMovement) = dao.insert(movement.toEntity())
}

// --- Mappers Entity <-> Domain ---

private fun CategoryEntity.toDomain() = Category(id, name)
private fun Category.toEntity() = CategoryEntity(id, name)

private fun BrandEntity.toDomain() = Brand(id, name)
private fun Brand.toEntity() = BrandEntity(id, name)

private fun SupplierEntity.toDomain() = Supplier(id, name, phone, notes)
private fun Supplier.toEntity() = SupplierEntity(id, name, phone, notes)

private fun ProductEntity.toDomain() = Product(
    id = id, barcode = barcode, name = name, brandId = brandId, categoryId = categoryId,
    supplierId = supplierId, photoUri = photoUri, costPrice = costPrice, salePrice = salePrice,
    quantity = quantity, minStock = minStock, notes = notes, createdAtEpochMillis = createdAtEpochMillis,
    lastSaleAtEpochMillis = lastSaleAtEpochMillis, totalSold = totalSold, totalProfit = totalProfit
)

private fun Product.toEntity() = ProductEntity(
    id = id, barcode = barcode, name = name, brandId = brandId, categoryId = categoryId,
    supplierId = supplierId, photoUri = photoUri, costPrice = costPrice, salePrice = salePrice,
    quantity = quantity, minStock = minStock, notes = notes, createdAtEpochMillis = createdAtEpochMillis,
    lastSaleAtEpochMillis = lastSaleAtEpochMillis, totalSold = totalSold, totalProfit = totalProfit
)

private fun StockMovementEntity.toDomain() = StockMovement(
    id = id, productId = productId, type = StockMovementType.valueOf(type),
    quantity = quantity, dateTimeEpochMillis = dateTimeEpochMillis, notes = notes
)

private fun StockMovement.toEntity() = StockMovementEntity(
    id = id, productId = productId, type = type.name,
    quantity = quantity, dateTimeEpochMillis = dateTimeEpochMillis, notes = notes
)
