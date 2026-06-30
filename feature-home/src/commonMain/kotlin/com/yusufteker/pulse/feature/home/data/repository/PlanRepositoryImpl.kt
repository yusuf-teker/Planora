package com.yusufteker.pulse.feature.home.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.yusufteker.pulse.core.database.PulseDatabase
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PlanRepositoryImpl(
    private val planApi: PlanApi,
    private val database: PulseDatabase
) : PlanRepository {

    override suspend fun createTask(request: CreateTaskRequest): Result<TaskDto> {
        return try {
            val task = planApi.createTask(request)
            // Çevrimdışı çalışabilmesi için veritabanına kaydet
            database.pulseDatabaseQueries.transaction {
                database.pulseDatabaseQueries.insertTask(
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
                    isAllDay = if (task.isAllDay) 1L else 0L
                )
                
                task.sharedRoomIds.forEach { roomId ->
                    database.pulseDatabaseQueries.insertTaskSharedRoom(taskId = task.id, roomId = roomId)
                }
            }
            Result.success(task)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun fetchMyTasks(fromTime: Long?, toTime: Long?): Result<Unit> {
        return try {
            val tasks = planApi.getMyTasks(fromTime, toTime)
            
            // Veritabanını güncelle
            database.pulseDatabaseQueries.transaction {
                tasks.forEach { task ->
                    database.pulseDatabaseQueries.insertTask(
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
                        isAllDay = if (task.isAllDay) 1L else 0L
                    )
                    task.sharedRoomIds.forEach { roomId ->
                        database.pulseDatabaseQueries.insertTaskSharedRoom(taskId = task.id, roomId = roomId)
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
            database.pulseDatabaseQueries.transaction {
                tasks.forEach { task ->
                    database.pulseDatabaseQueries.insertTask(
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
                        isAllDay = if (task.isAllDay) 1L else 0L
                    )
                    task.sharedRoomIds.forEach { room ->
                        database.pulseDatabaseQueries.insertTaskSharedRoom(taskId = task.id, roomId = room)
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeAllTasks(): Flow<List<TaskDto>> {
        return database.pulseDatabaseQueries.getAllTasks()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { entities ->
                entities.map { entity ->
                    val sharedRooms = database.pulseDatabaseQueries.getSharedRoomsForTask(taskId = entity.id).executeAsList()
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
                        isAllDay = entity.isAllDay == 1L
                    )
                }
            }
    }

    override suspend fun fetchMyRooms(): Result<Unit> {
        return try {
            val rooms = planApi.getMyRooms()
            database.pulseDatabaseQueries.transaction {
                // Şimdilik sadece sunucudan gelenleri güncelliyoruz/ekliyoruz.
                // Eğer ileride tam çevrimdışı silme desteği (isSync) eklersek, sadece senkronize olanları silip yenilerini yazacağız.
                rooms.forEach { room ->
                    database.pulseDatabaseQueries.insertPlanRoom(
                        id = room.id,
                        name = room.name,
                        creatorId = room.creatorId.toLong(),
                        createdAt = room.createdAt
                    )
                    
                    room.members.forEach { member ->
                        database.pulseDatabaseQueries.insertPlanRoomMember(
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
            database.pulseDatabaseQueries.transaction {
                database.pulseDatabaseQueries.insertPlanRoom(
                    id = room.id,
                    name = room.name,
                    creatorId = room.creatorId.toLong(),
                    createdAt = room.createdAt
                )
                room.members.forEach { member ->
                    database.pulseDatabaseQueries.insertPlanRoomMember(
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

    override fun observeAllPlanRooms(): Flow<List<PlanRoomDto>> {
        return database.pulseDatabaseQueries.getAllPlanRooms()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { entities ->
                entities.map { entity ->
                    val members = database.pulseDatabaseQueries.getMembersForRoom(entity.id).executeAsList().map { memberEntity ->
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
