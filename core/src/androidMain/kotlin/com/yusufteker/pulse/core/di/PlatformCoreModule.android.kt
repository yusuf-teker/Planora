package com.yusufteker.pulse.core.di

import com.yusufteker.pulse.core.preferences.createDataStore
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Android implementation for platform-specific dependencies.
 */
actual val platformCoreModule = module {
    single {
        createDataStore {
            androidContext().filesDir.resolve("pulse.preferences_pb").absolutePath
        }
    }
}
