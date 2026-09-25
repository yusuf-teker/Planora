package com.yusufteker.planora.server.routes

import com.yusufteker.planora.server.AppConfig
import com.yusufteker.planora.server.database.DatabaseFactory.dbQuery
import com.yusufteker.planora.server.database.tables.UserEntity
import com.yusufteker.planora.server.database.tables.UsersTable
import com.yusufteker.planora.server.database.tables.FcmTokenEntity
import com.yusufteker.planora.server.database.tables.FcmTokensTable
import com.yusufteker.planora.server.service.FcmService
import com.yusufteker.planora.shared.api.AdminSendPushRequest
import com.yusufteker.planora.shared.api.AdminSendPushResponse
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
import org.jetbrains.exposed.sql.selectAll

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

        /**
         * POST /admin/push
         * Belirli bir kullanıcıya (userId, username veya email) ya da tüm kullanıcılara push bildirimi gönderir.
         *
         * Header: X-Admin-Secret: <ADMIN_SECRET_KEY>
         * Body: {
         *   "userId": 5, // veya "username": "yusuf" veya "email": "test@test.com" veya "broadcastAll": true
         *   "title": "Selam!",
         *   "body": "Yeni görevin var.",
         *   "data": { "type": "announcement" }
         * }
         */
        post("/push") {
            val adminSecretHeader = call.request.headers["X-Admin-Secret"]
            if (adminSecretHeader.isNullOrBlank() || adminSecretHeader != AppConfig.adminSecretKey) {
                call.respond(HttpStatusCode.Forbidden, "Geçersiz veya eksik admin gizli anahtarı (X-Admin-Secret).")
                return@post
            }

            val request = try {
                call.receive<AdminSendPushRequest>()
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, "Geçersiz istek gövdesi: ${e.message}")
                return@post
            }

            if (request.title.isBlank() || request.body.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, "Başlık (title) ve içerik (body) boş bırakılamaz.")
                return@post
            }

            val pushData = request.data ?: emptyMap()

            if (request.broadcastAll) {
                val targetUserIds = dbQuery {
                    FcmTokensTable.selectAll()
                        .map { it[FcmTokensTable.userId].value }
                        .distinct()
                }

                if (targetUserIds.isEmpty()) {
                    call.respond(
                        HttpStatusCode.OK,
                        AdminSendPushResponse(
                            success = false,
                            message = "Kayıtlı aktif FCM cihazı olan kullanıcı bulunamadı.",
                            recipientCount = 0
                        )
                    )
                    return@post
                }

                FcmService.sendPushToUsers(targetUserIds, request.title, request.body, pushData)

                call.respond(
                    HttpStatusCode.OK,
                    AdminSendPushResponse(
                        success = true,
                        message = "${targetUserIds.size} kullanıcıya genel bildirim başarıyla iletildi.",
                        recipientCount = targetUserIds.size
                    )
                )
                return@post
            }

            // Tekil kullanıcı arama (userId, username veya email)
            val targetUserId = request.userId
            val targetUsername = request.username?.trim()
            val targetEmail = request.email?.lowercase()?.trim()

            val user = dbQuery {
                if (targetUserId != null) {
                    UserEntity.findById(targetUserId)
                } else if (!targetUsername.isNullOrBlank()) {
                    UserEntity.find { UsersTable.username eq targetUsername }.firstOrNull()
                } else if (!targetEmail.isNullOrBlank()) {
                    UserEntity.find { UsersTable.email eq targetEmail }.firstOrNull()
                } else {
                    null
                }
            }

            if (user == null) {
                call.respond(HttpStatusCode.NotFound, "Belirtilen kullanıcı (ID, username veya email) bulunamadı.")
                return@post
            }

            val tokenCount = dbQuery {
                FcmTokenEntity.find { FcmTokensTable.userId eq user.id.value }.count()
            }

            if (tokenCount == 0L) {
                call.respond(
                    HttpStatusCode.OK,
                    AdminSendPushResponse(
                        success = false,
                        message = "'${user.username}' kullanıcısı bulundu fakat kayıtlı aktif FCM cihaz/token'ı yok.",
                        recipientCount = 0
                    )
                )
                return@post
            }

            FcmService.sendPushToUser(user.id.value, request.title, request.body, pushData)

            call.respond(
                HttpStatusCode.OK,
                AdminSendPushResponse(
                    success = true,
                    message = "'${user.username}' kullanıcısına (${tokenCount} cihaz) bildirim başarıyla gönderildi.",
                    recipientCount = 1
                )
            )
        }
    }
}
