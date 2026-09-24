package com.yusufteker.planora.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.yusufteker.planora.core.preferences.ThemeColor
import com.yusufteker.planora.core.preferences.defaultSecondary
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
 * Generates a color scheme based on the selected primary and secondary theme colors.
 *
 * Standard themes tint the primary and secondary colors over the base scheme.
 * Premium themes (Midnight Black, Cyberpunk Neon, Rosé Gold, Nordic Minimalist)
 * provide fully custom color palettes for a premium differentiated experience.
 *
 * @param themeColor The primary theme color.
 * @param secondaryThemeColor The secondary (accent) theme color.
 * @param darkTheme Whether dark mode is active.
 */
fun getAppColorScheme(
    themeColor: ThemeColor,
    secondaryThemeColor: ThemeColor = themeColor.defaultSecondary(),
    darkTheme: Boolean
): ColorScheme {
    val baseScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val secondaryColor = resolveThemeColor(secondaryThemeColor)
    val primaryColor = resolveThemeColor(themeColor)

    // ── Premium Themes — Full Custom Schemes ──────────────────────
    when (themeColor) {
        ThemeColor.MIDNIGHT_BLACK -> {
            val primary = Color(0xFF8B5CF6) // Soft violet accent
            return if (darkTheme) {
                darkColorScheme(
                    primary = primary,
                    onPrimary = Color.White,
                    primaryContainer = Color(0xFF1A1025),
                    onPrimaryContainer = Color(0xFFE8DAFF),
                    secondary = secondaryColor,
                    onSecondary = Color.White,
                    secondaryContainer = secondaryColor.copy(alpha = 0.25f),
                    onSecondaryContainer = Color.White,
                    background = Color(0xFF000000),
                    onBackground = Color(0xFFECECF0),
                    surface = Color(0xFF050508),
                    onSurface = Color(0xFFECECF0),
                    surfaceVariant = Color(0xFF0C0C12),
                    onSurfaceVariant = Color(0xFFCACAD8),
                    outline = Color(0xFF2A2A3A),
                    outlineVariant = Color(0xFF161620),
                    error = PlanoraColors.ErrorDark,
                    onError = PlanoraColors.OnError,
                    errorContainer = PlanoraColors.ErrorContainerDark,
                    onErrorContainer = PlanoraColors.OnErrorContainerDark
                )
            } else {
                lightColorScheme(
                    primary = primary,
                    onPrimary = Color.White,
                    primaryContainer = Color(0xFFF3EEFF),
                    onPrimaryContainer = Color(0xFF2D1A5E),
                    secondary = secondaryColor,
                    onSecondary = Color.White,
                    secondaryContainer = secondaryColor.copy(alpha = 0.15f),
                    onSecondaryContainer = secondaryColor,
                    background = Color(0xFFF5F5FA),
                    onBackground = Color(0xFF1A1A2E),
                    surface = Color(0xFFFFFFFF),
                    onSurface = Color(0xFF1A1A2E),
                    surfaceVariant = Color(0xFFF0F0F8),
                    onSurfaceVariant = Color(0xFF46464F),
                    outline = Color(0xFFD4D4DE),
                    outlineVariant = Color(0xFFE8E8F0),
                    error = PlanoraColors.Error,
                    onError = PlanoraColors.OnError,
                    errorContainer = PlanoraColors.ErrorContainer,
                    onErrorContainer = PlanoraColors.OnErrorContainer
                )
            }
        }
        ThemeColor.CYBERPUNK_NEON -> {
            val primary = Color(0xFF00FF88) // Neon green
            return if (darkTheme) {
                darkColorScheme(
                    primary = primary,
                    onPrimary = Color(0xFF003318),
                    primaryContainer = Color(0xFF0A2018),
                    onPrimaryContainer = Color(0xFFB0FFD0),
                    secondary = secondaryColor,
                    onSecondary = Color.White,
                    secondaryContainer = secondaryColor.copy(alpha = 0.25f),
                    onSecondaryContainer = Color.White,
                    background = Color(0xFF0A0A14),
                    onBackground = Color(0xFFE0FFE8),
                    surface = Color(0xFF0D0D1A),
                    onSurface = Color(0xFFE0FFE8),
                    surfaceVariant = Color(0xFF121225),
                    onSurfaceVariant = Color(0xFFB0C8B8),
                    outline = Color(0xFF1A3328),
                    outlineVariant = Color(0xFF0F1F18),
                    error = Color(0xFFFF4466),
                    onError = Color.White,
                    errorContainer = Color(0xFF3D0015),
                    onErrorContainer = Color(0xFFFFD0D8)
                )
            } else {
                lightColorScheme(
                    primary = Color(0xFF00CC6E),
                    onPrimary = Color.White,
                    primaryContainer = Color(0xFFE0FFF0),
                    onPrimaryContainer = Color(0xFF003318),
                    secondary = secondaryColor,
                    onSecondary = Color.White,
                    secondaryContainer = secondaryColor.copy(alpha = 0.15f),
                    onSecondaryContainer = secondaryColor,
                    background = Color(0xFFF0FFF5),
                    onBackground = Color(0xFF0A2010),
                    surface = Color(0xFFFFFFFF),
                    onSurface = Color(0xFF0A2010),
                    surfaceVariant = Color(0xFFE8F8EE),
                    onSurfaceVariant = Color(0xFF3A5A44),
                    outline = Color(0xFFC0D8C8),
                    outlineVariant = Color(0xFFE0F0E6),
                    error = PlanoraColors.Error,
                    onError = PlanoraColors.OnError,
                    errorContainer = PlanoraColors.ErrorContainer,
                    onErrorContainer = PlanoraColors.OnErrorContainer
                )
            }
        }
        ThemeColor.ROSE_GOLD -> {
            val primary = Color(0xFFE8A0B0) // Rose gold
            return if (darkTheme) {
                darkColorScheme(
                    primary = primary,
                    onPrimary = Color(0xFF3D1520),
                    primaryContainer = Color(0xFF2A1018),
                    onPrimaryContainer = Color(0xFFFFE0E8),
                    secondary = secondaryColor,
                    onSecondary = Color.White,
                    secondaryContainer = secondaryColor.copy(alpha = 0.25f),
                    onSecondaryContainer = Color.White,
                    background = Color(0xFF0A0508),
                    onBackground = Color(0xFFF0E0E4),
                    surface = Color(0xFF0D0808),
                    onSurface = Color(0xFFF0E0E4),
                    surfaceVariant = Color(0xFF1A1215),
                    onSurfaceVariant = Color(0xFFD0C0C4),
                    outline = Color(0xFF3A2830),
                    outlineVariant = Color(0xFF201820),
                    error = PlanoraColors.ErrorDark,
                    onError = PlanoraColors.OnError,
                    errorContainer = PlanoraColors.ErrorContainerDark,
                    onErrorContainer = PlanoraColors.OnErrorContainerDark
                )
            } else {
                lightColorScheme(
                    primary = Color(0xFFC47888),
                    onPrimary = Color.White,
                    primaryContainer = Color(0xFFFFF0F3),
                    onPrimaryContainer = Color(0xFF4D1525),
                    secondary = secondaryColor,
                    onSecondary = Color.White,
                    secondaryContainer = secondaryColor.copy(alpha = 0.15f),
                    onSecondaryContainer = secondaryColor,
                    background = Color(0xFFFFF8F9),
                    onBackground = Color(0xFF2A1820),
                    surface = Color(0xFFFFFFFF),
                    onSurface = Color(0xFF2A1820),
                    surfaceVariant = Color(0xFFFAF0F2),
                    onSurfaceVariant = Color(0xFF5A4850),
                    outline = Color(0xFFE0D0D4),
                    outlineVariant = Color(0xFFF0E8EA),
                    error = PlanoraColors.Error,
                    onError = PlanoraColors.OnError,
                    errorContainer = PlanoraColors.ErrorContainer,
                    onErrorContainer = PlanoraColors.OnErrorContainer
                )
            }
        }
        ThemeColor.NORDIC_MINIMALIST -> {
            val primary = Color(0xFF5B8A8A) // Muted teal/sage
            return if (darkTheme) {
                darkColorScheme(
                    primary = primary,
                    onPrimary = Color.White,
                    primaryContainer = Color(0xFF102828),
                    onPrimaryContainer = Color(0xFFD0E8E8),
                    secondary = secondaryColor,
                    onSecondary = Color.White,
                    secondaryContainer = secondaryColor.copy(alpha = 0.25f),
                    onSecondaryContainer = Color.White,
                    background = Color(0xFF080A0A),
                    onBackground = Color(0xFFE0E4E4),
                    surface = Color(0xFF0A0C0C),
                    onSurface = Color(0xFFE0E4E4),
                    surfaceVariant = Color(0xFF141818),
                    onSurfaceVariant = Color(0xFFC0C8C8),
                    outline = Color(0xFF2A3030),
                    outlineVariant = Color(0xFF182020),
                    error = PlanoraColors.ErrorDark,
                    onError = PlanoraColors.OnError,
                    errorContainer = PlanoraColors.ErrorContainerDark,
                    onErrorContainer = PlanoraColors.OnErrorContainerDark
                )
            } else {
                lightColorScheme(
                    primary = primary,
                    onPrimary = Color.White,
                    primaryContainer = Color(0xFFECF4F4),
                    onPrimaryContainer = Color(0xFF1A3030),
                    secondary = secondaryColor,
                    onSecondary = Color.White,
                    secondaryContainer = secondaryColor.copy(alpha = 0.15f),
                    onSecondaryContainer = secondaryColor,
                    background = Color(0xFFF8FAFA),
                    onBackground = Color(0xFF1A2020),
                    surface = Color(0xFFFFFFFF),
                    onSurface = Color(0xFF1A2020),
                    surfaceVariant = Color(0xFFF0F4F4),
                    onSurfaceVariant = Color(0xFF485050),
                    outline = Color(0xFFD4DADA),
                    outlineVariant = Color(0xFFE8EEEE),
                    error = PlanoraColors.Error,
                    onError = PlanoraColors.OnError,
                    errorContainer = PlanoraColors.ErrorContainer,
                    onErrorContainer = PlanoraColors.OnErrorContainer
                )
            }
        }
        else -> { /* Standard themes — fall through to tinting logic below */ }
    }

    // We tint the primary and container colors so that FABs, navigation, and themed components tint correctly.
    val containerBg = baseScheme.background
    val primaryContainerColor = if (darkTheme) primaryColor.copy(alpha = 0.3f).compositeOver(containerBg) else primaryColor.copy(alpha = 0.15f).compositeOver(containerBg)
    val secondaryContainerColor = if (darkTheme) secondaryColor.copy(alpha = 0.3f).compositeOver(containerBg) else secondaryColor.copy(alpha = 0.15f).compositeOver(containerBg)

    return baseScheme.copy(
        primary = primaryColor,
        onPrimary = Color.White,
        primaryContainer = primaryContainerColor,
        onPrimaryContainer = if (darkTheme) Color.White else primaryColor,
        secondary = secondaryColor,
        onSecondary = Color.White,
        secondaryContainer = secondaryContainerColor,
        onSecondaryContainer = if (darkTheme) Color.White else secondaryColor
    )
}

/**
 * Maps a [ThemeColor] enum entry to its representative [Color] value.
 */
fun resolveThemeColor(themeColor: ThemeColor): Color = when (themeColor) {
    ThemeColor.DEFAULT -> PlanoraColors.EventColor
    ThemeColor.BLUE -> Color(0xFF6C5CE7)
    ThemeColor.RED -> Color(0xFFE63946)
    ThemeColor.GREEN -> PlanoraColors.TaskColor
    ThemeColor.PURPLE -> Color(0xFF9D4EDD)
    ThemeColor.PINK -> Color(0xFFE83E8C)
    ThemeColor.ORANGE -> Color(0xFFF4A261)
    ThemeColor.TEAL -> Color(0xFF00B4D8)
    ThemeColor.INDIGO -> Color(0xFF3F37C9)
    ThemeColor.AMBER -> Color(0xFFFFB703)
    ThemeColor.BROWN -> Color(0xFF7F4F24)
    ThemeColor.MIDNIGHT_BLACK -> Color(0xFF8B5CF6)
    ThemeColor.CYBERPUNK_NEON -> Color(0xFF00FF88)
    ThemeColor.ROSE_GOLD -> Color(0xFFE8A0B0)
    ThemeColor.NORDIC_MINIMALIST -> Color(0xFF5B8A8A)
}

/**
 * CompositionLocal to provide the current dark theme state across the app.
 */
val LocalIsDarkTheme = compositionLocalOf { false }

/**
 * CompositionLocal to provide the current dynamic secondary/accent color across the app.
 */
val LocalPlanoraSecondaryColor = compositionLocalOf { PlanoraColors.Secondary }

/**
 * Planora application theme.
 *
 * @param themeColor The selected primary ThemeColor.
 * @param secondaryThemeColor The selected secondary (accent) ThemeColor.
 * @param darkTheme Whether to use the dark color scheme.
 * @param content The composable content to theme.
 */
@Composable
fun PlanoraTheme(
    themeColor: ThemeColor = ThemeColor.DEFAULT,
    secondaryThemeColor: ThemeColor = themeColor.defaultSecondary(),
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = getAppColorScheme(
        themeColor = themeColor,
        secondaryThemeColor = secondaryThemeColor,
        darkTheme = darkTheme
    )

    CompositionLocalProvider(
        LocalIsDarkTheme provides darkTheme,
        LocalPlanoraSecondaryColor provides colorScheme.secondary
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = getPlanoraTypography(),
            shapes = PlanoraShapes,
            content = content
        )
    }
}

