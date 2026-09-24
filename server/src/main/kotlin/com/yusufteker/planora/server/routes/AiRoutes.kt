package com.yusufteker.planora.server.routes

import com.yusufteker.planora.server.service.AiService
import com.yusufteker.planora.shared.ai.AiChatServerRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

/**
 * Yapay zeka ve kullanım kotası rotaları.
 */
fun Route.aiRoutes() {
    authenticate("auth-jwt") {
        route("/ai") {
            /**
             * POST /ai/chat
             * Kullanıcının mesajını işler; kota kontrolü yapar ve gerekirse Gemini API çağırır.
             */
            post("/chat") {
                val currentUserId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asInt()
                if (currentUserId == null) {
                    call.respond(HttpStatusCode.Unauthorized, "Giriş yapmanız gerekiyor.")
                    return@post
                }

                val request = try {
                    call.receive<AiChatServerRequest>()
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, "Geçersiz istek gövdesi: ${e.message}")
                    return@post
                }

                val response = AiService.processChat(currentUserId, request)
                call.respond(HttpStatusCode.OK, response)
            }

            /**
             * GET /ai/quota
             * Kullanıcının güncel AI token haklarını döner (günlük ve haftalık kalan).
             */
            get("/quota") {
                val currentUserId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asInt()
                if (currentUserId == null) {
                    call.respond(HttpStatusCode.Unauthorized, "Giriş yapmanız gerekiyor.")
                    return@get
                }

                val quota = AiService.getUserQuota(currentUserId)
                call.respond(HttpStatusCode.OK, quota)
            }
        }
    }
}
