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
    
    // Database
    single {
        val driver = get<SqlDriver>()
        try {
            driver.execute(
                identifier = null,
                sql = "CREATE TABLE IF NOT EXISTS deletedTaskEntity (id TEXT NOT NULL PRIMARY KEY, ownerId TEXT NOT NULL, originalTaskJson TEXT NOT NULL, taskTitle TEXT NOT NULL, deletedAt INTEGER NOT NULL);",
                parameters = 0
            )
            driver.execute(
                identifier = null,
                sql = "CREATE INDEX IF NOT EXISTS index_deletedTask_ownerId ON deletedTaskEntity(ownerId);",
                parameters = 0
            )
        } catch (e: Exception) {
            io.github.aakira.napier.Napier.e(e) { "Failed to ensure deletedTaskEntity table: ${e.message}" }
        }
        PlanoraDatabase(driver)
    }

    // Snackbar Manager
    single<com.yusufteker.planora.core.snackbar.SnackbarManager> { 
        com.yusufteker.planora.core.snackbar.DefaultSnackbarManager() 
    }

    // FCM Api
    single { com.yusufteker.planora.core.data.api.FcmApi(get()) }
    single { com.yusufteker.planora.core.domain.usecase.RegisterFcmTokenUseCase(get()) }

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
}
