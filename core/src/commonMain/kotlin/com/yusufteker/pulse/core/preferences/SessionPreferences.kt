package com.yusufteker.pulse.core.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged

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
    private val userIdKey = stringPreferencesKey("user_id")

    suspend fun getAccessToken(): String? {
        return secureSettings.settings.getStringOrNull(accessTokenKeyString)
    }

    suspend fun getRefreshToken(): String? {
        return secureSettings.settings.getStringOrNull(refreshTokenKeyString)
    }

    // Flow tabanlı: DataStore değişince otomatik güncellenir
    val userNameFlow: kotlinx.coroutines.flow.Flow<String?> =
        dataStore.data.map { it[userNameKey] }.distinctUntilChanged()

    val userAvatarFlow: kotlinx.coroutines.flow.Flow<String?> =
        dataStore.data.map { it[userAvatarKey] }.distinctUntilChanged()
        
    val userIdFlow: kotlinx.coroutines.flow.Flow<String?> =
        dataStore.data.map { it[userIdKey] }.distinctUntilChanged()

    suspend fun getUserName(): String? {
        return dataStore.data.map { it[userNameKey] }.first()
    }

    suspend fun getUserAvatar(): String? {
        return dataStore.data.map { it[userAvatarKey] }.first()
    }
    
    suspend fun getUserId(): String? {
        return dataStore.data.map { it[userIdKey] }.first()
    }
    
    suspend fun getOwnerId(): String {
        return getUserId() ?: "guest"
    }

    suspend fun saveTokens(accessToken: String, refreshToken: String) {
        secureSettings.settings.putString(accessTokenKeyString, accessToken)
        secureSettings.settings.putString(refreshTokenKeyString, refreshToken)
    }

    suspend fun saveUserProfile(userId: String, name: String, avatarId: String) {
        dataStore.edit { prefs ->
            prefs[userIdKey] = userId
            prefs[userNameKey] = name
            prefs[userAvatarKey] = avatarId
        }
    }

    suspend fun updateProfileData(name: String, avatarId: String) {
        dataStore.edit { prefs ->
            prefs[userNameKey] = name
            prefs[userAvatarKey] = avatarId
        }
    }

    suspend fun clearSession() {
        // Clear secure tokens
        secureSettings.settings.remove(accessTokenKeyString)
        secureSettings.settings.remove(refreshTokenKeyString)
        
        // Clear DataStore profile info (Logout means you become guest, data stays in DB but you can't see it until you login)
        dataStore.edit { prefs ->
            prefs.remove(userIdKey)
            prefs.remove(userNameKey)
            prefs.remove(userAvatarKey)
        }
    }
}
