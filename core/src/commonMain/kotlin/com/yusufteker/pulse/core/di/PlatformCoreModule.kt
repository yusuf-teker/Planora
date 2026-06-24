package com.yusufteker.pulse.core.di

import org.koin.core.module.Module

/**
 * Platform-specific module to provide platform-dependent instances.
 * e.g., DataStore file paths.
 */
expect val platformCoreModule: Module
