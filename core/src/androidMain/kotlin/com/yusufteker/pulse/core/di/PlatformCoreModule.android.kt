package com.yusufteker.pulse.core.di

import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.russhwolf.settings.SharedPreferencesSettings
import com.yusufteker.pulse.core.preferences.SecureSettings
import com.yusufteker.pulse.core.preferences.createDataStore
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.yusufteker.pulse.core.database.PulsyDatabase
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

    single {
        val context = androidContext()
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

    single<SqlDriver> { AndroidSqliteDriver(PulsyDatabase.Schema, androidContext(), "pulsy_v3.db") }
}
