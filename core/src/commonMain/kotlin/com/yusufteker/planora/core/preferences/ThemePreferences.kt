package com.yusufteker.planora.core.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit

import kotlinx.coroutines.launch
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

/**
 * Defines the available dynamic theme colors.
 *
 * Standard themes are available to all users.
 * Premium themes (MIDNIGHT_BLACK, CYBERPUNK_NEON, ROSE_GOLD, NORDIC_MINIMALIST)
 * require an active Premium subscription.
 */
enum class ThemeColor {
    DEFAULT, BLUE, RED, GREEN, PURPLE, PINK, ORANGE, TEAL, INDIGO, AMBER, BROWN,
    // Premium-only themes
    MIDNIGHT_BLACK, CYBERPUNK_NEON, ROSE_GOLD, NORDIC_MINIMALIST
}

/**
 * Returns true if the given [ThemeColor] requires a Premium subscription.
 */
fun ThemeColor.isPremiumTheme(): Boolean = this in setOf(
    ThemeColor.MIDNIGHT_BLACK,
    ThemeColor.CYBERPUNK_NEON,
    ThemeColor.ROSE_GOLD,
    ThemeColor.NORDIC_MINIMALIST
)

/**
 * Manages theme preferences using DataStore.
 */
class ThemePreferences(
    private val dataStore: DataStore<Preferences>
) {
    private val darkThemeKey = booleanPreferencesKey("dark_theme")
    private val themeColorKey = stringPreferencesKey("theme_color")
    private val secondaryThemeColorKey = stringPreferencesKey("secondary_theme_color")

    private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default + kotlinx.coroutines.SupervisorJob())

    private val _themeColor = MutableStateFlow(ThemeColor.DEFAULT)
    /**
     * Emits the currently selected Primary ThemeColor.
     */
    val themeColor: StateFlow<ThemeColor> = _themeColor.asStateFlow()

    private val _secondaryThemeColor = MutableStateFlow(ThemeColor.DEFAULT.defaultSecondary())
    /**
     * Emits the currently selected Secondary (Accent) ThemeColor.
     */
    val secondaryThemeColor: StateFlow<ThemeColor> = _secondaryThemeColor.asStateFlow()

    init {
        scope.launch {
            dataStore.data.collect { preferences ->
                val primaryName = preferences[themeColorKey]
                val primary = if (primaryName != null) {
                    try {
                        ThemeColor.valueOf(primaryName)
                    } catch (e: Exception) {
                        ThemeColor.DEFAULT
                    }
                } else {
                    ThemeColor.DEFAULT
                }
                _themeColor.value = primary

                val secondaryName = preferences[secondaryThemeColorKey]
                val secondary = if (secondaryName != null) {
                    try {
                        ThemeColor.valueOf(secondaryName)
                    } catch (e: Exception) {
                        primary.defaultSecondary()
                    }
                } else {
                    primary.defaultSecondary()
                }
                _secondaryThemeColor.value = secondary
            }
        }
    }

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

    /**
     * Updates the selected primary theme color immediately in memory and persists to DataStore.
     */
    suspend fun setThemeColor(color: ThemeColor) {
        _themeColor.value = color
        dataStore.edit { preferences ->
            preferences[themeColorKey] = color.name
        }
    }

    /**
     * Updates the selected secondary (accent) theme color immediately in memory and persists to DataStore.
     */
    suspend fun setSecondaryThemeColor(color: ThemeColor) {
        _secondaryThemeColor.value = color
        dataStore.edit { preferences ->
            preferences[secondaryThemeColorKey] = color.name
        }
    }
}

/**
 * Returns a recommended harmonious secondary theme color for this [ThemeColor].
 */
fun ThemeColor.defaultSecondary(): ThemeColor = when (this) {
    ThemeColor.DEFAULT -> ThemeColor.GREEN
    ThemeColor.BLUE -> ThemeColor.PURPLE
    ThemeColor.RED -> ThemeColor.ORANGE
    ThemeColor.GREEN -> ThemeColor.TEAL
    ThemeColor.PURPLE -> ThemeColor.PINK
    ThemeColor.PINK -> ThemeColor.ORANGE
    ThemeColor.ORANGE -> ThemeColor.AMBER
    ThemeColor.TEAL -> ThemeColor.GREEN
    ThemeColor.INDIGO -> ThemeColor.BLUE
    ThemeColor.AMBER -> ThemeColor.ORANGE
    ThemeColor.BROWN -> ThemeColor.AMBER
    ThemeColor.MIDNIGHT_BLACK -> ThemeColor.INDIGO
    ThemeColor.CYBERPUNK_NEON -> ThemeColor.PINK
    ThemeColor.ROSE_GOLD -> ThemeColor.AMBER
    ThemeColor.NORDIC_MINIMALIST -> ThemeColor.BROWN
}

