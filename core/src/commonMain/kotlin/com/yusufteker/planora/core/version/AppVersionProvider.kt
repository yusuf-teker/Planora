package com.yusufteker.planora.core.version

/**
 * Interface for providing the current application version name.
 */
interface AppVersionProvider {
    /**
     * Returns the version string of the current installed application (e.g. "1.0.0").
     *
     * @return Version name string.
     */
    fun getAppVersion(): String
}
