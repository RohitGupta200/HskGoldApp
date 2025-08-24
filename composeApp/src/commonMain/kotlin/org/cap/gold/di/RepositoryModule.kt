package org.cap.gold.di

import org.cap.gold.data.remote.ProductApiService
import org.cap.gold.data.remote.CategoryApiService
import org.cap.gold.data.repository.AppOrderRepository
import org.cap.gold.data.repository.AppOrderRepositoryImpl
import org.cap.gold.data.repository.ProductRepository
import org.cap.gold.data.repository.ProductRepositoryImpl
import org.cap.gold.data.repository.CategoryRepository
import org.cap.gold.data.repository.CategoryRepositoryImpl
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * Koin module for repository dependencies.
 */
val repositoryModule = module {
    // Product Repository
    single<ProductRepository> { 
        ProductRepositoryImpl(
            apiService = get()
        ) 
    }

    // Category Repository
    single<CategoryRepository> {
        CategoryRepositoryImpl(api = get<CategoryApiService>())
    }

    // Order Repository for app (distinct from shared module's interface)
    single<AppOrderRepository> {
        AppOrderRepositoryImpl()
    }
}
