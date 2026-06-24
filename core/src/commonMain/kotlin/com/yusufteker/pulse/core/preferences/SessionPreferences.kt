package com.yusufteker.pulse.core.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Manages user session tokens (Access & Refresh) using DataStore.
 */
class SessionPreferences(
    private val dataStore: DataStore<Preferences>
) {
    private val accessTokenKey = stringPreferencesKey("access_token")
    private val refreshTokenKey = stringPreferencesKey("refresh_token")
    private val userNameKey = stringPreferencesKey("user_name")
    private val userAvatarKey = stringPreferencesKey("user_avatar")

    suspend fun getAccessToken(): String? {
        return dataStore.data.map { it[accessTokenKey] }.first()
    }

    suspend fun getRefreshToken(): String? {
        return dataStore.data.map { it[refreshTokenKey] }.first()
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
        dataStore.edit { prefs ->
            prefs[accessTokenKey] = accessToken
            prefs[refreshTokenKey] = refreshToken
        }
    }

    suspend fun saveUserProfile(name: String, avatarId: String) {
        dataStore.edit { prefs ->
            prefs[userNameKey] = name
            prefs[userAvatarKey] = avatarId
        }
    }

    suspend fun clearSession() {
        dataStore.edit { prefs ->
            prefs.remove(accessTokenKey)
            prefs.remove(refreshTokenKey)
            prefs.remove(userNameKey)
            prefs.remove(userAvatarKey)
        }
    }
}
