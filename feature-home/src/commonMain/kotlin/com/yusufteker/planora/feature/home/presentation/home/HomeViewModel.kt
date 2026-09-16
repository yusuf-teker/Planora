package com.yusufteker.planora.feature.home.presentation.home

import com.yusufteker.planora.core.base.BaseViewModel
import com.yusufteker.planora.core.preferences.SessionPreferences
import com.yusufteker.planora.core.utils.getCurrentTimeMs

import com.yusufteker.planora.feature.home.domain.repository.PlanRepository
import com.yusufteker.planora.shared.api.CreateTaskRequest
import com.yusufteker.planora.shared.api.ItemDetails
import com.yusufteker.planora.shared.api.TaskPriority
import com.yusufteker.planora.shared.api.TaskType
import com.yusufteker.planora.shared.api.extractBaseTaskId

import com.yusufteker.planora.core.utils.TimelineViewOption
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.Instant
import kotlinx.datetime.toInstant
import kotlinx.datetime.atStartOfDayIn
import com.yusufteker.planora.feature.home.domain.use_case.GetFilteredTasksUseCase
import com.yusufteker.planora.feature.home.domain.use_case.getEarliestEventDateInMonth
import com.yusufteker.planora.feature.home.domain.use_case.SubmitSmartInputUseCase
import com.yusufteker.planora.feature.home.presentation.home.HomeEffect.*
import com.yusufteker.planora.feature.home.data.mapper.toCreateTaskRequest
import com.yusufteker.planora.shared.api.TaskDto
import com.yusufteker.planora.shared.api.TaskStatus
import com.yusufteker.planora.shared.api.isUnscheduled
import io.github.aakira.napier.Napier

/**
 * ViewModel for the Home (Dashboard) screen.
 *
 * Manages home feed state and navigation.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val planRepository: PlanRepository,
    private val sessionPreferences: SessionPreferences,
    private val getFilteredTasksUseCase: GetFilteredTasksUseCase,
    private val submitSmartInputUseCase: SubmitSmartInputUseCase,
    private val holidayRepository: com.yusufteker.planora.core.holiday.HolidayRepository
) : BaseViewModel<HomeState, HomeEvent, HomeEffect>(
    initialState = HomeState()
) {
    private val fetchedMonths = mutableSetOf<LocalDate>()
    private val loadedHolidayYears = mutableSetOf<Int>()
    private var isInitialFetchCompleted = false

    override fun onCleared() {
        Napier.d ("HomeViewModel CLEARED: ${this.hashCode()}")
        super.onCleared()
    }
    init {

        Napier.d("HomeViewModel CREATED: ${this.hashCode()}")

        val today = Instant.fromEpochMilliseconds(getCurrentTimeMs()).toLocalDateTime(TimeZone.currentSystemDefault()).date
        val currentMonthStart = LocalDate(today.year, today.monthNumber, 1)
        setState { copy(visibleCalendarMonth = currentMonthStart) }
        fetchedMonths.add(currentMonthStart)

        // Başlangıçta sadece ±1 yıl aralığındaki tatilleri çek.
        // Diğer yıllar kullanıcı takvimde o yıla gittiğinde lazy yüklenir (CalendarMonthChanged event'i).
        for (y in (today.year - 1)..(today.year + 1)) {
            loadHolidaysForYear(y)
        }
        // Load stored filter options and view option.
        // NOTE: We intentionally do NOT recalculate upcomingTasks here to avoid a race condition
        // with the DB observer coroutine below. If preferences load after the DB emits its first
        // value, recalculating here would temporarily overwrite upcomingTasks using the stale
        // (empty) allFetchedTasks — causing a flash of "no data" on fast tab switches.
        // The DB observer coroutine already reads the latest filterOptions/viewOption from state
        // every time it emits, so upcomingTasks will be recalculated correctly after both
        // coroutines have settled.
        launch {
            try {
                val showOnlyNextRecurring = sessionPreferences.getShowOnlyNextRecurring()
                val showCompleted = sessionPreferences.getShowCompleted()
                val showRoomTasks = sessionPreferences.getShowRoomTasks()
                val savedViewOption = sessionPreferences.getViewOption()
                val newFilterOptions = TimelineFilterOptions(showOnlyNextRecurring, showCompleted, showRoomTasks)
                setState {
                    copy(
                        filterOptions = newFilterOptions,
                        viewOption = savedViewOption,
                        upcomingTasks = if (this.allFetchedTasks.isNotEmpty()) {
                            this.getFilteredTasks(
                                options = newFilterOptions,
                                viewOpt = savedViewOption
                            )
                        } else {
                            this.upcomingTasks
                        },
                        isPreferencesLoading = false
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                setState { copy(isPreferencesLoading = false) }
            }
        }

        // Observe tasks range dynamically. Base range: 1 year back and 1 year forward.
        // Takvimde ileri/geri gidildikçe aralık otomatik olarak genişler.
        launch {
            try {
                state
                    .map { s ->
                        val now = com.yusufteker.planora.core.utils.getCurrentTimeMs()
                        val oneYear = 86400000L * 365
                        var from = now - oneYear
                        var to = now + (oneYear * s.yearsAhead)

                        // Takvimde görüntülenen ay baz aralığın dışındaysa, aralığı genişlet
                        val visibleMonth = s.visibleCalendarMonth
                        if (visibleMonth != null) {
                            val zone = TimeZone.currentSystemDefault()
                            val monthStartMs = visibleMonth.atStartOfDayIn(zone).toEpochMilliseconds()
                            val monthEndMs = monthStartMs + 86400000L * 45
                            from = minOf(from, monthStartMs - 86400000L * 15)
                            to = maxOf(to, monthEndMs)
                        }
                        Pair(from, to)
                    }
                    .distinctUntilChanged()
                    .flatMapLatest { range ->
                        planRepository.observeTasksForRange(fromTimeMs = range.first, toTimeMs = range.second)
                    }
                    .retryWhen { cause, attempt ->
                        if (cause is kotlinx.coroutines.CancellationException) throw cause
                        Napier.w("observeTasksForRange ERROR (attempt $attempt): ${cause.message}")
                        if (attempt >= 5) return@retryWhen false
                        kotlinx.coroutines.delay(minOf(500L * (attempt + 1), 5000L))
                        true
                    }
                    .collect { tasks ->
                        Napier.d("observeTasksForRange COLLECT: size=${tasks.size}")
                        val filteredTasks = tasks.filter { it.type != com.yusufteker.planora.shared.api.TaskType.NOTE && it.type != com.yusufteker.planora.shared.api.TaskType.FOLDER && it.parentId == null }
                            .sortedBy { task ->
                                (task.specificDetails as? com.yusufteker.planora.shared.api.ItemDetails.Task)?.deadline ?: task.startTime
                            }
                        val wasLoadingMore = state.value.isLoadingMoreFutureTasks
                        val previousCount = state.value.allFetchedTasks.size

                        // Determine whether initial task load has finished:
                        // If DB already has tasks (cached/offline data) OR initial network fetch completed.
                        val shouldMarkLoaded = filteredTasks.isNotEmpty() || isInitialFetchCompleted

                        setState {
                            val nowMs = getCurrentTimeMs()
                            val todayDate = Instant.fromEpochMilliseconds(nowMs).toLocalDateTime(TimeZone.currentSystemDefault()).date
                            val visibleMonth = visibleCalendarMonth ?: LocalDate(todayDate.year, todayDate.monthNumber, 1)
                            val selectedDate = selectedCalendarDate ?: todayDate
                            copy(
                                allFetchedTasks = filteredTasks,
                                selectedCalendarDate = selectedDate,
                                upcomingTasks = getFilteredTasks(myTasks = filteredTasks, calendarDate = selectedDate),
                                hasLoadedTasks = shouldMarkLoaded,
                                isLoadingMoreFutureTasks = false
                            )
                        }
                        if (wasLoadingMore && filteredTasks.size <= previousCount) {
                            showSnackbar("Daha ileri tarihli başka görev veya etkinlik bulunamadı.", com.yusufteker.planora.core.snackbar.SnackbarType.INFO)
                        }
                    }
            } catch (e: Exception) {
                e.printStackTrace()
                setState { copy(hasLoadedTasks = true) }
            }
        }

        // Trigger initial network background fetch
        launch {
            try {
                val now = getCurrentTimeMs()
                planRepository.syncPendingChanges()
                val oneYear = 86400000L * 365
                planRepository.fetchMyTasks(fromTime = now - oneYear, toTime = now + oneYear)
            } finally {
                isInitialFetchCompleted = true
                setState { copy(hasLoadedTasks = true) }
            }
        }

        // Load accessible users for shared calendar
        launch {
            planRepository.fetchAccessibleUsers()
        }

        launch {
            planRepository.observeAccessibleUsers().collect { entities ->
                val users = entities.map {
                    AccessibleUser(
                        userId = it.userId.toInt(),
                        name = it.name,
                        username = it.username,
                        avatarId = it.avatarId,
                        color = it.color,
                        profileImageUrl = it.profileImageUrl
                    )
                }
                setState { copy(accessibleUsers = users) }
            }
        }
        
        launch {
            sessionPreferences.userAvatarFlow.collect { avatar ->
                setState { copy(currentUserAvatarId = avatar) }
            }
        }
        
        launch {
            sessionPreferences.userProfileImageUrlFlow.collect { url ->
                setState { copy(currentUserProfileImageUrl = url) }
            }
        }
    }

    override fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.ProfileClicked -> {
                setEffect(HomeEffect.NavigateToProfile)
            }

            is HomeEvent.SettingsClicked -> {
                setEffect(HomeEffect.NavigateToSettings)
            }

            is HomeEvent.LoadMoreFutureTasks -> {
                if (!state.value.isLoadingMoreFutureTasks) {
                    setState { copy(yearsAhead = yearsAhead + 2, isLoadingMoreFutureTasks = true) }
                }
            }

            is HomeEvent.RefreshRequested -> {
                setState { copy(isLoading = true) }
                fetchedMonths.clear()
                val today = Instant.fromEpochMilliseconds(getCurrentTimeMs()).toLocalDateTime(TimeZone.currentSystemDefault()).date
                val currentMonthStart = LocalDate(today.year, today.monthNumber, 1)
                fetchedMonths.add(currentMonthStart)
                
                launch {
                    val now = getCurrentTimeMs()
                    val oneYear = 86400000L * 365
                    planRepository.syncPendingChanges()
                    planRepository.fetchMyTasks(fromTime = now - oneYear, toTime = now + oneYear)
                    setState { copy(isLoading = false) }
                }
            }
            
            is HomeEvent.SmartInputChanged -> {
                setState { copy(smartInputText = event.text) }
            }
            
            is HomeEvent.SubmitSmartInput -> {
                val text = state.value.smartInputText
                if (text.isNotBlank()) {
                    setState { copy(isLoading = true, smartInputText = "") }
                    launch {
                        submitSmartInputUseCase(text)
                        setState { copy(isLoading = false) }
                    }
                }
            }
            is HomeEvent.CreateTaskClicked -> {
                val zone = TimeZone.currentSystemDefault()
                val nowMs = getCurrentTimeMs()
                val nowLdt = Instant.fromEpochMilliseconds(nowMs).toLocalDateTime(zone)
                val targetDate = state.value.selectedCalendarDate ?: nowLdt.date
                val initialLdt = kotlinx.datetime.LocalDateTime(
                    year = targetDate.year,
                    monthNumber = targetDate.monthNumber,
                    dayOfMonth = targetDate.dayOfMonth,
                    hour = nowLdt.hour,
                    minute = nowLdt.minute
                )
                val selectedDateMs = initialLdt.toInstant(zone).toEpochMilliseconds()
                setEffect(HomeEffect.NavigateToCreateTask(initialDateMs = selectedDateMs))
            }
            
            is HomeEvent.CreateEventClicked -> {
                val zone = TimeZone.currentSystemDefault()
                val nowMs = getCurrentTimeMs()
                val nowLdt = Instant.fromEpochMilliseconds(nowMs).toLocalDateTime(zone)
                val targetDate = state.value.selectedCalendarDate ?: nowLdt.date
                val initialLdt = kotlinx.datetime.LocalDateTime(
                    year = targetDate.year,
                    monthNumber = targetDate.monthNumber,
                    dayOfMonth = targetDate.dayOfMonth,
                    hour = nowLdt.hour,
                    minute = nowLdt.minute
                )
                val selectedDateMs = initialLdt.toInstant(zone).toEpochMilliseconds()
                setEffect(HomeEffect.NavigateToCreateEvent(initialDateMs = selectedDateMs))
            }
            
            is HomeEvent.ViewOptionChanged -> {
                setState {
                    val nowMs = getCurrentTimeMs()
                    val todayDate = Instant.fromEpochMilliseconds(nowMs).toLocalDateTime(TimeZone.currentSystemDefault()).date
                    val visibleMonth = visibleCalendarMonth ?: LocalDate(todayDate.year, todayDate.monthNumber, 1)
                    val monthTasks = getFilteredTasks(myTasks = allFetchedTasks, viewOpt = event.option, calendarMonth = visibleMonth, calendarDate = null)
                    val selectedDate = selectedCalendarDate ?: todayDate
                    copy(
                        viewOption = event.option,
                        selectedCalendarDate = selectedDate,
                        upcomingTasks = getFilteredTasks(viewOpt = event.option, calendarDate = selectedDate)
                    )
                }
                // Persist selected view option
                launch {
                    sessionPreferences.saveViewOption(event.option)
                }
            }

            is HomeEvent.TypeFilterChanged -> {
                setState {
                    copy(
                        typeFilter = event.filter,
                        upcomingTasks = getFilteredTasks(typeFilter = event.filter)
                    )
                }
            }

            is HomeEvent.QuickCreateTask -> {
                quickCreateTask(event.title)
            }
            
            is HomeEvent.CalendarDateSelected -> {
                setState {
                    val newDate = event.date
                    copy(
                        selectedCalendarDate = newDate,
                        upcomingTasks = getFilteredTasks(calendarDate = newDate)
                    )
                }
            }
            
            is HomeEvent.CalendarMonthChanged -> {
                for (y in (event.monthStart.year - 1)..(event.monthStart.year + 1)) {
                    loadHolidaysForYear(y)
                }
                setState {
                    copy(
                        visibleCalendarMonth = event.monthStart,
                        upcomingTasks = getFilteredTasks(calendarMonth = event.monthStart, calendarDate = selectedCalendarDate)
                    )
                }
                // Sadece bu ay daha önce sunucudan çekilmediyse istek at (mükerrer istekleri önler)
                if (!fetchedMonths.contains(event.monthStart)) {
                    fetchedMonths.add(event.monthStart)
                    launch {
                        try {
                            val zone = TimeZone.currentSystemDefault()
                            val monthStartMs = event.monthStart.atStartOfDayIn(zone).toEpochMilliseconds()
                            val fromTime = monthStartMs - 86400000L * 15
                            val toTime = monthStartMs + 86400000L * 45
                            
                            val result = planRepository.fetchMyTasks(fromTime = fromTime, toTime = toTime)
                            if (result.isFailure) {
                                // İstek başarısız olursa önbellekten kaldır ki kullanıcı tekrar denediğinde çekebilsin
                                fetchedMonths.remove(event.monthStart)
                            }
                            
                            // Fetch shared tasks for the same range if any other users are selected.
                            val selectedUsers = state.value.selectedSharedUserIds
                            selectedUsers.forEach { userId ->
                                planRepository.fetchSharedTasks(userId, fromTime, toTime).onSuccess { tasks ->
                                    setState {
                                        val newMap = sharedTasksByUser.toMutableMap()
                                        newMap[userId] = tasks
                                        copy(
                                            sharedTasksByUser = newMap,
                                            upcomingTasks = getFilteredTasks(sharedTasksMap = newMap)
                                        )
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            fetchedMonths.remove(event.monthStart)
                            e.printStackTrace()
                        }
                    }
                }
            }
            
            is HomeEvent.FilterOptionChanged -> {
                setState { 
                    copy(
                        filterOptions = event.filterOptions,
                        upcomingTasks = getFilteredTasks(options = event.filterOptions)
                    ) 
                }
                launch {
                    sessionPreferences.saveFilterOptions(
                        showOnlyNextRecurring = event.filterOptions.showOnlyNextRecurring,
                        showCompleted = event.filterOptions.showCompleted,
                        showRoomTasks = event.filterOptions.showRoomTasks
                    )
                }
            }
            
            is HomeEvent.ToggleFilterSheet -> {
                setState { copy(isFilterSheetVisible = event.isVisible) }
            }
            
            is HomeEvent.ToggleTaskCompletion -> {
                launch {
                    val task = event.task
                    val dateMs = (task.specificDetails as? com.yusufteker.planora.shared.api.ItemDetails.Task)?.deadline ?: task.startTime
                    val isCurrentlyCompleted = task.status == com.yusufteker.planora.shared.api.TaskStatus.COMPLETED
                    
                    val baseId = task.id.extractBaseTaskId()
                    
                    val result = planRepository.completeTaskInstance(
                        taskId = baseId,
                        dateMs = dateMs,
                        isCompleted = !isCurrentlyCompleted
                    )
                    
                    if (result.isFailure) {
                        setState { copy(error = result.exceptionOrNull()?.message ?: "Task durumu güncellenemedi") }
                    }
                }
            }
            
            is HomeEvent.QuickDuplicateTask -> {
                launch {
                    val task = event.task
                    val targetMs = event.targetDateMs
                    
                    val updatedSpecificDetails = when (task.type) {
                        TaskType.TASK -> {
                            val originalTaskDetails = task.specificDetails as? ItemDetails.Task
                            ItemDetails.Task(
                                priority = originalTaskDetails?.priority ?: TaskPriority.MEDIUM,
                                subtasks = originalTaskDetails?.subtasks ?: emptyList(),
                                deadline = targetMs,
                                estimatedMinutes = originalTaskDetails?.estimatedMinutes
                            )
                        }
                        else -> task.specificDetails
                    }

                    val updatedEndTime = when (task.type) {
                        TaskType.TASK, TaskType.NOTE, TaskType.FOLDER -> null
                        TaskType.EVENT -> {
                            if (task.endTime != null && task.startTime > 0) {
                                val duration = task.endTime!! - task.startTime
                                if (duration > 0) targetMs + duration else targetMs + 3600000L
                            } else {
                                null
                            }
                        }
                    }

                    val request = CreateTaskRequest(
                        title = task.title,
                        description = task.description,
                        startTime = targetMs,
                        endTime = updatedEndTime,
                        type = task.type,
                        status = com.yusufteker.planora.shared.api.TaskStatus.PENDING,
                        visibility = task.visibility,
                        sharedRoomIds = task.sharedRoomIds,
                        isRecurring = false,
                        recurrenceRule = null,
                        isFlexible = task.isFlexible,
                        isOptional = task.isOptional,
                        isPostponable = task.isPostponable,
                        isAllDay = task.isAllDay,
                        reminders = task.reminders,
                        participants = task.participants.associate { it.userId to it.name },
                        specificDetails = updatedSpecificDetails
                    )
                    planRepository.createTask(request, triggerSync = true)
                }
            }
            
            is HomeEvent.TimelineItemClicked -> {
                val isMine = state.value.allFetchedTasks.any { it.id == event.task.id }
                if (isMine) {
                    when (event.task.type) {
                        TaskType.TASK -> setEffect(NavigateToTaskDetail(event.task.id))
                        TaskType.EVENT -> setEffect(NavigateToEventDetail(event.task.id))
                        TaskType.NOTE -> setEffect(NavigateToNoteEditor(event.task.id))
                        TaskType.FOLDER -> {}
                    }
                } else {
                    setState { copy(selectedSharedTask = event.task) }
                }
            }
            
            is HomeEvent.DismissSharedTaskDetail -> {
                setState { copy(selectedSharedTask = null) }
            }
            
            is HomeEvent.ToggleMyUser -> {
                val currentlySelected = state.value.isMyTasksSelected
                val otherSelectedUsers = state.value.selectedSharedUserIds
                // Eğer kendisi seçiliyse, sadece en az 1 başka kullanıcı seçili ise kaldırmaya izin var
                val newIsMySelected = if (currentlySelected) {
                    if (otherSelectedUsers.isNotEmpty()) false else true
                } else {
                    true
                }

                if (newIsMySelected != currentlySelected) {
                    setState {
                        copy(
                            isMyTasksSelected = newIsMySelected,
                            upcomingTasks = getFilteredTasks(isIncludeMy = newIsMySelected)
                        )
                    }
                }
            }

            is HomeEvent.ToggleSharedUser -> {
                val userId = event.userId
                val currentSelected = state.value.selectedSharedUserIds
                val newSelected = if (currentSelected.contains(userId)) {
                    currentSelected - userId
                } else {
                    currentSelected + userId
                }

                val currentIsMySelected = state.value.isMyTasksSelected
                // Eğer diğer kullanıcılar silindiyse ve hiç seçili kullanıcı kalmadıysa, kendini otomatik seç
                val finalIsMySelected = if (newSelected.isEmpty() && !currentIsMySelected) {
                    true
                } else {
                    currentIsMySelected
                }

                setState {
                    copy(
                        selectedSharedUserIds = newSelected,
                        isMyTasksSelected = finalIsMySelected
                    )
                }

                if (newSelected.contains(userId) && !state.value.sharedTasksByUser.containsKey(userId)) {
                    launch {
                        val now = com.yusufteker.planora.core.utils.getCurrentTimeMs()
                        val oneYear = 86400000L * 365
                        val result = planRepository.fetchSharedTasks(userId, now - oneYear, now + oneYear)
                        result.onSuccess { tasks ->
                            setState {
                                val newMap = sharedTasksByUser.toMutableMap()
                                newMap[userId] = tasks
                                copy(
                                    sharedTasksByUser = newMap,
                                    upcomingTasks = getFilteredTasks(
                                        sharedTasksMap = newMap,
                                        selectedUsers = newSelected,
                                        isIncludeMy = finalIsMySelected
                                    )
                                )
                            }
                        }
                    }
                } else {
                    setState {
                        copy(
                            upcomingTasks = getFilteredTasks(
                                selectedUsers = newSelected,
                                isIncludeMy = finalIsMySelected
                            )
                        )
                    }
                }
            }

            is HomeEvent.OnDeleteTask -> {
                launch {
                    val result = planRepository.deleteTask(event.taskId)
                    if (result.isFailure) {
                        setState { copy(error = result.exceptionOrNull()?.message ?: "Görev silinemedi") }
                    }
                }
            }

            is HomeEvent.ChangeTaskPriority -> {
                launch {
                    val currentDetails = event.task.specificDetails as? ItemDetails.Task
                    val newDetails = ItemDetails.Task(
                        priority = event.newPriority,
                        subtasks = currentDetails?.subtasks ?: emptyList(),
                        deadline = currentDetails?.deadline,
                        estimatedMinutes = currentDetails?.estimatedMinutes
                    )
                    val request = event.task.toCreateTaskRequest().copy(specificDetails = newDetails)
                    val result = planRepository.updateTask(event.task.id, request)
                    if (result.isFailure) {
                        setState { copy(error = result.exceptionOrNull()?.message ?: "Öncelik güncellenemedi") }
                    }
                }
            }

            is HomeEvent.ToggleTaskPin -> {
                launch {
                    val result = planRepository.toggleTaskPinLocal(event.taskId, event.isPinned)
                    if (result.isFailure) {
                        setState { copy(error = result.exceptionOrNull()?.message ?: "Sabitleme durumu güncellenemedi") }
                    }
                }
            }

            is HomeEvent.MoveTaskOrder -> {
                moveTaskOrder(event.task, event.isUp)
            }
        }
    }


    private fun HomeState.getFilteredTasks(
        myTasks: List<TaskDto> = this.allFetchedTasks,
        sharedTasksMap: Map<Int, List<TaskDto>> = this.sharedTasksByUser,
        selectedUsers: Set<Int> = this.selectedSharedUserIds,
        isIncludeMy: Boolean = this.isMyTasksSelected,
        options: TimelineFilterOptions = this.filterOptions,
        viewOpt: TimelineViewOption = this.viewOption,
        typeFilter: com.yusufteker.planora.core.utils.TimelineTypeFilter = this.typeFilter,
        calendarDate: LocalDate? = this.selectedCalendarDate,
        calendarMonth: LocalDate? = this.visibleCalendarMonth
    ): List<TaskDto> {
        return getFilteredTasksUseCase(
            myTasks = myTasks,
            sharedTasksMap = sharedTasksMap,
            selectedUsers = selectedUsers,
            isIncludeMyTasks = isIncludeMy,
            options = options,
            viewOption = viewOpt,
            typeFilter = typeFilter,
            selectedCalendarDate = calendarDate,
            visibleCalendarMonth = calendarMonth
        )
    }

    /**
     * Kullanıcının Görevler merkezinden modal açmadan tek satırda yazdığı to-do öğesini
     * zamansız (backlog) olarak yerel veritabanına ve sunucuya ekler.
     *
     * @param title Eklenmek istenen yapılacak iş başlığı.
     */
    private fun quickCreateTask(title: String) {
        if (title.isBlank()) return
        launch {
            val now = getCurrentTimeMs()
            val request = com.yusufteker.planora.shared.api.CreateTaskRequest(
                title = title.trim(),
                description = null,
                startTime = now,
                endTime = null,
                type = com.yusufteker.planora.shared.api.TaskType.TASK,
                status = com.yusufteker.planora.shared.api.TaskStatus.PENDING,
                visibility = com.yusufteker.planora.shared.api.TaskVisibility.PRIVATE,
                sharedRoomIds = emptyList(),
                isRecurring = false,
                recurrenceRule = null,
                isFlexible = true,
                isOptional = false,
                isPostponable = true,
                isAllDay = false,
                reminders = emptyList(),
                participants = emptyMap(),
                specificDetails = com.yusufteker.planora.shared.api.ItemDetails.Task(
                    priority = com.yusufteker.planora.shared.api.TaskPriority.MEDIUM,
                    deadline = null
                ),
                parentId = null,
                tags = emptyList(),
                color = null,
                isPinned = false
            )
            val result = planRepository.createTask(request)
            if (result.isFailure) {
                showSnackbar(
                    result.exceptionOrNull()?.message ?: "Görev eklenemedi",
                    com.yusufteker.planora.core.snackbar.SnackbarType.ERROR
                )
            }
        }
    }

    /**
     * Yapılacaklar havuzundaki (backlog) zamansız bir görevin sırasını yukarı veya aşağı taşır.
     * Görevlerin sıralaması `isPinned DESC, startTime DESC` şeklinde tutulur.
     * Komşu öğe ile zaman damgası ve gerekirse sabitleme durumu takas edilir ve yerel veritabanına kaydedilir.
     *
     * @param task Sırası değiştirilecek görev.
     * @param isUp True ise yukarı (daha öne), false ise aşağı (daha geriye) taşır.
     */
    private fun moveTaskOrder(task: TaskDto, isUp: Boolean) {
        launch {
            val unscheduledTasks = state.value.allFetchedTasks
                .filter { it.type == TaskType.TASK && it.isUnscheduled() && it.status != TaskStatus.COMPLETED }
                .sortedWith(compareByDescending<TaskDto> { it.isPinned }.thenByDescending { it.startTime })

            val currentIndex = unscheduledTasks.indexOfFirst { it.id == task.id }
            if (currentIndex == -1) return@launch

            val targetIndex = if (isUp) currentIndex - 1 else currentIndex + 1
            if (targetIndex !in unscheduledTasks.indices) return@launch

            val targetTask = unscheduledTasks[targetIndex]

            if (task.isPinned == targetTask.isPinned) {
                if (task.startTime != targetTask.startTime) {
                    val reqCurrent = task.toCreateTaskRequest().copy(startTime = targetTask.startTime)
                    val reqTarget = targetTask.toCreateTaskRequest().copy(startTime = task.startTime)
                    planRepository.updateTask(task.id, reqCurrent)
                    planRepository.updateTask(targetTask.id, reqTarget)
                } else {
                    val newStartTime = if (isUp) targetTask.startTime + 1000L else targetTask.startTime - 1000L
                    val reqCurrent = task.toCreateTaskRequest().copy(startTime = newStartTime)
                    planRepository.updateTask(task.id, reqCurrent)
                }
            } else {
                if (isUp && !task.isPinned && targetTask.isPinned) {
                    val newStartTime = targetTask.startTime + 1000L
                    val reqCurrent = task.toCreateTaskRequest().copy(isPinned = true, startTime = newStartTime)
                    planRepository.updateTask(task.id, reqCurrent)
                    planRepository.toggleTaskPinLocal(task.id, true)
                } else if (!isUp && task.isPinned && !targetTask.isPinned) {
                    val newStartTime = targetTask.startTime - 1000L
                    val reqCurrent = task.toCreateTaskRequest().copy(isPinned = false, startTime = newStartTime)
                    planRepository.updateTask(task.id, reqCurrent)
                    planRepository.toggleTaskPinLocal(task.id, false)
                }
            }
        }
    }

    private fun loadHolidaysForYear(year: Int) {
        if (loadedHolidayYears.contains(year)) return
        loadedHolidayYears.add(year)

        launch {
            try {
                val countryCode = com.yusufteker.planora.core.holiday.getDeviceCountryCode()
                val holidaysMap = holidayRepository.getHolidays(countryCode, year)
                if (holidaysMap.isNotEmpty()) {
                    val parsedHolidays = holidaysMap.mapNotNull { (dateStr, name) ->
                        try {
                            LocalDate.parse(dateStr) to name
                        } catch (e: Exception) {
                            null
                        }
                    }.toMap()

                    setState {
                        copy(holidays = holidays + parsedHolidays)
                    }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Napier.w("Failed to load holidays: ${e.message}", tag = "HomeViewModel")
            }
        }
    }
}
