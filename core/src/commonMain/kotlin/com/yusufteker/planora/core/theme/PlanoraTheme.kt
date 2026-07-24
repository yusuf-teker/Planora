package com.yusufteker.planora.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.yusufteker.planora.core.preferences.ThemeColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.material3.ColorScheme

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf

/**
 * Light color scheme for Planora.
 */
private val LightColorScheme = lightColorScheme(
    primary = PlanoraColors.Primary,
    onPrimary = PlanoraColors.OnPrimary,
    primaryContainer = PlanoraColors.PrimaryContainer,
    onPrimaryContainer = PlanoraColors.OnPrimaryContainer,
    secondary = PlanoraColors.Secondary,
    onSecondary = PlanoraColors.OnSecondary,
    secondaryContainer = PlanoraColors.SecondaryContainer,
    onSecondaryContainer = PlanoraColors.OnSecondaryContainer,
    tertiary = PlanoraColors.Tertiary,
    onTertiary = PlanoraColors.OnTertiary,
    tertiaryContainer = PlanoraColors.TertiaryContainer,
    onTertiaryContainer = PlanoraColors.OnTertiaryContainer,
    background = PlanoraColors.BackgroundLight,
    onBackground = PlanoraColors.OnBackgroundLight,
    surface = PlanoraColors.SurfaceLight,
    onSurface = PlanoraColors.OnSurfaceLight,
    surfaceVariant = PlanoraColors.SurfaceVariantLight,
    onSurfaceVariant = PlanoraColors.OnSurfaceVariantLight,
    outline = PlanoraColors.OutlineLight,
    outlineVariant = PlanoraColors.OutlineVariantLight,
    error = PlanoraColors.Error,
    onError = PlanoraColors.OnError,
    errorContainer = PlanoraColors.ErrorContainer,
    onErrorContainer = PlanoraColors.OnErrorContainer
)

/**
 * Dark color scheme for Planora.
 */
private val DarkColorScheme = darkColorScheme(
    primary = PlanoraColors.Primary,
    onPrimary = PlanoraColors.OnPrimary,
    primaryContainer = PlanoraColors.PrimaryVariant,
    onPrimaryContainer = PlanoraColors.PrimaryContainer,
    secondary = PlanoraColors.Secondary,
    onSecondary = PlanoraColors.OnSecondary,
    secondaryContainer = PlanoraColors.SecondaryVariant,
    onSecondaryContainer = PlanoraColors.SecondaryContainer,
    tertiary = PlanoraColors.Tertiary,
    onTertiary = PlanoraColors.OnTertiary,
    tertiaryContainer = PlanoraColors.TertiaryContainer,
    onTertiaryContainer = PlanoraColors.OnTertiaryContainer,
    background = PlanoraColors.BackgroundDark,
    onBackground = PlanoraColors.OnBackgroundDark,
    surface = PlanoraColors.SurfaceDark,
    onSurface = PlanoraColors.OnSurfaceDark,
    surfaceVariant = PlanoraColors.SurfaceVariantDark,
    onSurfaceVariant = PlanoraColors.OnSurfaceVariantDark,
    outline = PlanoraColors.OutlineDark,
    outlineVariant = PlanoraColors.OutlineVariantDark,
    error = PlanoraColors.ErrorDark,
    onError = PlanoraColors.OnError,
    errorContainer = PlanoraColors.ErrorContainerDark,
    onErrorContainer = PlanoraColors.OnErrorContainerDark
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

    // We tint the primary and container colors so that FABs, navigation, and themed components tint correctly.
    val containerBg = baseScheme.background
    val primaryContainerColor = if (darkTheme) primaryColor.copy(alpha = 0.3f).compositeOver(containerBg) else primaryColor.copy(alpha = 0.15f).compositeOver(containerBg)
    val secondaryContainerColor = if (darkTheme) primaryColor.copy(alpha = 0.3f).compositeOver(containerBg) else primaryColor.copy(alpha = 0.15f).compositeOver(containerBg)

    return baseScheme.copy(
        primary = primaryColor,
        onPrimary = Color.White,
        primaryContainer = primaryContainerColor,
        onPrimaryContainer = if (darkTheme) Color.White else primaryColor,
        secondary = primaryColor.copy(alpha = 0.8f),
        onSecondary = Color.White,
        secondaryContainer = secondaryContainerColor,
        onSecondaryContainer = if (darkTheme) Color.White else primaryColor
    )
}

/**
 * CompositionLocal to provide the current dark theme state across the app.
 */
val LocalIsDarkTheme = compositionLocalOf { false }

/**
 * Planora application theme.
 *
 * @param themeColor The selected ThemeColor.
 * @param darkTheme Whether to use the dark color scheme.
 * @param content The composable content to theme.
 */
@Composable
fun PlanoraTheme(
    themeColor: ThemeColor = ThemeColor.BLUE,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = getAppColorScheme(themeColor, darkTheme)

    CompositionLocalProvider(LocalIsDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = getPlanoraTypography(),
            shapes = PlanoraShapes,
            content = content
        )
    }
}
