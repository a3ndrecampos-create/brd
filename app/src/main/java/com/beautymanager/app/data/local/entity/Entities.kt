package com.beautymanager.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)

@Entity(tableName = "brands")
data class BrandEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)

@Entity(tableName = "suppliers")
data class SupplierEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String? = null,
    val notes: String? = null
)

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val barcode: String?,
    val name: String,
    val brandId: Long?,
    val categoryId: Long?,
    val supplierId: Long?,
    val photoUri: String?,
    val costPrice: Double,
    val salePrice: Double,
    val quantity: Int,
    val minStock: Int,
    val notes: String?,
    val createdAtEpochMillis: Long,
    val lastSaleAtEpochMillis: Long?,
    val totalSold: Int = 0,
    val totalProfit: Double = 0.0
)

@Entity(tableName = "stock_movements")
data class StockMovementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long,
    val type: String,
    val quantity: Int,
    val dateTimeEpochMillis: Long,
    val notes: String?
)

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String?,
    val whatsapp: String?,
    val birthDateEpochMillis: Long?,
    val address: String?,
    val notes: String?,
    val favoriteBrandId: Long?,
    val createdAtEpochMillis: Long
)

@Entity(tableName = "sales")
data class SaleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerId: Long?,
    val sellerUserId: Long,
    val dateTimeEpochMillis: Long,
    val discount: Double,
    val paymentMethod: String,
    val totalAmount: Double,
    val totalProfit: Double
)

@Entity(tableName = "sale_items")
data class SaleItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val saleId: Long,
    val productId: Long,
    val quantity: Int,
    val unitPrice: Double,
    val unitCost: Double
)

@Entity(tableName = "reminder_rules")
data class ReminderRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: Long,
    val daysThreshold: Int,
    val messageTemplate: String
)

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerId: Long,
    val productId: Long,
    val ruleId: Long,
    val daysSincePurchase: Int,
    val status: String,
    val generatedAtEpochMillis: Long
)

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val role: String,
    val pinHash: String,
    val canManageProducts: Boolean,
    val canManageSales: Boolean,
    val canViewReports: Boolean,
    val canManageUsers: Boolean
)
