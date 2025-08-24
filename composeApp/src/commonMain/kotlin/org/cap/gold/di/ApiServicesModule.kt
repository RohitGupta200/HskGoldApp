package org.cap.gold.di

import org.cap.gold.api.ApiClient
import org.cap.gold.data.remote.ProductApiService
import org.cap.gold.data.remote.ProductApiServiceImpl
import org.cap.gold.data.remote.CategoryApiService
import org.cap.gold.data.remote.CategoryApiServiceImpl
import org.koin.dsl.module

/**
 * Binds API service interfaces to their Ktor implementations using the shared ApiClient.httpClient
 */
val apiServicesModule = module {
    single<ProductApiService> { ProductApiServiceImpl(ApiClient.httpClient) }
    single<CategoryApiService> { CategoryApiServiceImpl(ApiClient.httpClient) }
}
