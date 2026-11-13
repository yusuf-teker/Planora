package com.yusufteker.planora.server.routes

import com.yusufteker.planora.server.database.DatabaseFactory.dbQuery
import com.yusufteker.planora.server.database.tables.AppConfigEntity
import com.yusufteker.planora.shared.api.AppVersionConfigDto
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

/**
 * Route handler for client application version check and remote force-update configuration.
 *
 * Supports `?app=planora` (default) and `?app=healthtrack`.
 * Reads configuration from `app_configs` table in PostgreSQL, falling back to environment variables
 * (`MIN_SUPPORTED_VERSION`, `LATEST_APP_VERSION`, `STORE_URL_ANDROID`, `STORE_URL_IOS`) or defaults.
 */
fun Route.appConfigRoutes() {
    suspend fun resolveAppConfig(appId: String): AppVersionConfigDto {
        val normalizedAppId = when (appId.lowercase().trim()) {
            "healthtrack", "health" -> "healthtrack"
            else -> "planora"
        }

        val dbConfig = if (org.jetbrains.exposed.sql.transactions.TransactionManager.defaultDatabase != null) {
            runCatching {
                dbQuery {
                    AppConfigEntity.findById(normalizedAppId)?.let { entity ->
                        AppVersionConfigDto(
                            minSupportedVersion = entity.minSupportedVersion,
                            latestVersion = entity.latestVersion,
                            forceUpdateTitleTr = entity.forceUpdateTitleTr,
                            forceUpdateTitleEn = entity.forceUpdateTitleEn,
                            forceUpdateMessageTr = entity.forceUpdateMessageTr,
                            forceUpdateMessageEn = entity.forceUpdateMessageEn,
                            storeUrlAndroid = entity.storeUrlAndroid,
                            storeUrlIos = entity.storeUrlIos
                        )
                    }
                }
            }.getOrNull()
        } else null

        if (dbConfig != null) {
            return dbConfig
        }

        val isHealthTrack = normalizedAppId == "healthtrack"
        val envPrefix = if (isHealthTrack) "HEALTHTRACK_" else ""

        val minSupportedVersion = System.getenv("${envPrefix}MIN_SUPPORTED_VERSION")
            ?: System.getenv("MIN_SUPPORTED_VERSION")
            ?: "1.0.0"
        val latestVersion = System.getenv("${envPrefix}LATEST_APP_VERSION")
            ?: System.getenv("LATEST_APP_VERSION")
            ?: "1.0.0"
        val storeUrlAndroid = System.getenv("${envPrefix}STORE_URL_ANDROID")
            ?: System.getenv("STORE_URL_ANDROID")
            ?: ""
        val storeUrlIos = System.getenv("${envPrefix}STORE_URL_IOS")
            ?: System.getenv("STORE_URL_IOS")
            ?: ""

        val appName = if (isHealthTrack) "HealthTrack" else "Planora"
        val messageTr = if (isHealthTrack) {
            "HealthTrack'i kullanmaya devam edebilmek için lütfen uygulamayı son sürüme güncelleyin."
        } else {
            "Planora'yı kullanmaya devam edebilmek için lütfen uygulamayı son sürüme güncelleyin."
        }

        return AppVersionConfigDto(
            minSupportedVersion = minSupportedVersion,
            latestVersion = latestVersion,
            forceUpdateTitleTr = "Zorunlu Güncelleme",
            forceUpdateTitleEn = "Update Required",
            forceUpdateMessageTr = messageTr,
            forceUpdateMessageEn = "Please update $appName to the latest version to continue using the application.",
            storeUrlAndroid = storeUrlAndroid,
            storeUrlIos = storeUrlIos
        )
    }

    route("/api/app-config") {
        get {
            val appParam = call.request.queryParameters["app"] ?: "planora"
            call.respond(HttpStatusCode.OK, resolveAppConfig(appParam))
        }
    }

    route("/health/app-config") {
        get {
            call.respond(HttpStatusCode.OK, resolveAppConfig("healthtrack"))
        }
    }
}
