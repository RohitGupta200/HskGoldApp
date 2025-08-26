package org.cap.gold.di

import io.ktor.client.HttpClient
import org.cap.gold.data.remote.ProductApiService
import org.cap.gold.data.remote.ProductApiServiceImpl
import org.cap.gold.data.remote.CategoryApiService
import org.cap.gold.data.remote.CategoryApiServiceImpl
import org.cap.gold.data.remote.UsersApiService
import org.cap.gold.data.remote.UsersApiServiceImpl
import org.koin.dsl.module

/**
 * Binds API service interfaces to their Ktor implementations using the shared ApiClient.httpClient
 */
val apiServicesModule = module {
    // Use the Koin-provided HttpClient (from networkModule) that has Auth bearer + refresh
    single<ProductApiService> { ProductApiServiceImpl(get<HttpClient>()) }
    single<CategoryApiService> { CategoryApiServiceImpl(get<HttpClient>()) }
    single<UsersApiService> { UsersApiServiceImpl(get<HttpClient>()) }
}
