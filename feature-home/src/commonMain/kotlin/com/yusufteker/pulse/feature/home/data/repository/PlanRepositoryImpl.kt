package com.yusufteker.pulse.feature.home.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.yusufteker.pulse.core.database.PulsyDatabase
import com.yusufteker.pulse.core.utils.generateUUID
import com.yusufteker.pulse.feature.home.data.api.PlanApi
import com.yusufteker.pulse.feature.home.data.api.CalendarApi
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.yusufteker.pulse.core.utils.RecurringTaskEvaluator
import com.yusufteker.pulse.core.utils.getCurrentTimeMs
import com.yusufteker.pulse.shared.api.RecurrenceRule
import com.yusufteker.pulse.feature.home.data.mapper.insertTaskFromDto
import com.yusufteker.pulse.feature.home.data.mapper.insertTaskFromRequest
import kotlinx.coroutines.withContext
import io.github.aakira.napier.Napier

class PlanRepositoryImpl(
    private val planApi: PlanApi,
    private val calendarApi: CalendarApi,
    private val database: PulsyDatabase,
    private val scope: CoroutineScope
) : PlanRepository {

    private val syncMutex = Mutex()
    private val mapMutex = Mutex()
    private val localToRemoteIdMap = mutableMapOf<String, String>()

    private suspend fun getActualTaskId(taskId: String): String {
        return mapMutex.withLock {
            var actualTaskId = taskId
            val visited = mutableSetOf<String>()
            while (localToRemoteIdMap.containsKey(actualTaskId) && visited.add(actualTaskId)) {
                val next = localToRemoteIdMap[actualTaskId]
                if (next == null || next == actualTaskId) break
                actualTaskId = next
            }
            actualTaskId
        }
    }

    override suspend fun createTask(request: CreateTaskRequest): Result<TaskDto> {
        Napier.d { "PlanRepositoryImpl.createTask: title=${request.title}, type=${request.type}" }
        return try {
            val currentUserId = 0L
            val localTaskId = "local_${generateUUID()}"

            database.pulsyDatabaseQueries.transaction {
                database.pulsyDatabaseQueries.insertTaskFromRequest(
                    id = localTaskId, creatorId = currentUserId, request = request, isSynced = 0L
                )
                request.sharedRoomIds.forEach { roomId ->
                    database.pulsyDatabaseQueries.insertTaskSharedRoom(taskId = localTaskId, roomId = roomId)
                }
            }

            val localEntity = database.pulsyDatabaseQueries.getTaskById(localTaskId).executeAsOne()
            val localDto = mapTaskEntityToDto(localEntity)

            Napier.d { "PlanRepositoryImpl.createTask SUCCESS: localTaskId=$localTaskId, DTO title=${localDto.title}" }
            scope.launch(Dispatchers.IO) { syncPendingChanges() }
            Result.success(localDto)
        } catch (e: Exception) {
            Napier.e(e) { "PlanRepositoryImpl.createTask FAILED: ${e.message}" }
            Result.failure(e)
        }
    }

    override suspend fun updateTask(taskId: String, request: CreateTaskRequest): Result<Unit> {
        Napier.d { "PlanRepositoryImpl.updateTask: taskId=$taskId, title=${request.title}, type=${request.type}" }
        return try {
            val actualTaskId = getActualTaskId(taskId)
            Napier.d { "PlanRepositoryImpl.updateTask -> actualTaskId=$actualTaskId" }

            val existingTask = database.pulsyDatabaseQueries.getTaskById(actualTaskId).executeAsOneOrNull()
            if (existingTask == null) {
                Napier.w { "PlanRepositoryImpl.updateTask: task not found in local DB: $actualTaskId" }
            } else {
                Napier.d { "PlanRepositoryImpl.updateTask: task found in DB, old title=${existingTask.title}" }
            }
            val currentUserId = existingTask?.creatorId ?: 0L

            database.pulsyDatabaseQueries.transaction {
                database.pulsyDatabaseQueries.insertTaskFromRequest(
                    id = actualTaskId, creatorId = currentUserId, request = request, isSynced = 0L
                )
                database.pulsyDatabaseQueries.deleteTaskSharedRoomsForTask(actualTaskId)
                request.sharedRoomIds.forEach { roomId ->
                    database.pulsyDatabaseQueries.insertTaskSharedRoom(taskId = actualTaskId, roomId = roomId)
                }
            }
            Napier.d { "PlanRepositoryImpl.updateTask SUCCESS: actualTaskId=$actualTaskId" }

            scope.launch(Dispatchers.IO) { syncPendingChanges() }
            Result.success(Unit)
        } catch (e: Exception) {
            Napier.e(e) { "PlanRepositoryImpl.updateTask FAILED: taskId=$taskId, message=${e.message}" }
            Result.failure(e)
        }
    }

    override suspend fun deleteTask(taskId: String): Result<Unit> {
        Napier.d { "PlanRepositoryImpl.deleteTask: taskId=$taskId" }
        return try {
            val actualTaskId = getActualTaskId(taskId)
            Napier.d { "PlanRepositoryImpl.deleteTask -> actualTaskId=$actualTaskId" }

            // Veritabanından sil
            database.pulsyDatabaseQueries.transaction {
                database.pulsyDatabaseQueries.deleteTaskById(actualTaskId)
            }
            Napier.d { "PlanRepositoryImpl.deleteTask SUCCESS locally: actualTaskId=$actualTaskId" }

            // Arka planda sunucudan sil
            scope.launch(Dispatchers.IO) {
                try {
                    planApi.deleteTask(actualTaskId)
                    Napier.d { "PlanRepositoryImpl.deleteTask SUCCESS remotely: actualTaskId=$actualTaskId" }
                } catch (e: Exception) {
                    Napier.e(e) { "PlanRepositoryImpl.deleteTask remote sync failed for $actualTaskId: ${e.message}" }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Napier.e(e) { "PlanRepositoryImpl.deleteTask FAILED: taskId=$taskId, message=${e.message}" }
            Result.failure(e)
        }
    }

    override suspend fun syncPendingChanges(): Result<Unit> {
        return syncMutex.withLock {
            try {
                val pendingTasks = database.pulsyDatabaseQueries.getUnsyncedTasks().executeAsList()
                Napier.d { "syncPendingChanges: found ${pendingTasks.size} pending unsynced tasks" }
                pendingTasks.forEach { entity ->
                    try {
                        val request = CreateTaskRequest(
                            title = entity.title,
                            description = entity.description,
                            startTime = entity.startTime,
                            endTime = entity.endTime,
                            type = com.yusufteker.pulse.shared.api.TaskType.valueOf(entity.type),
                            status = com.yusufteker.pulse.shared.api.TaskStatus.valueOf(entity.status),
                            visibility = com.yusufteker.pulse.shared.api.TaskVisibility.valueOf(entity.visibility),
                            sharedRoomIds = database.pulsyDatabaseQueries.getSharedRoomsForTask(entity.id).executeAsList(),
                            isRecurring = entity.isRecurring == 1L,
                            recurrenceRule = entity.recurrenceRule,
                            isFlexible = entity.isFlexible == 1L,
                            isOptional = entity.isOptional == 1L,
                            isPostponable = entity.isPostponable == 1L,
                            isAllDay = entity.isAllDay == 1L,
                            parentId = entity.parentId,
                            aiMetadata = entity.aiMetadata?.let { Json.decodeFromString(it) },
                            reminders = entity.reminders?.let { Json.decodeFromString(it) } ?: emptyList(),
                            specificDetails = entity.specificDetails?.let { Json.decodeFromString(it) },
                            tags = entity.tags?.let { Json.decodeFromString(it) } ?: emptyList(),
                            color = entity.color,
                            participants = entity.participants?.let { 
                                try { 
                                    Json.decodeFromString<List<com.yusufteker.pulse.shared.api.TaskParticipantDto>>(it).associate { p -> p.userId to p.name } 
                                } catch(e: Exception) { emptyMap() } 
                            } ?: emptyMap(),
                            isPinned = entity.isPinned == 1L,
                            localId = if (entity.id.startsWith("local_")) entity.id else null
                        )

                        Napier.d { "syncPendingChanges: processing task ${entity.id}, title=${request.title}, startsWithLocal=${entity.id.startsWith("local_")}" }

                        if (entity.id.startsWith("local_")) {
                            Napier.d { "syncPendingChanges: sending POST to create remote task, title=${request.title}" }
                            val remoteTask = planApi.createTask(request)
                            mapMutex.withLock {
                                localToRemoteIdMap[entity.id] = remoteTask.id
                            }
                            database.pulsyDatabaseQueries.transaction {
                                val currentLocal = database.pulsyDatabaseQueries.getTaskById(entity.id).executeAsOneOrNull()
                                val needsResync = currentLocal != null && (
                                    currentLocal.title != entity.title ||
                                    currentLocal.description != entity.description ||
                                    currentLocal.startTime != entity.startTime ||
                                    currentLocal.endTime != entity.endTime ||
                                    currentLocal.status != entity.status ||
                                    currentLocal.specificDetails != entity.specificDetails ||
                                    currentLocal.isPinned != entity.isPinned
                                )

                                database.pulsyDatabaseQueries.deleteExceptionsForTask(entity.id)
                                database.pulsyDatabaseQueries.deleteTaskSharedRoomsForTask(entity.id)
                                database.pulsyDatabaseQueries.deleteTaskById(entity.id)
                                
                                if (needsResync && currentLocal != null) {
                                    Napier.d { "Local task ${entity.id} was modified during sync, inserting remote task ${remoteTask.id} with local modifications and keeping isSynced = 0" }
                                    database.pulsyDatabaseQueries.insertTask(
                                        id = remoteTask.id,
                                        creatorId = remoteTask.creatorId.toLong(),
                                        title = currentLocal.title,
                                        description = currentLocal.description,
                                        startTime = currentLocal.startTime,
                                        endTime = currentLocal.endTime,
                                        type = currentLocal.type,
                                        status = currentLocal.status,
                                        visibility = currentLocal.visibility,
                                        isRecurring = currentLocal.isRecurring,
                                        recurrenceRule = currentLocal.recurrenceRule,
                                        isFlexible = currentLocal.isFlexible,
                                        isOptional = currentLocal.isOptional,
                                        isPostponable = currentLocal.isPostponable,
                                        isAllDay = currentLocal.isAllDay,
                                        aiMetadata = currentLocal.aiMetadata,
                                        reminders = currentLocal.reminders,
                                        specificDetails = currentLocal.specificDetails,
                                        tags = currentLocal.tags,
                                        color = currentLocal.color,
                                        parentId = currentLocal.parentId,
                                        participants = currentLocal.participants,
                                        isPinned = currentLocal.isPinned,
                                        isSynced = 0L
                                    )
                                } else {
                                    database.pulsyDatabaseQueries.insertTaskFromDto(remoteTask, isSynced = 1L)
                                }
                                
                                database.pulsyDatabaseQueries.updateChildTaskParentIds(newParentId = remoteTask.id, oldParentId = entity.id)
                                remoteTask.sharedRoomIds.forEach { roomId ->
                                    database.pulsyDatabaseQueries.insertTaskSharedRoom(taskId = remoteTask.id, roomId = roomId)
                                }
                            }
                        } else {
                            try {
                                Napier.d { "syncPendingChanges: sending PUT to update remote task ${entity.id}, title=${request.title}" }
                                planApi.updateTask(entity.id, request)
                                database.pulsyDatabaseQueries.transaction {
                                    val currentLocal = database.pulsyDatabaseQueries.getTaskById(entity.id).executeAsOneOrNull()
                                    if (currentLocal != null &&
                                        currentLocal.title == entity.title &&
                                        currentLocal.description == entity.description &&
                                        currentLocal.startTime == entity.startTime &&
                                        currentLocal.endTime == entity.endTime &&
                                        currentLocal.status == entity.status &&
                                        currentLocal.specificDetails == entity.specificDetails &&
                                        currentLocal.isPinned == entity.isPinned
                                    ) {
                                        database.pulsyDatabaseQueries.updateTaskSyncStatus(1L, entity.id)
                                    } else {
                                        Napier.d { "Task ${entity.id} was modified locally during sync, keeping isSynced = 0" }
                                    }
                                }
                            } catch (e: io.ktor.client.plugins.ClientRequestException) {
                                if (e.response.status.value == 404 || e.response.status.value == 403) {
                                    // Geriye dönük uyumluluk veya sunucudan silinmiş görevler için fallback: Yeniden oluştur.
                                    // ID'nin aynı kalması ve timeout durumunda sunucuda sonsuz döngüyle veri çoklanmaması için localId veriyoruz.
                                    val fallbackRequest = request.copy(localId = entity.id)
                                    val remoteTask = planApi.createTask(fallbackRequest)
                                    mapMutex.withLock {
                                        localToRemoteIdMap[entity.id] = remoteTask.id
                                    }
                                    database.pulsyDatabaseQueries.transaction {
                                        val currentLocal = database.pulsyDatabaseQueries.getTaskById(entity.id).executeAsOneOrNull()
                                        val needsResync = currentLocal != null && (
                                            currentLocal.title != entity.title ||
                                            currentLocal.description != entity.description ||
                                            currentLocal.startTime != entity.startTime ||
                                            currentLocal.endTime != entity.endTime ||
                                            currentLocal.status != entity.status ||
                                            currentLocal.specificDetails != entity.specificDetails ||
                                            currentLocal.isPinned != entity.isPinned
                                        )

                                        database.pulsyDatabaseQueries.deleteExceptionsForTask(entity.id)
                                        database.pulsyDatabaseQueries.deleteTaskSharedRoomsForTask(entity.id)
                                        database.pulsyDatabaseQueries.deleteTaskById(entity.id)
                                        
                                        if (needsResync && currentLocal != null) {
                                            database.pulsyDatabaseQueries.insertTask(
                                                id = remoteTask.id,
                                                creatorId = remoteTask.creatorId.toLong(),
                                                title = currentLocal.title,
                                                description = currentLocal.description,
                                                startTime = currentLocal.startTime,
                                                endTime = currentLocal.endTime,
                                                type = currentLocal.type,
                                                status = currentLocal.status,
                                                visibility = currentLocal.visibility,
                                                isRecurring = currentLocal.isRecurring,
                                                recurrenceRule = currentLocal.recurrenceRule,
                                                isFlexible = currentLocal.isFlexible,
                                                isOptional = currentLocal.isOptional,
                                                isPostponable = currentLocal.isPostponable,
                                                isAllDay = currentLocal.isAllDay,
                                                aiMetadata = currentLocal.aiMetadata,
                                                reminders = currentLocal.reminders,
                                                specificDetails = currentLocal.specificDetails,
                                                tags = currentLocal.tags,
                                                color = currentLocal.color,
                                                parentId = currentLocal.parentId,
                                                participants = currentLocal.participants,
                                                isPinned = currentLocal.isPinned,
                                                isSynced = 0L
                                            )
                                        } else {
                                            database.pulsyDatabaseQueries.insertTaskFromDto(remoteTask, isSynced = 1L)
                                        }
                                        remoteTask.sharedRoomIds.forEach { roomId ->
                                            database.pulsyDatabaseQueries.insertTaskSharedRoom(taskId = remoteTask.id, roomId = roomId)
                                        }
                                    }
                                } else {
                                    throw e
                                }
                            }
                        }
                    } catch (e: Exception) {
                        println("Failed to sync task ${entity.id}: ${e.message}")
                    }
                }
                Result.success(Unit)
            } catch (e: Exception) {
                println("Sync pending changes failed: ${e.message}")
                Result.failure(e)
            }
        }
    }
    
    override suspend fun autoScheduleTasks(taskIds: List<String>): Result<Unit> {
        return try {
            val request = com.yusufteker.pulse.shared.api.AutoScheduleRequest(taskIds = taskIds)
            planApi.autoScheduleTasks(request)
            
            // After scheduling, fetch tasks again to update local DB with new times
            fetchMyTasks()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun fetchMyTasks(fromTime: Long?, toTime: Long?): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val tasks = planApi.getMyTasks(fromTime, toTime)

            // Veritabanını güncelle
            database.pulsyDatabaseQueries.transaction {
                val remoteTaskIds = tasks.map { it.id }.toSet()
                val localTasks = if (fromTime != null && toTime != null) {
                    database.pulsyDatabaseQueries.getTasksByTimeRange(fromTime, toTime).executeAsList()
                } else {
                    database.pulsyDatabaseQueries.getAllTasks().executeAsList()
                }
                
                localTasks.forEach { localTask ->
                    if (!remoteTaskIds.contains(localTask.id) && localTask.isSynced == 1L) {
                        database.pulsyDatabaseQueries.deleteExceptionsForTask(localTask.id)
                        database.pulsyDatabaseQueries.deleteTaskSharedRoomsForTask(localTask.id)
                        database.pulsyDatabaseQueries.deleteTaskById(localTask.id)
                    }
                }

                tasks.forEach { task ->
                    val existingTask = database.pulsyDatabaseQueries.getTaskById(task.id).executeAsOneOrNull()
                    if (existingTask != null && existingTask.isSynced == 0L) {
                        return@forEach // Skip overwriting un-synced local changes
                    }
                    database.pulsyDatabaseQueries.insertTaskFromDto(task, isSynced = 1L)
                    database.pulsyDatabaseQueries.deleteTaskSharedRoomsForTask(task.id)
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
                val remoteTaskIds = tasks.map { it.id }.toSet()
                val localTasks = if (fromTime != null && toTime != null) {
                    database.pulsyDatabaseQueries.getTasksByTimeRange(fromTime, toTime).executeAsList()
                } else {
                    database.pulsyDatabaseQueries.getAllTasks().executeAsList()
                }
                val roomLocalTaskIds = database.pulsyDatabaseQueries.getTaskIdsForRoom(roomId).executeAsList().toSet()

                localTasks.forEach { localTask ->
                    if (roomLocalTaskIds.contains(localTask.id) && !remoteTaskIds.contains(localTask.id) && localTask.isSynced == 1L) {
                        database.pulsyDatabaseQueries.deleteExceptionsForTask(localTask.id)
                        database.pulsyDatabaseQueries.deleteTaskSharedRoomsForTask(localTask.id)
                        database.pulsyDatabaseQueries.deleteTaskById(localTask.id)
                    }
                }

                tasks.forEach { task ->
                    val existingTask = database.pulsyDatabaseQueries.getTaskById(task.id).executeAsOneOrNull()
                    if (existingTask != null && existingTask.isSynced == 0L) {
                        return@forEach // Skip overwriting un-synced local changes
                    }
                    database.pulsyDatabaseQueries.insertTaskFromDto(task, isSynced = 1L)
                    database.pulsyDatabaseQueries.deleteTaskSharedRoomsForTask(task.id)
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
            val actualTaskId = getActualTaskId(taskId)

            val task = database.pulsyDatabaseQueries.getTaskById(actualTaskId).executeAsOneOrNull()
            
            if (task != null && task.isRecurring == 0L) {
                // Non-recurring task: update status directly
                val newStatus = if (isCompleted) TaskStatus.COMPLETED.name else TaskStatus.PENDING.name
                database.pulsyDatabaseQueries.updateTaskStatus(newStatus, 0L, actualTaskId)
                
                // Arka planda sunucuya senkronize etmeyi dene
                val dto = mapTaskEntityToDto(task)

                val request = CreateTaskRequest(
                        title = dto.title,
                        description = dto.description,
                        startTime = dto.startTime,
                        endTime = dto.endTime,
                        type = dto.type,
                        status = TaskStatus.valueOf(newStatus),
                        visibility = dto.visibility,
                        sharedRoomIds = dto.sharedRoomIds,
                        isRecurring = dto.isRecurring,
                        recurrenceRule = dto.recurrenceRule,
                        isFlexible = dto.isFlexible,
                        isOptional = dto.isOptional,
                        isPostponable = dto.isPostponable,
                        isAllDay = dto.isAllDay,
                        aiMetadata = dto.aiMetadata,
                        reminders = dto.reminders,
                        participants = dto.participants.associate { it.userId to it.name },
                        specificDetails = dto.specificDetails,
                        tags = dto.tags,
                        color = dto.color,
                        parentId = dto.parentId
                    )
                    scope.launch(Dispatchers.IO) {
                        try {
                            planApi.updateTask(actualTaskId, request)
                            database.pulsyDatabaseQueries.updateTaskStatus(newStatus, 1L, actualTaskId)
                        } catch (e: Exception) {
                            println("Failed to sync task status completion: ${e.message}")
                        }
                    }
                }
             else if (task != null) {
                // Recurring task: insert exception
                database.pulsyDatabaseQueries.insertTaskException(
                    taskId = actualTaskId,
                    dateMs = dateMs,
                    isCompleted = if (isCompleted) 1L else 0L,
                    isSynced = 0L // Not synced yet
                )
                // TODO: Sunucuya recurring task exception sync eklenebilir. Şimdilik sadece PENDING/COMPLETED Tasklar senkronize ediliyor.
            }
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
                val baseTaskDto = mapTaskEntityToDto(entity)

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
                entities.map { entity ->
                    mapTaskEntityToDto(entity)
                }
            }
    }

    private fun mapTaskEntityToDto(entity: com.yusufteker.pulse.core.database.TaskEntity): TaskDto {
        val sharedRooms = database.pulsyDatabaseQueries.getSharedRoomsForTask(taskId = entity.id).executeAsList()
        return TaskDto(
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
                participants = entity.participants?.let { try { Json.decodeFromString<List<com.yusufteker.pulse.shared.api.TaskParticipantDto>>(it) } catch(e: Exception) { emptyList() } } ?: emptyList(),
                isPinned = entity.isPinned == 1L,
                isSynced = entity.isSynced == 1L
            )
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

    override suspend fun fetchAccessibleUsers(): Result<Unit> {
        return try {
            calendarApi.getAccessibleUsers().onSuccess { dtos ->
                database.pulsyDatabaseQueries.transaction {
                    database.pulsyDatabaseQueries.deleteAllCalendarAccess()
                    dtos.forEach { dto ->
                        database.pulsyDatabaseQueries.insertCalendarAccess(
                            userId = dto.userId.toLong(),
                            name = dto.name,
                            username = dto.username,
                            avatarId = dto.avatarId,
                            color = dto.color,
                            profileImageUrl = dto.profileImageUrl
                        )
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeAccessibleUsers(): Flow<List<com.yusufteker.pulse.core.database.CalendarAccessEntity>> {
        return database.pulsyDatabaseQueries.getAllCalendarAccess().asFlow().mapToList(Dispatchers.IO)
    }

    override suspend fun fetchSharedTasks(userId: Int, from: Long?, to: Long?): Result<List<TaskDto>> {
        return calendarApi.getSharedTasks(userId, from, to)
    }
}