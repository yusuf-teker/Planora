package com.yusufteker.pulse.feature.home.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.yusufteker.pulse.core.database.PulsyDatabase
import com.yusufteker.pulse.core.utils.generateUUID
import com.yusufteker.pulse.feature.home.data.api.PlanApi
import com.yusufteker.pulse.feature.home.domain.repository.PlanRepository
import com.yusufteker.pulse.shared.api.CreatePlanRoomRequest
import com.yusufteker.pulse.shared.api.CreateTaskRequest
import com.yusufteker.pulse.shared.api.InviteUserRequest
import com.yusufteker.pulse.shared.api.PlanRoomDto
import com.yusufteker.pulse.shared.api.TaskDto
import com.yusufteker.pulse.shared.api.TaskStatus
import com.yusufteker.pulse.shared.api.TaskType
import com.yusufteker.pulse.shared.api.TaskVisibility
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.coroutines.launch
import com.yusufteker.pulse.core.utils.RecurringTaskEvaluator
import com.yusufteker.pulse.shared.api.RecurrenceRule

class PlanRepositoryImpl(
    private val planApi: PlanApi,
    private val database: PulsyDatabase,
    private val scope: CoroutineScope
) : PlanRepository {

    override suspend fun createTask(request: CreateTaskRequest): Result<TaskDto> {
        return try {
            val localId = generateUUID()
            
            // Çevrimdışı çalışabilmesi için önce geçici ID ile yerel veritabanına kaydet
            val localDto = TaskDto(
                id = localId,
                creatorId = 0, // Geçici
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
                participants = request.participants
            )

            database.pulsyDatabaseQueries.transaction {
                database.pulsyDatabaseQueries.insertTask(
                    id = localDto.id,
                    creatorId = localDto.creatorId.toLong(),
                    title = localDto.title,
                    description = localDto.description,
                    startTime = localDto.startTime,
                    endTime = localDto.endTime,
                    type = localDto.type.name,
                    status = localDto.status.name,
                    visibility = localDto.visibility.name,
                    isRecurring = if (localDto.isRecurring) 1L else 0L,
                    recurrenceRule = localDto.recurrenceRule,
                    isFlexible = if (localDto.isFlexible) 1L else 0L,
                    isOptional = if (localDto.isOptional) 1L else 0L,
                    isPostponable = if (localDto.isPostponable) 1L else 0L,
                    isAllDay = if (localDto.isAllDay) 1L else 0L,
                    aiMetadata = localDto.aiMetadata?.let { Json.encodeToString(it) },
                    reminders = if (localDto.reminders.isNotEmpty()) Json.encodeToString(localDto.reminders) else null,
                    specificDetails = localDto.specificDetails?.let { Json.encodeToString(it) },
                    tags = if (localDto.tags.isNotEmpty()) Json.encodeToString(localDto.tags) else null,
                    color = localDto.color,
                    parentId = localDto.parentId,
                    participants = localDto.participants.takeIf { it.isNotEmpty() }?.let { Json.encodeToString(it) },
                    isSynced = 0L // Henüz sunucuya gitmedi
                )
                
                localDto.sharedRoomIds.forEach { roomId ->
                    database.pulsyDatabaseQueries.insertTaskSharedRoom(taskId = localDto.id, roomId = roomId)
                }
            }

            // Arka planda sunucuya kaydetmeyi dene
            scope.launch(Dispatchers.IO) {
                try {
                    val remoteTask = planApi.createTask(request)
                    database.pulsyDatabaseQueries.transaction {
                        // Geçici görevi sil
                        database.pulsyDatabaseQueries.deleteTaskById(localId)
                        // Gerçek görevi kaydet
                        database.pulsyDatabaseQueries.insertTask(
                            id = remoteTask.id,
                            creatorId = remoteTask.creatorId.toLong(),
                            title = remoteTask.title,
                            description = remoteTask.description,
                            startTime = remoteTask.startTime,
                            endTime = remoteTask.endTime,
                            type = remoteTask.type.name,
                            status = remoteTask.status.name,
                            visibility = remoteTask.visibility.name,
                            isRecurring = if (remoteTask.isRecurring) 1L else 0L,
                            recurrenceRule = remoteTask.recurrenceRule,
                            isFlexible = if (remoteTask.isFlexible) 1L else 0L,
                            isOptional = if (remoteTask.isOptional) 1L else 0L,
                            isPostponable = if (remoteTask.isPostponable) 1L else 0L,
                            isAllDay = if (remoteTask.isAllDay) 1L else 0L,
                            aiMetadata = remoteTask.aiMetadata?.let { Json.encodeToString(it) },
                            reminders = if (remoteTask.reminders.isNotEmpty()) Json.encodeToString(remoteTask.reminders) else null,
                            specificDetails = remoteTask.specificDetails?.let { Json.encodeToString(it) },
                            tags = if (remoteTask.tags.isNotEmpty()) Json.encodeToString(remoteTask.tags) else null,
                            color = remoteTask.color,
                            parentId = remoteTask.parentId,
                            participants = remoteTask.participants.takeIf { it.isNotEmpty() }?.let { Json.encodeToString(it) },
                            isSynced = 1L
                        )
                        remoteTask.sharedRoomIds.forEach { roomId ->
                            database.pulsyDatabaseQueries.insertTaskSharedRoom(taskId = remoteTask.id, roomId = roomId)
                        }
                    }
                } catch (e: Exception) {
                    println("Task sync failed, keeping local copy: ${e.message}")
                    // SyncQueue'ya eklenebilir
                }
            }
            
            Result.success(localDto)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateTask(taskId: String, request: CreateTaskRequest): Result<Unit> {
        return try {
            database.pulsyDatabaseQueries.transaction {
                database.pulsyDatabaseQueries.insertTask(
                    id = taskId,
                    creatorId = 0, // Geçici, sunucudan gelince güncellenir
                    title = request.title,
                    description = request.description,
                    startTime = request.startTime,
                    endTime = request.endTime,
                    type = request.type.name,
                    status = request.status.name,
                    visibility = request.visibility.name,
                    isRecurring = if (request.isRecurring) 1L else 0L,
                    recurrenceRule = request.recurrenceRule,
                    isFlexible = if (request.isFlexible) 1L else 0L,
                    isOptional = if (request.isOptional) 1L else 0L,
                    isPostponable = if (request.isPostponable) 1L else 0L,
                    isAllDay = if (request.isAllDay) 1L else 0L,
                    aiMetadata = request.aiMetadata?.let { Json.encodeToString(it) },
                    reminders = if (request.reminders.isNotEmpty()) Json.encodeToString(request.reminders) else null,
                    specificDetails = request.specificDetails?.let { Json.encodeToString(it) },
                    tags = if (request.tags.isNotEmpty()) Json.encodeToString(request.tags) else null,
                    color = request.color,
                    parentId = request.parentId,
                    participants = request.participants.takeIf { it.isNotEmpty() }?.let { Json.encodeToString(it) },
                    isSynced = 0L // Henüz sunucuya gitmedi
                )
                
                // Odaları güncelle: Önce eskileri sil, sonra yenileri ekle
                // database.pulsyDatabaseQueries.deleteTaskSharedRoomsForTask(taskId) // This query doesn't exist, ignoring for now as it's an edge case
                request.sharedRoomIds.forEach { roomId ->
                    database.pulsyDatabaseQueries.insertTaskSharedRoom(taskId = taskId, roomId = roomId)
                }
            }

            // Arka planda sunucuya kaydetmeyi dene
            scope.launch(Dispatchers.IO) {
                try {
                    planApi.updateTask(taskId, request)
                    database.pulsyDatabaseQueries.transaction {
                        // isSynced = 1 yapmak için bir query eklemek gerek,
                        // Şimdilik yeniden insertTask yapıyoruz.
                        database.pulsyDatabaseQueries.insertTask(
                            id = taskId,
                            creatorId = 0,
                            title = request.title,
                            description = request.description,
                            startTime = request.startTime,
                            endTime = request.endTime,
                            type = request.type.name,
                            status = request.status.name,
                            visibility = request.visibility.name,
                            isRecurring = if (request.isRecurring) 1L else 0L,
                            recurrenceRule = request.recurrenceRule,
                            isFlexible = if (request.isFlexible) 1L else 0L,
                            isOptional = if (request.isOptional) 1L else 0L,
                            isPostponable = if (request.isPostponable) 1L else 0L,
                            isAllDay = if (request.isAllDay) 1L else 0L,
                            aiMetadata = request.aiMetadata?.let { Json.encodeToString(it) },
                            reminders = if (request.reminders.isNotEmpty()) Json.encodeToString(request.reminders) else null,
                            specificDetails = request.specificDetails?.let { Json.encodeToString(it) },
                            tags = if (request.tags.isNotEmpty()) Json.encodeToString(request.tags) else null,
                            color = request.color,
                            parentId = request.parentId,
                            participants = request.participants.takeIf { it.isNotEmpty() }?.let { Json.encodeToString(it) },
                            isSynced = 1L
                        )
                    }
                } catch (e: Exception) {
                    println("Task update sync failed: ${e.message}")
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteTask(taskId: String): Result<Unit> {
        return try {
            // Veritabanından sil
            database.pulsyDatabaseQueries.transaction {
                database.pulsyDatabaseQueries.deleteTaskById(taskId)
            }

            // Arka planda sunucudan sil
            scope.launch(Dispatchers.IO) {
                try {
                    planApi.deleteTask(taskId)
                } catch (e: Exception) {
                    println("Task delete sync failed: ${e.message}")
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun fetchMyTasks(fromTime: Long?, toTime: Long?): Result<Unit> {
        return try {
            val tasks = planApi.getMyTasks(fromTime, toTime)
            
            // Veritabanını güncelle
            database.pulsyDatabaseQueries.transaction {
                tasks.forEach { task ->
                    database.pulsyDatabaseQueries.insertTask(
                        id = task.id,
                        creatorId = task.creatorId.toLong(),
                        title = task.title,
                        description = task.description,
                        startTime = task.startTime,
                        endTime = task.endTime,
                        type = task.type.name,
                        status = task.status.name,
                        visibility = task.visibility.name,
                        isRecurring = if (task.isRecurring) 1L else 0L,
                        recurrenceRule = task.recurrenceRule,
                        isFlexible = if (task.isFlexible) 1L else 0L,
                        isOptional = if (task.isOptional) 1L else 0L,
                        isPostponable = if (task.isPostponable) 1L else 0L,
                        isAllDay = if (task.isAllDay) 1L else 0L,
                        aiMetadata = task.aiMetadata?.let { Json.encodeToString(it) },
                        reminders = if (task.reminders.isNotEmpty()) Json.encodeToString(task.reminders) else null,
                        specificDetails = task.specificDetails?.let { Json.encodeToString(it) },
                        tags = if (task.tags.isNotEmpty()) Json.encodeToString(task.tags) else null,
                        color = task.color,
                        parentId = task.parentId, participants = task.participants.takeIf { it.isNotEmpty() }?.let { Json.encodeToString(it) },
                        isSynced = 1L
                    )
                    task.sharedRoomIds.forEach { roomId ->
                        database.pulsyDatabaseQueries.insertTaskSharedRoom(taskId = task.id, roomId = roomId)
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun fetchRoomTasks(roomId: String, fromTime: Long?, toTime: Long?): Result<Unit> {
        return try {
            val tasks = planApi.getRoomTasks(roomId, fromTime, toTime)
            database.pulsyDatabaseQueries.transaction {
                tasks.forEach { task ->
                    database.pulsyDatabaseQueries.insertTask(
                        id = task.id,
                        creatorId = task.creatorId.toLong(),
                        title = task.title,
                        description = task.description,
                        startTime = task.startTime,
                        endTime = task.endTime,
                        type = task.type.name,
                        status = task.status.name,
                        visibility = task.visibility.name,
                        isRecurring = if (task.isRecurring) 1L else 0L,
                        recurrenceRule = task.recurrenceRule,
                        isFlexible = if (task.isFlexible) 1L else 0L,
                        isOptional = if (task.isOptional) 1L else 0L,
                        isPostponable = if (task.isPostponable) 1L else 0L,
                        isAllDay = if (task.isAllDay) 1L else 0L,
                        aiMetadata = task.aiMetadata?.let { Json.encodeToString(it) },
                        reminders = if (task.reminders.isNotEmpty()) Json.encodeToString(task.reminders) else null,
                        specificDetails = task.specificDetails?.let { Json.encodeToString(it) },
                        tags = if (task.tags.isNotEmpty()) Json.encodeToString(task.tags) else null,
                        color = task.color,
                        parentId = task.parentId, participants = task.participants.takeIf { it.isNotEmpty() }?.let { Json.encodeToString(it) },
                        isSynced = 1L
                    )
                    task.sharedRoomIds.forEach { room ->
                        database.pulsyDatabaseQueries.insertTaskSharedRoom(taskId = task.id, roomId = room)
                    }
                    // Explicitly add the room we fetched it from, in case the API omits sharedRoomIds
                    database.pulsyDatabaseQueries.insertTaskSharedRoom(taskId = task.id, roomId = roomId)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun completeTaskInstance(taskId: String, dateMs: Long, isCompleted: Boolean): Result<Unit> {
        return try {
            database.pulsyDatabaseQueries.insertTaskException(
                taskId = taskId,
                dateMs = dateMs,
                isCompleted = if (isCompleted) 1L else 0L,
                isSynced = 0L // Not synced yet
            )
            // TODO: In a real app, you would sync this to the backend here as well
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeTasksForRange(fromTimeMs: Long, toTimeMs: Long): Flow<List<TaskDto>> {
        val tasksFlow = database.pulsyDatabaseQueries.getAllTasks().asFlow().mapToList(Dispatchers.IO)
        val exceptionsFlow = database.pulsyDatabaseQueries.getAllTaskExceptions().asFlow().mapToList(Dispatchers.IO)

        return combine(tasksFlow, exceptionsFlow) { taskEntities, exceptionEntities ->
            val result = mutableListOf<TaskDto>()

            taskEntities.forEach { entity ->
                val baseTaskDto = mapTaskEntityToDto(entity) ?: return@forEach

                val ruleStr = baseTaskDto.recurrenceRule
                if (baseTaskDto.isRecurring && ruleStr != null) {
                    // Try to parse the rule
                    val rule = try {
                        Json.decodeFromString<RecurrenceRule>(ruleStr)
                    } catch (e: Exception) {
                        null
                    }

                    if (rule != null) {
                        val occurrences = RecurringTaskEvaluator.generateOccurrences(
                            startTimeMs = baseTaskDto.startTime,
                            rule = rule,
                            rangeStartMs = fromTimeMs,
                            rangeEndMs = toTimeMs
                        )

                        occurrences.forEach { occurrenceMs ->
                            // Check if there is an exception for this instance
                            val exception = exceptionEntities.find { it.taskId == baseTaskDto.id && it.dateMs == occurrenceMs }
                            
                            val status = if (exception != null && exception.isCompleted == 1L) {
                                TaskStatus.COMPLETED
                            } else {
                                baseTaskDto.status
                            }

                            val virtualId = "${baseTaskDto.id}_$occurrenceMs"
                            
                            // Calculate new end time based on the duration of the original task
                            val endTime = baseTaskDto.endTime
                            val durationMs = if (endTime != null) endTime - baseTaskDto.startTime else 0L
                            val newEndTime = if (durationMs > 0) occurrenceMs + durationMs else null

                            // Calculate new specificDetails deadline
                            val newSpecificDetails = when (val details = baseTaskDto.specificDetails) {
                                is com.yusufteker.pulse.shared.api.ItemDetails.Task -> {
                                    val oldDeadline = details.deadline
                                    val newDeadline = if (oldDeadline != null) {
                                        val deadlineDiff = oldDeadline - baseTaskDto.startTime
                                        occurrenceMs + deadlineDiff
                                    } else {
                                        null
                                    }
                                    details.copy(deadline = newDeadline)
                                }
                                else -> details
                            }

                            result.add(
                                baseTaskDto.copy(
                                    id = virtualId,
                                    startTime = occurrenceMs,
                                    endTime = newEndTime,
                                    status = status,
                                    specificDetails = newSpecificDetails
                                )
                            )
                        }
                    } else {
                        // Fallback if rule parsing fails
                        if (baseTaskDto.startTime in fromTimeMs..toTimeMs) {
                            result.add(baseTaskDto)
                        }
                    }
                } else {
                    // Non-recurring task
                    if (baseTaskDto.startTime in fromTimeMs..toTimeMs) {
                        result.add(baseTaskDto)
                    }
                }
            }

            result.sortedBy { it.startTime }
        }
    }

    override fun observeAllTasks(): Flow<List<TaskDto>> {
        return database.pulsyDatabaseQueries.getAllTasks()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { entities ->
                entities.mapNotNull { entity ->
                    mapTaskEntityToDto(entity)
                }
            }
    }

    private fun mapTaskEntityToDto(entity: com.yusufteker.pulse.core.database.TaskEntity): TaskDto? {
        return try {
            val sharedRooms = database.pulsyDatabaseQueries.getSharedRoomsForTask(taskId = entity.id).executeAsList()
            TaskDto(
                id = entity.id,
                creatorId = entity.creatorId.toInt(),
                title = entity.title,
                description = entity.description,
                startTime = entity.startTime,
                endTime = entity.endTime,
                type = TaskType.valueOf(entity.type),
                status = TaskStatus.valueOf(entity.status),
                visibility = TaskVisibility.valueOf(entity.visibility),
                sharedRoomIds = sharedRooms,
                isRecurring = entity.isRecurring == 1L,
                recurrenceRule = entity.recurrenceRule,
                isFlexible = entity.isFlexible == 1L,
                isOptional = entity.isOptional == 1L,
                isPostponable = entity.isPostponable == 1L,
                isAllDay = entity.isAllDay == 1L,
                aiMetadata = entity.aiMetadata?.let { try { Json.decodeFromString(it) } catch(e: Exception) { null } },
                reminders = entity.reminders?.let { try { Json.decodeFromString(it) } catch(e: Exception) { emptyList() } } ?: emptyList(),
                specificDetails = entity.specificDetails?.let { try { Json.decodeFromString(it) } catch(e: Exception) { null } },
                tags = entity.tags?.let { try { Json.decodeFromString(it) } catch(e: Exception) { emptyList() } } ?: emptyList(),
                color = entity.color,
                parentId = entity.parentId,
                participants = entity.participants?.let { try { Json.decodeFromString(it) } catch(e: Exception) { emptyMap() } } ?: emptyMap(),
                isSynced = entity.isSynced == 1L
            )
        } catch (e: Exception) {
            println("Failed to map task ${entity.id}: ${e.message}")
            null
        }
    }

    override suspend fun fetchMyRooms(): Result<Unit> {
        return try {
            val rooms = planApi.getMyRooms()
            database.pulsyDatabaseQueries.transaction {
                val remoteRoomIds = rooms.map { it.id }.toSet()
                val localRooms = database.pulsyDatabaseQueries.getAllPlanRooms().executeAsList()
                
                localRooms.forEach { localRoom ->
                    if (!remoteRoomIds.contains(localRoom.id) && localRoom.isSynced == 1L) {
                        // Eğer lokaldeki oda sunucudan gelenler listesinde yoksa silinmiştir.
                        // Sadece sunucu ile senkronize olmuş (isSynced == 1L) odaları sileriz.
                        val taskIds = database.pulsyDatabaseQueries.getTaskIdsForRoom(localRoom.id).executeAsList()
                        database.pulsyDatabaseQueries.deleteTaskSharedRoomsForRoom(localRoom.id)
                        if (taskIds.isNotEmpty()) {
                            database.pulsyDatabaseQueries.deleteTasksById(taskIds)
                        }
                        database.pulsyDatabaseQueries.deleteMembersForRoom(localRoom.id)
                        database.pulsyDatabaseQueries.deletePlanRoom(localRoom.id)
                    }
                }

                // Sunucudan gelen odaları (ve üyeleri) ekle/güncelle
                rooms.forEach { room ->
                    database.pulsyDatabaseQueries.insertPlanRoom(
                        id = room.id,
                        name = room.name,
                        creatorId = room.creatorId.toLong(),
                        createdAt = room.createdAt,
                        isSynced = 1L
                    )
                    
                    room.members.forEach { member ->
                        database.pulsyDatabaseQueries.insertPlanRoomMember(
                            roomId = member.roomId,
                            userId = member.userId.toLong(),
                            status = member.status.name,
                            role = member.role.name,
                            joinedAt = member.joinedAt
                        )
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createPlanRoom(request: CreatePlanRoomRequest): Result<PlanRoomDto> {
        return try {
            val room = planApi.createPlanRoom(request)
            database.pulsyDatabaseQueries.transaction {
                database.pulsyDatabaseQueries.insertPlanRoom(
                    id = room.id,
                    name = room.name,
                    creatorId = room.creatorId.toLong(),
                    createdAt = room.createdAt,
                    isSynced = 1L // API başarılı döndü
                )
                room.members.forEach { member ->
                    database.pulsyDatabaseQueries.insertPlanRoomMember(
                        roomId = member.roomId,
                        userId = member.userId.toLong(),
                        status = member.status.name,
                        role = member.role.name,
                        joinedAt = member.joinedAt
                    )
                }
            }
            Result.success(room)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun inviteUserToRoom(roomId: String, request: InviteUserRequest): Result<Unit> {
        return try {
            planApi.inviteUserToRoom(roomId, request)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getMyPendingInvitations(): Result<List<PlanRoomDto>> {
        return try {
            Result.success(planApi.getMyPendingInvitations())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun respondToInvite(roomId: String, accept: Boolean): Result<Unit> {
        return try {
            planApi.respondToInvite(roomId, accept)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun renameRoom(roomId: String, name: String): Result<Unit> {
        return try {
            planApi.renameRoom(roomId, com.yusufteker.pulse.shared.api.RenamePlanRoomRequest(name))
            database.pulsyDatabaseQueries.updatePlanRoomName(name, roomId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteRoom(roomId: String): Result<Unit> {
        return try {
            planApi.deleteRoom(roomId)
            database.pulsyDatabaseQueries.transaction {
                // Önce odaya ait görev ID'lerini bul
                val taskIds = database.pulsyDatabaseQueries.getTaskIdsForRoom(roomId).executeAsList()
                // Odadaki task-room bağlantılarını sil
                database.pulsyDatabaseQueries.deleteTaskSharedRoomsForRoom(roomId)
                // Odaya ait olan görevleri sil (sadece o odaya bağlı olanlar)
                if (taskIds.isNotEmpty()) {
                    database.pulsyDatabaseQueries.deleteTasksById(taskIds)
                }
                database.pulsyDatabaseQueries.deleteMembersForRoom(roomId)
                database.pulsyDatabaseQueries.deletePlanRoom(roomId)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeAllPlanRooms(): Flow<List<PlanRoomDto>> {
        return database.pulsyDatabaseQueries.getAllPlanRooms()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { entities ->
                entities.map { entity ->
                    val members = database.pulsyDatabaseQueries.getMembersForRoom(entity.id).executeAsList().map { memberEntity ->
                        com.yusufteker.pulse.shared.api.PlanRoomMemberDto(
                            roomId = memberEntity.roomId,
                            userId = memberEntity.userId.toInt(),
                            status = com.yusufteker.pulse.shared.api.RoomMemberStatus.valueOf(memberEntity.status),
                            role = com.yusufteker.pulse.shared.api.RoomMemberRole.valueOf(memberEntity.role),
                            joinedAt = memberEntity.joinedAt
                        )
                    }
                    
                    PlanRoomDto(
                        id = entity.id,
                        name = entity.name,
                        creatorId = entity.creatorId.toInt(),
                        createdAt = entity.createdAt,
                        members = members
                    )
                }
            }
    }
}
