package com.yusufteker.planora.core.preferences

import com.russhwolf.settings.Settings

/**
 * Wrapper class for secure multiplatform-settings.
 * This ensures Koin doesn't accidentally inject non-secure Settings
 * when secure settings are requested.
 */
class SecureSettings(val settings: Settings)
