package com.yusufteker.planora.di

import com.yusufteker.planora.core.di.coreModule
import com.yusufteker.planora.feature.auth.di.authModule
import com.yusufteker.planora.feature.home.di.homeModule
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

/**
 * Initializes Koin dependency injection for the entire application.
 *
 * Call this from each platform's entry point:
 * - Android: Application.onCreate()
 * - iOS: MainViewController
 *
 * @param config Optional platform-specific configuration
 *               (e.g., androidContext() on Android)
 */
fun initKoin(config: KoinAppDeclaration? = null): KoinApplication {
    return startKoin {
        config?.invoke(this)
        modules(
            coreModule,
            authModule,
            homeModule
        )
    }
}
