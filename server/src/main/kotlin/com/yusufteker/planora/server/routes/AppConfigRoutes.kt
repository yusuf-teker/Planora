package com.yusufteker.planora.server.routes

import com.yusufteker.planora.shared.api.AppVersionConfigDto
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

/**
 * Route handler for client application version check and remote update configuration.
 *
 * Environment variables:
 * - `MIN_SUPPORTED_VERSION`: e.g. "1.0.0"
 * - `LATEST_APP_VERSION`: e.g. "1.1.0"
 * - `STORE_URL_ANDROID`: Can be Play Store link, Huawei AppGallery URL, Xiaomi GetApps URL, direct APK link, or custom download webpage.
 * - `STORE_URL_IOS`: Can be App Store link or custom download webpage.
 */
fun Route.appConfigRoutes() {
    route("/api/app-config") {
        get {
            val minSupportedVersion = System.getenv("MIN_SUPPORTED_VERSION") ?: "1.0.0"
            val latestVersion = System.getenv("LATEST_APP_VERSION") ?: "1.0.0"
            val storeUrlAndroid = System.getenv("STORE_URL_ANDROID") ?: ""
            val storeUrlIos = System.getenv("STORE_URL_IOS") ?: ""

            val config = AppVersionConfigDto(
                minSupportedVersion = minSupportedVersion,
                latestVersion = latestVersion,
                forceUpdateTitleTr = "Zorunlu Güncelleme",
                forceUpdateTitleEn = "Update Required",
                forceUpdateMessageTr = "Planora'yı kullanmaya devam edebilmek için lütfen uygulamayı son sürüme güncelleyin.",
                forceUpdateMessageEn = "Please update Planora to the latest version to continue using the application.",
                storeUrlAndroid = storeUrlAndroid,
                storeUrlIos = storeUrlIos
            )

            call.respond(HttpStatusCode.OK, config)
        }
    }
}
