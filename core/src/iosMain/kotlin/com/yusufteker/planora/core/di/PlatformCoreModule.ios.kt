package com.yusufteker.planora.core.di

import com.russhwolf.settings.KeychainSettings
import com.yusufteker.planora.core.preferences.SecureSettings
import com.yusufteker.planora.core.preferences.createDataStore
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.yusufteker.planora.core.database.PlanoraDatabase
import com.yusufteker.planora.core.reminder.IosReminderManager
import com.yusufteker.planora.core.reminder.ReminderManager
import com.yusufteker.planora.core.ai.IosAiManager
import com.yusufteker.planora.core.ai.OfflineAiManager
import kotlinx.cinterop.ExperimentalForeignApi
import org.koin.dsl.module
import com.yusufteker.planora.core.share.IosShareManager
import com.yusufteker.planora.core.share.ShareManager
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
            requireNotNull(documentDirectory).path + "/planora.preferences_pb"
        }
    }

    single {
        // KeychainSettings uses iOS Keychain for secure storage
        SecureSettings(KeychainSettings(service = "PlanoraSecureStore"))
    }

    single<SqlDriver> { NativeSqliteDriver(PlanoraDatabase.Schema, "planora.db") }

    // Reminder Manager
    single<ReminderManager> { IosReminderManager() }

    // Offline AI Manager
    single<OfflineAiManager> { IosAiManager(get()) }
    
    // Share Manager
    single<ShareManager> { IosShareManager() }
}
