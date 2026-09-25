package com.yusufteker.planora.core.icon

import platform.Foundation.NSUserDefaults

/**
 * Bridge between Kotlin Multiplatform and Swift for managing alternate iOS app icons.
 * The Swift layer registers handlers that invoke [UIApplication.setAlternateIconName].
 */
object IosAppIconBridge {
    /**
     * Lambda invoked by Kotlin to request switching the iOS alternate icon.
     * Passes the icon name defined in Info.plist (or null to revert to the primary icon).
     */
    var onSetIconRequested: ((String?) -> Unit)? = null

    /**
     * Lambda invoked by Kotlin to query the current alternate icon name.
     */
    var onGetCurrentIconRequested: (() -> String?)? = null
}

/**
 * iOS implementation of [AppIconManager] that delegates to [IosAppIconBridge]
 * connected to Swift's [UIApplication.setAlternateIconName] implementation.
 */
class IosAppIconManager : AppIconManager {

    override fun getCurrentIcon(): AppIcon {
        val savedId = NSUserDefaults.standardUserDefaults.stringForKey("planora_active_icon_id")
        if (savedId != null) {
            return AppIcon.fromId(savedId)
        }
        val currentName = IosAppIconBridge.onGetCurrentIconRequested?.invoke()
        return AppIcon.entries.find { it.iosIconName == currentName } ?: AppIcon.DEFAULT
    }

    override fun setIcon(icon: AppIcon) {
        NSUserDefaults.standardUserDefaults.setObject(icon.id, "planora_active_icon_id")
        NSUserDefaults.standardUserDefaults.synchronize()
        IosAppIconBridge.onSetIconRequested?.invoke(icon.iosIconName)
    }
}
