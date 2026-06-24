package com.yusufteker.pulse.core.di

import com.yusufteker.pulse.core.preferences.createDataStore
import kotlinx.cinterop.ExperimentalForeignApi
import org.koin.dsl.module
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

/**
 * iOS implementation for platform-specific dependencies.
 */
@OptIn(ExperimentalForeignApi::class)
actual val platformCoreModule = module {
    single {
        createDataStore {
            val documentDirectory: NSURL? = NSFileManager.defaultManager.URLForDirectory(
                directory = NSDocumentDirectory,
                inDomain = NSUserDomainMask,
                appropriateForURL = null,
                create = false,
                error = null,
            )
            requireNotNull(documentDirectory).path + "/pulse.preferences_pb"
        }
    }
}
