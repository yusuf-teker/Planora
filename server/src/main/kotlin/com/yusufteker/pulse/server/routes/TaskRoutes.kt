package com.yusufteker.pulse.server.routes

import org.jetbrains.exposed.sql.update
import com.yusufteker.pulse.server.database.tables.UsersTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import com.yusufteker.pulse.server.database.DatabaseFactory.dbQuery
import com.yusufteker.pulse.server.database.tables.PlanRoomMembersTable
import com.yusufteker.pulse.server.database.tables.TaskEntity
import com.yusufteker.pulse.server.database.tables.TaskParticipantsTable
import com.yusufteker.pulse.server.database.tables.TaskSharedRoomsTable
import com.yusufteker.pulse.server.database.tables.TasksTable
import com.yusufteker.pulse.server.database.tables.UserEntity
import com.yusufteker.pulse.shared.api.CreateTaskRequest
import com.yusufteker.pulse.shared.api.AutoScheduleRequest
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
import org.jetbrains.exposed.sql.deleteWhere
import io.ktor.server.routing.put
import io.ktor.server.routing.delete
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.util.UUID
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers
import org.apache.commons.logging.Log

fun Route.taskRoutes() {
    val routeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    authenticate("auth-jwt") {

        route("/tasks") {
            post("/auto-schedule") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asInt()
                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized)
                    return@post
                }
                
                val request = call.receiveNullable<AutoScheduleRequest>()
                if (request == null || request.taskIds.isEmpty()) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid request body")
                    return@post
                }
                
                var success = false
                dbQuery {
                    val tasksToSchedule = TaskEntity.find { 
                        (TasksTable.id inList request.taskIds) and (TasksTable.creatorId eq userId)
                    }.toList()
                    
                    var currentTime = System.currentTimeMillis()
                    
                    tasksToSchedule.forEach { task ->
                        // Schedule each task for 30 mins sequentially
                        task.startTime = currentTime
                        task.endTime = currentTime + (30 * 60 * 1000)
                        task.isFlexible = false
                        
                        currentTime += (30 * 60 * 1000)
                    }
                    success = true
                }
                
                if (success) {
                    call.respond(HttpStatusCode.OK, "Tasks scheduled successfully")
                } else {
                    call.respond(HttpStatusCode.InternalServerError)
                }
            }

            // 1. Task/Not oluşturma
            post {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asInt()
                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized)
                    return@post
                }
                val request = call.receiveNullable<CreateTaskRequest>()
                if (request == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid request body")
                    return@post
                }

                var newTaskDto: TaskDto? = null
                dbQuery {
                    val localId = request.localId
                    val newTaskId = localId ?: UUID.randomUUID().toString()

                    if (localId != null) {
                        val existingTask = TaskEntity.findById(localId)
                        if (existingTask != null) {
                            if (existingTask.creator.id.value == userId) {
                                // Already created, this is an idempotent retry, return the existing task
                                val participantsList = UsersTable.selectAll().where { UsersTable.id inList request.participants.keys }.map {
                                    com.yusufteker.pulse.shared.api.TaskParticipantDto(
                                        userId = it[UsersTable.id].value,
                                        name = it[UsersTable.name],
                                        avatarId = it[UsersTable.avatarId],
                                        profileImageUrl = it[UsersTable.profileImageUrl]
                                    )
                                }
                                newTaskDto = TaskDto(
                                    id = existingTask.id.value,
                                    creatorId = existingTask.creator.id.value,
                                    title = existingTask.title,
                                    description = existingTask.description,
                                    startTime = existingTask.startTime,
                                    endTime = existingTask.endTime,
                                    type = existingTask.type,
                                    status = existingTask.status,
                                    visibility = existingTask.visibility,
                                    isRecurring = existingTask.isRecurring,
                                    recurrenceRule = existingTask.recurrenceRule,
                                    isFlexible = existingTask.isFlexible,
                                    isOptional = existingTask.isOptional,
                                    isPostponable = existingTask.isPostponable,
                                    isAllDay = existingTask.isAllDay,
                                    aiMetadata = existingTask.aiMetadata?.let { Json.decodeFromString(it) },
                                    reminders = existingTask.reminders?.let { Json.decodeFromString(it) } ?: emptyList(),
                                    specificDetails = existingTask.specificDetails?.let { Json.decodeFromString(it) },
                                    tags = existingTask.tags?.let { Json.decodeFromString(it) } ?: emptyList(),
                                    color = existingTask.color,
                                    parentId = existingTask.parentId,
                                    participants = participantsList,
                                    sharedRoomIds = TaskSharedRoomsTable.selectAll().where { TaskSharedRoomsTable.taskId eq existingTask.id.value }.map { it[TaskSharedRoomsTable.roomId] }
                                )
                                return@dbQuery
                            }
                        }
                    }

                    TaskEntity.new(newTaskId) {
                        this.creator = UserEntity[userId]
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
                        this.aiMetadata = request.aiMetadata?.let { Json.encodeToString(it) }
                        this.reminders = if (request.reminders.isNotEmpty()) Json.encodeToString(request.reminders) else null
                        this.specificDetails = request.specificDetails?.let { Json.encodeToString(it) }
                        this.tags = if (request.tags.isNotEmpty()) Json.encodeToString(request.tags) else null
                        this.color = request.color
                        this.parentId = request.parentId
                    }

                    // Insert shared rooms
                    request.sharedRoomIds.forEach { roomIdToInsert ->
                        TaskSharedRoomsTable.insert {
                            it[taskId] = newTaskId
                            it[roomId] = roomIdToInsert
                        }
                    }

                    // Insert participants
                    request.participants.keys.forEach { pId ->
                        TaskParticipantsTable.insert {
                            it[taskId] = newTaskId
                            it[TaskParticipantsTable.userId] = pId
                            it[status] = "ACCEPTED"
                            if (pId == userId) {
                                it[reminders] = if (request.reminders.isNotEmpty()) kotlinx.serialization.json.Json.encodeToString(request.reminders) else null
                            }
                        }
                    }

                    val participantsList = UsersTable.selectAll().where { UsersTable.id inList request.participants.keys }.map {
                        com.yusufteker.pulse.shared.api.TaskParticipantDto(
                            userId = it[UsersTable.id].value,
                            name = it[UsersTable.name],
                            avatarId = it[UsersTable.avatarId],
                            profileImageUrl = it[UsersTable.profileImageUrl]
                        )
                    }

                    newTaskDto = TaskDto(
                        id = newTaskId,
                        creatorId = userId,
                        title = request.title,
                        description = request.description,
                        startTime = request.startTime,
                        endTime = request.endTime,
                        type = request.type,
                        status = request.status,
                        visibility = request.visibility,
                        sharedRoomIds = request.sharedRoomIds,
                        isRecurring = request.isRecurring,
                        recurrenceRule = request.recurrenceRule,
                        isFlexible = request.isFlexible,
                        isOptional = request.isOptional,
                        isPostponable = request.isPostponable,
                        isAllDay = request.isAllDay,
                        aiMetadata = request.aiMetadata,
                        reminders = request.reminders,
                        specificDetails = request.specificDetails,
                        tags = request.tags,
                        color = request.color,
                        parentId = request.parentId,
                        participants = participantsList
                    )
                }

                if (newTaskDto != null) {
                    // Trigger FCM sync for room members if shared
                    if (request.visibility == TaskVisibility.ROOM_SHARED && request.sharedRoomIds.isNotEmpty()) {
                        request.sharedRoomIds.forEach { roomId ->
                            routeScope.launch {
                                com.yusufteker.pulse.server.service.FcmService.sendSyncTriggerToRoomMembers(roomId, excludeUserId = userId)
                            }
                        }
                    }
                    
                    // Send push notifications to newly added participants
                    val toAdd = request.participants.keys.filter { it != userId }
                    if (toAdd.isNotEmpty()) {
                        val creatorName = org.jetbrains.exposed.sql.transactions.transaction {
                            com.yusufteker.pulse.server.database.tables.UsersTable.selectAll().where { com.yusufteker.pulse.server.database.tables.UsersTable.id eq userId }.firstOrNull()?.get(com.yusufteker.pulse.server.database.tables.UsersTable.name) ?: "Birisi"
                        }
                        toAdd.forEach { addedUserId ->
                            routeScope.launch {
                                com.yusufteker.pulse.server.service.FcmService.sendPushToUser(
                                    userId = addedUserId,
                                    title = "Yeni Görev",
                                    body = "$creatorName seni '${request.title}' planına ekledi."
                                )
                            }
                        }
                    }
                    call.respond(HttpStatusCode.Created, newTaskDto)
                } else {
                    call.respond(HttpStatusCode.InternalServerError)
                }
            }


            // Update Task
            put("/{id}") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asInt()
                val taskId = call.parameters["id"]
                
                if (userId == null || taskId == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid request")
                    return@put
                }
                
                // Offline first yapısına geçtiğimiz için, gelen CreateTaskRequest modelini
                // UpdateTaskRequest gibi kullanıyoruz.
                val request = call.receiveNullable<CreateTaskRequest>()
                if (request == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid request body")
                    return@put
                }

                var hasPermission = false
                dbQuery {
                    val taskEntity = TaskEntity.findById(taskId)
                    if (taskEntity != null) {
                        val isOwner = taskEntity.creator.id.value == userId
                        
                        val sharedRoomIds = TaskSharedRoomsTable.selectAll()
                            .where { TaskSharedRoomsTable.taskId eq taskId }
                            .map { it[TaskSharedRoomsTable.roomId] }

                        val isRoomMember = if (sharedRoomIds.isNotEmpty()) {
                            PlanRoomMembersTable.selectAll().where { 
                                (PlanRoomMembersTable.roomId inList sharedRoomIds) and 
                                (PlanRoomMembersTable.userId eq userId) and 
                                (PlanRoomMembersTable.status eq com.yusufteker.pulse.shared.api.RoomMemberStatus.ACCEPTED) 
                            }.count() > 0
                        } else false

                        hasPermission = isOwner || isRoomMember

                        if (hasPermission) {
                            taskEntity.title = request.title
                            taskEntity.description = request.description
                            taskEntity.startTime = request.startTime
                            taskEntity.endTime = request.endTime
                            taskEntity.type = request.type
                            taskEntity.status = request.status
                            taskEntity.visibility = request.visibility
                            taskEntity.isRecurring = request.isRecurring
                            taskEntity.recurrenceRule = request.recurrenceRule
                            taskEntity.isFlexible = request.isFlexible
                            taskEntity.isOptional = request.isOptional
                            taskEntity.isPostponable = request.isPostponable
                            taskEntity.isAllDay = request.isAllDay
                            taskEntity.aiMetadata = request.aiMetadata?.let { Json.encodeToString(it) }
                            taskEntity.reminders = if (request.reminders.isNotEmpty()) Json.encodeToString(request.reminders) else null
                            taskEntity.specificDetails = request.specificDetails?.let { Json.encodeToString(it) }
                            taskEntity.tags = if (request.tags.isNotEmpty()) Json.encodeToString(request.tags) else null
                            taskEntity.color = request.color
                            taskEntity.parentId = request.parentId

                            // Update shared rooms
                            TaskSharedRoomsTable.deleteWhere { TaskSharedRoomsTable.taskId eq taskId }
                            request.sharedRoomIds.forEach { roomIdToInsert ->
                                TaskSharedRoomsTable.insert {
                                    it[TaskSharedRoomsTable.taskId] = taskId
                                    it[roomId] = roomIdToInsert
                                }
                            }

                            // Update participants safely
                            val existingParticipantIds = TaskParticipantsTable.selectAll()
                                .where { TaskParticipantsTable.taskId eq taskId }
                                .map { it[TaskParticipantsTable.userId] }
                            
                            val requestedParticipantIds = request.participants.keys
                            val toRemove = existingParticipantIds - requestedParticipantIds
                            val toAdd = requestedParticipantIds - existingParticipantIds

                            if (toRemove.isNotEmpty()) {
                                TaskParticipantsTable.deleteWhere { 
                                    (TaskParticipantsTable.taskId eq taskId) and (TaskParticipantsTable.userId inList toRemove) 
                                }
                            }

                            toAdd.forEach { pId ->
                                TaskParticipantsTable.insert {
                                    it[TaskParticipantsTable.taskId] = taskId
                                    it[TaskParticipantsTable.userId] = pId
                                    it[TaskParticipantsTable.status] = "ACCEPTED"
                                }
                            }

                            // Update reminders for the current user if they are a participant
                            if (requestedParticipantIds.contains(userId)) {
                                TaskParticipantsTable.update({ (TaskParticipantsTable.taskId eq taskId) and (TaskParticipantsTable.userId eq userId) }) {
                                    it[TaskParticipantsTable.reminders] = if (request.reminders.isNotEmpty()) kotlinx.serialization.json.Json.encodeToString(request.reminders) else null
                                }
                            }
                            
                            // Send push notifications to newly added users
                            if (toAdd.isNotEmpty()) {
                                val creatorName = UsersTable.selectAll().where { UsersTable.id eq userId }.firstOrNull()?.get(UsersTable.name) ?: "Birisi"
                                toAdd.forEach { addedUserId ->
                                    if (addedUserId != userId) {
                                        routeScope.launch {
                                            com.yusufteker.pulse.server.service.FcmService.sendPushToUser(
                                                userId = addedUserId,
                                                title = "Yeni Görev",
                                                body = "$creatorName seni '${request.title}' planına ekledi."
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (!hasPermission) {
                    call.respond(HttpStatusCode.Forbidden, "Not owner or room member")
                    return@put
                }

                if (request.visibility == TaskVisibility.ROOM_SHARED && request.sharedRoomIds.isNotEmpty()) {
                    request.sharedRoomIds.forEach { roomId ->
                        routeScope.launch {
                            com.yusufteker.pulse.server.service.FcmService.sendSyncTriggerToRoomMembers(roomId, excludeUserId = userId)
                        }
                    }
                }
                call.respond(HttpStatusCode.OK, "Updated successfully")
            }

            // Delete Task
            delete("/{id}") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asInt()
                val taskId = call.parameters["id"]
                
                if (userId == null || taskId == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid request")
                    return@delete
                }

                var sharedRoomIdsForDeletedTask = emptyList<String>()

                var hasPermission = false

                dbQuery {
                    // Önce task sahibinin kullanıcı olduğunu doğrula
                    val isOwner = TasksTable.selectAll().where { 
                        (TasksTable.id eq taskId) and (TasksTable.creatorId eq userId)
                    }.count() > 0

                    val taskSharedRoomIds = TaskSharedRoomsTable.selectAll()
                        .where { TaskSharedRoomsTable.taskId eq taskId }
                        .map { it[TaskSharedRoomsTable.roomId] }

                    val isRoomMember = if (taskSharedRoomIds.isNotEmpty()) {
                        PlanRoomMembersTable.selectAll().where { 
                            (PlanRoomMembersTable.roomId inList taskSharedRoomIds) and 
                            (PlanRoomMembersTable.userId eq userId) and 
                            (PlanRoomMembersTable.status eq com.yusufteker.pulse.shared.api.RoomMemberStatus.ACCEPTED) 
                        }.count() > 0
                    } else false

                    hasPermission = isOwner || isRoomMember

                    if (hasPermission) {
                        sharedRoomIdsForDeletedTask = TaskSharedRoomsTable.selectAll()
                            .where { TaskSharedRoomsTable.taskId eq taskId }
                            .map { it[TaskSharedRoomsTable.roomId] }

                        TaskSharedRoomsTable.deleteWhere { TaskSharedRoomsTable.taskId eq taskId }
                        TaskParticipantsTable.deleteWhere { TaskParticipantsTable.taskId eq taskId }
                        TasksTable.deleteWhere { TasksTable.id eq taskId }
                    }
                }

                if (!hasPermission) {
                    call.respond(HttpStatusCode.Forbidden, "Not owner or room member")
                    return@delete
                }

                if (sharedRoomIdsForDeletedTask.isNotEmpty()) {
                    sharedRoomIdsForDeletedTask.forEach { roomId ->
                        routeScope.launch {
                            com.yusufteker.pulse.server.service.FcmService.sendSyncTriggerToRoomMembers(roomId, excludeUserId = userId)
                        }
                    }
                }

                call.respond(HttpStatusCode.OK, "Deleted successfully")
            }

            // 2. Kişinin kendi görevlerini VE odalar aracılığıyla paylaşılan görevleri getirme
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
                    // 1. Kullanıcının kendi oluşturduğu görevler
                    val ownTaskIds = TasksTable.selectAll()
                        .where { TasksTable.creatorId eq userId }
                        .map { it[TasksTable.id].value }
                        .toSet()

                    // 2. Kullanıcının katılımcı olduğu görevler
                    val participantTaskIds = TaskParticipantsTable.selectAll()
                        .where { TaskParticipantsTable.userId eq userId }
                        .map { it[TaskParticipantsTable.taskId] }
                        .toSet()

                    // 3. Kullanıcının üye olduğu odalar aracılığıyla paylaşılan görevler
                    val memberRoomIds = PlanRoomMembersTable.selectAll().where {
                        (PlanRoomMembersTable.userId eq userId) and
                        (PlanRoomMembersTable.status eq RoomMemberStatus.ACCEPTED)
                    }.map { it[PlanRoomMembersTable.roomId] }

                    val sharedViaRoomTaskIds = if (memberRoomIds.isNotEmpty()) {
                        TaskSharedRoomsTable.selectAll()
                            .where { TaskSharedRoomsTable.roomId inList memberRoomIds }
                            .map { it[TaskSharedRoomsTable.taskId] }
                            .toSet()
                    } else {
                        emptySet()
                    }

                    // Tüm benzersiz görev ID'leri
                    val allTaskIds = (ownTaskIds + participantTaskIds + sharedViaRoomTaskIds).toList()

                    if (allTaskIds.isEmpty()) {
                        return@dbQuery emptyList<TaskDto>()
                    }

                    val query = TasksTable.selectAll().where { TasksTable.id inList allTaskIds }
                    val entities = TaskEntity.wrapRows(query).toList()
                    
                    // In-memory filter for time (can also be done in DB query)
                    val filteredEntities = entities.filter {
                        if (it.type == com.yusufteker.pulse.shared.api.TaskType.FOLDER) return@filter true
                        
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
                        
                        val participantsList = (TaskParticipantsTable innerJoin com.yusufteker.pulse.server.database.tables.UsersTable).selectAll()
                            .where { TaskParticipantsTable.taskId eq entity.id.value }
                            .map { 
                                com.yusufteker.pulse.shared.api.TaskParticipantDto(
                                    userId = it[TaskParticipantsTable.userId],
                                    name = it[com.yusufteker.pulse.server.database.tables.UsersTable.name],
                                    avatarId = it[com.yusufteker.pulse.server.database.tables.UsersTable.avatarId],
                                    profileImageUrl = it[com.yusufteker.pulse.server.database.tables.UsersTable.profileImageUrl]
                                ) 
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
                            isAllDay = entity.isAllDay,
                            aiMetadata = entity.aiMetadata?.let { try { Json.decodeFromString(it) } catch(e: Exception) { null } },
                            reminders = TaskParticipantsTable.selectAll()
                                .where { (TaskParticipantsTable.taskId eq entity.id.value) and (TaskParticipantsTable.userId eq userId) }
                                .firstOrNull()?.get(TaskParticipantsTable.reminders)?.let { 
                                    try { kotlinx.serialization.json.Json.decodeFromString<List<Int>>(it) } catch(e: Exception) { emptyList() } 
                                } ?: entity.reminders?.let { try { kotlinx.serialization.json.Json.decodeFromString<List<Int>>(it) } catch(e: Exception) { emptyList() } } ?: emptyList(),
                            specificDetails = entity.specificDetails?.let { try { Json.decodeFromString(it) } catch(e: Exception) { null } },
                            tags = entity.tags?.let { try { Json.decodeFromString(it) } catch(e: Exception) { emptyList() } } ?: emptyList(),
                            color = entity.color,
                            parentId = entity.parentId,
                            participants = participantsList
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

            try {
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
                        if (it.type == com.yusufteker.pulse.shared.api.TaskType.FOLDER) return@filter true
                        
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
                        val roomIds = TaskSharedRoomsTable.selectAll()
                            .where { TaskSharedRoomsTable.taskId eq entity.id.value }
                            .map { it[TaskSharedRoomsTable.roomId] }

                        val participantsList = (TaskParticipantsTable innerJoin com.yusufteker.pulse.server.database.tables.UsersTable).selectAll()
                            .where { TaskParticipantsTable.taskId eq entity.id.value }
                            .map { 
                                com.yusufteker.pulse.shared.api.TaskParticipantDto(
                                    userId = it[TaskParticipantsTable.userId],
                                    name = it[com.yusufteker.pulse.server.database.tables.UsersTable.name],
                                    avatarId = it[com.yusufteker.pulse.server.database.tables.UsersTable.avatarId],
                                    profileImageUrl = it[com.yusufteker.pulse.server.database.tables.UsersTable.profileImageUrl]
                                ) 
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
                            isAllDay = entity.isAllDay,
                            aiMetadata = entity.aiMetadata?.let { try { Json.decodeFromString(it) } catch(e: Exception) { null } },
                            reminders = TaskParticipantsTable.selectAll()
                                .where { (TaskParticipantsTable.taskId eq entity.id.value) and (TaskParticipantsTable.userId eq userId) }
                                .firstOrNull()?.get(TaskParticipantsTable.reminders)?.let { 
                                    try { kotlinx.serialization.json.Json.decodeFromString<List<Int>>(it) } catch(e: Exception) { emptyList() } 
                                } ?: entity.reminders?.let { try { kotlinx.serialization.json.Json.decodeFromString<List<Int>>(it) } catch(e: Exception) { emptyList() } } ?: emptyList(),
                            specificDetails = entity.specificDetails?.let { try { Json.decodeFromString(it) } catch(e: Exception) { null } },
                            tags = entity.tags?.let { try { Json.decodeFromString(it) } catch(e: Exception) { emptyList() } } ?: emptyList(),
                            color = entity.color,
                            parentId = entity.parentId,
                            participants = participantsList
                        )
                    }
                }

                if (tasks != null) {
                    call.respond(HttpStatusCode.OK, tasks)
                } else {
                    // Respond with an empty list instead of 403 to prevent client deserialization crashes
                    // if the client doesn't handle 403 properly.
                    call.respond(HttpStatusCode.OK, emptyList<TaskDto>())
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Return an empty list on failure so the client doesn't crash trying to parse HTML
                call.respond(HttpStatusCode.OK, emptyList<TaskDto>())
            }
        }
    }
}
