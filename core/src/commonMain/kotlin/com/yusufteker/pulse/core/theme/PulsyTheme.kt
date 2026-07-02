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
 * Light color scheme for Pulsy.
 */
private val LightColorScheme = lightColorScheme(
    primary = PulsyColors.Primary,
    onPrimary = PulsyColors.OnPrimary,
    primaryContainer = PulsyColors.PrimaryContainer,
    onPrimaryContainer = PulsyColors.OnPrimaryContainer,
    secondary = PulsyColors.Secondary,
    onSecondary = PulsyColors.OnSecondary,
    secondaryContainer = PulsyColors.SecondaryContainer,
    onSecondaryContainer = PulsyColors.OnSecondaryContainer,
    tertiary = PulsyColors.Tertiary,
    onTertiary = PulsyColors.OnTertiary,
    tertiaryContainer = PulsyColors.TertiaryContainer,
    onTertiaryContainer = PulsyColors.OnTertiaryContainer,
    background = PulsyColors.BackgroundLight,
    onBackground = PulsyColors.OnBackgroundLight,
    surface = PulsyColors.SurfaceLight,
    onSurface = PulsyColors.OnSurfaceLight,
    surfaceVariant = PulsyColors.SurfaceVariantLight,
    onSurfaceVariant = PulsyColors.OnSurfaceVariantLight,
    outline = PulsyColors.OutlineLight,
    outlineVariant = PulsyColors.OutlineVariantLight,
    error = PulsyColors.Error,
    onError = PulsyColors.OnError,
    errorContainer = PulsyColors.ErrorContainer,
    onErrorContainer = PulsyColors.OnErrorContainer
)

/**
 * Dark color scheme for Pulsy.
 */
private val DarkColorScheme = darkColorScheme(
    primary = PulsyColors.Primary,
    onPrimary = PulsyColors.OnPrimary,
    primaryContainer = PulsyColors.PrimaryVariant,
    onPrimaryContainer = PulsyColors.PrimaryContainer,
    secondary = PulsyColors.Secondary,
    onSecondary = PulsyColors.OnSecondary,
    secondaryContainer = PulsyColors.SecondaryVariant,
    onSecondaryContainer = PulsyColors.SecondaryContainer,
    tertiary = PulsyColors.Tertiary,
    onTertiary = PulsyColors.OnTertiary,
    tertiaryContainer = PulsyColors.TertiaryContainer,
    onTertiaryContainer = PulsyColors.OnTertiaryContainer,
    background = PulsyColors.BackgroundDark,
    onBackground = PulsyColors.OnBackgroundDark,
    surface = PulsyColors.SurfaceDark,
    onSurface = PulsyColors.OnSurfaceDark,
    surfaceVariant = PulsyColors.SurfaceVariantDark,
    onSurfaceVariant = PulsyColors.OnSurfaceVariantDark,
    outline = PulsyColors.OutlineDark,
    outlineVariant = PulsyColors.OutlineVariantDark,
    error = PulsyColors.ErrorDark,
    onError = PulsyColors.OnError,
    errorContainer = PulsyColors.ErrorContainerDark,
    onErrorContainer = PulsyColors.OnErrorContainerDark
)



/**
 * Generates a color scheme based on the selected theme color.
 */
fun getAppColorScheme(themeColor: ThemeColor, darkTheme: Boolean): ColorScheme {
    val baseScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
    if (themeColor == ThemeColor.DEFAULT) {
        return baseScheme
    }

    val primaryColor = when(themeColor) {
        ThemeColor.DEFAULT -> return baseScheme // Zaten yukarıda halledildi
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
 * Pulsy application theme.
 *
 * @param themeColor The selected ThemeColor.
 * @param darkTheme Whether to use the dark color scheme.
 * @param content The composable content to theme.
 */
@Composable
fun PulsyTheme(
    themeColor: ThemeColor = ThemeColor.BLUE,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = getAppColorScheme(themeColor, darkTheme)

    CompositionLocalProvider(LocalIsDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = PulsyTypography,
            shapes = PulsyShapes,
            content = content
        )
    }
}
