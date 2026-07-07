package com.yusufteker.pulse.core.di

import com.yusufteker.pulse.core.preferences.ThemePreferences
import com.yusufteker.pulse.core.preferences.SessionPreferences
import com.yusufteker.pulse.core.network.createHttpClient
import com.yusufteker.pulse.core.database.PulsyDatabase
import app.cash.sqldelight.db.SqlDriver
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

import com.yusufteker.pulse.core.analytics.AnalyticsManager
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

    singleOf(::ThemePreferences)
    singleOf(::SessionPreferences)

    // HttpClient depends on SessionPreferences
    single { createHttpClient(get()) }
    
    // Database
    single { PulsyDatabase(get<SqlDriver>()) }

    // Snackbar Manager
    single<com.yusufteker.pulse.core.snackbar.SnackbarManager> { 
        com.yusufteker.pulse.core.snackbar.DefaultSnackbarManager() 
    }

    // FCM Api
    single { com.yusufteker.pulse.core.data.api.FcmApi(get()) }
    single { com.yusufteker.pulse.core.domain.usecase.RegisterFcmTokenUseCase(get()) }

    // Global Application Scope
    single { CoroutineScope(SupervisorJob() + Dispatchers.Default) }

    // Analytics
    singleOf(::AnalyticsManager)
    
    // Cloud AI
    single { com.yusufteker.pulse.core.ai.CloudAiManager(get()) }
}
