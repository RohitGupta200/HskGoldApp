package org.cap.gold.di

import org.koin.core.module.Module

// JVM actuals: server doesn't need platform-specific DI beyond common modules
actual fun getPlatformModules(): List<Module> = emptyList()
