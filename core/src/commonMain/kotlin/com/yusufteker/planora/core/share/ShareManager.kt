package com.yusufteker.planora.core.share

/**
 * Interface for native sharing capabilities.
 */
interface ShareManager {
    /**
     * Opens the native share sheet to share a text content.
     *
     * @param text The text/URL to be shared.
     * @param title The title for the share chooser (used on Android).
     */
    fun shareText(text: String, title: String = "Share")
}
