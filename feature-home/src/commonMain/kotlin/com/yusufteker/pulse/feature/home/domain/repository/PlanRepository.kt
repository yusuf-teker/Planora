package com.yusufteker.pulse.feature.home.domain.repository

import com.yusufteker.pulse.shared.api.CreatePlanRoomRequest
import com.yusufteker.pulse.shared.api.CreateTaskRequest
import com.yusufteker.pulse.shared.api.InviteUserRequest
import com.yusufteker.pulse.shared.api.PlanRoomDto
import com.yusufteker.pulse.shared.api.TaskDto
import kotlinx.coroutines.flow.Flow

interface PlanRepository {
    // --- TASKS ---
    suspend fun createTask(request: CreateTaskRequest): Result<TaskDto>
    suspend fun updateTask(taskId: String, request: CreateTaskRequest): Result<Unit>
    suspend fun deleteTask(taskId: String): Result<Unit>
    suspend fun completeTaskInstance(taskId: String, dateMs: Long, isCompleted: Boolean): Result<Unit>
    
    suspend fun fetchMyTasks(fromTime: Long? = null, toTime: Long? = null): Result<Unit>
    suspend fun fetchRoomTasks(roomId: String, fromTime: Long? = null, toTime: Long? = null): Result<Unit>
    suspend fun syncPendingChanges(): Result<Unit>
    
    /**
     * Local database'den tüm görevleri Flow olarak dinler (Offline-first)
     */
    fun observeAllTasks(): Flow<List<TaskDto>>

    /**
     * Verilen tarih aralığındaki sanal (virtual) tekrarlı görevleri ve normal görevleri
     * hesaplayıp Flow olarak döndürür.
     */
    fun observeTasksForRange(fromTimeMs: Long, toTimeMs: Long): Flow<List<TaskDto>>


    // --- PLAN ROOMS ---
    suspend fun fetchMyRooms(): Result<Unit>
    suspend fun createPlanRoom(request: CreatePlanRoomRequest): Result<PlanRoomDto>
    suspend fun inviteUserToRoom(roomId: String, request: InviteUserRequest): Result<Unit>
    suspend fun getMyPendingInvitations(): Result<List<PlanRoomDto>>
    suspend fun respondToInvite(roomId: String, accept: Boolean): Result<Unit>
    
    suspend fun renameRoom(roomId: String, name: String): Result<Unit>
    suspend fun deleteRoom(roomId: String): Result<Unit>
    
    fun observeAllPlanRooms(): Flow<List<PlanRoomDto>>
}
