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

    /**
     * Get the current access token. Suspends until read is complete.
     */
    suspend fun getAccessToken(): String? {
        return dataStore.data.map { it[accessTokenKey] }.first()
    }

    /**
     * Get the current refresh token.
     */
    suspend fun getRefreshToken(): String? {
        return dataStore.data.map { it[refreshTokenKey] }.first()
    }

    /**
     * Save tokens after a successful login or token refresh.
     */
    suspend fun saveTokens(accessToken: String, refreshToken: String) {
        dataStore.edit { prefs ->
            prefs[accessTokenKey] = accessToken
            prefs[refreshTokenKey] = refreshToken
        }
    }

    /**
     * Clear tokens (e.g. upon logout).
     */
    suspend fun clearSession() {
        dataStore.edit { prefs ->
            prefs.remove(accessTokenKey)
            prefs.remove(refreshTokenKey)
        }
    }
}
