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

    // ── Main Graph (Container for Bottom Navigation) ─────────
    
    /** Main container screen holding the Bottom Navigation */
    @Serializable
    data object Main : Screen

    /** 
     * Destinations within the Main Graph (Bottom Navigation Tabs).
     * These are not part of the root Screen hierarchy, but their own nested hierarchy.
     */
    sealed interface MainDestination {
        @Serializable
        data object Home : MainDestination

        @Serializable
        data object Profile : MainDestination

        @Serializable
        data object Settings : MainDestination
    }
}

