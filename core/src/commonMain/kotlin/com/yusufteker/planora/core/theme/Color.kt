package com.yusufteker.planora.core.theme

import androidx.compose.ui.graphics.Color

/**
 * Planora color palette.
 *
 * Colors are organized by semantic meaning rather than raw values.
 * This enables consistent theming across the entire application.
 */
object PlanoraColors {

    // ── Primary ──────────────────────────────────────────────
    val Primary = Color(0xFF1D9BF0) // Sleek Vibrant Blue
    val PrimaryVariant = Color(0xFF0C85D0)
    val OnPrimary = Color(0xFFFFFFFF)
    val PrimaryContainer = Color(0xFFE1F5FE)
    val OnPrimaryContainer = Color(0xFF003355)

    // ── Secondary ────────────────────────────────────────────
    val Secondary = Color(0xFF00CEC9)
    val SecondaryVariant = Color(0xFF00B5B0)
    val OnSecondary = Color(0xFFFFFFFF)
    val SecondaryContainer = Color(0xFFCCF5F4)
    val OnSecondaryContainer = Color(0xFF003736)

    // ── Tertiary ─────────────────────────────────────────────
    val Tertiary = Color(0xFFFF6B6B)
    val OnTertiary = Color(0xFFFFFFFF)
    val TertiaryContainer = Color(0xFFFFE0E0)
    val OnTertiaryContainer = Color(0xFF410002)

    // ── Background & Surface (Light) ─────────────────────────
    val BackgroundLight = Color(0xFFF8F9FE)
    val OnBackgroundLight = Color(0xFF1A1C2E)
    val SurfaceLight = Color(0xFFFFFFFF)
    val OnSurfaceLight = Color(0xFF1A1C2E)
    val SurfaceVariantLight = Color(0xFFF0F0F8)
    val OnSurfaceVariantLight = Color(0xFF46464F)
    val OutlineLight = Color(0xFFD4D4DE)
    val OutlineVariantLight = Color(0xFFE8E8F0)

    // ── Background & Surface (Dark) ──────────────────────────
    val BackgroundDark = Color(0xFF000000)
    val OnBackgroundDark = Color(0xFFE4E4F0)
    val SurfaceDark = Color(0xFF000000)
    val OnSurfaceDark = Color(0xFFE4E4F0)
    val SurfaceVariantDark = Color(0xFF161616)
    val OnSurfaceVariantDark = Color(0xFFCACAD8)
    val OutlineDark = Color(0xFF333333)
    val OutlineVariantDark = Color(0xFF1F1F1F)

    // ── Error ────────────────────────────────────────────────
    val Error = Color(0xFFFF4757)
    val OnError = Color(0xFFFFFFFF)
    val ErrorContainer = Color(0xFFFFDADA)
    val OnErrorContainer = Color(0xFF410002)

    // ── Dark Error ───────────────────────────────────────────
    val ErrorDark = Color(0xFFFF6B7A)
    val ErrorContainerDark = Color(0xFF5C1520)
    val OnErrorContainerDark = Color(0xFFFFDADA)

    // ── Aura & Glassmorphism ─────────────────────────────────
    val GlassSurfaceLight = Color(0x99FFFFFF) // 60% opacity white
    val GlassSurfaceDark = Color(0x66000000)  // 40% opacity black
    val PlanoraGlow = Color(0xFF1D9BF0)         // Primary glow

    val MeshGradientLight1 = Color(0xFFE1F5FE)
    val MeshGradientLight2 = Color(0xFFCCF5F4)
    val MeshGradientLight3 = Color(0xFFFFE0E0)

    val MeshGradientDark1 = Color(0xFF003355)
    val MeshGradientDark2 = Color(0xFF003736)
    val MeshGradientDark3 = Color(0xFF410002)

    // ── Dribbble Modern Gradients & Accents ─────────────────
    val GradientPrimary = listOf(Color(0xFF1D9BF0), Color(0xFF00CEC9))
    val GradientPurpleCyan = listOf(Color(0xFF6C5CE7), Color(0xFF00CEC9))
    val GradientCoralSunset = listOf(Color(0xFFFF6B6B), Color(0xFFFF8E53))
    val GradientEmeraldTeal = listOf(Color(0xFF10B981), Color(0xFF06B6D4))
    val GradientIndigoViolet = listOf(Color(0xFF4F46E5), Color(0xFF7C3AED))
    val GradientGoldAmber = listOf(Color(0xFFF59E0B), Color(0xFFFBBF24))
}

