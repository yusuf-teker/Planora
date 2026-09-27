package com.yusufteker.planora.server.routes

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.yusufteker.planora.server.AppConfig
import com.yusufteker.planora.server.database.DatabaseFactory.dbQuery
import com.yusufteker.planora.server.database.tables.*
import com.yusufteker.planora.server.service.FcmService
import com.yusufteker.planora.shared.api.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Admin yönetim rotaları.
 * Hem `X-Admin-Secret` başlığı hem de `ADMIN` rolüne sahip JWT Bearer token ile korunur.
 */
fun Route.adminRoutes() {

    suspend fun ApplicationCall.ensureAdminAuthorized(): Boolean {
        // 1. X-Admin-Secret başlığı kontrolü
        val secretHeader = request.headers["X-Admin-Secret"]
        if (!secretHeader.isNullOrBlank() && secretHeader == AppConfig.adminSecretKey) {
            return true
        }

        // 2. JWT Bearer token içindeki "role" claim'i kontrolü
        val authHeader = request.headers["Authorization"]
        if (!authHeader.isNullOrBlank() && authHeader.startsWith("Bearer ", ignoreCase = true)) {
            val token = authHeader.removePrefix("Bearer ").trim()
            try {
                val verifier = JWT.require(Algorithm.HMAC256(AppConfig.jwtSecret))
                    .withIssuer(AppConfig.jwtIssuer)
                    .build()
                val decoded = verifier.verify(token)
                val role = decoded.getClaim("role")?.asString()
                if (role == "ADMIN") {
                    return true
                }
            } catch (_: Exception) {}
        }

        respond(HttpStatusCode.Forbidden, "Yetkisiz erişim: Bu işlem yalnızca ADMIN yetkisiyle gerçekleştirilebilir.")
        return false
    }

    route("/admin") {

        /**
         * GET /admin/stats
         * Sistem genelindeki toplam kullanıcı, görev, oda, post ve premium kullanıcı sayılarını döner.
         */
        get("/stats") {
            if (!call.ensureAdminAuthorized()) return@get

            val stats = dbQuery {
                val totalUsers = UserEntity.count()
                val totalTasks = TaskEntity.count()
                val totalRooms = PlanRoomEntity.count()
                val totalPosts = PostEntity.count()
                val totalPremiumUsers = UserEntity.find { UsersTable.isPremium eq true }.count()
                val totalFcmDevices = FcmTokenEntity.count()

                AdminDashboardStatsDto(
                    totalUsers = totalUsers,
                    totalTasks = totalTasks,
                    totalRooms = totalRooms,
                    totalPosts = totalPosts,
                    totalPremiumUsers = totalPremiumUsers,
                    totalFcmDevices = totalFcmDevices
                )
            }

            call.respond(HttpStatusCode.OK, stats)
        }

        /**
         * GET /admin/users
         * Kullanıcıları arar ve listeler. İsteğe bağlı `?query=ahmet` parametresi alır.
         */
        get("/users") {
            if (!call.ensureAdminAuthorized()) return@get

            val queryParam = call.request.queryParameters["query"]?.trim()?.lowercase()

            val users = dbQuery {
                val entities = if (!queryParam.isNullOrBlank()) {
                    UserEntity.find {
                        (UsersTable.username.lowerCase() like "%$queryParam%") or
                        (UsersTable.email.lowerCase() like "%$queryParam%") or
                        (UsersTable.name.lowerCase() like "%$queryParam%")
                    }.limit(100)
                } else {
                    UserEntity.all().limit(100)
                }

                entities.map { user ->
                    val taskCount = TasksTable.selectAll().where { TasksTable.creatorId eq user.id.value }.count()
                    val roomCount = PlanRoomMembersTable.selectAll().where { PlanRoomMembersTable.userId eq user.id.value }.count()
                    val fcmCount = FcmTokensTable.selectAll().where { FcmTokensTable.userId eq user.id.value }.count()

                    AdminUserSummaryDto(
                        id = user.id.value,
                        name = user.name,
                        username = user.username,
                        email = user.email,
                        role = user.role,
                        isPremium = user.isPremiumActive(),
                        premiumUntil = user.premiumUntil?.toString(),
                        createdAt = user.createdAt.toString(),
                        taskCount = taskCount,
                        roomCount = roomCount,
                        fcmDeviceCount = fcmCount
                    )
                }
            }

            call.respond(HttpStatusCode.OK, users)
        }

        /**
         * GET /admin/users/{id}/tasks
         * Belirtilen kullanıcının görevlerini listeler.
         */
        get("/users/{id}/tasks") {
            if (!call.ensureAdminAuthorized()) return@get

            val targetId = call.parameters["id"]?.toIntOrNull()
            if (targetId == null) {
                call.respond(HttpStatusCode.BadRequest, "Geçersiz kullanıcı ID")
                return@get
            }

            val result = dbQuery {
                val user = UserEntity.findById(targetId) ?: return@dbQuery null
                val taskEntities = TaskEntity.find { TasksTable.creatorId eq targetId }.toList()

                val tasksList = taskEntities.map { entity ->
                    TaskDto(
                        id = entity.id.value,
                        creatorId = targetId,
                        title = entity.title,
                        description = entity.description,
                        startTime = entity.startTime,
                        endTime = entity.endTime,
                        type = entity.type,
                        status = entity.status,
                        visibility = entity.visibility,
                        sharedRoomIds = emptyList(),
                        isRecurring = entity.isRecurring,
                        recurrenceRule = entity.recurrenceRule,
                        isFlexible = entity.isFlexible,
                        isOptional = entity.isOptional,
                        isPostponable = entity.isPostponable,
                        isAllDay = entity.isAllDay,
                        aiMetadata = null,
                        reminders = emptyList(),
                        specificDetails = null,
                        tags = emptyList(),
                        color = entity.color,
                        parentId = entity.parentId,
                        participants = emptyList()
                    )
                }

                AdminUserTasksResponse(
                    userId = user.id.value,
                    username = user.username,
                    tasks = tasksList
                )
            }

            if (result == null) {
                call.respond(HttpStatusCode.NotFound, "Kullanıcı bulunamadı")
            } else {
                call.respond(HttpStatusCode.OK, result)
            }
        }

        /**
         * PUT /admin/users/{id}/role
         * Kullanıcının rolünü (ADMIN veya USER) günceller.
         */
        put("/users/{id}/role") {
            if (!call.ensureAdminAuthorized()) return@put

            val targetId = call.parameters["id"]?.toIntOrNull()
            if (targetId == null) {
                call.respond(HttpStatusCode.BadRequest, "Geçersiz kullanıcı ID")
                return@put
            }

            val request = try {
                call.receive<UpdateUserRoleRequest>()
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, "Geçersiz istek gövdesi")
                return@put
            }

            val targetRole = request.role.uppercase().trim()
            if (targetRole != "ADMIN" && targetRole != "USER") {
                call.respond(HttpStatusCode.BadRequest, "Rol sadece 'ADMIN' veya 'USER' olabilir")
                return@put
            }

            val updatedUser = dbQuery {
                val user = UserEntity.findById(targetId) ?: return@dbQuery null
                user.role = targetRole

                val taskCount = TasksTable.selectAll().where { TasksTable.creatorId eq user.id.value }.count()
                val roomCount = PlanRoomMembersTable.selectAll().where { PlanRoomMembersTable.userId eq user.id.value }.count()
                val fcmCount = FcmTokensTable.selectAll().where { FcmTokensTable.userId eq user.id.value }.count()

                AdminUserSummaryDto(
                    id = user.id.value,
                    name = user.name,
                    username = user.username,
                    email = user.email,
                    role = user.role,
                    isPremium = user.isPremiumActive(),
                    premiumUntil = user.premiumUntil?.toString(),
                    createdAt = user.createdAt.toString(),
                    taskCount = taskCount,
                    roomCount = roomCount,
                    fcmDeviceCount = fcmCount
                )
            }

            if (updatedUser == null) {
                call.respond(HttpStatusCode.NotFound, "Kullanıcı bulunamadı")
            } else {
                call.respond(HttpStatusCode.OK, updatedUser)
            }
        }

        /**
         * POST /admin/users/set-premium
         * Belirtilen kullanıcıya (email veya userId) manuel olarak Premium tanımlar veya kaldırır.
         */
        post("/users/set-premium") {
            if (!call.ensureAdminAuthorized()) return@post

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
         */
        post("/push") {
            if (!call.ensureAdminAuthorized()) return@post

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
                call.respond(HttpStatusCode.NotFound, "Belirtilen kullanıcı bulunamadı.")
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
