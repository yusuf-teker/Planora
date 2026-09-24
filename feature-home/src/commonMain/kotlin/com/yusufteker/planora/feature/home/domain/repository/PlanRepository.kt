package com.yusufteker.planora.feature.home.domain.repository

import com.yusufteker.planora.shared.api.CreatePlanRoomRequest
import com.yusufteker.planora.shared.api.CreateTaskRequest
import com.yusufteker.planora.shared.api.InviteUserRequest
import com.yusufteker.planora.shared.api.PlanRoomDto
import com.yusufteker.planora.shared.api.TaskDto
import kotlinx.coroutines.flow.Flow

interface PlanRepository {
    // --- TASKS ---
    suspend fun createTask(request: CreateTaskRequest, triggerSync: Boolean = true): Result<TaskDto>
    suspend fun updateTask(taskId: String, request: CreateTaskRequest, triggerSync: Boolean = true): Result<Unit>
    suspend fun toggleTaskPinLocal(taskId: String, isPinned: Boolean): Result<Unit>
    suspend fun deleteTask(taskId: String): Result<Unit>
    suspend fun completeTaskInstance(taskId: String, dateMs: Long, isCompleted: Boolean): Result<Unit>
    suspend fun joinTask(taskId: String, roomId: String): Result<Unit>
    
    suspend fun fetchMyTasks(fromTime: Long? = null, toTime: Long? = null): Result<Unit>
    suspend fun fetchRoomTasks(roomId: String, fromTime: Long? = null, toTime: Long? = null): Result<Unit>
    suspend fun syncPendingChanges(): Result<Unit>
    suspend fun autoScheduleTasks(taskIds: List<String>): Result<Unit>
    
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
    suspend fun leaveRoom(roomId: String): Result<Unit>
    suspend fun removeMemberFromRoom(roomId: String, targetUserId: Int): Result<Unit>
    suspend fun uploadRoomImage(roomId: String, imageBytes: ByteArray): Result<String>
    
    fun observeAllPlanRooms(): Flow<List<PlanRoomDto>>

    // --- CALENDAR ACCESS ---
    suspend fun fetchAccessibleUsers(): Result<Unit>
    fun observeAccessibleUsers(): Flow<List<com.yusufteker.planora.core.database.CalendarAccessEntity>>
    suspend fun fetchSharedTasks(userId: Int, from: Long?, to: Long?): Result<List<TaskDto>>

    /**
     * Permanently deletes the user's account and all associated backend & local data.
     */
    suspend fun deleteAccount(): Result<Unit>

    // --- RECYCLE BIN (TRASH) ---
    /**
     * Observes the list of soft-deleted tasks for the currently authenticated user.
     * Deleted tasks are kept for up to 30 days before being automatically purged.
     */
    fun observeDeletedTasks(): Flow<List<com.yusufteker.planora.feature.home.domain.model.DeletedTaskItem>>

    /**
     * Restores a soft-deleted task from the Recycle Bin back to active tasks.
     *
     * @param taskId Unique identifier of the task to restore.
     * @return [Result] indicating success or failure.
     */
    suspend fun restoreDeletedTask(taskId: String): Result<Unit>

    /**
     * Permanently deletes a task from the Recycle Bin.
     *
     * @param taskId Unique identifier of the task to permanently remove.
     * @return [Result] indicating success or failure.
     */
    suspend fun permanentlyDeleteTask(taskId: String): Result<Unit>

    /**
     * Empties the Recycle Bin by permanently deleting all soft-deleted tasks for the user.
     *
     * @return [Result] indicating success or failure.
     */
    suspend fun clearAllDeletedTasks(): Result<Unit>
}

