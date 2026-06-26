package com.yusufteker.pulse.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.yusufteker.pulse.core.preferences.ThemeColor
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.ColorScheme

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf

/**
 * Light color scheme for Pulse.
 */
private val LightColorScheme = lightColorScheme(
    primary = PulseColors.Primary,
    onPrimary = PulseColors.OnPrimary,
    primaryContainer = PulseColors.PrimaryContainer,
    onPrimaryContainer = PulseColors.OnPrimaryContainer,
    secondary = PulseColors.Secondary,
    onSecondary = PulseColors.OnSecondary,
    secondaryContainer = PulseColors.SecondaryContainer,
    onSecondaryContainer = PulseColors.OnSecondaryContainer,
    tertiary = PulseColors.Tertiary,
    onTertiary = PulseColors.OnTertiary,
    tertiaryContainer = PulseColors.TertiaryContainer,
    onTertiaryContainer = PulseColors.OnTertiaryContainer,
    background = PulseColors.BackgroundLight,
    onBackground = PulseColors.OnBackgroundLight,
    surface = PulseColors.SurfaceLight,
    onSurface = PulseColors.OnSurfaceLight,
    surfaceVariant = PulseColors.SurfaceVariantLight,
    onSurfaceVariant = PulseColors.OnSurfaceVariantLight,
    outline = PulseColors.OutlineLight,
    outlineVariant = PulseColors.OutlineVariantLight,
    error = PulseColors.Error,
    onError = PulseColors.OnError,
    errorContainer = PulseColors.ErrorContainer,
    onErrorContainer = PulseColors.OnErrorContainer
)

/**
 * Dark color scheme for Pulse.
 */
private val DarkColorScheme = darkColorScheme(
    primary = PulseColors.Primary,
    onPrimary = PulseColors.OnPrimary,
    primaryContainer = PulseColors.PrimaryVariant,
    onPrimaryContainer = PulseColors.PrimaryContainer,
    secondary = PulseColors.Secondary,
    onSecondary = PulseColors.OnSecondary,
    secondaryContainer = PulseColors.SecondaryVariant,
    onSecondaryContainer = PulseColors.SecondaryContainer,
    tertiary = PulseColors.Tertiary,
    onTertiary = PulseColors.OnTertiary,
    tertiaryContainer = PulseColors.TertiaryContainer,
    onTertiaryContainer = PulseColors.OnTertiaryContainer,
    background = PulseColors.BackgroundDark,
    onBackground = PulseColors.OnBackgroundDark,
    surface = PulseColors.SurfaceDark,
    onSurface = PulseColors.OnSurfaceDark,
    surfaceVariant = PulseColors.SurfaceVariantDark,
    onSurfaceVariant = PulseColors.OnSurfaceVariantDark,
    outline = PulseColors.OutlineDark,
    outlineVariant = PulseColors.OutlineVariantDark,
    error = PulseColors.ErrorDark,
    onError = PulseColors.OnError,
    errorContainer = PulseColors.ErrorContainerDark,
    onErrorContainer = PulseColors.OnErrorContainerDark
)



/**
 * Generates a color scheme based on the selected theme color.
 */
fun getAppColorScheme(themeColor: ThemeColor, darkTheme: Boolean): ColorScheme {
    val primaryColor = when(themeColor) {
        ThemeColor.BLUE -> Color(0xFF6C5CE7)
        ThemeColor.RED -> Color(0xFFE63946)
        ThemeColor.GREEN -> Color(0xFF2A9D8F)
        ThemeColor.PURPLE -> Color(0xFF9D4EDD)
        ThemeColor.PINK -> Color(0xFFE83E8C)
        ThemeColor.ORANGE -> Color(0xFFF4A261)
        ThemeColor.TEAL -> Color(0xFF00B4D8)
        ThemeColor.INDIGO -> Color(0xFF3F37C9)
        ThemeColor.AMBER -> Color(0xFFFFB703)
        ThemeColor.BROWN -> Color(0xFF7F4F24)
    }

    val baseScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
    // We tint the primary color. In a full production app, we could generate all tonal palettes.
    return baseScheme.copy(
        primary = primaryColor,
        // Match secondary and secondaryContainer to the theme color so that Bottom Navigation and other components tint correctly
        secondary = primaryColor.copy(alpha = 0.8f),
        secondaryContainer = primaryColor.copy(alpha = 0.2f),
        onSecondaryContainer = primaryColor
    )
}

/**
 * CompositionLocal to provide the current dark theme state across the app.
 */
val LocalIsDarkTheme = compositionLocalOf { false }

/**
 * Pulse application theme.
 *
 * @param themeColor The selected ThemeColor.
 * @param darkTheme Whether to use the dark color scheme.
 * @param content The composable content to theme.
 */
@Composable
fun PulseTheme(
    themeColor: ThemeColor = ThemeColor.BLUE,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = getAppColorScheme(themeColor, darkTheme)

    CompositionLocalProvider(LocalIsDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = PulseTypography,
            shapes = PulseShapes,
            content = content
        )
    }
}
