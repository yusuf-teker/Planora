package com.yusufteker.planora.core.version

import platform.Foundation.NSBundle

/**
 * iOS implementation of [AppVersionProvider] reading CFBundleShortVersionString.
 */
class IosAppVersionProvider : AppVersionProvider {
    override fun getAppVersion(): String {
        return try {
            (NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String) ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }
}
