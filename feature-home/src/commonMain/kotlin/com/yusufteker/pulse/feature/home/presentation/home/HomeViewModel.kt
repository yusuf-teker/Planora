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
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * ViewModel for the Home (Dashboard) screen.
 *
 * Manages home feed state and navigation.
 */
class HomeViewModel(
    private val planRepository: PlanRepository,
    private val profileRepository: com.yusufteker.pulse.feature.home.domain.repository.ProfileRepository,
    private val sessionPreferences: com.yusufteker.pulse.core.preferences.SessionPreferences
) : BaseViewModel<HomeState, HomeEvent, HomeEffect>(
    initialState = HomeState()
) {

    init {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val currentMonthStart = LocalDate(today.year, today.monthNumber, 1)
        setState { copy(visibleCalendarMonth = currentMonthStart) }
        // Indicate preferences are loading
        setState { copy(isPreferencesLoading = true) }

        // Load stored filter options and view option
        launch {
            val showOnlyNextRecurring = sessionPreferences.getShowOnlyNextRecurring()
            val showCompleted = sessionPreferences.getShowCompleted()
            val savedViewOption = sessionPreferences.getViewOption()
            val newFilterOptions = TimelineFilterOptions(showOnlyNextRecurring, showCompleted)
            setState {
                copy(
                    filterOptions = newFilterOptions,
                    viewOption = savedViewOption,
                    upcomingTasks = applyFilters(allFetchedTasks, newFilterOptions, viewOption = savedViewOption),
                    isPreferencesLoading = false
                )
            }
        }

        // Observe tasks range
        launch {
            val now = com.yusufteker.pulse.core.utils.getCurrentTimeMs()
            val thirtyDays = 86400000L * 30
            planRepository.observeTasksForRange(fromTimeMs = now - thirtyDays, toTimeMs = now + thirtyDays)
                .collect { tasks ->
                    val filteredTasks = tasks.filter { it.type != com.yusufteker.pulse.shared.api.TaskType.NOTE }
                        .sortedBy { task ->
                            (task.specificDetails as? ItemDetails.Task)?.deadline ?: task.startTime
                        }
                    setState {
                        copy(
                            allFetchedTasks = filteredTasks,
                            upcomingTasks = applyFilters(filteredTasks, state.value.filterOptions, viewOption = state.value.viewOption)
                        )
                    }
                }
        }

        // Trigger a background fetch
        launch {
            val now = com.yusufteker.pulse.core.utils.getCurrentTimeMs()
            planRepository.syncPendingChanges()
            val thirtyDays = 86400000L * 30
            planRepository.fetchMyTasks(fromTime = now - thirtyDays, toTime = now + thirtyDays)
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
                        // AI Simulation: Check if any follower's name is in the text
                        val followers = profileRepository.getFollowingUsers().getOrNull() ?: emptyList()
                        val mentionedUser = followers.find { text.contains(it.username, ignoreCase = true) }
                        
                        val participantsMap = if (mentionedUser != null) {
                            mapOf(mentionedUser.id to "PENDING")
                        } else {
                            emptyMap()
                        }
                        
                        val request = CreateTaskRequest(
                            title = if (mentionedUser != null) "AI Gen: Event with ${mentionedUser.username}" else "AI Gen: ${
                                text.take(
                                    15
                                )
                            }...",
                            description = text,
                            startTime = getCurrentTimeMs(),
                            type = if (mentionedUser != null) TaskType.EVENT else TaskType.NOTE,
                            specificDetails = ItemDetails.Event(),
                            participants = participantsMap
                        )
                        planRepository.createTask(request)
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
                        upcomingTasks = applyFilters(allFetchedTasks, filterOptions, viewOption = event.option)
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
                        upcomingTasks = applyFilters(allFetchedTasks, filterOptions, selectedCalendarDate = newDate)
                    )
                }
            }
            
            is HomeEvent.CalendarMonthChanged -> {
                setState {
                    copy(
                        visibleCalendarMonth = event.monthStart,
                        upcomingTasks = applyFilters(allFetchedTasks, filterOptions, visibleCalendarMonth = event.monthStart)
                    )
                }
            }
            
            is HomeEvent.FilterOptionChanged -> {
                setState { 
                    copy(
                        filterOptions = event.filterOptions,
                        upcomingTasks = applyFilters(allFetchedTasks, event.filterOptions)
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
                when (event.task.type) {
                    com.yusufteker.pulse.shared.api.TaskType.TASK -> setEffect(HomeEffect.NavigateToTaskEditor(event.task.id))
                    com.yusufteker.pulse.shared.api.TaskType.EVENT -> setEffect(HomeEffect.NavigateToEventDetail(event.task.id))
                    com.yusufteker.pulse.shared.api.TaskType.NOTE -> setEffect(HomeEffect.NavigateToNoteEditor(event.task.id))
                }
            }
        }
    }
    
    private fun applyFilters(
        tasks: List<com.yusufteker.pulse.shared.api.TaskDto>, 
        options: TimelineFilterOptions,
        viewOption: TimelineViewOption = state.value.viewOption,
        selectedCalendarDate: LocalDate? = state.value.selectedCalendarDate,
        visibleCalendarMonth: LocalDate? = state.value.visibleCalendarMonth
    ): List<com.yusufteker.pulse.shared.api.TaskDto> {
        var filtered = tasks
        
        // 1. Completed filter (if implemented, for now assuming we just hide if not showCompleted)
        if (!options.showCompleted) {
            filtered = filtered.filter { it.status != com.yusufteker.pulse.shared.api.TaskStatus.COMPLETED }
        }
        
        // 2. Recurring next-only filter
        if (options.showOnlyNextRecurring) {
            val uniqueTasks = mutableListOf<com.yusufteker.pulse.shared.api.TaskDto>()
            val seenRecurringBaseIds = mutableSetOf<String>()
            
            for (task in filtered) {
                if (task.isRecurring) {
                    val baseId = task.id.substringBeforeLast("_")
                    if (baseId !in seenRecurringBaseIds) {
                        seenRecurringBaseIds.add(baseId)
                        uniqueTasks.add(task)
                    }
                } else {
                    uniqueTasks.add(task)
                }
            }
            filtered = uniqueTasks
        }
        
        // 3. Calendar filtering
        if (viewOption == TimelineViewOption.CALENDAR) {
            filtered = filtered.filter { task ->
                val taskDate = Instant.fromEpochMilliseconds(task.startTime)
                    .toLocalDateTime(TimeZone.currentSystemDefault()).date
                
                if (selectedCalendarDate != null) {
                    taskDate == selectedCalendarDate
                } else if (visibleCalendarMonth != null) {
                    taskDate.year == visibleCalendarMonth.year && taskDate.monthNumber == visibleCalendarMonth.monthNumber
                } else {
                    true
                }
            }
        }
        
        return filtered
    }
}
