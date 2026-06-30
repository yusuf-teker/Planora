package com.yusufteker.pulse.server.routes

import com.yusufteker.pulse.server.database.DatabaseFactory.dbQuery
import com.yusufteker.pulse.server.database.tables.PlanRoomMembersTable
import com.yusufteker.pulse.server.database.tables.TaskEntity
import com.yusufteker.pulse.server.database.tables.TaskSharedRoomsTable
import com.yusufteker.pulse.server.database.tables.TasksTable
import com.yusufteker.pulse.server.database.tables.UserEntity
import com.yusufteker.pulse.shared.api.CreateTaskRequest
import com.yusufteker.pulse.shared.api.RoomMemberStatus
import com.yusufteker.pulse.shared.api.TaskDto
import com.yusufteker.pulse.shared.api.TaskVisibility
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
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.util.UUID

fun Route.taskRoutes() {
    authenticate("auth-jwt") {

        route("/tasks") {
            // 1. Task/Not oluşturma
            post {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asInt()
                
                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized)
                    return@post
                }

                val request = call.receiveNullable<CreateTaskRequest>()
                if (request == null || request.title.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, "Title cannot be empty")
                    return@post
                }

                val taskId = UUID.randomUUID().toString()

                val createdTaskDto = dbQuery {
                    val user = UserEntity.findById(userId) ?: return@dbQuery null

                    // 1. Create the task in TasksTable
                    val task = TaskEntity.new(taskId) {
                        this.creator = user
                        this.title = request.title
                        this.description = request.description
                        this.startTime = request.startTime
                        this.endTime = request.endTime
                        this.type = request.type
                        this.status = request.status
                        this.visibility = request.visibility
                        this.isRecurring = request.isRecurring
                        this.recurrenceRule = request.recurrenceRule
                        this.isFlexible = request.isFlexible
                        this.isOptional = request.isOptional
                        this.isPostponable = request.isPostponable
                        this.isAllDay = request.isAllDay
                    }

                    // 2. If visibility is ROOM_SHARED and sharedRoomIds is provided, insert into bridge table
                    if (request.visibility == TaskVisibility.ROOM_SHARED && request.sharedRoomIds.isNotEmpty()) {
                        request.sharedRoomIds.forEach { roomId ->
                            // Optional: Verify if user is actually a member of this room before sharing
                            val isMember = PlanRoomMembersTable.selectAll().where {
                                (PlanRoomMembersTable.roomId eq roomId) and
                                (PlanRoomMembersTable.userId eq userId) and
                                (PlanRoomMembersTable.status eq RoomMemberStatus.ACCEPTED)
                            }.count() > 0
                            
                            if (isMember) {
                                TaskSharedRoomsTable.insert {
                                    it[TaskSharedRoomsTable.taskId] = taskId
                                    it[TaskSharedRoomsTable.roomId] = roomId
                                }
                            }
                        }
                    }

                    TaskDto(
                        id = task.id.value,
                        creatorId = user.id.value,
                        title = task.title,
                        description = task.description,
                        startTime = task.startTime,
                        endTime = task.endTime,
                        type = task.type,
                        status = task.status,
                        visibility = task.visibility,
                        sharedRoomIds = request.sharedRoomIds,
                        isRecurring = task.isRecurring,
                        recurrenceRule = task.recurrenceRule,
                        isFlexible = task.isFlexible,
                        isOptional = task.isOptional,
                        isPostponable = task.isPostponable,
                        isAllDay = task.isAllDay
                    )
                }

                if (createdTaskDto != null) {
                    call.respond(HttpStatusCode.Created, createdTaskDto)
                } else {
                    call.respond(HttpStatusCode.InternalServerError, "Failed to create task")
                }
            }

            // 2. Kişinin kendi görevlerini getirme (aylık/haftalık filter)
            get {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asInt()
                
                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized)
                    return@get
                }

                // Optional time filters for weekly/monthly queries
                val fromTime = call.request.queryParameters["from"]?.toLongOrNull()
                val toTime = call.request.queryParameters["to"]?.toLongOrNull()

                val tasks = dbQuery {
                    val query = TasksTable.selectAll().where { TasksTable.creatorId eq userId }

                    val entities = TaskEntity.wrapRows(query).toList()
                    
                    // In-memory filter for time (can also be done in DB query)
                    val filteredEntities = entities.filter {
                        var include = true
                        if (fromTime != null) {
                            include = include && (it.startTime >= fromTime)
                        }
                        if (toTime != null) {
                            include = include && (it.startTime <= toTime)
                        }
                        include
                    }

                    filteredEntities.map { entity ->
                        // If it's shared, fetch the room IDs
                        val roomIds = if (entity.visibility == TaskVisibility.ROOM_SHARED) {
                            TaskSharedRoomsTable.selectAll()
                                .where { TaskSharedRoomsTable.taskId eq entity.id.value }
                                .map { it[TaskSharedRoomsTable.roomId] }
                        } else {
                            emptyList()
                        }

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
                            sharedRoomIds = roomIds,
                            isRecurring = entity.isRecurring,
                            recurrenceRule = entity.recurrenceRule,
                            isFlexible = entity.isFlexible,
                            isOptional = entity.isOptional,
                            isPostponable = entity.isPostponable,
                            isAllDay = entity.isAllDay
                        )
                    }
                }

                call.respond(HttpStatusCode.OK, tasks)
            }
        }

        // 3. Belirli bir Plan Odasındaki TÜM üyelerin o odada paylaşılmış görevleri
        get("/rooms/{roomId}/tasks") {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal?.payload?.getClaim("userId")?.asInt()
            val roomId = call.parameters["roomId"]
            
            if (userId == null || roomId == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid request")
                return@get
            }

            val fromTime = call.request.queryParameters["from"]?.toLongOrNull()
            val toTime = call.request.queryParameters["to"]?.toLongOrNull()

            val tasks = dbQuery {
                // Check if current user is an accepted member of this room
                val isMember = PlanRoomMembersTable.selectAll().where {
                    (PlanRoomMembersTable.roomId eq roomId) and
                    (PlanRoomMembersTable.userId eq userId) and
                    (PlanRoomMembersTable.status eq RoomMemberStatus.ACCEPTED)
                }.count() > 0

                if (!isMember) {
                    return@dbQuery null
                }

                // Find all tasks that are shared in this room
                val sharedTaskIds = TaskSharedRoomsTable.selectAll()
                    .where { TaskSharedRoomsTable.roomId eq roomId }
                    .map { it[TaskSharedRoomsTable.taskId] }

                if (sharedTaskIds.isEmpty()) {
                    return@dbQuery emptyList<TaskDto>()
                }

                // Fetch those tasks
                val query = TasksTable.selectAll().where { TasksTable.id inList sharedTaskIds }
                val entities = TaskEntity.wrapRows(query).toList()

                val filteredEntities = entities.filter {
                    var include = true
                    if (fromTime != null) {
                        include = include && (it.startTime >= fromTime)
                    }
                    if (toTime != null) {
                        include = include && (it.startTime <= toTime)
                    }
                    include
                }

                filteredEntities.map { entity ->
                    // Since these are shared in this room, we can just return this room or all rooms it's shared in.
                    // Let's fetch all rooms it's shared in to be complete.
                    val roomIds = TaskSharedRoomsTable.selectAll()
                        .where { TaskSharedRoomsTable.taskId eq entity.id.value }
                        .map { it[TaskSharedRoomsTable.roomId] }

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
                        sharedRoomIds = roomIds,
                        isRecurring = entity.isRecurring,
                        recurrenceRule = entity.recurrenceRule,
                        isFlexible = entity.isFlexible,
                        isOptional = entity.isOptional,
                        isPostponable = entity.isPostponable,
                        isAllDay = entity.isAllDay
                    )
                }
            }

            if (tasks != null) {
                call.respond(HttpStatusCode.OK, tasks)
            } else {
                call.respond(HttpStatusCode.Forbidden, "You are not a member of this room")
            }
        }
    }
}
