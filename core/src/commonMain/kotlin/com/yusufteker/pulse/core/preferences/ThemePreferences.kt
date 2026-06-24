package com.yusufteker.pulse.core.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

import androidx.datastore.preferences.core.stringPreferencesKey

/**
 * Defines the available dynamic theme colors.
 */
enum class ThemeColor {
    BLUE, RED, GREEN, PURPLE, PINK, ORANGE, TEAL, INDIGO, AMBER, BROWN
}

/**
 * Manages theme preferences using DataStore.
 */
class ThemePreferences(
    private val dataStore: DataStore<Preferences>
) {
    private val darkThemeKey = booleanPreferencesKey("dark_theme")
    private val themeColorKey = stringPreferencesKey("theme_color")

    /**
     * Emits the current dark mode preference.
     * Null means no user preference is set (system default should be used).
     */
    val isDarkMode: Flow<Boolean?> = dataStore.data.map { preferences ->
        preferences[darkThemeKey]
    }

    /**
     * Emits the currently selected ThemeColor.
     */
    val themeColor: Flow<ThemeColor> = dataStore.data.map { preferences ->
        val colorName = preferences[themeColorKey] ?: ThemeColor.BLUE.name
        try {
            ThemeColor.valueOf(colorName)
        } catch (e: Exception) {
            ThemeColor.BLUE
        }
    }

    /**
     * Updates the dark mode preference.
     */
    suspend fun setDarkMode(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[darkThemeKey] = enabled
        }
    }

    /**
     * Updates the selected theme color.
     */
    suspend fun setThemeColor(color: ThemeColor) {
        dataStore.edit { preferences ->
            preferences[themeColorKey] = color.name
        }
    }
}
