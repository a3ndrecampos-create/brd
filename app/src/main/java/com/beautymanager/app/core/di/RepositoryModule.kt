package com.beautymanager.app.core.di

import com.beautymanager.app.data.repository.*
import com.beautymanager.app.domain.repository.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Ponto-chave da Clean Architecture: o domínio só conhece as interfaces (contratos).
 * Este módulo é o único lugar que sabe qual implementação concreta é usada — permite,
 * por exemplo, trocar o Room por sincronização em nuvem no futuro (fase 2 do produto)
 * sem tocar em telas, ViewModels ou UseCases.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton abstract fun bindCategoryRepository(impl: CategoryRepositoryImpl): CategoryRepository
    @Binds @Singleton abstract fun bindBrandRepository(impl: BrandRepositoryImpl): BrandRepository
    @Binds @Singleton abstract fun bindSupplierRepository(impl: SupplierRepositoryImpl): SupplierRepository
    @Binds @Singleton abstract fun bindProductRepository(impl: ProductRepositoryImpl): ProductRepository
    @Binds @Singleton abstract fun bindStockMovementRepository(impl: StockMovementRepositoryImpl): StockMovementRepository
    @Binds @Singleton abstract fun bindCustomerRepository(impl: CustomerRepositoryImpl): CustomerRepository
    @Binds @Singleton abstract fun bindSaleRepository(impl: SaleRepositoryImpl): SaleRepository
    @Binds @Singleton abstract fun bindReminderRuleRepository(impl: ReminderRuleRepositoryImpl): ReminderRuleRepository
    @Binds @Singleton abstract fun bindReminderRepository(impl: ReminderRepositoryImpl): ReminderRepository
    @Binds @Singleton abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository
    @Binds @Singleton abstract fun bindSessionRepository(impl: SessionRepositoryImpl): SessionRepository
    @Binds @Singleton abstract fun bindBarcodeLookupRepository(impl: BarcodeLookupRepositoryImpl): BarcodeLookupRepository
}
