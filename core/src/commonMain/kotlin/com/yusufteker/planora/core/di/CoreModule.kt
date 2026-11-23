package com.yusufteker.planora.core.di

import com.yusufteker.planora.core.preferences.ThemePreferences
import com.yusufteker.planora.core.preferences.SessionPreferences
import com.yusufteker.planora.core.network.createHttpClient
import com.yusufteker.planora.core.database.PlanoraDatabase
import app.cash.sqldelight.db.SqlDriver
import com.yusufteker.planora.core.ui.version.AppVersionViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

import com.yusufteker.planora.core.analytics.AnalyticsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Koin module for core dependencies.
 *
 * This module provides shared dependencies used across the application.
 * Feature modules should not duplicate these definitions.
 */
val coreModule = module {
    includes(platformCoreModule)

    single { ThemePreferences(get(), getOrNull()) }
    singleOf(::SessionPreferences)

    // HttpClient depends on SessionPreferences
    single { createHttpClient(get()) }
    
    // Database (AndroidSqliteDriver.Callback.onOpen ensures deletedTaskEntity lazily on open)
    single {
        PlanoraDatabase(get<SqlDriver>())
    }

    // Snackbar Manager
    single<com.yusufteker.planora.core.snackbar.SnackbarManager> { 
        com.yusufteker.planora.core.snackbar.DefaultSnackbarManager() 
    }

    // FCM Api
    single { com.yusufteker.planora.core.data.api.FcmApi(get()) }
    single { com.yusufteker.planora.core.domain.usecase.RegisterFcmTokenUseCase(get()) }

    // Logout & Session Revocation
    single { com.yusufteker.planora.core.domain.usecase.LogoutUseCase(get(), get(), get(), getOrNull()) }

    // Global Application Scope
    single { CoroutineScope(SupervisorJob() + Dispatchers.Default) }

    // Analytics
    singleOf(::AnalyticsManager)
    
    // Cloud AI
    single { com.yusufteker.planora.core.ai.CloudAiManager() }

    // App Version & Force Update
    single { com.yusufteker.planora.core.data.api.AppVersionApi(get()) }
    single<com.yusufteker.planora.core.domain.repository.AppVersionRepository> {
        com.yusufteker.planora.core.data.repository.AppVersionRepositoryImpl(get())
    }
    single { com.yusufteker.planora.core.domain.usecase.CheckAppVersionUseCase(get(), get()) }
    viewModelOf(::AppVersionViewModel)

    // Holiday Management
    single { com.yusufteker.planora.core.holiday.HolidayApi(get()) }
    single { com.yusufteker.planora.core.holiday.HolidayRepository(get(), get()) }

    // Subscription & Billing
    single { com.yusufteker.planora.core.billing.SubscriptionApi(get()) }
    single<com.yusufteker.planora.core.billing.StoreBillingClient> {
        com.yusufteker.planora.core.billing.PendingStoreBillingClient()
    }
}
