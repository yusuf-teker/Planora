package com.yusufteker.pulse.core.navigation

import kotlinx.serialization.Serializable

/**
 * Navigation destinations for the Pulse application.
 *
 * Each screen is represented as a [Serializable] object or data class.
 * These serve as type-safe keys for Navigation 3's back stack.
 *
 * Organized by navigation graph for clarity.
 */
sealed interface Screen {

    // ── Splash Graph ─────────────────────────────────────────

    /** Splash screen — app entry point with brand animation */
    @Serializable
    data object Splash : Screen

    /** Onboarding screen — first-time user introduction */
    @Serializable
    data object Onboarding : Screen

    // ── Auth Graph ───────────────────────────────────────────

    /** Login screen */
    @Serializable
    data object Login : Screen

    /** Register screen */
    @Serializable
    data object Register : Screen

    // ── Home Graph ───────────────────────────────────────────

    /** Home feed screen */
    @Serializable
    data object Home : Screen

    /** User profile screen */
    @Serializable
    data object Profile : Screen

    /** Settings screen */
    @Serializable
    data object Settings : Screen
}
