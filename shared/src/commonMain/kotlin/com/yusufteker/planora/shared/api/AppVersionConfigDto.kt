package com.yusufteker.planora.shared.api

import kotlinx.serialization.Serializable

/**
 * Data Transfer Object representing application version and update configuration.
 *
 * @property minSupportedVersion Minimum version required to run the application (e.g., "1.0.0").
 * @property latestVersion Latest available version in the store (e.g., "1.2.0").
 * @property forceUpdateTitleTr Optional custom title string for force update dialog in Turkish.
 * @property forceUpdateTitleEn Optional custom title string for force update dialog in English.
 * @property forceUpdateMessageTr Optional custom message string for force update dialog in Turkish.
 * @property forceUpdateMessageEn Optional custom message string for force update dialog in English.
 * @property storeUrlAndroid Target update URL for Android (Play Store, Huawei AppGallery, Xiaomi GetApps, APK link, or custom web URL).
 * @property storeUrlIos Target update URL for iOS (App Store or custom web URL).
 */
@Serializable
data class AppVersionConfigDto(
    val minSupportedVersion: String = "1.0.0",
    val latestVersion: String = "1.0.0",
    val forceUpdateTitleTr: String? = null,
    val forceUpdateTitleEn: String? = null,
    val forceUpdateMessageTr: String? = null,
    val forceUpdateMessageEn: String? = null,
    val storeUrlAndroid: String = "",
    val storeUrlIos: String = ""
)
