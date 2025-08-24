package org.cap.gold.di

import org.cap.gold.auth.TokenManager
import org.cap.gold.network.NetworkClient
import org.cap.gold.profile.ProfileService
import org.cap.gold.profile.ProfileServiceImpl
import org.koin.dsl.module

val profileModule = module {
    single { NetworkClient(baseUrl = getProperty("api.base.url"), tokenManager = get<TokenManager>()) }
    single<ProfileService> { ProfileServiceImpl(baseUrl = getProperty("api.base.url"), network = get()) }
}
