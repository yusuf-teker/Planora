package com.yusufteker.pulse.core.di

import com.yusufteker.pulse.core.preferences.ThemePreferences
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

/**
 * Koin module for core dependencies.
 *
 * This module provides shared dependencies used across the application.
 * Feature modules should not duplicate these definitions.
 *
 * Currently a placeholder — will be populated with:
 * - HttpClient configuration
 * - DataStore instances
 * - SQLDelight database drivers
 * - Common utilities
 */
val coreModule = module {
    includes(platformCoreModule)

    singleOf(::ThemePreferences)
}
