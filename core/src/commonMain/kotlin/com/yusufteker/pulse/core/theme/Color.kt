package com.yusufteker.pulse.core.theme

import androidx.compose.ui.graphics.Color

/**
 * Pulse color palette.
 *
 * Colors are organized by semantic meaning rather than raw values.
 * This enables consistent theming across the entire application.
 */
object PulseColors {

    // ── Primary ──────────────────────────────────────────────
    val Primary = Color(0xFF6C5CE7)
    val PrimaryVariant = Color(0xFF5A4BD1)
    val OnPrimary = Color(0xFFFFFFFF)
    val PrimaryContainer = Color(0xFFE8E0FF)
    val OnPrimaryContainer = Color(0xFF1E0A4E)

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
}
