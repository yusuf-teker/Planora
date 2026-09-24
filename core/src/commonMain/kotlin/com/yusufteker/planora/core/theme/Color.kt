package com.yusufteker.planora.core.theme

import androidx.compose.ui.graphics.Color

/**
 * Planora color palette.
 *
 * Colors are organized by semantic meaning rather than raw values.
 * This enables consistent theming across the entire application.
 */
object PlanoraColors {

    // ── Primary (Default: Royal Blue / Event Color) ─────────
    val Primary = Color(0xFF6366F1) // Royal Blue / Indigo (Event Color)
    val PrimaryVariant = Color(0xFF4F46E5)
    val OnPrimary = Color(0xFFFFFFFF)
    val PrimaryContainer = Color(0xFFEEF2FF)
    val OnPrimaryContainer = Color(0xFF1E1B4B)

    // ── Secondary (Default: Emerald / Task Color) ────────────
    val Secondary = Color(0xFF10B981) // Emerald Green (Task Color)
    val SecondaryVariant = Color(0xFF059669)
    val OnSecondary = Color(0xFFFFFFFF)
    val SecondaryContainer = Color(0xFFD1FAE5)
    val OnSecondaryContainer = Color(0xFF064E3B)

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


    // ── Semantic Item Type Colors ───────────────────────────
    val TaskColor = Color(0xFF10B981)     // Emerald (#10B981) - Official Task Brand Color
    val EventColor = Color(0xFF6366F1)    // Indigo (#6366F1) - Official Event Brand Color
    val NoteColor = Color(0xFFF59E0B)     // Amber (#F59E0B) - Official Note Brand Color
    val FolderColor = Color(0xFF22C55E)   // Lime (#22C55E) - Official Folder Brand Color

    // ── Dribbble Modern Gradients & Accents ─────────────────
    val GradientPrimary = listOf(Color(0xFF6366F1), Color(0xFF10B981))
    val GradientPurpleCyan = listOf(Color(0xFF6C5CE7), Color(0xFF00CEC9))
    val GradientCoralSunset = listOf(Color(0xFFFF6B6B), Color(0xFFFF8E53))
    val GradientEmeraldTeal = listOf(Color(0xFF10B981), Color(0xFF06B6D4))
    val GradientIndigoViolet = listOf(Color(0xFF4F46E5), Color(0xFF7C3AED))
    val GradientGoldAmber = listOf(Color(0xFFF59E0B), Color(0xFFFBBF24))

    // ── Premium Brand ────────────────────────────────────────
    val Premium = Color(0xFFE63946)
    val PremiumVariant = Color(0xFFD90429)
    val PremiumGradient = listOf(Color(0xFFFF3B30), Color(0xFFE63946), Color(0xFFD90429))
}

/**
 * Official Premium brand color across the application.
 */
val premiumColor: Color = PlanoraColors.Premium

/**
 * Official Premium brand gradient.
 */
val premiumGradient: List<Color> = PlanoraColors.PremiumGradient

/**
 * Official Task color (#10B981).
 */
val taskColor: Color = PlanoraColors.TaskColor

/**
 * Official Event color (#6366F1).
 */
val eventColor: Color = PlanoraColors.EventColor

/**
 * Official Note color (#F59E0B).
 */
val noteColor: Color = PlanoraColors.NoteColor

/**
 * Official Folder color (#22C55E).
 */
val folderColor: Color = PlanoraColors.FolderColor

/**
 * Returns the brand color associated with a given [com.yusufteker.planora.shared.api.TaskType].
 */
fun getTaskTypeColor(type: com.yusufteker.planora.shared.api.TaskType): Color = when (type) {
    com.yusufteker.planora.shared.api.TaskType.TASK -> PlanoraColors.TaskColor
    com.yusufteker.planora.shared.api.TaskType.EVENT -> PlanoraColors.EventColor
    com.yusufteker.planora.shared.api.TaskType.NOTE -> PlanoraColors.NoteColor
    com.yusufteker.planora.shared.api.TaskType.FOLDER -> PlanoraColors.FolderColor
}

