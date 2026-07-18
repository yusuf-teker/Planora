package com.yusufteker.pulse.feature.home.presentation.home

import com.yusufteker.pulse.core.base.BaseViewModel
import com.yusufteker.pulse.core.preferences.SessionPreferences
import com.yusufteker.pulse.core.utils.getCurrentTimeMs

import com.yusufteker.pulse.feature.home.domain.repository.PlanRepository
import com.yusufteker.pulse.shared.api.CreateTaskRequest
import com.yusufteker.pulse.shared.api.ItemDetails
import com.yusufteker.pulse.shared.api.TaskType
import com.yusufteker.pulse.shared.api.extractBaseTaskId

import com.yusufteker.pulse.core.utils.TimelineViewOption
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
// Yeni
import kotlinx.datetime.Instant
import kotlinx.datetime.atStartOfDayIn
import com.yusufteker.pulse.core.utils.getCurrentTimeMs
import com.yusufteker.pulse.feature.home.domain.use_case.GetFilteredTasksUseCase
import com.yusufteker.pulse.feature.home.domain.use_case.SubmitSmartInputUseCase
import com.yusufteker.pulse.feature.home.presentation.home.HomeEffect.*
import com.yusufteker.pulse.shared.api.TaskDto
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
    private val submitSmartInputUseCase: SubmitSmartInputUseCase
) : BaseViewModel<HomeState, HomeEvent, HomeEffect>(
    initialState = HomeState()
) {
    private val fetchedMonths = mutableSetOf<LocalDate>()

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
                val savedViewOption = sessionPreferences.getViewOption()
                val newFilterOptions = TimelineFilterOptions(showOnlyNextRecurring, showCompleted)
                setState {
                    copy(
                        filterOptions = newFilterOptions,
                        viewOption = savedViewOption,
                        upcomingTasks = if (this.allFetchedTasks.isNotEmpty()) {
                            // updated state'e göre (bu copy işlemi henüz _state'e yansımadı)
                            // this.getFilteredTasks(...) kullanarak hesapla
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

        // Observe tasks range dynamically. We observe 1 year back and 1 year forward.
        launch {
            try {
                state
                    .map { s ->
                        val now = com.yusufteker.pulse.core.utils.getCurrentTimeMs()
                        val oneYear = 86400000L * 365
                        Pair(now - oneYear, now + oneYear)
                    }
                    .distinctUntilChanged()
                    .flatMapLatest { range ->
                        planRepository.observeTasksForRange(fromTimeMs = range.first, toTimeMs = range.second)
                    }
                    .retryWhen { cause, attempt ->
                        println("observeTasksForRange ERROR: ${cause.message}")
                        cause.printStackTrace()
                        kotlinx.coroutines.delay(500)
                        true
                    }
                    .collect { tasks ->
                        println("observeTasksForRange COLLECT: size=${tasks.size}")
                        val filteredTasks = tasks.filter { it.type != com.yusufteker.pulse.shared.api.TaskType.NOTE && it.type != com.yusufteker.pulse.shared.api.TaskType.FOLDER }
                            .sortedBy { task ->
                                (task.specificDetails as? com.yusufteker.pulse.shared.api.ItemDetails.Task)?.deadline ?: task.startTime
                            }
                        setState {
                            copy(
                                allFetchedTasks = filteredTasks,
                                upcomingTasks = getFilteredTasks(myTasks = filteredTasks),
                                hasLoadedTasks = true
                            )
                        }
                    }
            } catch (e: Exception) {
                e.printStackTrace()
                setState { copy(hasLoadedTasks = true) }
            }
        }

        // Trigger a background fetch
        launch {
            val now = com.yusufteker.pulse.core.utils.getCurrentTimeMs()
            planRepository.syncPendingChanges()
            val oneYear = 86400000L * 365
            planRepository.fetchMyTasks(fromTime = now - oneYear, toTime = now + oneYear)
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

            is HomeEvent.RefreshRequested -> {
                setState { copy(isLoading = true) }
                fetchedMonths.clear()
                val today = Instant.fromEpochMilliseconds(getCurrentTimeMs()).toLocalDateTime(TimeZone.currentSystemDefault()).date
                val currentMonthStart = LocalDate(today.year, today.monthNumber, 1)
                fetchedMonths.add(currentMonthStart)
                
                launch {
                    val now = com.yusufteker.pulse.core.utils.getCurrentTimeMs()
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
                setEffect(HomeEffect.NavigateToCreateTask)
            }
            
            is HomeEvent.CreateEventClicked -> {
                setEffect(HomeEffect.NavigateToCreateEvent)
            }
            
            is HomeEvent.ViewOptionChanged -> {
                setState {
                    copy(
                        viewOption = event.option,
                        upcomingTasks = getFilteredTasks(viewOpt = event.option)
                    )
                }
                // Persist selected view option
                launch {
                    sessionPreferences.saveViewOption(event.option)
                }
            }
            
            is HomeEvent.CalendarDateSelected -> {
                setState {
                    val newDate = if (selectedCalendarDate == event.date) null else event.date
                    copy(
                        selectedCalendarDate = newDate,
                        upcomingTasks = getFilteredTasks(calendarDate = newDate)
                    )
                }
            }
            
            is HomeEvent.CalendarMonthChanged -> {
                setState {
                    copy(
                        visibleCalendarMonth = event.monthStart,
                        upcomingTasks = getFilteredTasks(calendarMonth = event.monthStart)
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
                        showCompleted = event.filterOptions.showCompleted
                    )
                }
            }
            
            is HomeEvent.ToggleFilterSheet -> {
                setState { copy(isFilterSheetVisible = event.isVisible) }
            }
            
            is HomeEvent.ToggleTaskCompletion -> {
                launch {
                    val task = event.task
                    val dateMs = (task.specificDetails as? com.yusufteker.pulse.shared.api.ItemDetails.Task)?.deadline ?: task.startTime
                    val isCurrentlyCompleted = task.status == com.yusufteker.pulse.shared.api.TaskStatus.COMPLETED
                    
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
            
            is HomeEvent.TimelineItemClicked -> {
                val isMine = state.value.allFetchedTasks.any { it.id == event.task.id }
                if (isMine) {
                    when (event.task.type) {
                        TaskType.TASK -> setEffect(NavigateToTaskEditor(event.task.id))
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
            
            is HomeEvent.ToggleSharedUser -> {
                val userId = event.userId
                val currentSelected = state.value.selectedSharedUserIds
                val newSelected = if (currentSelected.contains(userId)) {
                    currentSelected - userId
                } else {
                    currentSelected + userId
                }
                
                setState { copy(selectedSharedUserIds = newSelected) }
                
                if (newSelected.contains(userId) && !state.value.sharedTasksByUser.containsKey(userId)) {
                    launch {
                        val now = com.yusufteker.pulse.core.utils.getCurrentTimeMs()
                        val oneYear = 86400000L * 365
                        val result = planRepository.fetchSharedTasks(userId, now - oneYear, now + oneYear)
                        result.onSuccess { tasks ->
                            setState {
                                val newMap = sharedTasksByUser.toMutableMap()
                                newMap[userId] = tasks
                                copy(
                                    sharedTasksByUser = newMap,
                                    upcomingTasks = getFilteredTasks(sharedTasksMap = newMap, selectedUsers = newSelected)
                                )
                            }
                        }
                    }
                } else {
                    setState {
                        copy(
                            upcomingTasks = getFilteredTasks(selectedUsers = newSelected)
                        )
                    }
                }
            }
        }
    }
    
    private fun HomeState.getFilteredTasks(
        myTasks: List<TaskDto> = this.allFetchedTasks,
        sharedTasksMap: Map<Int, List<TaskDto>> = this.sharedTasksByUser,
        selectedUsers: Set<Int> = this.selectedSharedUserIds,
        options: TimelineFilterOptions = this.filterOptions,
        viewOpt: TimelineViewOption = this.viewOption,
        calendarDate: LocalDate? = this.selectedCalendarDate,
        calendarMonth: LocalDate? = this.visibleCalendarMonth
    ): List<TaskDto> {
        return getFilteredTasksUseCase(
            myTasks = myTasks,
            sharedTasksMap = sharedTasksMap,
            selectedUsers = selectedUsers,
            options = options,
            viewOption = viewOpt,
            selectedCalendarDate = calendarDate,
            visibleCalendarMonth = calendarMonth
        )
    }
}
