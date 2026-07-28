package com.yusufteker.planora.core.navigation

import kotlinx.serialization.Serializable

/**
 * Navigation destinations for the Planora application.
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

    /** Forgot password screen */
    @Serializable
    data object ForgotPassword : Screen

    // ── Main Graph (Container for Bottom Navigation) ─────────
    
    /** Main container screen holding the Bottom Navigation */
    @Serializable
    data class Main(val initialDestination: String? = null) : Screen

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
     * Takipçi/Takip edilen/İstekler listesi ekranı.
     */
    @Serializable
    data class FollowList(val initialTab: Int = 0) : Screen

    /**
     * Diğer kullanıcıların veya kendimizin profil detay ekranı.
     */
    @Serializable
    data class Profile(val userId: Int? = null) : Screen
    
    /**
     * Plan Odası Detay ekranı.
     */
    @Serializable
    data class PlanRoomDetail(val roomId: String) : Screen
    
    @Serializable
    data class TaskDetail(
        val taskId: String? = null,
        val planRoomId: String? = null,
        val sharedTitle: String? = null,
        val sharedNote: String? = null,
        val sharedDate: Long? = null,
        val sharedSender: String? = null
    ) : Screen

    @Serializable
    data class TaskEditor(
        val taskId: String? = null, 
        val planRoomId: String? = null, 
        val parentId: String? = null,
        val sharedTitle: String? = null,
        val sharedNote: String? = null,
        val sharedDate: Long? = null,
        val sharedSender: String? = null,
        val copyFromTaskId: String? = null
    ) : Screen

    @Serializable
    data class NoteEditor(
        val noteId: String? = null, 
        val planRoomId: String? = null, 
        val parentId: String? = null,
        val sharedNote: String? = null,
        val sharedSender: String? = null
    ) : Screen

    @Serializable
    data class EventDetail(
        val eventId: String? = null, 
        val planRoomId: String? = null,
        val sharedTitle: String? = null,
        val sharedNote: String? = null,
        val sharedDate: Long? = null,
        val sharedSender: String? = null
    ) : Screen

    @Serializable
    data class EventEditor(
        val eventId: String? = null, 
        val planRoomId: String? = null,
        val sharedTitle: String? = null,
        val sharedNote: String? = null,
        val sharedDate: Long? = null,
        val sharedSender: String? = null,
        val copyFromEventId: String? = null
    ) : Screen
    
    @Serializable
    data object AiChat : Screen

    /**
     * Odaklanma (Focus/Pomodoro) Ekranı
     * Belirli bir göreve odaklanmak için kullanılır.
     */
    @Serializable
    data class Focus(val taskId: String? = null) : Screen


    /**  
     * Destinations within the Main Graph (Bottom Navigation Tabs).
     * These are not part of the root Screen hierarchy, but their own nested hierarchy.
     */
    sealed interface MainDestination {
        @Serializable
        data object Home : MainDestination

        /*@Serializable
        data object Social : MainDestination
*/
        @Serializable
        data object Profile : MainDestination

        @Serializable
        data object Settings : MainDestination
        
        @Serializable
        data object PlanRooms : MainDestination
        
        @Serializable
        data object Notes : MainDestination
    }
}

