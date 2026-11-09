package com.yusufteker.planora.di

import com.yusufteker.planora.core.di.coreModule
import com.yusufteker.planora.feature.auth.di.authModule
import com.yusufteker.planora.feature.home.di.homeModule
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import org.koin.mp.KoinPlatformTools

/**
 * Initializes Koin dependency injection for the entire application.
 *
 * Call this from each platform's entry point:
 * - Android: Application.onCreate()
 * - iOS: AppDelegate.application(_:didFinishLaunchingWithOptions:) or MainViewController
 *
 * If Koin is already started, this function safely returns without throwing [org.koin.core.error.KoinAppAlreadyStartedException].
 *
 * @param config Optional platform-specific configuration
 *               (e.g., androidContext() on Android)
 */
fun initKoin(config: KoinAppDeclaration? = null): KoinApplication {
    val existingKoin = KoinPlatformTools.defaultContext().getOrNull()
    if (existingKoin != null) {
        return KoinApplication.init()
    }
    return startKoin {
        config?.invoke(this)
        modules(
            coreModule,
            authModule,
            homeModule
        )
    }
}
