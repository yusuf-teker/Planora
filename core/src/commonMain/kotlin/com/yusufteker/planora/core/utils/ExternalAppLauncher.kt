package com.yusufteker.planora.core.utils

import androidx.compose.runtime.Composable

enum class ExternalApp(
    val packageName: String,
    val scheme: String,
    val appName: String
) {
    PLANORA(
        packageName = "com.yusufteker.planora",
        scheme = "planora://",
        appName = "Planora"
    ),
    HEALTH_TRACK(
        packageName = "com.yusufteker.healthtrack",
        scheme = "healthtrack://",
        appName = "HealthTrack"
    )
}

interface ExternalAppLauncher {
    /**
     * Checks whether the target application is installed on the device.
     */
    fun isAppInstalled(app: ExternalApp): Boolean

    /**
     * Launches the target application if installed.
     * Returns true if successfully launched, false otherwise.
     */
    fun openApp(app: ExternalApp): Boolean
}

/**
 * Creates and remembers an [ExternalAppLauncher] instance for the current platform.
 */
@Composable
expect fun rememberExternalAppLauncher(): ExternalAppLauncher
