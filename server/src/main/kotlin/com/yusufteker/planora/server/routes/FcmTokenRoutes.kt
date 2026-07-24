package com.yusufteker.planora.server.routes

import com.yusufteker.planora.server.database.DatabaseFactory.dbQuery
import com.yusufteker.planora.server.database.tables.FcmTokenEntity
import com.yusufteker.planora.server.database.tables.FcmTokensTable
import com.yusufteker.planora.server.database.tables.UserEntity
import com.yusufteker.planora.shared.api.RegisterFcmTokenRequest
import com.yusufteker.planora.shared.api.UnregisterFcmTokenRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receiveNullable
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import java.time.Instant

fun Route.fcmTokenRoutes() {
    authenticate("auth-jwt") {
        route("/fcm") {
            post("/register") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asInt()

                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized)
                    return@post
                }

                val request = call.receiveNullable<RegisterFcmTokenRequest>()
                if (request == null || request.token.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid token")
                    return@post
                }

                dbQuery {
                    val user = UserEntity.findById(userId) ?: return@dbQuery
                    
                    val existingToken = FcmTokenEntity.find { FcmTokensTable.token eq request.token }.firstOrNull()
                    if (existingToken != null) {
                        existingToken.user = user
                        existingToken.platform = request.platform
                        existingToken.updatedAt = Instant.now()
                    } else {
                        FcmTokenEntity.new {
                            this.user = user
                            this.token = request.token
                            this.platform = request.platform
                            this.updatedAt = Instant.now()
                        }
                    }
                }
                
                call.respond(HttpStatusCode.OK)
            }

            post("/unregister") {
                val request = call.receiveNullable<UnregisterFcmTokenRequest>()
                if (request == null || request.token.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid token")
                    return@post
                }

                dbQuery {
                    val existingToken = FcmTokenEntity.find { FcmTokensTable.token eq request.token }.firstOrNull()
                    existingToken?.delete()
                }

                call.respond(HttpStatusCode.OK)
            }
        }
    }
}
