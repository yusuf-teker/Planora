package com.yusufteker.planora.core.icon

import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import planora.core.generated.resources.Res
import planora.core.generated.resources.app_icon_blue
import planora.core.generated.resources.app_icon_dark
import planora.core.generated.resources.app_icon_dark_green
import planora.core.generated.resources.app_icon_dark_orange
import planora.core.generated.resources.app_icon_dark_pink
import planora.core.generated.resources.app_icon_dark_purple
import planora.core.generated.resources.app_icon_dark_red
import planora.core.generated.resources.app_icon_green
import planora.core.generated.resources.app_icon_light
import planora.core.generated.resources.app_icon_light_green
import planora.core.generated.resources.app_icon_light_orange
import planora.core.generated.resources.app_icon_light_pink
import planora.core.generated.resources.app_icon_light_purple
import planora.core.generated.resources.app_icon_light_red
import planora.core.generated.resources.app_icon_orange
import planora.core.generated.resources.app_icon_pink
import planora.core.generated.resources.app_icon_purple
import planora.core.generated.resources.app_icon_red
import planora.core.generated.resources.planora_icon_blue
import planora.core.generated.resources.planora_icon_green
import planora.core.generated.resources.planora_icon_orange
import planora.core.generated.resources.planora_icon_pink
import planora.core.generated.resources.planora_icon_purple
import planora.core.generated.resources.planora_icon_red

/**
 * Defines the available dynamic launcher app icon themes and colors.
 *
 * Each theme provides an associated color palette, component alias for Android,
 * alternate icon name for iOS, Compose drawable resource for in-app previewing,
 * dark/light splash screen logo variants, and premium tier entitlement.
 *
 * @property id Unique identifier used for persistence in preferences.
 * @property titleRes Localized string resource describing the icon color theme.
 * @property primaryColorHex Dominant accent color hex integer for UI borders and highlights.
 * @property aliasName Android activity-alias component name declared in AndroidManifest.xml.
 * @property iosIconName iOS CFBundleAlternateIcons key defined in Info.plist (null for default).
 * @property iconDrawable Compose drawable resource representing the preview of this icon.
 * @property splashIconDark Splash screen logo resource when dark theme is active.
 * @property splashIconLight Splash screen logo resource when light theme is active.
 * @property isPremium True if this icon requires an active Premium subscription.
 */
enum class AppIcon(
    val id: String,
    val titleRes: StringResource,
    val primaryColorHex: Long,
    val aliasName: String,
    val iosIconName: String?,
    val iconDrawable: DrawableResource,
    val splashIconDark: DrawableResource,
    val splashIconLight: DrawableResource,
    val isPremium: Boolean = true
) {
    DEFAULT(
        id = "default",
        titleRes = Res.string.app_icon_blue,
        primaryColorHex = 0xFF3EB9F8,
        aliasName = "MainActivityDefault",
        iosIconName = null,
        iconDrawable = Res.drawable.planora_icon_blue,
        splashIconDark = Res.drawable.app_icon_dark,
        splashIconLight = Res.drawable.app_icon_light,
        isPremium = false
    ),
    PURPLE(
        id = "purple",
        titleRes = Res.string.app_icon_purple,
        primaryColorHex = 0xFFAC3EF8,
        aliasName = "MainActivityPurple",
        iosIconName = "AppIcon-Purple",
        iconDrawable = Res.drawable.planora_icon_purple,
        splashIconDark = Res.drawable.app_icon_dark_purple,
        splashIconLight = Res.drawable.app_icon_light_purple,
        isPremium = true
    ),
    GREEN(
        id = "green",
        titleRes = Res.string.app_icon_green,
        primaryColorHex = 0xFF3EF87D,
        aliasName = "MainActivityGreen",
        iosIconName = "AppIcon-Green",
        iconDrawable = Res.drawable.planora_icon_green,
        splashIconDark = Res.drawable.app_icon_dark_green,
        splashIconLight = Res.drawable.app_icon_light_green,
        isPremium = true
    ),
    ORANGE(
        id = "orange",
        titleRes = Res.string.app_icon_orange,
        primaryColorHex = 0xFFF89C3E,
        aliasName = "MainActivityOrange",
        iosIconName = "AppIcon-Orange",
        iconDrawable = Res.drawable.planora_icon_orange,
        splashIconDark = Res.drawable.app_icon_dark_orange,
        splashIconLight = Res.drawable.app_icon_light_orange,
        isPremium = true
    ),
    PINK(
        id = "pink",
        titleRes = Res.string.app_icon_pink,
        primaryColorHex = 0xFFF83EAA,
        aliasName = "MainActivityPink",
        iosIconName = "AppIcon-Pink",
        iconDrawable = Res.drawable.planora_icon_pink,
        splashIconDark = Res.drawable.app_icon_dark_pink,
        splashIconLight = Res.drawable.app_icon_light_pink,
        isPremium = true
    ),
    RED(
        id = "red",
        titleRes = Res.string.app_icon_red,
        primaryColorHex = 0xFFF84F3E,
        aliasName = "MainActivityRed",
        iosIconName = "AppIcon-Red",
        iconDrawable = Res.drawable.planora_icon_red,
        splashIconDark = Res.drawable.app_icon_dark_red,
        splashIconLight = Res.drawable.app_icon_light_red,
        isPremium = true
    );

    /**
     * Resolves the appropriate splash screen logo based on whether dark theme is enabled.
     *
     * @param isDark True if dark theme is currently active.
     * @return The corresponding splash [DrawableResource].
     */
    fun getSplashIcon(isDark: Boolean): DrawableResource = if (isDark) splashIconDark else splashIconLight

    /**
     * Helper returning whether this icon requires Premium.
     */
    fun isPremiumIcon(): Boolean = isPremium

    companion object {
        /**
         * Resolves an [AppIcon] from its unique string [id].
         * Defaults to [DEFAULT] if null or unmatched.
         *
         * @param id The string identifier of the icon.
         * @return The matching [AppIcon] enum entry.
         */
        fun fromId(id: String?): AppIcon = entries.find { it.id == id } ?: DEFAULT
    }
}

/**
 * Extension helper returning true if this icon requires an active Premium subscription.
 */
fun AppIcon.isPremiumIcon(): Boolean = this.isPremium
