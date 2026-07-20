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

/**
 * Görev ve Plan Odası (PlanRoom) verilerinin kaynağını yöneten repository implementasyonu.
 *
 * Uygulama "offline-first" (çevrimdışı öncelikli) bir mimaride çalışır:
 * - Tüm yazma işlemleri (oluşturma, güncelleme, silme) önce yerel SQLite veritabanına yapılır.
 * - Değişiklikler `isSynced = 0` (senkronize edilmedi) olarak işaretlenir.
 * - Arka planda `syncPendingChanges()` çağrılarak bu bekleyen değişiklikler sunucuya gönderilir.
 * - Bu sayede ağ bağlantısı olmadığında bile uygulama sorunsuz çalışmaya devam eder.
 */
class PlanRepositoryImpl(
    private val planApi: PlanApi,
    private val calendarApi: CalendarApi,
    private val database: PulsyDatabase,
    private val scope: CoroutineScope
) : PlanRepository {

    /**
     * Senkronizasyon işleminin aynı anda birden fazla kez çalışmasını engeller.
     * `syncPendingChanges` çağrıları sıraya alınır; aynı anda sadece bir tanesi çalışır.
     */
    private val syncMutex = Mutex()

    /**
     * ID eşleme haritası için ayrı bir mutex.
     * Geçici yerel ID'lerin sunucu ID'leriyle eşlenmesi sırasında thread-safe erişim sağlar.
     */
    private val mapMutex = Mutex()

    /**
     * Geçici yerel ID'den sunucu tarafından atanan gerçek ID'ye eşleme haritası.
     *
     * Nasıl çalışır:
     * - Çevrimdışıyken oluşturulan görevler "local_xxxx" şeklinde geçici bir ID alır.
     * - Senkronizasyon sırasında sunucu kalıcı bir ID atar; bu eşleme burada saklanır.
     * - Görev üzerindeki sonraki işlemler (güncelleme, silme) için artık sunucu ID'si kullanılır.
     */
    private val localToRemoteIdMap = mutableMapOf<String, String>()

    /**
     * Verilen görev ID'si için gerçek (güncel) ID'yi döner.
     *
     * Bir görev çevrimdışıyken oluşturulup daha sonra senkronize edildiyse,
     * [localToRemoteIdMap] üzerinden asıl sunucu ID'sine ulaşırız.
     * Döngüsel referansları önlemek için `visited` seti kullanılır.
     */
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

    // ─────────────────────────────────────────
    // GÖREV YAZMA İŞLEMLERİ (Create / Update / Delete)
    // ─────────────────────────────────────────

    /**
     * Yeni bir görev oluşturur.
     *
     * Akış:
     * 1. Geçici bir "local_xxxx" ID'siyle görevi hemen yerel DB'ye kaydeder (isSynced = 0).
     * 2. Kullanıcıya anında başarı döner (optimistik güncelleme).
     * 3. Arka planda [syncPendingChanges] çağrılarak görev sunucuya gönderilir.
     */
    override suspend fun createTask(request: CreateTaskRequest, triggerSync: Boolean): Result<TaskDto> {
        return try {
            val currentUserId = 0L
            val localTaskId = "local_${generateUUID()}"

            // Görevi ve oda bağlantısını tek bir transaction içinde kaydet (atomik)
            database.pulsyDatabaseQueries.transaction { // herhangi biri hata alırsa tüm hepsi hata alır
                database.pulsyDatabaseQueries.insertTaskFromRequest(
                    id = localTaskId, creatorId = currentUserId, request = request, isSynced = 0L
                )
                request.sharedRoomIds.forEach { roomId ->
                    database.pulsyDatabaseQueries.insertTaskSharedRoom(taskId = localTaskId, roomId = roomId)
                }
            }

            val localEntity = database.pulsyDatabaseQueries.getTaskById(localTaskId).executeAsOne()
            val localDto = mapTaskEntityToDto(localEntity)

            // Arka planda sunucu senkronizasyonunu başlat (eğer triggerSync true ise)
            if (triggerSync) {
                scope.launch(Dispatchers.IO) { syncPendingChanges() }
            }
            Result.success(localDto)
        } catch (e: Exception) {
            Napier.e(e) { "PlanRepositoryImpl.createTask FAILED: ${e.message}" }
            Result.failure(e)
        }
    }

    /**
     * Mevcut bir görevi günceller.
     *
     * Akış:
     * 1. `getActualTaskId` ile geçici ID varsa sunucu ID'sine çevirir.
     * 2. Görevi yerel DB'de üzerine yazar ve isSynced = 0 yapar.
     * 3. Arka planda [syncPendingChanges] ile sunucuya gönderir.
     */
    override suspend fun updateTask(taskId: String, request: CreateTaskRequest, triggerSync: Boolean): Result<Unit> {
        return try {
            val actualTaskId = getActualTaskId(taskId)

            val existingTask = database.pulsyDatabaseQueries.getTaskById(actualTaskId).executeAsOneOrNull()
            if (existingTask == null) {
                Napier.w { "PlanRepositoryImpl.updateTask: task not found in local DB: $actualTaskId" }
            }
            val currentUserId = existingTask?.creatorId ?: 0L

            // Görevi ve oda bağlantısını atomic transaction içinde güncelle
            database.pulsyDatabaseQueries.transaction {
                database.pulsyDatabaseQueries.insertTaskFromRequest(
                    id = actualTaskId, creatorId = currentUserId, request = request, isSynced = 0L
                )
                database.pulsyDatabaseQueries.deleteTaskSharedRoomsForTask(actualTaskId)
                request.sharedRoomIds.forEach { roomId ->
                    database.pulsyDatabaseQueries.insertTaskSharedRoom(taskId = actualTaskId, roomId = roomId)
                }
            }

            if (triggerSync) {
                scope.launch(Dispatchers.IO) { syncPendingChanges() }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Napier.e(e) { "PlanRepositoryImpl.updateTask FAILED: taskId=$taskId, message=${e.message}" }
            Result.failure(e)
        }
    }

    /**
     * Bir görevi önce yerel DB'den, ardından arka planda sunucudan siler.
     *
     * Önce yerel silme yapıldığı için kullanıcı anında silme geri bildirimini alır.
     * Sunucu silme başarısız olsa bile yerel veri temiz kalır.
     */
    override suspend fun deleteTask(taskId: String): Result<Unit> {
        return try {
            val actualTaskId = getActualTaskId(taskId)

            // 1. Adım: Yerel veritabanından hemen sil
            database.pulsyDatabaseQueries.transaction {
                database.pulsyDatabaseQueries.deleteTaskById(actualTaskId)
            }

            // 2. Adım: Arka planda sunucudan sil (ağ hatası kullanıcıyı etkilemez)
            scope.launch(Dispatchers.IO) {
                try {
                    planApi.deleteTask(actualTaskId)
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

    // ─────────────────────────────────────────
    // SENKRONIZASYON (Offline → Sunucu)
    // ─────────────────────────────────────────

    /**
     * Yerel DB'de `isSynced = 0` olan tüm görevleri sunucuyla senkronize eder.
     *
     * Bu fonksiyon her yazma işlemi sonrası arka planda otomatik olarak çağrılır.
     * [syncMutex] sayesinde aynı anda sadece bir senkronizasyon çalışır.
     *
     * Senkronizasyon sırasında çakışma tespiti (Conflict Detection):
     * - Görev sunucuya gönderilirken, kullanıcı aynı görevi yerel olarak değiştirmiş olabilir.
     * - Ağ isteği tamamlandıktan sonra DB'deki güncel veriyle orijinal veri karşılaştırılır.
     * - Eğer değişiklik tespit edilirse `isSynced = 0` korunur, böylece görev bir sonraki
     *   senkronizasyonda tekrar sunucuya gönderilir.
     */
    override suspend fun syncPendingChanges(): Result<Unit> {
        return syncMutex.withLock {
            try {
                val pendingTasks = database.pulsyDatabaseQueries.getUnsyncedTasks().executeAsList()

                pendingTasks.forEach { entity ->
                    try {
                        // DB entity'sini API isteğine dönüştür
                        val request = buildRequestFromEntity(entity)

                        if (entity.id.startsWith("local_")) {
                            // ── YENİ GÖREV: Sunucuda henüz oluşturulmadı, POST at ──
                            syncNewLocalTask(entity, request)
                        } else {
                            // ── VAR OLAN GÖREV: Sunucuda güncelle, PUT at ──
                            syncExistingTask(entity, request)
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

    /**
     * Geçici yerel ID'li (local_xxxx) bir görevi sunucuya POST ederek oluşturur.
     *
     * Sunucu gerçek ID'yi atadıktan sonra:
     * 1. Yerel ID → Sunucu ID eşlemesi [localToRemoteIdMap]'e eklenir.
     * 2. Yerel görev silinir, sunucu ID'siyle tekrar eklenir.
     * 3. Alt görevlerin parentId'leri yeni ID ile güncellenir.
     * 4. Senkronizasyon sırasında görev yerel olarak değiştirildiyse `isSynced = 0` kalır.
     */
    private suspend fun syncNewLocalTask(
        entity: com.yusufteker.pulse.core.database.TaskEntity,
        request: CreateTaskRequest
    ) {
        val remoteTask = planApi.createTask(request)

        // Geçici ID → Sunucu ID eşlemesini kaydet
        mapMutex.withLock {
            localToRemoteIdMap[entity.id] = remoteTask.id
        }

        database.pulsyDatabaseQueries.transaction {
            val currentLocal = database.pulsyDatabaseQueries.getTaskById(entity.id).executeAsOneOrNull()

            // Ağ isteği süresince yerel değişiklik olup olmadığını kontrol et
            val wasModifiedDuringSync = currentLocal != null && hasTaskChanged(currentLocal, entity)

            // Eski yerel kaydı sil
            database.pulsyDatabaseQueries.deleteExceptionsForTask(entity.id)
            database.pulsyDatabaseQueries.deleteTaskSharedRoomsForTask(entity.id)
            database.pulsyDatabaseQueries.deleteTaskById(entity.id)

            if (wasModifiedDuringSync && currentLocal != null) {
                // Ağ isteği süresince değiştirildiyse: sunucu ID'si ile ama yerel içerik ile kaydet.
                // isSynced = 0 bırakılır ki bir sonraki sync bu değişikliği sunucuya gönderin.
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
                // Değişiklik yoksa sunucudan gelen DTO'yu direkt kaydet (isSynced = 1)
                database.pulsyDatabaseQueries.insertTaskFromDto(remoteTask, isSynced = 1L)
            }

            // Alt görevlerin parentId'sini eski yerel ID'den yeni sunucu ID'sine güncelle
            database.pulsyDatabaseQueries.updateChildTaskParentIds(
                newParentId = remoteTask.id,
                oldParentId = entity.id
            )
            remoteTask.sharedRoomIds.forEach { roomId ->
                database.pulsyDatabaseQueries.insertTaskSharedRoom(taskId = remoteTask.id, roomId = roomId)
            }
        }
    }

    /**
     * Sunucuda zaten var olan bir görevi PUT isteğiyle günceller.
     *
     * 404/403 hata durumunda fallback olarak görevi yeniden oluşturmayı dener.
     * Bu, sunucudan silinen veya izin kaldırılan görevler için geriye dönük uyumluluk sağlar.
     */
    private suspend fun syncExistingTask(
        entity: com.yusufteker.pulse.core.database.TaskEntity,
        request: CreateTaskRequest
    ) {
        try {
            planApi.updateTask(entity.id, request)

            database.pulsyDatabaseQueries.transaction {
                val currentLocal = database.pulsyDatabaseQueries.getTaskById(entity.id).executeAsOneOrNull()

                // Senkronizasyon sırasında yerel değişiklik olmadıysa isSynced = 1 yap
                val wasModifiedDuringSync = currentLocal != null && hasTaskChanged(currentLocal, entity)
                if (!wasModifiedDuringSync) {
                    database.pulsyDatabaseQueries.updateTaskSyncStatus(1L, entity.id)
                }
                // Değişiklik tespit edildiyse isSynced = 0 kalır → sonraki sync bunu tekrar gönderir
            }
        } catch (e: io.ktor.client.plugins.ClientRequestException) {
            if (e.response.status.value == 404 || e.response.status.value == 403) {
                // Sunucuda bulunamayan veya erişim reddedilen görev için yeniden oluşturma dene.
                // Timeout durumunda sonsuz döngüyle veri çoğalmasını önlemek için localId ekliyoruz.
                val fallbackRequest = request.copy(localId = entity.id)
                syncNewLocalTask(entity, fallbackRequest)
            } else {
                throw e
            }
        }
    }

    /**
     * DB entity'sindeki bir alanın senkronizasyon sırasında değişip değişmediğini kontrol eder.
     * Senkronize edilmesi gereken kritik alanları karşılaştırır.
     */
    private fun hasTaskChanged(
        current: com.yusufteker.pulse.core.database.TaskEntity,
        original: com.yusufteker.pulse.core.database.TaskEntity
    ): Boolean {
        return current.title != original.title ||
            current.description != original.description ||
            current.startTime != original.startTime ||
            current.endTime != original.endTime ||
            current.status != original.status ||
            current.specificDetails != original.specificDetails ||
            current.isPinned != original.isPinned
    }

    /**
     * Bir DB entity'sini sunucuya gönderilebilecek [CreateTaskRequest] nesnesine dönüştürür.
     */
    private fun buildRequestFromEntity(entity: com.yusufteker.pulse.core.database.TaskEntity): CreateTaskRequest {
        return CreateTaskRequest(
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
                    Json.decodeFromString<List<com.yusufteker.pulse.shared.api.TaskParticipantDto>>(it)
                        .associate { p -> p.userId to p.name }
                } catch (e: Exception) { emptyMap() }
            } ?: emptyMap(),
            isPinned = entity.isPinned == 1L,
            localId = if (entity.id.startsWith("local_")) entity.id else null
        )
    }

    // ─────────────────────────────────────────
    // OTOMATİK PLANLAMA
    // ─────────────────────────────────────────

    override suspend fun autoScheduleTasks(taskIds: List<String>): Result<Unit> {
        return try {
            val request = com.yusufteker.pulse.shared.api.AutoScheduleRequest(taskIds = taskIds)
            planApi.autoScheduleTasks(request)
            // Sunucu görevleri yeniden zamanladıktan sonra yerel DB'yi güncelle
            fetchMyTasks()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ─────────────────────────────────────────
    // GÖREV OKUMA / FETCH İŞLEMLERİ
    // ─────────────────────────────────────────

    /**
     * Sunucudan kişisel görevleri çeker ve yerel DB ile senkronize eder.
     *
     * - Sunucuda olmayan ancak yerel `isSynced = 1` olan görevler silinir (sunucudan silinmiş demektir).
     * - Henüz senkronize edilmemiş (`isSynced = 0`) yerel görevlerin üzerine yazılmaz.
     */
    override suspend fun fetchMyTasks(fromTime: Long?, toTime: Long?): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val tasks = planApi.getMyTasks(fromTime, toTime)

            database.pulsyDatabaseQueries.transaction {
                val remoteTaskIds = tasks.map { it.id }.toSet()
                val localTasks = if (fromTime != null && toTime != null) {
                    database.pulsyDatabaseQueries.getTasksByTimeRange(fromTime, toTime).executeAsList()
                } else {
                    database.pulsyDatabaseQueries.getAllTasks().executeAsList()
                }

                // Sunucuda olmayan ama isSynced=1 olan görevleri sil (sunucu sili tetikledi)
                localTasks.forEach { localTask ->
                    if (!remoteTaskIds.contains(localTask.id) && localTask.isSynced == 1L) {
                        database.pulsyDatabaseQueries.deleteExceptionsForTask(localTask.id)
                        database.pulsyDatabaseQueries.deleteTaskSharedRoomsForTask(localTask.id)
                        database.pulsyDatabaseQueries.deleteTaskById(localTask.id)
                    }
                }

                // Sunucudan gelen görevleri ekle/güncelle (bekleyen yerel değişikliklere dokunma)
                tasks.forEach { task ->
                    val existingTask = database.pulsyDatabaseQueries.getTaskById(task.id).executeAsOneOrNull()
                    if (existingTask != null && existingTask.isSynced == 0L) {
                        return@forEach // Senkronize edilmemiş yerel değişikliği koru
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

    /**
     * Belirli bir plan odasına ait görevleri sunucudan çekip yerel DB ile senkronize eder.
     */
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

                // Bu odaya ait olup sunucudan kaldırılmış görevleri sil
                localTasks.forEach { localTask ->
                    if (roomLocalTaskIds.contains(localTask.id) &&
                        !remoteTaskIds.contains(localTask.id) &&
                        localTask.isSynced == 1L
                    ) {
                        database.pulsyDatabaseQueries.deleteExceptionsForTask(localTask.id)
                        database.pulsyDatabaseQueries.deleteTaskSharedRoomsForTask(localTask.id)
                        database.pulsyDatabaseQueries.deleteTaskById(localTask.id)
                    }
                }

                // Sunucudan gelen görevleri ekle/güncelle
                tasks.forEach { task ->
                    val existingTask = database.pulsyDatabaseQueries.getTaskById(task.id).executeAsOneOrNull()
                    if (existingTask != null && existingTask.isSynced == 0L) {
                        return@forEach // Senkronize edilmemiş yerel değişikliği koru
                    }
                    database.pulsyDatabaseQueries.insertTaskFromDto(task, isSynced = 1L)
                    database.pulsyDatabaseQueries.deleteTaskSharedRoomsForTask(task.id)
                    task.sharedRoomIds.forEach { room ->
                        database.pulsyDatabaseQueries.insertTaskSharedRoom(taskId = task.id, roomId = room)
                    }
                    // API bazen sharedRoomIds listesini boş döndürebilir; odanın bağlantısını garantile
                    database.pulsyDatabaseQueries.insertTaskSharedRoom(taskId = task.id, roomId = roomId)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ─────────────────────────────────────────
    // GÖREV TAMAMLAMA (Tekrarlı / Tekrarsız)
    // ─────────────────────────────────────────

    /**
     * Bir görev örneğini tamamlandı/tamamlanmadı olarak işaretler.
     *
     * - Tekrarsız görevler: Durum direkt güncellenir ve arka planda sunucuya gönderilir.
     * - Tekrarlı görevler: Sadece o tarih için bir "exception" kaydı oluşturulur.
     *   Bu sayede diğer tekrar günleri etkilenmez.
     */
    override suspend fun completeTaskInstance(taskId: String, dateMs: Long, isCompleted: Boolean): Result<Unit> {
        return try {
            val actualTaskId = getActualTaskId(taskId)
            val task = database.pulsyDatabaseQueries.getTaskById(actualTaskId).executeAsOneOrNull()

            if (task != null && task.isRecurring == 0L) {
                // ── TEKRARSız GÖREV: Status'u güncelle ──
                val newStatus = if (isCompleted) TaskStatus.COMPLETED.name else TaskStatus.PENDING.name
                database.pulsyDatabaseQueries.updateTaskStatus(newStatus, 0L, actualTaskId)

                // Arka planda sunucuya senkronize et
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
            } else if (task != null) {
                // ── TEKRARLI GÖREV: Sadece bu gün için exception ekle ──
                // Diğer tekrar günleri etkilenmez. Her gün bağımsız olarak tamamlanabilir.
                database.pulsyDatabaseQueries.insertTaskException(
                    taskId = actualTaskId,
                    dateMs = dateMs,
                    isCompleted = if (isCompleted) 1L else 0L,
                    isSynced = 0L
                )
                // TODO: Recurring task exception'larını sunucuya senkronize et.
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun joinTask(taskId: String, roomId: String): Result<Unit> {
        return try {
            planApi.joinTask(taskId, roomId)
            
            // Sync current task list locally from server to pull the task we just joined.
            // Or we could wait for FCM. Since they click a link and open the app, 
            // fetching the room tasks right away is a safe approach.
            fetchRoomTasks(roomId)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Napier.e(e) { "PlanRepositoryImpl.joinTask FAILED: taskId=$taskId, roomId=$roomId, message=${e.message}" }
            Result.failure(e)
        }
    }

    // ─────────────────────────────────────────
    // GÖREV OKUMA / GÖZLEMLEME (Observe)
    // ─────────────────────────────────────────

    /**
     * Belirli bir zaman aralığındaki görevleri gözlemler.
     *
     * Tekrarlı görevler için özel işlem:
     * - Her tekrarlı görev [RecurringTaskEvaluator] aracılığıyla belirtilen aralıkta
     *   kaç kez tekrar edeceği hesaplanır.
     * - Her tekrar için "sanal" bir TaskDto oluşturulur. Bu sanal görev ID'si
     *   "anaId_zamanDamgası" formatındadır (örn: "abc123_1720000000000").
     * - Kullanıcının o gün için tamamladığı/tamamlamadığı istisna (exception) kaydı
     *   varsa o tekrarın durumu buna göre ayarlanır.
     */
    override fun observeTasksForRange(fromTimeMs: Long, toTimeMs: Long): Flow<List<TaskDto>> {
        val tasksFlow = database.pulsyDatabaseQueries.getAllTasks().asFlow().mapToList(Dispatchers.IO)
        val exceptionsFlow = database.pulsyDatabaseQueries.getAllTaskExceptions().asFlow().mapToList(Dispatchers.IO)

        return combine(tasksFlow, exceptionsFlow) { taskEntities, exceptionEntities ->
            val result = mutableListOf<TaskDto>()

            taskEntities.forEach { entity ->
                val baseTaskDto = mapTaskEntityToDto(entity)
                val ruleStr = baseTaskDto.recurrenceRule

                if (baseTaskDto.isRecurring && ruleStr != null) {
                    // Tekrarlı görev: Kuralı parse et ve belirtilen aralıktaki tekrarları hesapla
                    val rule = try {
                        Json.decodeFromString<RecurrenceRule>(ruleStr)
                    } catch (e: Exception) { null }

                    if (rule != null) {
                        val occurrences = RecurringTaskEvaluator.generateOccurrences(
                            startTimeMs = baseTaskDto.startTime,
                            rule = rule,
                            rangeStartMs = fromTimeMs,
                            rangeEndMs = toTimeMs
                        )

                        occurrences.forEach { occurrenceMs ->
                            // Bu tekrar için exception (tamamlama istisnası) var mı?
                            val exception = exceptionEntities.find {
                                it.taskId == baseTaskDto.id && it.dateMs == occurrenceMs
                            }
                            val status = if (exception != null && exception.isCompleted == 1L) {
                                TaskStatus.COMPLETED
                            } else {
                                baseTaskDto.status
                            }

                            // Sanal görev ID'si: "anaId_occurrenceMs"
                            val virtualId = "${baseTaskDto.id}_$occurrenceMs"

                            // Bitiş zamanını orijinal süreye göre hesapla
                            val endTime = baseTaskDto.endTime
                            val durationMs = if (endTime != null) endTime - baseTaskDto.startTime else 0L
                            val newEndTime = if (durationMs > 0) occurrenceMs + durationMs else null

                            // Deadline'ı da orijinal ofset ile ilerlet
                            val newSpecificDetails = when (val details = baseTaskDto.specificDetails) {
                                is com.yusufteker.pulse.shared.api.ItemDetails.Task -> {
                                    val oldDeadline = details.deadline
                                    val newDeadline = if (oldDeadline != null) {
                                        val deadlineDiff = oldDeadline - baseTaskDto.startTime
                                        occurrenceMs + deadlineDiff
                                    } else null
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
                        // Kural parse edilemezse görevi başlangıç zamanına göre ekle
                        val effectiveDate = (baseTaskDto.specificDetails as? com.yusufteker.pulse.shared.api.ItemDetails.Task)?.deadline ?: baseTaskDto.startTime
                        val effectiveEndDate = baseTaskDto.endTime ?: effectiveDate
                        if (effectiveDate in fromTimeMs..toTimeMs || effectiveEndDate in fromTimeMs..toTimeMs || (effectiveDate <= fromTimeMs && effectiveEndDate >= toTimeMs)) {
                            result.add(baseTaskDto)
                        }
                    }
                } else {
                    // Tekrarsız görev: Sadece aralıkta başlıyorsa ekle
                    val effectiveDate = (baseTaskDto.specificDetails as? com.yusufteker.pulse.shared.api.ItemDetails.Task)?.deadline ?: baseTaskDto.startTime
                    val effectiveEndDate = baseTaskDto.endTime ?: effectiveDate
                    if (effectiveDate in fromTimeMs..toTimeMs || effectiveEndDate in fromTimeMs..toTimeMs || (effectiveDate <= fromTimeMs && effectiveEndDate >= toTimeMs)) {
                        result.add(baseTaskDto)
                    }
                }
            }

            result.sortedBy { it.startTime }
        }
    }

    /**
     * Tüm görevleri gerçek zamanlı olarak gözlemler (tarih filtresi olmadan).
     * TaskEditor ve NoteEditor gibi detay ekranlarında kullanılır.
     */
    override fun observeAllTasks(): Flow<List<TaskDto>> {
        return database.pulsyDatabaseQueries.getAllTasks()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { entities ->
                entities.map { entity -> mapTaskEntityToDto(entity) }
            }
    }

    /**
     * DB entity'sini sunucu DTO'suna dönüştüren yardımcı fonksiyon.
     * JSON alanlarını parse ederken olası hataları güvenli şekilde yakalar.
     */
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
            aiMetadata = entity.aiMetadata?.let { try { Json.decodeFromString(it) } catch (e: Exception) { null } },
            reminders = entity.reminders?.let { try { Json.decodeFromString(it) } catch (e: Exception) { emptyList() } } ?: emptyList(),
            specificDetails = entity.specificDetails?.let { try { Json.decodeFromString(it) } catch (e: Exception) { null } },
            tags = entity.tags?.let { try { Json.decodeFromString(it) } catch (e: Exception) { emptyList() } } ?: emptyList(),
            color = entity.color,
            parentId = entity.parentId,
            participants = entity.participants?.let { try { Json.decodeFromString<List<com.yusufteker.pulse.shared.api.TaskParticipantDto>>(it) } catch (e: Exception) { emptyList() } } ?: emptyList(),
            isPinned = entity.isPinned == 1L,
            isSynced = entity.isSynced == 1L
        )
    }

    // ─────────────────────────────────────────
    // PLAN ODASI (PlanRoom) İŞLEMLERİ
    // ─────────────────────────────────────────

    override suspend fun fetchMyRooms(): Result<Unit> {
        return try {
            val rooms = planApi.getMyRooms()
            database.pulsyDatabaseQueries.transaction {
                val remoteRoomIds = rooms.map { it.id }.toSet()
                val localRooms = database.pulsyDatabaseQueries.getAllPlanRooms().executeAsList()

                // Sunucuda olmayan ama isSynced=1 olan odaları ve onlara ait görevleri sil
                localRooms.forEach { localRoom ->
                    if (!remoteRoomIds.contains(localRoom.id) && localRoom.isSynced == 1L) {
                        val taskIds = database.pulsyDatabaseQueries.getTaskIdsForRoom(localRoom.id).executeAsList()
                        database.pulsyDatabaseQueries.deleteTaskSharedRoomsForRoom(localRoom.id)
                        if (taskIds.isNotEmpty()) {
                            database.pulsyDatabaseQueries.deleteTasksById(taskIds)
                        }
                        database.pulsyDatabaseQueries.deleteMembersForRoom(localRoom.id)
                        database.pulsyDatabaseQueries.deletePlanRoom(localRoom.id)
                    }
                }

                // Sunucudan gelen odaları ve üyeleri ekle/güncelle
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
                val taskIds = database.pulsyDatabaseQueries.getTaskIdsForRoom(roomId).executeAsList()
                database.pulsyDatabaseQueries.deleteTaskSharedRoomsForRoom(roomId)
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

    // ─────────────────────────────────────────
    // TAKVİM ERİŞİMİ (Calendar Access)
    // ─────────────────────────────────────────

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