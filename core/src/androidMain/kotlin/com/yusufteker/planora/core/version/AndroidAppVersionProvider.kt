package com.yusufteker.planora.core.version

import android.content.Context

/**
 * Android implementation of [AppVersionProvider] using package manager.
 *
 * @property context Android application context.
 */
class AndroidAppVersionProvider(
    private val context: Context
) : AppVersionProvider {
    override fun getAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }
}
