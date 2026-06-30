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
     * "Bekleyen Gönderiler / Taslaklar" ekranı.
     * Offline-First mimarisinde gönderilmeyi bekleyen veya taslak olarak kaydedilen postları listeler.
     */
    @Serializable
    data object PendingPosts : Screen

    /**
     * Yeni gönderi oluşturma veya var olan taslağı düzenleme ekranı.
     * @param postId Düzenlenecek taslağın ID'si, yeni gönderi oluşturuluyorsa null olur.
     */
    @Serializable
    data class CreatePost(val postId: String? = null, val id: Int = kotlin.random.Random.nextInt()) : Screen

    /**
     * Kullanıcı arama ekranı.
     */
    @Serializable
    data object SearchUsers : Screen
    
    /**
     * Plan Odası Detay ekranı.
     */
    @Serializable
    data class PlanRoomDetail(val roomId: String) : Screen

    /** 
     * Destinations within the Main Graph (Bottom Navigation Tabs).
     * These are not part of the root Screen hierarchy, but their own nested hierarchy.
     */
    sealed interface MainDestination {
        @Serializable
        data object Home : MainDestination

        @Serializable
        data object Social : MainDestination

        @Serializable
        data object Profile : MainDestination

        @Serializable
        data object Settings : MainDestination
        
        @Serializable
        data object PlanRooms : MainDestination
    }
}

