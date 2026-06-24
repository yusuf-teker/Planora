package com.yusufteker.pulse.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

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
 * Pulse application theme.
 *
 * Wraps [MaterialTheme] with Pulse's custom color schemes,
 * typography, and shapes. Automatically switches between
 * light and dark themes based on system preference.
 *
 * @param darkTheme Whether to use the dark color scheme.
 *                  Defaults to the system dark theme setting.
 * @param content The composable content to theme.
 */
@Composable
fun PulseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = PulseTypography,
        shapes = PulseShapes,
        content = content
    )
}
