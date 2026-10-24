package com.yusufteker.planora.feature.home.presentation.global_search

import androidx.lifecycle.viewModelScope
import com.yusufteker.planora.core.base.BaseViewModel
import com.yusufteker.planora.feature.home.domain.repository.PlanRepository
import com.yusufteker.planora.shared.api.ItemDetails
import com.yusufteker.planora.shared.api.PlanRoomDto
import com.yusufteker.planora.shared.api.TaskDto
import com.yusufteker.planora.shared.api.TaskStatus
import com.yusufteker.planora.shared.api.TaskType
import com.yusufteker.planora.shared.api.extractBaseTaskId
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Planora genel arama ekranı için ViewModel.
 *
 * Kullanıcının yerel veritabanındaki tüm görevlerini, etkinliklerini,
 * notlarını ve ortak plan odalarını izleyerek metin ve etiket bazlı
 * dinamik arama ve filtreleme sağlar.
 *
 * @param planRepository Görev ve oda verilerini sağlayan depo katmanı.
 */
class GlobalSearchViewModel(
    private val planRepository: PlanRepository
) : BaseViewModel<GlobalSearchState, GlobalSearchEvent, GlobalSearchEffect>(
    initialState = GlobalSearchState()
) {

    private var cachedTasks: List<TaskDto> = emptyList()
    private var cachedRooms: List<PlanRoomDto> = emptyList()

    init {
        viewModelScope.launch {
            combine(
                planRepository.observeAllTasks(),
                planRepository.observeAllPlanRooms()
            ) { tasks, rooms ->
                tasks to rooms
            }.collect { (tasks, rooms) ->
                cachedTasks = tasks
                cachedRooms = rooms

                val tags = tasks.flatMap { it.tags }
                    .filter { it.isNotBlank() }
                    .distinctBy { it.lowercase() }
                    .sorted()

                setState { copy(availableTags = tags) }
                performFilter()
            }
        }
    }

    override fun onEvent(event: GlobalSearchEvent) {
        when (event) {
            is GlobalSearchEvent.OnQueryChanged -> {
                setState { copy(query = event.query) }
                performFilter()
            }

            is GlobalSearchEvent.OnCategoryChanged -> {
                setState { copy(selectedCategory = event.category) }
            }

            is GlobalSearchEvent.OnTagSelected -> {
                val newTag = if (state.value.selectedTag.equals(event.tag, ignoreCase = true)) null else event.tag
                setState { copy(selectedTag = newTag) }
                performFilter()
            }

            GlobalSearchEvent.OnClearSearch -> {
                setState { copy(query = "", selectedTag = null) }
                performFilter()
            }

            is GlobalSearchEvent.OnToggleTaskStatus -> {
                viewModelScope.launch {
                    val task = event.task
                    val dateMs = (task.specificDetails as? ItemDetails.Task)?.deadline ?: task.startTime
                    val isCurrentlyCompleted = task.status == TaskStatus.COMPLETED
                    val baseId = task.id.extractBaseTaskId()

                    planRepository.completeTaskInstance(
                        taskId = baseId,
                        dateMs = dateMs,
                        isCompleted = !isCurrentlyCompleted
                    )
                }
            }

            is GlobalSearchEvent.OnTaskClicked -> {
                when (event.task.type) {
                    TaskType.EVENT -> setEffect(GlobalSearchEffect.NavigateToEventEditor(event.task.id))
                    TaskType.NOTE -> setEffect(GlobalSearchEffect.NavigateToNoteEditor(event.task.id))
                    else -> setEffect(GlobalSearchEffect.NavigateToTaskEditor(event.task.id))
                }
            }

            is GlobalSearchEvent.OnRoomClicked -> {
                setEffect(GlobalSearchEffect.NavigateToRoomDetail(event.roomId))
            }

            GlobalSearchEvent.OnBackClicked -> {
                setEffect(GlobalSearchEffect.NavigateBack)
            }
        }
    }

    /**
     * Arama metni, seçili kategori ve etiket filtresine göre sonuçları filtreler.
     */
    private fun performFilter() {
        val currentState = state.value
        val query = currentState.query.trim().lowercase()
        val tagFilter = currentState.selectedTag?.lowercase()

        val filteredTasks = cachedTasks.filter { task ->
            val matchesTag = tagFilter == null || task.tags.any { it.equals(tagFilter, ignoreCase = true) }
            val noteContent = (task.specificDetails as? ItemDetails.Note)?.content ?: ""

            val matchesQuery = query.isBlank() ||
                task.title.lowercase().contains(query) ||
                (task.description?.lowercase()?.contains(query) == true) ||
                noteContent.lowercase().contains(query) ||
                task.tags.any { it.lowercase().contains(query) }

            matchesTag && matchesQuery
        }

        val tasks = filteredTasks.filter { it.type == TaskType.TASK }
        val events = filteredTasks.filter { it.type == TaskType.EVENT }
        val notes = filteredTasks.filter { it.type == TaskType.NOTE }

        val rooms = if (tagFilter == null) {
            cachedRooms.filter { room ->
                query.isBlank() || room.name.lowercase().contains(query)
            }
        } else {
            emptyList()
        }

        setState {
            copy(
                filteredTasks = tasks,
                filteredEvents = events,
                filteredNotes = notes,
                filteredRooms = rooms
            )
        }
    }
}
