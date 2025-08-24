package org.cap.gold.di

import org.cap.gold.data.repository.UserRepository
import org.cap.gold.data.repository.UserRepositoryImpl
import org.cap.gold.ui.screens.admin.UsersViewModel
import org.koin.dsl.module

/**
 * Koin module that provides user management related dependencies.
 */
val userModule = module {
    // User Repository
    single<UserRepository> { UserRepositoryImpl() }
    // Users ViewModel
    factory { UsersViewModel() }
}
