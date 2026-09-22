package com.yusufteker.planora.feature.home.data.repository

import com.yusufteker.planora.core.database.CalendarAccessEntity
import com.yusufteker.planora.feature.home.domain.repository.PlanRepository
import com.yusufteker.planora.shared.api.CreatePlanRoomRequest
import com.yusufteker.planora.shared.api.CreateTaskRequest
import com.yusufteker.planora.shared.api.InviteUserRequest
import com.yusufteker.planora.shared.api.PlanRoomDto
import com.yusufteker.planora.shared.api.TaskDto
import com.yusufteker.planora.shared.api.TaskStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map

/**
 * 4. KONU: Test Çiftleri (Test Doubles) - Fake vs Mock Deseni
 *
 * InMemory Fake PlanRepository implementasyonu.
 *
 * Mock vs Fake Neden Önemlidir?
 * 1. Mock (MockK, Mockito):
 *    - Bir nesnenin davranışını yapay olarak stub'lar (`every { repo.get() } returns x`).
 *    - Testleri kodun iç implementasyon detayına (implementation details) bağımlı kılar.
 *    - Kotlin Multiplatform (KMP) ortak kodunda (commonTest) Mockito çalışmaz, MockK ise
 *      iOS / Native hedeflerinde stabil değildir ve bytecode manipülasyonuna muhtaçtır.
 *
 * 2. Fake (InMemory Test Double):
 *    - Gerçek arayüzü (interface PlanRepository) uygulayan, veriyi bellekte (RAM) tutan
 *      minyatür ve çalışan bir veritabanı gibidir.
 *    - Sıfır 3rd party bağımlılık: %100 saf Kotlin Multiplatform (Android, iOS, Desktop) uyumludur.
 *    - State tabanlı test yapılmasını sağlar; kodun nasıl çalıştığına değil, sonucuna odaklanır.
 */
open class FakePlanRepository : PlanRepository {

    // --- Simülasyon Kontrol Bayrakları ---
    var shouldFailNetwork: Boolean = false
    var networkErrorMessage: String = "Simüle edilmiş ağ bağlantısı hatası"
    var completeTaskResultOverride: Result<Unit>? = null
    var createTaskResultOverride: Result<TaskDto>? = null
    var completeTaskDelayMs: Long = 0L

    // --- InMemory Veri Havuzları (Single Source of Truth) ---
    private val tasksMap = mutableMapOf<String, TaskDto>()
    private val _tasksFlow = MutableStateFlow<List<TaskDto>>(emptyList())

    private val roomsMap = mutableMapOf<String, PlanRoomDto>()
    private val _roomsFlow = MutableStateFlow<List<PlanRoomDto>>(emptyList())

    /**
     * Test senaryolarında başlangıç verisi yüklemek için yardımcı fonksiyon.
     */
    fun seedTasks(tasks: List<TaskDto>) {
        tasksMap.clear()
        tasks.forEach { tasksMap[it.id] = it }
        _tasksFlow.value = tasksMap.values.toList()
    }

    /**
     * Test senaryolarında başlangıç odaları yüklemek için yardımcı fonksiyon.
     */
    fun seedRooms(rooms: List<PlanRoomDto>) {
        roomsMap.clear()
        rooms.forEach { roomsMap[it.id] = it }
        _roomsFlow.value = roomsMap.values.toList()
    }

    /**
     * Bellekteki tüm verileri sıfırlar.
     */
    fun clearAll() {
        tasksMap.clear()
        _tasksFlow.value = emptyList()
        roomsMap.clear()
        _roomsFlow.value = emptyList()
        shouldFailNetwork = false
        completeTaskResultOverride = null
        createTaskResultOverride = null
    }

    // ==========================================
    // TASKS İŞLEMLERİ
    // ==========================================

    override suspend fun createTask(request: CreateTaskRequest, triggerSync: Boolean): Result<TaskDto> {
        createTaskResultOverride?.let { return it }
        if (shouldFailNetwork) {
            return Result.failure(RuntimeException(networkErrorMessage))
        }

        val newId = request.localId ?: "task_${tasksMap.size + 1}"
        val createdTask = TaskDto(
            id = newId,
            creatorId = 1,
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
            parentId = request.parentId,
            aiMetadata = request.aiMetadata,
            reminders = request.reminders,
            specificDetails = request.specificDetails,
            tags = request.tags,
            color = request.color,
            participants = emptyList(),
            isPinned = request.isPinned,
            isSynced = triggerSync
        )

        tasksMap[newId] = createdTask
        _tasksFlow.value = tasksMap.values.toList()
        return Result.success(createdTask)
    }

    override suspend fun updateTask(taskId: String, request: CreateTaskRequest, triggerSync: Boolean): Result<Unit> {
        if (shouldFailNetwork) {
            return Result.failure(RuntimeException(networkErrorMessage))
        }
        val existing = tasksMap[taskId] ?: return Result.failure(NoSuchElementException("Görev bulunamadı: $taskId"))
        val updated = existing.copy(
            title = request.title,
            description = request.description,
            startTime = request.startTime,
            endTime = request.endTime,
            type = request.type,
            visibility = request.visibility,
            sharedRoomIds = request.sharedRoomIds,
            isPinned = request.isPinned
        )
        tasksMap[taskId] = updated
        _tasksFlow.value = tasksMap.values.toList()
        return Result.success(Unit)
    }

    override suspend fun toggleTaskPinLocal(taskId: String, isPinned: Boolean): Result<Unit> {
        val existing = tasksMap[taskId] ?: return Result.failure(NoSuchElementException("Görev bulunamadı: $taskId"))
        tasksMap[taskId] = existing.copy(isPinned = isPinned)
        _tasksFlow.value = tasksMap.values.toList()
        return Result.success(Unit)
    }

    override suspend fun deleteTask(taskId: String): Result<Unit> {
        if (shouldFailNetwork) {
            return Result.failure(RuntimeException(networkErrorMessage))
        }
        val removed = tasksMap.remove(taskId)
        _tasksFlow.value = tasksMap.values.toList()
        return if (removed != null) Result.success(Unit) else Result.failure(NoSuchElementException("Görev silinemedi: $taskId"))
    }

    override suspend fun completeTaskInstance(taskId: String, dateMs: Long, isCompleted: Boolean): Result<Unit> {
        completeTaskResultOverride?.let { return it }
        if (completeTaskDelayMs > 0) {
            delay(completeTaskDelayMs)
        }
        if (shouldFailNetwork) {
            return Result.failure(RuntimeException(networkErrorMessage))
        }
        val task = tasksMap[taskId]
        if (task != null) {
            tasksMap[taskId] = task.copy(status = if (isCompleted) TaskStatus.COMPLETED else TaskStatus.PENDING)
            _tasksFlow.value = tasksMap.values.toList()
        }
        return Result.success(Unit)
    }

    override suspend fun joinTask(taskId: String, roomId: String): Result<Unit> {
        val task = tasksMap[taskId] ?: return Result.failure(NoSuchElementException("Görev bulunamadı"))
        val updatedRooms = (task.sharedRoomIds + roomId).distinct()
        tasksMap[taskId] = task.copy(sharedRoomIds = updatedRooms)
        _tasksFlow.value = tasksMap.values.toList()
        return Result.success(Unit)
    }

    override suspend fun fetchMyTasks(fromTime: Long?, toTime: Long?): Result<Unit> = Result.success(Unit)

    override suspend fun fetchRoomTasks(roomId: String, fromTime: Long?, toTime: Long?): Result<Unit> = Result.success(Unit)

    override suspend fun syncPendingChanges(): Result<Unit> = Result.success(Unit)

    override suspend fun autoScheduleTasks(taskIds: List<String>): Result<Unit> = Result.success(Unit)

    override fun observeAllTasks(): Flow<List<TaskDto>> = _tasksFlow.asStateFlow()

    override fun observeTasksForRange(fromTimeMs: Long, toTimeMs: Long): Flow<List<TaskDto>> {
        return _tasksFlow.map { tasks ->
            tasks.filter { it.startTime in fromTimeMs..toTimeMs }
        }
    }

    // ==========================================
    // PLAN ROOMS İŞLEMLERİ
    // ==========================================

    override suspend fun fetchMyRooms(): Result<Unit> = Result.success(Unit)

    override suspend fun createPlanRoom(request: CreatePlanRoomRequest): Result<PlanRoomDto> {
        if (shouldFailNetwork) {
            return Result.failure(RuntimeException(networkErrorMessage))
        }
        val newRoomId = "room_${roomsMap.size + 1}"
        val room = PlanRoomDto(
            id = newRoomId,
            name = request.name,
            creatorId = 1,
            createdAt = 1000L,
            members = emptyList()
        )
        roomsMap[newRoomId] = room
        _roomsFlow.value = roomsMap.values.toList()
        return Result.success(room)
    }

    override suspend fun inviteUserToRoom(roomId: String, request: InviteUserRequest): Result<Unit> = Result.success(Unit)

    override suspend fun getMyPendingInvitations(): Result<List<PlanRoomDto>> = Result.success(emptyList())

    override suspend fun respondToInvite(roomId: String, accept: Boolean): Result<Unit> = Result.success(Unit)

    override suspend fun renameRoom(roomId: String, name: String): Result<Unit> {
        val existing = roomsMap[roomId] ?: return Result.failure(NoSuchElementException("Oda bulunamadı"))
        roomsMap[roomId] = existing.copy(name = name)
        _roomsFlow.value = roomsMap.values.toList()
        return Result.success(Unit)
    }

    override suspend fun deleteRoom(roomId: String): Result<Unit> {
        roomsMap.remove(roomId)
        _roomsFlow.value = roomsMap.values.toList()
        return Result.success(Unit)
    }

    override suspend fun leaveRoom(roomId: String): Result<Unit> = Result.success(Unit)

    override suspend fun removeMemberFromRoom(roomId: String, targetUserId: Int): Result<Unit> = Result.success(Unit)

    override suspend fun uploadRoomImage(roomId: String, imageBytes: ByteArray): Result<String> = Result.success("https://planora.mock/image.png")

    override fun observeAllPlanRooms(): Flow<List<PlanRoomDto>> = _roomsFlow.asStateFlow()

    // ==========================================
    // DİĞER İŞLEMLER
    // ==========================================

    override suspend fun fetchAccessibleUsers(): Result<Unit> = Result.success(Unit)

    override fun observeAccessibleUsers(): Flow<List<CalendarAccessEntity>> = emptyFlow()

    override suspend fun fetchSharedTasks(userId: Int, from: Long?, to: Long?): Result<List<TaskDto>> = Result.success(emptyList())

    override suspend fun deleteAccount(): Result<Unit> {
        clearAll()
        return Result.success(Unit)
    }
}
