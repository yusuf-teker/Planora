package com.yusufteker.pulse.feature.home.presentation.home

import com.yusufteker.pulse.core.base.BaseViewModel

import com.yusufteker.pulse.feature.home.domain.repository.PlanRepository
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * ViewModel for the Home (Dashboard) screen.
 *
 * Manages home feed state and navigation.
 */
class HomeViewModel(
    private val planRepository: PlanRepository,
    private val profileRepository: com.yusufteker.pulse.feature.home.domain.repository.ProfileRepository
) : BaseViewModel<HomeState, HomeEvent, HomeEffect>(
    initialState = HomeState()
) {

    init {
        launch {
            planRepository.observeAllTasks().collect { tasks ->
                val filteredTasks = tasks.filter { it.type != com.yusufteker.pulse.shared.api.TaskType.NOTE }
                setState { copy(upcomingTasks = filteredTasks) }
            }
        }
            
        // Trigger a background fetch
        launch {
            val now = com.yusufteker.pulse.core.utils.getCurrentTimeMs()
            // Fetch from 30 days ago to 30 days in the future to ensure we don't miss recent tasks
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
                        
                        val request = com.yusufteker.pulse.shared.api.CreateTaskRequest(
                            title = if (mentionedUser != null) "AI Gen: Event with ${mentionedUser.username}" else "AI Gen: ${text.take(15)}...",
                            description = text,
                            startTime = com.yusufteker.pulse.core.utils.getCurrentTimeMs(),
                            type = if (mentionedUser != null) com.yusufteker.pulse.shared.api.TaskType.EVENT else com.yusufteker.pulse.shared.api.TaskType.NOTE,
                            specificDetails = com.yusufteker.pulse.shared.api.ItemDetails.Event(),
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
            
            is HomeEvent.TimelineItemClicked -> {
                when (event.task.type) {
                    com.yusufteker.pulse.shared.api.TaskType.TASK -> setEffect(HomeEffect.NavigateToTaskEditor(event.task.id))
                    com.yusufteker.pulse.shared.api.TaskType.EVENT -> setEffect(HomeEffect.NavigateToEventDetail(event.task.id))
                    com.yusufteker.pulse.shared.api.TaskType.NOTE -> setEffect(HomeEffect.NavigateToNoteEditor(event.task.id))
                }
            }
        }
    }
}
