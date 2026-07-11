package com.yusufteker.pulse.feature.home.presentation.home

import com.yusufteker.pulse.core.base.BaseViewModel
import com.yusufteker.pulse.core.utils.getCurrentTimeMs

import com.yusufteker.pulse.feature.home.domain.repository.PlanRepository
import com.yusufteker.pulse.shared.api.CreateTaskRequest
import com.yusufteker.pulse.shared.api.ItemDetails
import com.yusufteker.pulse.shared.api.TaskType

import com.yusufteker.pulse.core.utils.TimelineViewOption
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
// Yeni
import kotlinx.datetime.Instant
import com.yusufteker.pulse.core.utils.getCurrentTimeMs

/**
 * ViewModel for the Home (Dashboard) screen.
 *
 * Manages home feed state and navigation.
 */
class HomeViewModel(
    private val planRepository: PlanRepository,
    private val sessionPreferences: com.yusufteker.pulse.core.preferences.SessionPreferences,
    private val getFilteredTasksUseCase: com.yusufteker.pulse.feature.home.domain.use_case.GetFilteredTasksUseCase,
    private val submitSmartInputUseCase: com.yusufteker.pulse.feature.home.domain.use_case.SubmitSmartInputUseCase
) : BaseViewModel<HomeState, HomeEvent, HomeEffect>(
    initialState = HomeState()
) {

    init {
        val today = Instant.fromEpochMilliseconds(getCurrentTimeMs()).toLocalDateTime(TimeZone.currentSystemDefault()).date
        val currentMonthStart = LocalDate(today.year, today.monthNumber, 1)
        setState { copy(visibleCalendarMonth = currentMonthStart) }
        // Load stored filter options and view option
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
                        upcomingTasks = getFilteredTasks(options = newFilterOptions, viewOpt = savedViewOption),
                        isPreferencesLoading = false
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                setState { copy(isPreferencesLoading = false) }
            }
        }

        // Observe tasks range
        launch {
            try {
                val now = com.yusufteker.pulse.core.utils.getCurrentTimeMs()
                val thirtyDays = 86400000L * 30
                planRepository.observeTasksForRange(fromTimeMs = now - thirtyDays, toTimeMs = now + thirtyDays)
                    .collect { tasks ->
                        val filteredTasks = tasks.filter { it.type != com.yusufteker.pulse.shared.api.TaskType.NOTE }
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
            val thirtyDays = 86400000L * 30
            planRepository.fetchMyTasks(fromTime = now - thirtyDays, toTime = now + thirtyDays)
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
                        color = it.color
                    )
                }
                setState { copy(accessibleUsers = users) }
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
                launch {
                    val now = com.yusufteker.pulse.core.utils.getCurrentTimeMs()
                    val thirtyDays = 86400000L * 30
                    planRepository.syncPendingChanges()
                    planRepository.fetchMyTasks(fromTime = now - thirtyDays, toTime = now + thirtyDays)
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
                    
                    val baseId = task.id.substringBeforeLast("_")
                    
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
                        com.yusufteker.pulse.shared.api.TaskType.TASK -> setEffect(HomeEffect.NavigateToTaskEditor(event.task.id))
                        com.yusufteker.pulse.shared.api.TaskType.EVENT -> setEffect(HomeEffect.NavigateToEventDetail(event.task.id))
                        com.yusufteker.pulse.shared.api.TaskType.NOTE -> setEffect(HomeEffect.NavigateToNoteEditor(event.task.id))
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
                        val thirtyDays = 86400000L * 30
                        val result = planRepository.fetchSharedTasks(userId, now - thirtyDays, now + thirtyDays)
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
    
    private fun getFilteredTasks(
        myTasks: List<com.yusufteker.pulse.shared.api.TaskDto> = state.value.allFetchedTasks,
        sharedTasksMap: Map<Int, List<com.yusufteker.pulse.shared.api.TaskDto>> = state.value.sharedTasksByUser,
        selectedUsers: Set<Int> = state.value.selectedSharedUserIds,
        options: TimelineFilterOptions = state.value.filterOptions,
        viewOpt: TimelineViewOption = state.value.viewOption,
        calendarDate: LocalDate? = state.value.selectedCalendarDate,
        calendarMonth: LocalDate? = state.value.visibleCalendarMonth
    ): List<com.yusufteker.pulse.shared.api.TaskDto> {
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
