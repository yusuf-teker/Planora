package com.yusufteker.pulse.server.routes

import com.yusufteker.pulse.server.database.DatabaseFactory.dbQuery
import com.yusufteker.pulse.server.database.tables.PlanRoomEntity
import com.yusufteker.pulse.server.database.tables.PlanRoomMembersTable
import com.yusufteker.pulse.server.database.tables.UserEntity
import com.yusufteker.pulse.shared.api.CreatePlanRoomRequest
import com.yusufteker.pulse.shared.api.InviteUserRequest
import com.yusufteker.pulse.shared.api.PlanRoomDto
import com.yusufteker.pulse.shared.api.PlanRoomMemberDto
import com.yusufteker.pulse.shared.api.RespondToInviteRequest
import com.yusufteker.pulse.shared.api.RoomMemberRole
import com.yusufteker.pulse.shared.api.RoomMemberStatus
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receiveNullable
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.time.Instant
import java.util.UUID

fun Route.planRoomRoutes() {
    authenticate("auth-jwt") {
        route("/rooms") {

            // 1. Oda oluşturma
            post {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asInt()
                
                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, "Unauthorized")
                    return@post
                }

                val request = call.receiveNullable<CreatePlanRoomRequest>()
                if (request == null || request.name.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, "Room name cannot be empty")
                    return@post
                }

                val roomId = UUID.randomUUID().toString()

                val newRoom = dbQuery {
                    val user = UserEntity.findById(userId) ?: return@dbQuery null

                    // Create the PlanRoom
                    val room = PlanRoomEntity.new(roomId) {
                        this.name = request.name
                        this.creator = user
                        this.createdAt = Instant.now().toEpochMilli()
                    }

                    // Add the creator as an ACCEPTED ADMIN member
                    PlanRoomMembersTable.insert {
                        it[PlanRoomMembersTable.roomId] = roomId
                        it[PlanRoomMembersTable.userId] = userId
                        it[status] = RoomMemberStatus.ACCEPTED
                        it[role] = RoomMemberRole.ADMIN
                        it[joinedAt] = Instant.now().toEpochMilli()
                    }

                    PlanRoomDto(
                        id = room.id.value,
                        name = room.name,
                        creatorId = user.id.value,
                        createdAt = room.createdAt,
                        members = listOf(
                            PlanRoomMemberDto(
                                roomId = roomId,
                                userId = userId,
                                status = RoomMemberStatus.ACCEPTED,
                                role = RoomMemberRole.ADMIN,
                                joinedAt = Instant.now().toEpochMilli()
                            )
                        )
                    )
                }

                if (newRoom != null) {
                    call.respond(HttpStatusCode.Created, newRoom)
                } else {
                    call.respond(HttpStatusCode.InternalServerError, "Failed to create room")
                }
            }

            // 2. Odaya birini davet etme
            post("/{roomId}/invite") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asInt()
                val roomId = call.parameters["roomId"]
                
                if (userId == null || roomId == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid request")
                    return@post
                }

                val request = call.receiveNullable<InviteUserRequest>()
                if (request == null) {
                    call.respond(HttpStatusCode.BadRequest, "User ID to invite is missing")
                    return@post
                }

                val success = dbQuery {
                    val room = PlanRoomEntity.findById(roomId) ?: return@dbQuery false
                    val targetUser = UserEntity.findById(request.userId) ?: return@dbQuery false

                    // Check if current user is an admin of the room
                    val isAdmin = PlanRoomMembersTable.selectAll().where {
                        (PlanRoomMembersTable.roomId eq roomId) and 
                        (PlanRoomMembersTable.userId eq userId) and 
                        (PlanRoomMembersTable.role eq RoomMemberRole.ADMIN)
                    }.count() > 0

                    if (!isAdmin) return@dbQuery false

                    // Check if target user is already in the room (pending, accepted, etc.)
                    val alreadyMember = PlanRoomMembersTable.selectAll().where {
                        (PlanRoomMembersTable.roomId eq roomId) and 
                        (PlanRoomMembersTable.userId eq targetUser.id.value)
                    }.count() > 0

                    if (alreadyMember) return@dbQuery false

                    // Insert as PENDING
                    PlanRoomMembersTable.insert {
                        it[PlanRoomMembersTable.roomId] = roomId
                        it[PlanRoomMembersTable.userId] = targetUser.id.value
                        it[status] = RoomMemberStatus.PENDING
                        it[role] = RoomMemberRole.MEMBER
                        it[joinedAt] = null
                    }
                    true
                }

                if (success) {
                    call.respond(HttpStatusCode.OK, "User invited successfully")
                } else {
                    call.respond(HttpStatusCode.Forbidden, "Cannot invite user. Either not admin or user already invited.")
                }
            }

            // 3. Kullanıcının bekleyen davetlerini listeleme
            get("/invitations") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asInt()
                
                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized)
                    return@get
                }

                val invitations = dbQuery {
                    PlanRoomMembersTable.selectAll().where {
                        (PlanRoomMembersTable.userId eq userId) and 
                        (PlanRoomMembersTable.status eq RoomMemberStatus.PENDING)
                    }.map { row ->
                        val roomId = row[PlanRoomMembersTable.roomId]
                        val roomEntity = PlanRoomEntity.findById(roomId)
                        
                        PlanRoomDto(
                            id = roomId,
                            name = roomEntity?.name ?: "Unknown Room",
                            creatorId = roomEntity?.creator?.id?.value ?: 0,
                            createdAt = roomEntity?.createdAt ?: 0L,
                            members = emptyList() // Don't need to load all members just for invitation list
                        )
                    }
                }

                call.respond(HttpStatusCode.OK, invitations)
            }

            // 4. Daveti kabul etme veya reddetme
            post("/{roomId}/invitations/respond") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asInt()
                val roomId = call.parameters["roomId"]
                
                if (userId == null || roomId == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid request")
                    return@post
                }

                val request = call.receiveNullable<RespondToInviteRequest>()
                if (request == null) {
                    call.respond(HttpStatusCode.BadRequest, "Accept/Decline status missing")
                    return@post
                }

                val success = dbQuery {
                    val pendingInvite = PlanRoomMembersTable.selectAll().where {
                        (PlanRoomMembersTable.roomId eq roomId) and 
                        (PlanRoomMembersTable.userId eq userId) and 
                        (PlanRoomMembersTable.status eq RoomMemberStatus.PENDING)
                    }.count() > 0

                    if (!pendingInvite) return@dbQuery false

                    PlanRoomMembersTable.update({
                        (PlanRoomMembersTable.roomId eq roomId) and (PlanRoomMembersTable.userId eq userId)
                    }) {
                        it[status] = if (request.accept) RoomMemberStatus.ACCEPTED else RoomMemberStatus.DECLINED
                        if (request.accept) {
                            it[joinedAt] = Instant.now().toEpochMilli()
                        }
                    }
                    true
                }

                if (success) {
                    call.respond(HttpStatusCode.OK, "Invitation responded successfully")
                } else {
                    call.respond(HttpStatusCode.BadRequest, "Invitation not found or already responded")
                }
            }

        }
    }
}
