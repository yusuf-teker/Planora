package com.yusufteker.pulse.core.di

import com.russhwolf.settings.KeychainSettings
import com.yusufteker.pulse.core.preferences.SecureSettings
import com.yusufteker.pulse.core.preferences.createDataStore
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.yusufteker.pulse.core.database.PulsyDatabase
import com.yusufteker.pulse.core.reminder.IosReminderManager
import com.yusufteker.pulse.core.reminder.ReminderManager
import com.yusufteker.pulse.core.ai.IosAiManager
import com.yusufteker.pulse.core.ai.OfflineAiManager
import kotlinx.cinterop.ExperimentalForeignApi
import org.koin.dsl.module
import com.yusufteker.pulse.core.share.IosShareManager
import com.yusufteker.pulse.core.share.ShareManager
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

    single {
        // KeychainSettings uses iOS Keychain for secure storage
        SecureSettings(KeychainSettings(service = "PulsySecureStore"))
    }

    single<SqlDriver> { NativeSqliteDriver(PulsyDatabase.Schema, "pulsy_v3.db") }

    // Reminder Manager
    single<ReminderManager> { IosReminderManager() }

    // Offline AI Manager
    single<OfflineAiManager> { IosAiManager(get()) }
    
    // Share Manager
    single<ShareManager> { IosShareManager() }
}
