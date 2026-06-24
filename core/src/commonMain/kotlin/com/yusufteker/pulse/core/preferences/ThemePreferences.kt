package com.yusufteker.pulse.core.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Manages theme preferences using DataStore.
 */
class ThemePreferences(
    private val dataStore: DataStore<Preferences>
) {
    private val darkThemeKey = booleanPreferencesKey("dark_theme")

    /**
     * Emits the current dark mode preference.
     * Null means no user preference is set (system default should be used).
     */
    val isDarkMode: Flow<Boolean?> = dataStore.data.map { preferences ->
        preferences[darkThemeKey]
    }

    /**
     * Updates the dark mode preference.
     */
    suspend fun setDarkMode(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[darkThemeKey] = enabled
        }
    }
}
