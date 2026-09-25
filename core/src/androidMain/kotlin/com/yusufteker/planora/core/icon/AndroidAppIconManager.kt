package com.yusufteker.planora.core.icon

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

/**
 * Android implementation of [AppIconManager] that dynamically switches launcher icons
 * by enabling the target `<activity-alias>` and disabling inactive aliases via [PackageManager].
 *
 * @param context Application context used to access system [PackageManager].
 */
class AndroidAppIconManager(private val context: Context) : AppIconManager {

    private val packageName: String = context.packageName
    private val packageManager: PackageManager = context.packageManager
    private val prefs = context.getSharedPreferences("planora_app_icon", Context.MODE_PRIVATE)

    override fun getCurrentIcon(): AppIcon {
        // 1. Fast path: check synchronous persistent preferences first
        val savedIconId = prefs.getString("active_icon_id", null)
        if (savedIconId != null) {
            return AppIcon.fromId(savedIconId)
        }

        // 2. Fallback: inspect packageManager activity aliases
        for (icon in AppIcon.entries) {
            try {
                val component = ComponentName(packageName, "$packageName.${icon.aliasName}")
                val state = packageManager.getComponentEnabledSetting(component)
                if (state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
                    return icon
                }
            } catch (_: Exception) {
                // Ignore query failure
            }
        }
        return AppIcon.DEFAULT
    }

    override fun setIcon(icon: AppIcon) {
        // 1. Persist immediately and synchronously so subsequent getCurrentIcon() calls return instantly
        prefs.edit().putString("active_icon_id", icon.id).commit()

        val targetComponent = ComponentName(packageName, "$packageName.${icon.aliasName}")

        // 2. Enable the selected alias first so that there is always at least one launcher component active
        try {
            packageManager.setComponentEnabledSetting(
                targetComponent,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
        } catch (e: Exception) {
            io.github.aakira.napier.Napier.e("Failed to enable alias $targetComponent: ${e.message}")
        }

        // 3. Disable all other aliases
        for (otherIcon in AppIcon.entries) {
            if (otherIcon != icon) {
                try {
                    val otherComponent = ComponentName(packageName, "$packageName.${otherIcon.aliasName}")
                    packageManager.setComponentEnabledSetting(
                        otherComponent,
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP
                    )
                } catch (e: Exception) {
                    io.github.aakira.napier.Napier.e("Failed to disable alias for ${otherIcon.id}: ${e.message}")
                }
            }
        }
    }
}
