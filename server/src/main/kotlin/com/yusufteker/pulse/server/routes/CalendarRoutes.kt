package com.yusufteker.pulse.server.routes

import com.yusufteker.pulse.server.database.DatabaseFactory.dbQuery
import com.yusufteker.pulse.server.database.tables.CalendarAccessEntity
import com.yusufteker.pulse.server.database.tables.CalendarAccessTable
import com.yusufteker.pulse.server.database.tables.TaskEntity
import com.yusufteker.pulse.server.database.tables.TasksTable
import com.yusufteker.pulse.server.database.tables.UserEntity
import com.yusufteker.pulse.server.database.tables.UsersTable
import com.yusufteker.pulse.shared.api.CalendarAccessGrantDto
import com.yusufteker.pulse.shared.api.CalendarAccessRequestDto
import com.yusufteker.pulse.shared.api.TaskDto
import com.yusufteker.pulse.shared.api.TaskVisibility
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import java.time.Instant
import kotlinx.serialization.json.Json
import kotlinx.coroutines.launch
import kotlinx.coroutines.GlobalScope

fun Route.calendarRoutes() {
    authenticate("auth-jwt") {
        route("/calendar") {

            // POST /calendar/request/{userId}
            post("/request/{userId}") {
                val currentUserId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asInt()
                val targetUserId = call.parameters["userId"]?.toIntOrNull()

                if (currentUserId == null || targetUserId == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid user ID")
                    return@post
                }
                
                if (currentUserId == targetUserId) {
                    call.respond(HttpStatusCode.BadRequest, "Cannot request calendar access to yourself")
                    return@post
                }

                dbQuery {
                    val targetUser = UserEntity.findById(targetUserId)
                    val currentUser = UserEntity.findById(currentUserId)
                    
                    if (targetUser == null || currentUser == null) {
                        return@dbQuery
                    }

                    // Check if already requested or granted
                    val existingAccess = CalendarAccessEntity.find { 
                        (CalendarAccessTable.requesterId eq currentUserId) and (CalendarAccessTable.granterId eq targetUserId) 
                    }.firstOrNull()

                    if (existingAccess != null) {
                        existingAccess.delete() // Toggle: cancel request or revoke
                    } else {
                        CalendarAccessEntity.new {
                            requester = currentUser
                            granter = targetUser
                            status = "PENDING"
                            createdAt = Instant.now()
                        }
                        
                        // Send push notification to target user
                        com.yusufteker.pulse.server.service.FcmService.sendPushToUser(
                            userId = targetUserId,
                            title = "Takvim Erişim İsteği",
                            body = "@${currentUser.username} takvimini görmek için izin istiyor.",
                            data = mapOf("type" to "calendar_request")
                        )
                    }
                }
                call.respond(HttpStatusCode.OK)
            }

            // GET /calendar/requests
            get("/requests") {
                val currentUserId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asInt()
                if (currentUserId == null) {
                    call.respond(HttpStatusCode.Unauthorized, "Unauthorized")
                    return@get
                }

                val requests = dbQuery {
                    CalendarAccessEntity.find {
                        (CalendarAccessTable.granterId eq currentUserId) and
                        (CalendarAccessTable.status eq "PENDING")
                    }.map {
                        val requester = it.requester
                        CalendarAccessRequestDto(
                            id = it.id.value,
                            requesterId = requester.id.value,
                            requesterName = requester.name,
                            requesterUsername = requester.username,
                            requesterAvatarId = requester.avatarId,
                            status = it.status
                        )
                    }
                }
                call.respond(HttpStatusCode.OK, requests)
            }

            // POST /calendar/requests/{requestId}/accept
            post("/requests/{requestId}/accept") {
                val currentUserId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asInt()
                val requestId = call.parameters["requestId"]?.toIntOrNull()
                if (currentUserId == null || requestId == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid Request ID")
                    return@post
                }

                dbQuery {
                    val request = CalendarAccessEntity.findById(requestId)
                    if (request != null && request.granter.id.value == currentUserId && request.status == "PENDING") {
                        request.status = "GRANTED"
                        
                        // Generate random color for this user on the requester's calendar
                        val colors = listOf("#E91E63", "#9C27B0", "#673AB7", "#3F51B5", "#2196F3", "#00BCD4", "#009688", "#4CAF50", "#FF9800", "#FF5722")
                        request.color = colors.random()
                    }
                }
                call.respond(HttpStatusCode.OK)
            }

            // POST /calendar/requests/{requestId}/reject
            post("/requests/{requestId}/reject") {
                val currentUserId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asInt()
                val requestId = call.parameters["requestId"]?.toIntOrNull()
                if (currentUserId == null || requestId == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid Request ID")
                    return@post
                }

                dbQuery {
                    val request = CalendarAccessEntity.findById(requestId)
                    if (request != null && request.granter.id.value == currentUserId) {
                        request.delete() // Delete request
                    }
                }
                call.respond(HttpStatusCode.OK)
            }

            // GET /calendar/grants
            get("/grants") {
                val currentUserId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asInt()
                if (currentUserId == null) {
                    call.respond(HttpStatusCode.Unauthorized, "Unauthorized")
                    return@get
                }

                val grants = dbQuery {
                    CalendarAccessEntity.find {
                        (CalendarAccessTable.granterId eq currentUserId) and
                        (CalendarAccessTable.status eq "GRANTED")
                    }.map {
                        val user = it.requester
                        CalendarAccessGrantDto(
                            userId = user.id.value,
                            name = user.name,
                            username = user.username,
                            avatarId = user.avatarId,
                            color = it.color ?: "#2196F3"
                        )
                    }
                }
                call.respond(HttpStatusCode.OK, grants)
            }

            // GET /calendar/accessible
            get("/accessible") {
                val currentUserId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asInt()
                if (currentUserId == null) {
                    call.respond(HttpStatusCode.Unauthorized, "Unauthorized")
                    return@get
                }

                val accessible = dbQuery {
                    CalendarAccessEntity.find {
                        (CalendarAccessTable.requesterId eq currentUserId) and
                        (CalendarAccessTable.status eq "GRANTED")
                    }.map {
                        val user = it.granter
                        CalendarAccessGrantDto(
                            userId = user.id.value,
                            name = user.name,
                            username = user.username,
                            avatarId = user.avatarId,
                            color = it.color ?: "#E91E63"
                        )
                    }
                }
                call.respond(HttpStatusCode.OK, accessible)
            }

            // POST /calendar/revoke/{userId}
            post("/revoke/{userId}") {
                val currentUserId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asInt()
                val targetUserId = call.parameters["userId"]?.toIntOrNull()
                if (currentUserId == null || targetUserId == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid User ID")
                    return@post
                }

                dbQuery {
                    // It could be that I am the granter and want to revoke
                    val existingGrantAsGranter = CalendarAccessEntity.find {
                        (CalendarAccessTable.granterId eq currentUserId) and
                        (CalendarAccessTable.requesterId eq targetUserId)
                    }.firstOrNull()

                    existingGrantAsGranter?.delete()
                }
                call.respond(HttpStatusCode.OK)
            }

            // GET /calendar/tasks/{userId}
            get("/tasks/{userId}") {
                val currentUserId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asInt()
                val targetUserId = call.parameters["userId"]?.toIntOrNull()
                if (currentUserId == null || targetUserId == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid User ID")
                    return@get
                }

                val fromTime = call.request.queryParameters["from"]?.toLongOrNull()
                val toTime = call.request.queryParameters["to"]?.toLongOrNull()

                val tasks = dbQuery {
                    // Check if current user is granted access by target user
                    val access = CalendarAccessEntity.find {
                        (CalendarAccessTable.requesterId eq currentUserId) and
                        (CalendarAccessTable.granterId eq targetUserId) and
                        (CalendarAccessTable.status eq "GRANTED")
                    }.firstOrNull()

                    if (access == null) {
                        return@dbQuery emptyList<TaskDto>()
                    }

                    val query = TasksTable.selectAll().where { TasksTable.creatorId eq targetUserId }
                    val entities = TaskEntity.wrapRows(query).toList()
                    
                    val filteredEntities = entities.filter {
                        var include = true
                        if (fromTime != null) include = include && (it.startTime >= fromTime)
                        if (toTime != null) include = include && (it.startTime <= toTime)
                        include
                    }

                    filteredEntities.map { entity ->
                        // Since the user has GRANTED calendar access, they can see the details of PRIVATE tasks too.
                        val isPrivate = false // We no longer hide details for granted access
                        TaskDto(
                            id = entity.id.value,
                            creatorId = entity.creator.id.value,
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
                            aiMetadata = entity.aiMetadata?.let { Json.decodeFromString(it) },
                            reminders = emptyList(), // Do not share reminders
                            specificDetails = entity.specificDetails?.let { Json.decodeFromString(it) },
                            tags = if (isPrivate) emptyList() else entity.tags?.let { Json.decodeFromString(it) } ?: emptyList(),
                            color = entity.color,
                            parentId = entity.parentId,
                            participants = emptyMap(),
                            isSynced = true
                        )
                    }
                }
                call.respond(HttpStatusCode.OK, tasks)
            }
        }
    }
}
