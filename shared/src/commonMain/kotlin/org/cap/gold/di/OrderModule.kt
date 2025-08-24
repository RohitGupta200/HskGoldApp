package org.cap.gold.di

import org.cap.gold.data.repository.OrderRepository
import org.cap.gold.data.repository.OrderRepositoryImpl
import org.koin.dsl.module

val orderModule = module {
    single<OrderRepository> { OrderRepositoryImpl() }
}
