package com.beautymanager.app.core.backup

import androidx.room.withTransaction
import com.beautymanager.app.data.local.dao.*
import com.beautymanager.app.data.local.database.BeautyManagerDatabase
import com.beautymanager.app.data.local.entity.*
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject

/**
 * Snapshot completo do banco local, usado para backup/restauração. Como o app é
 * 100% local (ver README), este é o único jeito de tirar os dados do aparelho —
 * seja para trocar de celular, seja como segurança contra perda do dispositivo.
 */
@Serializable
data class BackupData(
    val version: Int = 1,
    val exportedAtEpochMillis: Long,
    val categories: List<CategoryEntity>,
    val brands: List<BrandEntity>,
    val suppliers: List<SupplierEntity>,
    val products: List<ProductEntity>,
    val stockMovements: List<StockMovementEntity>,
    val customers: List<CustomerEntity>,
    val sales: List<SaleEntity>,
    val saleItems: List<SaleItemEntity>,
    val reminderRules: List<ReminderRuleEntity>,
    val reminders: List<ReminderEntity>,
    val users: List<UserEntity>
)

class BackupManager @Inject constructor(
    private val database: BeautyManagerDatabase,
    private val categoryDao: CategoryDao,
    private val brandDao: BrandDao,
    private val supplierDao: SupplierDao,
    private val productDao: ProductDao,
    private val stockMovementDao: StockMovementDao,
    private val customerDao: CustomerDao,
    private val saleDao: SaleDao,
    private val saleItemDao: SaleItemDao,
    private val reminderRuleDao: ReminderRuleDao,
    private val reminderDao: ReminderDao,
    private val userDao: UserDao
) {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    suspend fun exportToJson(): String {
        val backup = BackupData(
            exportedAtEpochMillis = System.currentTimeMillis(),
            categories = categoryDao.observeAll().first(),
            brands = brandDao.observeAll().first(),
            suppliers = supplierDao.observeAll().first(),
            products = productDao.observeAll(null).first(),
            stockMovements = stockMovementDao.observeAll().first(),
            customers = customerDao.observeAll(null).first(),
            sales = saleDao.observeAll().first(),
            saleItems = saleDao.observeAll().first().map { sale -> saleItemDao.observeForSale(sale.id).first() }.flatten(),
            reminderRules = reminderRuleDao.observeAll().first(),
            reminders = emptyList(), // regenerados pelo worker diário; não precisa persistir no backup
            users = userDao.getAll()
        )
        return json.encodeToString(BackupData.serializer(), backup)
    }

    /**
     * Restaura um backup, SUBSTITUINDO todos os dados atuais do aparelho — por isso
     * a tela de Configurações deve confirmar explicitamente com o usuário antes de
     * chamar isto (é uma operação destrutiva e irreversível sobre os dados locais).
     */
    suspend fun importFromJson(content: String) {
        val backup = json.decodeFromString(BackupData.serializer(), content)
        database.withTransaction {
            database.clearAllTables()
            backup.categories.forEach { categoryDao.upsert(it) }
            backup.brands.forEach { brandDao.upsert(it) }
            backup.suppliers.forEach { supplierDao.upsert(it) }
            backup.products.forEach { productDao.upsert(it) }
            backup.stockMovements.forEach { stockMovementDao.insert(it) }
            backup.customers.forEach { customerDao.upsert(it) }
            backup.sales.forEach { saleDao.insert(it) }
            if (backup.saleItems.isNotEmpty()) saleItemDao.insertAll(backup.saleItems)
            backup.reminderRules.forEach { reminderRuleDao.upsert(it) }
            backup.users.forEach { userDao.upsert(it) }
        }
    }
}
