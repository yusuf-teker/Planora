package com.yusufteker.planora.feature.home.presentation.settings

import com.yusufteker.planora.core.icon.AppIcon
import com.yusufteker.planora.core.icon.AppIconManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Fake implementation of [AppIconManager] for unit testing.
 */
class FakeAppIconManager(initialIcon: AppIcon = AppIcon.DEFAULT) : AppIconManager {
    var activeIcon: AppIcon = initialIcon
        private set

    override fun getCurrentIcon(): AppIcon = activeIcon

    override fun setIcon(icon: AppIcon) {
        activeIcon = icon
    }
}

/**
 * Unit tests verifying dynamic app icon resolution, color mappings, and manager switching.
 */
class SettingsAppIconTest {

    @Test
    fun appIcon_fromId_resolvesKnownIconsCorrectly() {
        assertEquals(AppIcon.DEFAULT, AppIcon.fromId("default"))
        assertEquals(AppIcon.PURPLE, AppIcon.fromId("purple"))
        assertEquals(AppIcon.GREEN, AppIcon.fromId("green"))
        assertEquals(AppIcon.ORANGE, AppIcon.fromId("orange"))
        assertEquals(AppIcon.PINK, AppIcon.fromId("pink"))
        assertEquals(AppIcon.RED, AppIcon.fromId("red"))
    }

    @Test
    fun appIcon_fromId_defaultsToDefaultOnNullOrUnknown() {
        assertEquals(AppIcon.DEFAULT, AppIcon.fromId(null))
        assertEquals(AppIcon.DEFAULT, AppIcon.fromId(""))
        assertEquals(AppIcon.DEFAULT, AppIcon.fromId("unknown_color"))
    }

    @Test
    fun fakeAppIconManager_updatesActiveIconOnSet() {
        val manager = FakeAppIconManager()
        assertEquals(AppIcon.DEFAULT, manager.getCurrentIcon())

        manager.setIcon(AppIcon.PURPLE)
        assertEquals(AppIcon.PURPLE, manager.getCurrentIcon())

        manager.setIcon(AppIcon.GREEN)
        assertEquals(AppIcon.GREEN, manager.getCurrentIcon())

        manager.setIcon(AppIcon.ORANGE)
        assertEquals(AppIcon.ORANGE, manager.getCurrentIcon())

        manager.setIcon(AppIcon.PINK)
        assertEquals(AppIcon.PINK, manager.getCurrentIcon())
    }

    @Test
    fun allAppIcons_haveValidMetadata() {
        for (icon in AppIcon.entries) {
            assertNotNull(icon.id)
            assertNotNull(icon.aliasName)
            assertNotNull(icon.titleRes)
            assertNotNull(icon.iconDrawable)
            assertNotNull(icon.splashIconDark)
            assertNotNull(icon.splashIconLight)
            assertNotNull(icon.getSplashIcon(isDark = true))
            assertNotNull(icon.getSplashIcon(isDark = false))
            // Default icon has null iosIconName (uses primary bundle icon), others specify alternate name
            if (icon == AppIcon.DEFAULT) {
                assertNull(icon.iosIconName)
            } else {
                assertNotNull(icon.iosIconName)
            }
        }
    }

    @Test
    fun appIcon_premiumEntitlement_onlyDefaultIsFree() {
        assertEquals(false, AppIcon.DEFAULT.isPremium)
        assertEquals(true, AppIcon.PURPLE.isPremium)
        assertEquals(true, AppIcon.GREEN.isPremium)
        assertEquals(true, AppIcon.ORANGE.isPremium)
        assertEquals(true, AppIcon.PINK.isPremium)
        assertEquals(true, AppIcon.RED.isPremium)
    }
}
