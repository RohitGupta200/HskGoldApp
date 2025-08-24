package org.cap.gold.di

import org.koin.dsl.module

// Shared module should not bind composeApp (UI) services.
// Provide an empty module placeholder; platform-specific bindings belong in composeApp.
val productModule = module { }
