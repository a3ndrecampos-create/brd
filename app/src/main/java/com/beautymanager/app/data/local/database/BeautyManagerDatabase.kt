package com.beautymanager.app.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.beautymanager.app.data.local.dao.*
import com.beautymanager.app.data.local.entity.*

@Database(
    entities = [
        CategoryEntity::class,
        BrandEntity::class,
        SupplierEntity::class,
        ProductEntity::class,
        StockMovementEntity::class,
        CustomerEntity::class,
        SaleEntity::class,
        SaleItemEntity::class,
        ReminderRuleEntity::class,
        ReminderEntity::class,
        UserEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class BeautyManagerDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun brandDao(): BrandDao
    abstract fun supplierDao(): SupplierDao
    abstract fun productDao(): ProductDao
    abstract fun stockMovementDao(): StockMovementDao
    abstract fun customerDao(): CustomerDao
    abstract fun saleDao(): SaleDao
    abstract fun saleItemDao(): SaleItemDao
    abstract fun reminderRuleDao(): ReminderRuleDao
    abstract fun reminderDao(): ReminderDao
    abstract fun userDao(): UserDao

    companion object {
        const val DATABASE_NAME = "beautymanager.db"
    }
}
