package com.yusufteker.pulse.core.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Manages user session tokens (Access & Refresh) using SecureSettings (Keychain/EncryptedPrefs)
 * and non-sensitive user profile data using DataStore.
 */
class SessionPreferences(
    private val dataStore: DataStore<Preferences>,
    private val secureSettings: SecureSettings
) {
    // Keys for Settings (Tokens)
    private val accessTokenKeyString = "access_token"
    private val refreshTokenKeyString = "refresh_token"

    // Keys for DataStore (Profile info)
    private val userNameKey = stringPreferencesKey("user_name")
    private val userAvatarKey = stringPreferencesKey("user_avatar")

    suspend fun getAccessToken(): String? {
        return secureSettings.settings.getStringOrNull(accessTokenKeyString)
    }

    suspend fun getRefreshToken(): String? {
        return secureSettings.settings.getStringOrNull(refreshTokenKeyString)
    }

    // Flow tabanlı: DataStore değişince otomatik güncellenir
    val userNameFlow: kotlinx.coroutines.flow.Flow<String?> =
        dataStore.data.map { it[userNameKey] }

    val userAvatarFlow: kotlinx.coroutines.flow.Flow<String?> =
        dataStore.data.map { it[userAvatarKey] }

    suspend fun getUserName(): String? {
        return dataStore.data.map { it[userNameKey] }.first()
    }

    suspend fun getUserAvatar(): String? {
        return dataStore.data.map { it[userAvatarKey] }.first()
    }

    suspend fun saveTokens(accessToken: String, refreshToken: String) {
        secureSettings.settings.putString(accessTokenKeyString, accessToken)
        secureSettings.settings.putString(refreshTokenKeyString, refreshToken)
    }

    suspend fun saveUserProfile(name: String, avatarId: String) {
        dataStore.edit { prefs ->
            prefs[userNameKey] = name
            prefs[userAvatarKey] = avatarId
        }
    }

    suspend fun clearSession() {
        // Clear secure tokens
        secureSettings.settings.remove(accessTokenKeyString)
        secureSettings.settings.remove(refreshTokenKeyString)
        
        // Clear DataStore profile info
        dataStore.edit { prefs ->
            prefs.remove(userNameKey)
            prefs.remove(userAvatarKey)
        }
    }
}
