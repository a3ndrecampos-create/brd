package com.beautymanager.app.core.di

import android.content.Context
import androidx.room.Room
import com.beautymanager.app.data.local.dao.*
import com.beautymanager.app.data.local.database.BeautyManagerDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): BeautyManagerDatabase =
        Room.databaseBuilder(context, BeautyManagerDatabase::class.java, BeautyManagerDatabase.DATABASE_NAME)
            .fallbackToDestructiveMigration() // trocar por migrações reais antes de ir para produção
            .build()

    @Provides fun provideCategoryDao(db: BeautyManagerDatabase): CategoryDao = db.categoryDao()
    @Provides fun provideBrandDao(db: BeautyManagerDatabase): BrandDao = db.brandDao()
    @Provides fun provideSupplierDao(db: BeautyManagerDatabase): SupplierDao = db.supplierDao()
    @Provides fun provideProductDao(db: BeautyManagerDatabase): ProductDao = db.productDao()
    @Provides fun provideStockMovementDao(db: BeautyManagerDatabase): StockMovementDao = db.stockMovementDao()
    @Provides fun provideCustomerDao(db: BeautyManagerDatabase): CustomerDao = db.customerDao()
    @Provides fun provideSaleDao(db: BeautyManagerDatabase): SaleDao = db.saleDao()
    @Provides fun provideSaleItemDao(db: BeautyManagerDatabase): SaleItemDao = db.saleItemDao()
    @Provides fun provideReminderRuleDao(db: BeautyManagerDatabase): ReminderRuleDao = db.reminderRuleDao()
    @Provides fun provideReminderDao(db: BeautyManagerDatabase): ReminderDao = db.reminderDao()
    @Provides fun provideUserDao(db: BeautyManagerDatabase): UserDao = db.userDao()
}
