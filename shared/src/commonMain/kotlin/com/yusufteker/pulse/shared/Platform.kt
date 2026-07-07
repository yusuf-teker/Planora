package com.yusufteker.pulse.shared

/**
 * Platform-specific utilities interface.
 *
 * Provides expect/actual declarations for platform-dependent functionality.
 */
expect fun getPlatformName(): String

expect fun isEmulator(): Boolean
