package com.yusufteker.planora.feature.home.presentation.task_editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yusufteker.planora.core.utils.getCurrentTimeMs
import com.yusufteker.planora.feature.home.domain.repository.PlanRepository
import com.yusufteker.planora.feature.home.domain.repository.ProfileRepository
import com.yusufteker.planora.shared.api.TaskPriority
import com.yusufteker.planora.shared.api.TaskStatus
import com.yusufteker.planora.shared.api.TaskType
import com.yusufteker.planora.shared.api.TaskVisibility
import com.yusufteker.planora.feature.home.presentation.utils.encodeUrlParameter
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import com.yusufteker.planora.shared.api.RoomMemberStatus
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.getString
import planora.core.generated.resources.Res
import planora.core.generated.resources.*
import com.yusufteker.planora.shared.api.extractBaseTaskId
import io.github.aakira.napier.Napier
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlin.time.Duration.Companion.milliseconds

/**
 * Görev Oluşturma/Düzenleme ekranının durum yönetimini yapan ViewModel.
 * Görevin yerel veritabanından yüklenmesi, güncellenmesi, silinmesi ve
 * katılımcı/hatırlatıcı gibi detayların yönetilmesinden sorumludur.
 */
@OptIn(FlowPreview::class)
class TaskEditorViewModel(
    private val planRepository: PlanRepository,
    private val profileRepository: ProfileRepository,
    private val sessionPreferences: com.yusufteker.planora.core.preferences.SessionPreferences
) : ViewModel() {

    private val _state = MutableStateFlow(TaskEditorState())
    val state = _state.asStateFlow()
    
    // Silme işlemi tetiklendiğinde mükerrer kaydetme isteklerini engellemek için durum takibi.
    private var isDeleted = false

    // Ekrandan tetiklenecek tek seferlik olaylar (Geri dönme, Snackbar gösterme vb.)
    private val _effect = MutableSharedFlow<TaskEditorEffect>()
    val effect = _effect.asSharedFlow()

    // Auto-save tetikleyicisi
    private val _saveTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    // Ekranda çalışan veri dinleme coroutine'inin referansı.
    // Yeni bir yükleme tetiklendiğinde eskisini iptal etmek için kullanılır.
    private var loadJob: Job? = null

    init {

        _saveTrigger
            .debounce(1000L.milliseconds)
            .onEach { autoSaveTask() }
            .launchIn(viewModelScope)

        var cachedRooms = emptyList<com.yusufteker.planora.shared.api.PlanRoomDto>()
        viewModelScope.launch {
            planRepository.observeAllPlanRooms().collect { rooms ->
                cachedRooms = rooms
                _state.update { currentState ->
                    val currentRoomId = currentState.planRoomId ?: currentState.originalTask?.sharedRoomIds?.firstOrNull()
                    val roomName = rooms.find { it.id == currentRoomId }?.name
                    currentState.copy(planRoomName = roomName ?: currentState.planRoomName)
                }
            }
        }
    }

    /**
     * Kullanıcı arayüzünden gelen olayları işleyen ana fonksiyon.
     */
    fun onEvent(event: TaskEditorEvent) {
        when (event) {
            is TaskEditorEvent.OnLoadTask -> loadTask(
                taskId = event.taskId, 
                planRoomId = event.planRoomId, 
                parentId = event.parentId,
                sharedTitle = event.sharedTitle,
                sharedNote = event.sharedNote,
                sharedDate = event.sharedDate,
                sharedSender = event.sharedSender,
                copyFromTaskId = event.copyFromTaskId
            )
            is TaskEditorEvent.TitleChanged -> { 
                _state.update { it.copy(title = event.title) } 
                _saveTrigger.tryEmit(Unit)
            }
            is TaskEditorEvent.DescriptionChanged -> { 
                _state.update { it.copy(description = event.description) } 
                _saveTrigger.tryEmit(Unit)
            }
            
            is TaskEditorEvent.OnDeadlinePickerVisibilityChanged -> _state.update { it.copy(isDeadlinePickerVisible = event.isVisible) }
            is TaskEditorEvent.OnDeadlineSelected -> { 
                _state.update { 
                    it.copy(
                        deadlineDateMs = event.dateMs, 
                        hasDeadline = event.dateMs != null,
                        isDeadlinePickerVisible = false
                    ) 
                } 
                _saveTrigger.tryEmit(Unit)
            }
            is TaskEditorEvent.OnHasDeadlineToggled -> {
                _state.update {
                    if (event.hasDeadline) {
                        it.copy(
                            hasDeadline = true,
                            deadlineDateMs = it.deadlineDateMs ?: getCurrentTimeMs()
                        )
                    } else {
                        it.copy(
                            hasDeadline = false,
                            deadlineDateMs = null,
                            isRecurring = false,
                            recurrenceRule = null,
                            reminders = emptyList()
                        )
                    }
                }
                _saveTrigger.tryEmit(Unit)
            }
            
            is TaskEditorEvent.OnIsRecurringChanged -> { 
                _state.update { it.copy(isRecurring = event.isRecurring) } 
                _saveTrigger.tryEmit(Unit)
            }
            is TaskEditorEvent.OnRepeatPickerVisibilityChanged -> _state.update { it.copy(isRepeatPickerVisible = event.isVisible) }
            is TaskEditorEvent.OnRecurrenceRuleChanged -> {
                _state.update {
                    it.copy(recurrenceRule = event.rule, isRecurring = event.rule != null)
                }
                _saveTrigger.tryEmit(Unit)
            }
            
            is TaskEditorEvent.OnIsOptionalChanged -> { 
                _state.update { it.copy(isOptional = event.isOptional) } 
                _saveTrigger.tryEmit(Unit)
            }
            is TaskEditorEvent.OnReminderPickerVisibilityChanged -> _state.update { it.copy(isReminderPickerVisible = event.isVisible) }
            is TaskEditorEvent.OnReminderToggled -> {
                _state.update {
                    val newReminders = if (it.reminders.contains(event.minutes)) {
                        it.reminders - event.minutes
                    } else {
                        it.reminders + event.minutes
                    }
                    it.copy(reminders = newReminders)
                }
                _saveTrigger.tryEmit(Unit)
            }
            is TaskEditorEvent.StatusChanged -> {
                _state.update { 
                    it.copy(status = if (event.isCompleted) TaskStatus.COMPLETED else TaskStatus.PENDING) 
                }
                _saveTrigger.tryEmit(Unit)
            }
            
            is TaskEditorEvent.OnParticipantPickerVisibilityChanged -> _state.update { it.copy(isParticipantPickerVisible = event.isVisible) }
            is TaskEditorEvent.OnParticipantToggled -> {
                val currentMap = _state.value.participants.toMutableMap()
                if (currentMap.containsKey(event.userId)) {
                    // En az 1 katılımcı bulunması zorunludur.
                    if (currentMap.size > 1) {
                        currentMap.remove(event.userId)
                        _state.update { it.copy(participants = currentMap) }
                        _saveTrigger.tryEmit(Unit)
                    } else {
                        setEffect(TaskEditorEffect.ShowSnackbar("En az 1 katılımcı olmalıdır."))
                    }
                } else {
                    val user = _state.value.roomMembers.find { u -> u.id == event.userId }
                    if (user != null) {
                        currentMap[event.userId] = user.name
                        _state.update { it.copy(participants = currentMap) }
                        _saveTrigger.tryEmit(Unit)
                    }
                }
            }
            is TaskEditorEvent.OnPriorityChanged -> {
                _state.update { it.copy(priority = event.priority, isPriorityPickerVisible = false) }
                _saveTrigger.tryEmit(Unit)
            }
            is TaskEditorEvent.OnPriorityPickerVisibilityChanged -> _state.update { it.copy(isPriorityPickerVisible = event.isVisible) }
            is TaskEditorEvent.SaveClicked -> saveTask()
            is TaskEditorEvent.DeleteClicked -> deleteTask()
            is TaskEditorEvent.OnBackClick -> setEffect(TaskEditorEffect.NavigateBack)
            is TaskEditorEvent.OnDispose -> {}
            is TaskEditorEvent.OnShareClick -> {
                viewModelScope.launch {
                    val sender = sessionPreferences.getUserName() ?: ""
                    val title = _state.value.title.encodeUrlParameter()
                    val note = _state.value.description.encodeUrlParameter()
                    val date = _state.value.deadlineDateMs
                    val senderEncoded = sender.encodeUrlParameter()
                    val url = "https://planora.yusufteker.com/share/task?title=$title&note=$note&date=$date&sender=$senderEncoded"
                    val shareText = """
                        $sender sana bir görev paylaştı:
                        
                        ${_state.value.title}
                        ${_state.value.description}
                        
                        Planora'de aç: $url
                    """.trimIndent()
                    setEffect(TaskEditorEffect.ShareItem(shareText))
                }
            }
        }
    }

    /**
     * Görev verisini yerel veritabanından veya oda üyelerinden yükler.
     * @param taskId Yüklenecek görevin ID'si. Null ise yeni görev oluşturma modudur.
     * @param planRoomId Görevin ait olduğu paylaşımlı oda ID'si (varsa).
     * @param parentId Alt görev ise, bağlı olduğu ana görevin ID'si (varsa).
     */
    private fun loadTask(
        taskId: String?, 
        planRoomId: String?, 
        parentId: String?,
        sharedTitle: String? = null,
        sharedNote: String? = null,
        sharedDate: Long? = null,
        sharedSender: String? = null,
        copyFromTaskId: String? = null
    ) {
        // Varsa önceki dinleme/yükleme coroutine'ini iptal et (Mükerrer akışları önler)
        loadJob?.cancel()

        loadJob = viewModelScope.launch {
            // --- GÖREV OLUŞTURMA MODU (taskId == null) ---
            if (taskId == null) {
                val currentUserId = sessionPreferences.getUserId()?.toIntOrNull()
                val currentUserName = sessionPreferences.getUserName()
                val defaultParticipants = if (currentUserId != null && currentUserName != null && planRoomId != null) {
                    mapOf(currentUserId to currentUserName)
                } else emptyMap()

                val finalNote = buildString {
                    if (!sharedNote.isNullOrBlank()) append(sharedNote)
                    if (!sharedSender.isNullOrBlank()) {
                        if (isNotEmpty()) append("\n\n")
                        append("$sharedSender tarafından paylaşıldı.")
                    }
                }

                val initialDeadline = sharedDate
                _state.value = TaskEditorState(
                    planRoomId = planRoomId, 
                    parentId = parentId, 
                    participants = defaultParticipants,
                    title = sharedTitle ?: "",
                    description = finalNote,
                    hasDeadline = initialDeadline != null,
                    deadlineDateMs = initialDeadline
                )

                if (parentId != null && sharedDate == null) {
                    viewModelScope.launch {
                        planRepository.observeAllTasks().collect { tasks ->
                            val parentItem = tasks.find { it.id == parentId }
                            if (parentItem != null) {
                                val parentStartTime = parentItem.startTime
                                _state.update { currentState ->
                                    if (sharedDate == null) {
                                        currentState.copy(deadlineDateMs = parentStartTime)
                                    } else currentState
                                }
                            }
                        }
                    }
                }
                
                // Eğer baska bir görevden kopyalanıyorsa orijinal görevin detaylarını çekip önceden doldur
                if (!copyFromTaskId.isNullOrBlank()) {
                    val copyBaseId = copyFromTaskId.extractBaseTaskId()
                    val instanceTimestamp = copyFromTaskId.substringAfterLast("_").toLongOrNull()

                    planRepository.observeAllTasks().collect { tasks ->
                        val sourceTask = tasks.find { it.id == copyBaseId && it.type == TaskType.TASK }
                        if (sourceTask != null) {
                            val details = sourceTask.specificDetails as? com.yusufteker.planora.shared.api.ItemDetails.Task
                            val ruleObj = try {
                                sourceTask.recurrenceRule?.let { Json.decodeFromString<com.yusufteker.planora.shared.api.RecurrenceRule>(it) }
                            } catch (e: Exception) { null }

                            val targetRoomId = planRoomId ?: sourceTask.sharedRoomIds.firstOrNull()
                            if (targetRoomId != null) {
                                loadRoomMembers(targetRoomId)
                            }

                            val targetDeadline = instanceTimestamp ?: details?.deadline ?: sourceTask.startTime
                            val todayDeadline = com.yusufteker.planora.core.utils.getTodayWithOriginalTime(targetDeadline)

                            _state.update { currentState ->
                                currentState.copy(
                                    id = null, // Yeni kayıt olarak kalsın!
                                    isCopyMode = true,
                                    title = sourceTask.title,
                                    description = sourceTask.description ?: "",
                                    originalStartTime = todayDeadline,
                                    deadlineDateMs = todayDeadline,
                                    priority = details?.priority ?: TaskPriority.MEDIUM,
                                    isOptional = sourceTask.isOptional,
                                    isRecurring = sourceTask.isRecurring,
                                    recurrenceRule = ruleObj,
                                    reminders = sourceTask.reminders,
                                    participants = sourceTask.participants.associate { it.userId to it.name },
                                    planRoomId = targetRoomId
                                )
                            }
                        }
                    }
                    return@launch
                }
                
                // Oda üyelerini yükle
                if (planRoomId != null) {
                    loadRoomMembers(planRoomId)
                }
                return@launch
            }

            // --- GÖREV DÜZENLEME MODU (taskId != null) ---
            // Sanal tekrarlı görevlerin ID'si 'anaId_zamanDamgasi' şeklindedir.
            // Düzenleme yaparken ana görevi güncellemek için gerçek ID'yi (baseId) çıkartıyoruz.
            val baseId = taskId.extractBaseTaskId()
            _state.update { it.copy(isLoading = true, id = baseId, planRoomId = planRoomId, parentId = parentId) }

            // Oda üyelerini yükle
            if (planRoomId != null) {
                loadRoomMembers(planRoomId)
            }

            // Veritabanındaki tüm görevlerin güncel akışını dinle
            planRepository.observeAllTasks().collect { tasks ->
                val task = tasks.find { it.id == baseId && it.type == TaskType.TASK }
                
                // Bu görevin altında bulunan alt ögeleri (Sub-tasks / Notes) filtrele
                val subItemsList = tasks.filter { it.parentId == baseId }
                
                if (task != null) {
                    val actualPlanRoomId = _state.value.planRoomId ?: task.sharedRoomIds.firstOrNull()
                    if (actualPlanRoomId != null) {
                        if (_state.value.roomMembers.isEmpty()) {
                            loadRoomMembers(actualPlanRoomId)
                        }
                        viewModelScope.launch {
                            planRepository.observeAllPlanRooms().collect { rooms ->
                                val name = rooms.find { it.id == actualPlanRoomId }?.name
                                if (name != null) {
                                    _state.update { it.copy(planRoomName = name) }
                                }
                            }
                        }
                    }

                    val ruleObj = try {
                        task.recurrenceRule?.let { Json.decodeFromString<com.yusufteker.planora.shared.api.RecurrenceRule>(it) }
                    } catch (e: Exception) {
                        null
                    }

                    val details = task.specificDetails as? com.yusufteker.planora.shared.api.ItemDetails.Task
                    val deadline = details?.deadline ?: task.endTime

                    _state.update { currentState ->
                        // Kullanıcının ekranda yaptığı değişikliklerin DB güncellemeleriyle ezilmesini önlüyoruz.
                        // Eğer başlık/açıklama/son tarih henüz değiştirilmemiş veya DB'deki orijinal değer ile aynıysa güncelleriz.
                        val newTitle = if (currentState.title.isBlank() || currentState.title == currentState.originalTask?.title) task.title else currentState.title
                        val newDescription = if (currentState.description.isBlank() || currentState.description == (currentState.originalTask?.description ?: "")) task.description ?: "" else currentState.description
                        val newDeadline = if (currentState.deadlineDateMs == (currentState.originalTask?.specificDetails as? com.yusufteker.planora.shared.api.ItemDetails.Task)?.deadline) deadline else currentState.deadlineDateMs

                        currentState.copy(
                            originalTask = task,
                            title = newTitle,
                            description = newDescription,
                            originalStartTime = task.startTime,
                            hasDeadline = newDeadline != null,
                            deadlineDateMs = newDeadline,
                            planRoomId = actualPlanRoomId,
                            status = task.status,
                            priority = details?.priority ?: com.yusufteker.planora.shared.api.TaskPriority.MEDIUM,
                            isRecurring = task.isRecurring,
                            recurrenceRule = ruleObj,
                            isOptional = task.isOptional,
                            reminders = task.reminders,
                            participants = task.participants.associate { it.userId to it.name },
                            subItems = subItemsList,
                            isLoading = false,
                            parentId = task.parentId
                        )
                    }
                } else {
                    val errorMsg = getString(Res.string.error_task_not_found)
                    Napier.w { "TaskEditorViewModel loadTask: task $baseId not found in DB list!" }
                    _state.update { it.copy(isLoading = false, error = errorMsg) }
                }
            }
        }
    }

    /**
     * Odanın üyelerini planRepository üzerinden çeker ve profil bilgilerini tamamlar.
     * `CoroutineScope` üzerinden extension fonksiyon olarak tanımlanmıştır.
     * Bu sayede üst coroutine (loadJob) iptal edildiğinde akış dinleme otomatik olarak sonlanır.
     * Yeni bir görev oluşturulurken odadaki tüm üyeler varsayılan olarak katılımcı olarak eklenir.
     */
    private fun CoroutineScope.loadRoomMembers(planRoomId: String) {
        launch {
            _state.update { it.copy(isRoomMembersLoading = true) }
            var defaultParticipantsApplied = false
            planRepository.observeAllPlanRooms().collect { rooms ->
                val room = rooms.find { it.id == planRoomId }
                if (room != null) {
                    val validMembers = room.members.filter { it.status != RoomMemberStatus.DECLINED }
                    val profiles = kotlinx.coroutines.coroutineScope {
                        validMembers.map { member ->
                            async {
                                profileRepository.getProfile(member.userId.toString()).getOrNull()
                            }
                        }.awaitAll().filterNotNull()
                    }
                    _state.update { currentState ->
                        val isNewTask = currentState.id == null && !currentState.isCopyMode
                        val newParticipants = if (!defaultParticipantsApplied && isNewTask && profiles.isNotEmpty()) {
                            defaultParticipantsApplied = true
                            val map = profiles.associate { p -> p.id to p.name }.toMutableMap()
                            val currentUserId = sessionPreferences.getUserId()?.toIntOrNull()
                            val currentUserName = sessionPreferences.getUserName()
                            if (currentUserId != null && currentUserName != null && !map.containsKey(currentUserId)) {
                                map[currentUserId] = currentUserName
                            }
                            map
                        } else {
                            currentState.participants
                        }

                        currentState.copy(
                            roomMembers = profiles,
                            isRoomMembersLoading = false,
                            planRoomName = currentState.planRoomName ?: room.name,
                            participants = newParticipants
                        )
                    }
                } else {
                    _state.update { it.copy(isRoomMembersLoading = false) }
                }
            }
        }
    }

    /**
     * Sadece yerel veritabanına kaydeder (debounce sonrası). API isteği atılmaz.
     */
    private fun autoSaveTask() {
        if (isDeleted) return
        val currentState = _state.value
        if (currentState.isDeleted || currentState.isLoading || currentState.title.isBlank()) return
        
        // Yeni görevler için (id null iken) otomatik kaydetmeyi devre dışı bırak
        if (currentState.id == null) return

        val request = buildCreateTaskRequest(currentState)

        viewModelScope.launch(Dispatchers.IO) {
            planRepository.updateTask(currentState.id, request, triggerSync = false)
        }
    }

    /**
     * Görevi yerel veritabanına kaydeder ve arka planda sunucu senkronizasyonunu tetikler.
     */
    @OptIn(DelicateCoroutinesApi::class)
    private fun saveTask() {
        if (isDeleted) return
        val currentState = _state.value
        
        // Eğer görev zaten silindiyse tekrar kaydedilmesini engeller.
        if (currentState.isDeleted) return
        if (currentState.title.isBlank()) {
            setEffect(TaskEditorEffect.ShowSnackbar("Lütfen bir başlık girin."))
            return
        }

        val request = buildCreateTaskRequest(currentState)

        viewModelScope.launch(Dispatchers.IO) {
            _state.update { it.copy(isLoading = true) }
            val isSuccess = if (currentState.id != null) {
                planRepository.updateTask(currentState.id, request, triggerSync = true).isSuccess
            } else {
                planRepository.createTask(request, triggerSync = true).isSuccess
            }
            _state.update { it.copy(isLoading = false) }
            
            if (isSuccess) {
                setEffect(TaskEditorEffect.NavigateBack)
            } else {
                setEffect(TaskEditorEffect.ShowSnackbar("Görev güncellenemedi, lütfen tekrar deneyin."))
            }
        }
    }

    /**
     * Mevcut UI State'inden API ve Veritabanı için CreateTaskRequest nesnesi hazırlar.
     */
    private fun buildCreateTaskRequest(state: TaskEditorState): com.yusufteker.planora.shared.api.CreateTaskRequest {
        val effectiveDeadline = if (state.hasDeadline) state.deadlineDateMs else null
        val now = state.originalStartTime ?: effectiveDeadline ?: getCurrentTimeMs()
        val recurrenceStr = if (state.hasDeadline) state.recurrenceRule?.let { Json.encodeToString(it) } else null

        return com.yusufteker.planora.shared.api.CreateTaskRequest(
            title = state.title,
            description = state.description.ifBlank { null },
            startTime = now,
            endTime = state.originalTask?.endTime,
            type = state.originalTask?.type ?: TaskType.TASK,
            status = state.status,
            visibility = if (state.planRoomId != null) TaskVisibility.ROOM_SHARED else (state.originalTask?.visibility ?: TaskVisibility.PRIVATE),
            sharedRoomIds = state.planRoomId?.let { listOf(it) } ?: emptyList(),
            isRecurring = if (state.hasDeadline) (state.isRecurring || state.recurrenceRule != null) else false,
            recurrenceRule = recurrenceStr,
            isFlexible = !state.hasDeadline || (state.originalTask?.isFlexible ?: true),
            isOptional = state.isOptional,
            isPostponable = state.originalTask?.isPostponable ?: true,
            isAllDay = state.originalTask?.isAllDay ?: false,
            reminders = if (state.hasDeadline) state.reminders else emptyList(),
            participants = state.participants,
            specificDetails = com.yusufteker.planora.shared.api.ItemDetails.Task(
                subtasks = state.originalTask?.specificDetails?.let { (it as? com.yusufteker.planora.shared.api.ItemDetails.Task)?.subtasks } ?: emptyList(),
                priority = state.priority,
                deadline = effectiveDeadline
            ),
            parentId = state.parentId,
            tags = state.originalTask?.tags ?: emptyList(),
            color = state.originalTask?.color,
            isPinned = state.originalTask?.isPinned ?: false
        )
    }

    /**
     * Görevi yerel veritabanından siler ve sunucu silme işlemini arka planda başlatır.
     */
    private fun deleteTask() {
        val taskId = _state.value.id ?: return
        isDeleted = true

        _state.update { it.copy(isDeleted = true) }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val result = planRepository.deleteTask(taskId)
            _state.update { it.copy(isLoading = false) }
            if (result.isSuccess) {
                setEffect(TaskEditorEffect.NavigateBack)
            } else {
                val error = result.exceptionOrNull()
                Napier.e(error) { "TaskEditorViewModel.deleteTask FAILED: ${error?.message}" }
                setEffect(TaskEditorEffect.ShowSnackbar("Görev silinemedi, lütfen tekrar deneyin."))
            }
        }
    }

    /**
     * Ekran efektlerini (Snackbar, Geri Yönlendirme vb.) arayüze iletir.
     */
    private fun setEffect(effect: TaskEditorEffect) {
        viewModelScope.launch {
            _effect.emit(effect)
        }
    }
}
