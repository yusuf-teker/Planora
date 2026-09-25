package com.yusufteker.planora.core.icon

/**
 * Platform-agnostic interface for inspecting and modifying the launcher app icon.
 *
 * Implementations adapt the platform-specific mechanisms:
 * - Android: Activates/deactivates target `<activity-alias>` components via `PackageManager`.
 * - iOS: Invokes `UIApplication.setAlternateIconName` to switch between bundle alternate icons.
 */
interface AppIconManager {
    /**
     * Inspects and returns the currently active launcher app icon on this device.
     *
     * @return The active [AppIcon].
     */
    fun getCurrentIcon(): AppIcon

    /**
     * Changes the application's launcher icon to the specified [icon].
     *
     * @param icon The target [AppIcon] to activate.
     */
    fun setIcon(icon: AppIcon)
}
