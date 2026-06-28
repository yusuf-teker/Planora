package com.yusufteker.pulse.core.di

import com.yusufteker.pulse.core.preferences.ThemePreferences
import com.yusufteker.pulse.core.preferences.SessionPreferences
import com.yusufteker.pulse.core.network.createHttpClient
import com.yusufteker.pulse.core.database.PulseDatabase
import app.cash.sqldelight.db.SqlDriver
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

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
    single { PulseDatabase(get<SqlDriver>()) }

    // Snackbar Manager
    single<com.yusufteker.pulse.core.snackbar.SnackbarManager> { 
        com.yusufteker.pulse.core.snackbar.DefaultSnackbarManager() 
    }
}
