package com.beautymanager.app.data.repository

import androidx.room.withTransaction
import com.beautymanager.app.data.local.dao.*
import com.beautymanager.app.data.local.database.BeautyManagerDatabase
import com.beautymanager.app.data.local.entity.*
import com.beautymanager.app.domain.model.*
import com.beautymanager.app.domain.repository.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class CustomerRepositoryImpl @Inject constructor(
    private val dao: CustomerDao
) : CustomerRepository {
    override fun observeAll(query: String?): Flow<List<Customer>> =
        dao.observeAll(query).map { list -> list.map { it.toDomain() } }

    override suspend fun getById(id: Long): Customer? = dao.getById(id)?.toDomain()

    override suspend fun upsert(customer: Customer): Long = dao.upsert(customer.toEntity())

    override suspend fun delete(id: Long) = dao.delete(id)

    override suspend fun getBirthdaysInMonth(month: Int): List<Customer> =
        dao.getBirthdaysInMonth(month).map { it.toDomain() }
}

/**
 * Persistir venda + itens é a única operação do app que precisa de transação real
 * (se falhar no meio, não pode sobrar item órfão nem venda sem itens) — por isso o
 * repositório injeta a RoomDatabase diretamente para usar withTransaction, além dos DAOs.
 */
class SaleRepositoryImpl @Inject constructor(
    private val database: BeautyManagerDatabase,
    private val saleDao: SaleDao,
    private val saleItemDao: SaleItemDao,
    private val productDao: ProductDao
) : SaleRepository {

    override fun observeAll(): Flow<List<Sale>> = saleDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeForCustomer(customerId: Long): Flow<List<Sale>> =
        saleDao.observeForCustomer(customerId).map { list -> list.map { it.toDomain() } }

    override fun observeItemsForSale(saleId: Long): Flow<List<SaleItem>> =
        saleItemDao.observeForSale(saleId).map { list -> list.map { it.toDomain() } }

    override suspend fun registerSale(sale: Sale, items: List<SaleItem>): Long = database.withTransaction {
        val saleId = saleDao.insert(sale.toEntity())
        saleItemDao.insertAll(items.map { it.copy(saleId = saleId).toEntity() })
        saleId
    }

    override suspend fun getTotalSoldBetween(startEpochMillis: Long, endEpochMillis: Long): Double =
        saleDao.getTotalSoldBetween(startEpochMillis, endEpochMillis)

    override suspend fun getTotalProfitBetween(startEpochMillis: Long, endEpochMillis: Long): Double =
        saleDao.getTotalProfitBetween(startEpochMillis, endEpochMillis)

    override suspend fun getSalesCountBetween(startEpochMillis: Long, endEpochMillis: Long): Int =
        saleDao.getSalesCountBetween(startEpochMillis, endEpochMillis)

    override suspend fun getTopProducts(limit: Int, sinceEpochMillis: Long): List<Product> =
        productDao.getTopProducts(limit, sinceEpochMillis).map { it.toProductDomain() }
}

class ReminderRuleRepositoryImpl @Inject constructor(
    private val dao: ReminderRuleDao
) : ReminderRuleRepository {
    override fun observeAll(): Flow<List<ReminderRule>> = dao.observeAll().map { list -> list.map { it.toDomain() } }
    override suspend fun upsert(rule: ReminderRule): Long = dao.upsert(rule.toEntity())
    override suspend fun delete(id: Long) = dao.delete(id)
}

/**
 * Regra: para cada par (cliente, categoria), olha a última compra e compara com o
 * limiar de dias da regra daquela categoria. Se ultrapassou o limiar, cria (ou atualiza)
 * um lembrete PENDENTE. Idempotente: rodar de novo no mesmo dia não duplica lembretes,
 * porque a chave lógica é (customerId, productId, ruleId) via upsert.
 */
class ReminderRepositoryImpl @Inject constructor(
    private val dao: ReminderDao
) : ReminderRepository {
    override fun observePending(): Flow<List<Reminder>> = dao.observePending().map { list -> list.map { it.toDomain() } }

    override suspend fun upsert(reminder: Reminder): Long = dao.upsert(reminder.toEntity())

    override suspend fun markStatus(id: Long, status: ReminderStatus) = dao.markStatus(id, status.name)

    override suspend fun regenerateFromRules(rules: List<ReminderRule>, now: Long) {
        val rulesByCategory = rules.associateBy { it.categoryId }
        val lastPurchases = dao.getLastPurchasePerCustomerCategory()

        lastPurchases.forEach { row ->
            val rule = rulesByCategory[row.categoryId] ?: return@forEach
            val daysSince = TimeUnit.MILLISECONDS.toDays(now - row.lastPurchaseEpochMillis).toInt()
            if (daysSince >= rule.daysThreshold) {
                dao.upsert(
                    ReminderEntity(
                        customerId = row.customerId,
                        productId = row.productId,
                        ruleId = rule.id,
                        daysSincePurchase = daysSince,
                        status = ReminderStatus.PENDENTE.name,
                        generatedAtEpochMillis = now
                    )
                )
            }
        }
    }
}

class UserRepositoryImpl @Inject constructor(
    private val dao: UserDao
) : UserRepository {
    override fun observeAll(): Flow<List<AppUser>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getById(id: Long): AppUser? = dao.getById(id)?.toDomain()

    override suspend fun upsert(user: AppUser, pin: String?): Long {
        val existing = if (user.id != 0L) dao.getById(user.id) else null
        val pinHash = pin?.let { com.beautymanager.app.core.util.SecurityUtils.hashPin(it) }
            ?: existing?.pinHash
            ?: error("PIN é obrigatório ao criar um novo usuário")
        return dao.upsert(user.toEntity(pinHash))
    }

    override suspend fun delete(id: Long) = dao.delete(id)

    override suspend fun verifyPin(userId: Long, pin: String): Boolean {
        val user = dao.getById(userId) ?: return false
        return user.pinHash == com.beautymanager.app.core.util.SecurityUtils.hashPin(pin)
    }
}

// --- Mappers Entity <-> Domain ---

private fun CustomerEntity.toDomain() = Customer(
    id = id, name = name, phone = phone, whatsapp = whatsapp, birthDateEpochMillis = birthDateEpochMillis,
    address = address, notes = notes, favoriteBrandId = favoriteBrandId, createdAtEpochMillis = createdAtEpochMillis
)

private fun Customer.toEntity() = CustomerEntity(
    id = id, name = name, phone = phone, whatsapp = whatsapp, birthDateEpochMillis = birthDateEpochMillis,
    address = address, notes = notes, favoriteBrandId = favoriteBrandId, createdAtEpochMillis = createdAtEpochMillis
)

private fun SaleEntity.toDomain() = Sale(
    id = id, customerId = customerId, sellerUserId = sellerUserId, dateTimeEpochMillis = dateTimeEpochMillis,
    discount = discount, paymentMethod = PaymentMethod.valueOf(paymentMethod), totalAmount = totalAmount,
    totalProfit = totalProfit
)

private fun Sale.toEntity() = SaleEntity(
    id = id, customerId = customerId, sellerUserId = sellerUserId, dateTimeEpochMillis = dateTimeEpochMillis,
    discount = discount, paymentMethod = paymentMethod.name, totalAmount = totalAmount, totalProfit = totalProfit
)

private fun SaleItemEntity.toDomain() = SaleItem(
    id = id, saleId = saleId, productId = productId, quantity = quantity, unitPrice = unitPrice, unitCost = unitCost
)

private fun SaleItem.toEntity() = SaleItemEntity(
    id = id, saleId = saleId, productId = productId, quantity = quantity, unitPrice = unitPrice, unitCost = unitCost
)

private fun ReminderRuleEntity.toDomain() = ReminderRule(id, categoryId, daysThreshold, messageTemplate)
private fun ReminderRule.toEntity() = ReminderRuleEntity(id, categoryId, daysThreshold, messageTemplate)

private fun ReminderEntity.toDomain() = Reminder(
    id = id, customerId = customerId, productId = productId, ruleId = ruleId,
    daysSincePurchase = daysSincePurchase, status = ReminderStatus.valueOf(status),
    generatedAtEpochMillis = generatedAtEpochMillis
)

private fun Reminder.toEntity() = ReminderEntity(
    id = id, customerId = customerId, productId = productId, ruleId = ruleId,
    daysSincePurchase = daysSincePurchase, status = status.name,
    generatedAtEpochMillis = generatedAtEpochMillis
)

private fun UserEntity.toDomain() = AppUser(
    id = id, name = name, role = UserRole.valueOf(role), canManageProducts = canManageProducts,
    canManageSales = canManageSales, canViewReports = canViewReports, canManageUsers = canManageUsers
)

private fun AppUser.toEntity(pinHash: String) = UserEntity(
    id = id, name = name, role = role.name, pinHash = pinHash, canManageProducts = canManageProducts,
    canManageSales = canManageSales, canViewReports = canViewReports, canManageUsers = canManageUsers
)

/** Mapper de ProductEntity usado só dentro deste arquivo (getTopProducts retorna Product de domínio). */
private fun ProductEntity.toProductDomain() = Product(
    id = id, barcode = barcode, name = name, brandId = brandId, categoryId = categoryId,
    supplierId = supplierId, photoUri = photoUri, costPrice = costPrice, salePrice = salePrice,
    quantity = quantity, minStock = minStock, notes = notes, createdAtEpochMillis = createdAtEpochMillis,
    lastSaleAtEpochMillis = lastSaleAtEpochMillis, totalSold = totalSold, totalProfit = totalProfit
)
