package com.yusufteker.planora.server.routes

import com.yusufteker.planora.server.AppConfig
import com.yusufteker.planora.server.database.DatabaseFactory.dbQuery
import com.yusufteker.planora.server.database.tables.UserEntity
import com.yusufteker.planora.server.database.tables.UsersTable
import com.yusufteker.planora.shared.api.SetPremiumRequest
import com.yusufteker.planora.shared.api.SetPremiumResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Admin yönetim rotaları.
 * Güvenlik için `X-Admin-Secret` başlığı ile korunur.
 */
fun Route.adminRoutes() {
    route("/admin") {
        /**
         * POST /admin/users/set-premium
         * Belirtilen kullanıcıya (email veya userId) manuel olarak Premium tanımlar veya kaldırır.
         *
         * Header: X-Admin-Secret: <ADMIN_SECRET_KEY>
         * Body: { "email": "ornek@gmail.com", "isPremium": true, "days": 30 }
         */
        post("/users/set-premium") {
            val adminSecretHeader = call.request.headers["X-Admin-Secret"]
            if (adminSecretHeader.isNullOrBlank() || adminSecretHeader != AppConfig.adminSecretKey) {
                call.respond(HttpStatusCode.Forbidden, "Geçersiz veya eksik admin gizli anahtarı (X-Admin-Secret).")
                return@post
            }

            val request = try {
                call.receive<SetPremiumRequest>()
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, "Geçersiz istek gövdesi: ${e.message}")
                return@post
            }

            val userEntity = dbQuery {
                if (request.userId != null) {
                    UserEntity.findById(request.userId!!)
                } else if (!request.email.isNullOrBlank()) {
                    UserEntity.find { UsersTable.email eq request.email!!.lowercase().trim() }.firstOrNull()
                } else {
                    null
                }
            }

            if (userEntity == null) {
                call.respond(HttpStatusCode.NotFound, "Belirtilen kullanıcı (email veya ID) bulunamadı.")
                return@post
            }

            val premiumUntil = if (request.isPremium && request.days != null && request.days!! > 0) {
                Instant.now().plus(request.days!!.toLong(), ChronoUnit.DAYS)
            } else {
                null
            }

            dbQuery {
                userEntity.isPremium = request.isPremium
                userEntity.premiumUntil = premiumUntil
            }

            val message = if (request.isPremium) {
                if (premiumUntil != null) {
                    "Kullanıcıya ${request.days} günlük Premium başarıyla tanımlandı."
                } else {
                    "Kullanıcıya süresiz/kalıcı Premium başarıyla tanımlandı."
                }
            } else {
                "Kullanıcının Premium durumu başarıyla kaldırıldı."
            }

            call.respond(
                HttpStatusCode.OK,
                SetPremiumResponse(
                    success = true,
                    message = message,
                    userId = userEntity.id.value,
                    email = userEntity.email,
                    isPremium = userEntity.isPremium,
                    premiumUntil = premiumUntil?.toString()
                )
            )
        }
    }
}
