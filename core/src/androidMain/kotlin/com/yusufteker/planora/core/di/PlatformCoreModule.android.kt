package com.yusufteker.planora.core.di

import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.russhwolf.settings.SharedPreferencesSettings
import com.yusufteker.planora.core.preferences.SecureSettings
import com.yusufteker.planora.core.preferences.createDataStore
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.yusufteker.planora.core.database.PlanoraDatabase
import com.yusufteker.planora.core.reminder.AndroidReminderManager
import com.yusufteker.planora.core.reminder.ReminderManager
import com.yusufteker.planora.core.ai.AndroidAiManager
import com.yusufteker.planora.core.ai.OfflineAiManager
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import com.yusufteker.planora.core.export.TaskCsvExporter
import com.yusufteker.planora.core.share.AndroidShareManager
import com.yusufteker.planora.core.share.ShareManager

/**
 * Android implementation for platform-specific dependencies.
 */
actual val platformCoreModule = module {
    single {
        createDataStore {
            androidContext().filesDir.resolve("planora.preferences_pb").absolutePath
        }
    }

    single {
        val context = androidContext()
        
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
                
            val sharedPreferences = EncryptedSharedPreferences.create(
                context,
                "secure_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            SecureSettings(SharedPreferencesSettings(sharedPreferences))
        } catch (e: Exception) {
            // This happens if the app was uninstalled and reinstalled, 
            // and Android Auto Backup restored the secure_prefs.xml but the Keystore key was deleted.
            // We must delete the corrupted file and try again.
            context.getSharedPreferences("secure_prefs", android.content.Context.MODE_PRIVATE).edit().clear().apply()
            
            // Also try to delete the file physically just in case
            val dir = java.io.File(context.applicationInfo.dataDir, "shared_prefs")
            val file = java.io.File(dir, "secure_prefs.xml")
            if (file.exists()) {
                file.delete()
            }

            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            val sharedPreferences = EncryptedSharedPreferences.create(
                context,
                "secure_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            SecureSettings(SharedPreferencesSettings(sharedPreferences))
        }
    }

    single<SqlDriver> { AndroidSqliteDriver(PlanoraDatabase.Schema, androidContext(), "planora.db") }

    // Reminder Manager
    single<ReminderManager> { AndroidReminderManager(androidContext()) }

    // Offline AI Manager
    single<OfflineAiManager> { AndroidAiManager(androidContext(), get()) }
    
    // Share Manager
    single<ShareManager> { AndroidShareManager(androidContext()) }

    // Task Data Exporter (CSV & Report)
    single { TaskCsvExporter() }

    // Calendar Sync Manager (Google Calendar & Native Calendar)
    single { com.yusufteker.planora.core.calendar.CalendarSyncManager() }

    // App Version Provider
    single<com.yusufteker.planora.core.version.AppVersionProvider> { 
        com.yusufteker.planora.core.version.AndroidAppVersionProvider(androidContext()) 
    }
}
