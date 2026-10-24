package com.yusufteker.planora.feature.home.presentation.global_search

import com.yusufteker.planora.core.base.UiEffect
import com.yusufteker.planora.core.base.UiEvent
import com.yusufteker.planora.core.base.UiState
import com.yusufteker.planora.shared.api.PlanRoomDto
import com.yusufteker.planora.shared.api.TaskDto

/**
 * Genel arama filtre kategorileri.
 */
enum class SearchCategory {
    ALL,
    TASKS,
    EVENTS,
    NOTES,
    ROOMS
}

/**
 * Genel arama ekranının durum nesnesi (UiState).
 *
 * @param query Kullanıcının arama çubuğuna yazdığı arama metni.
 * @param selectedCategory Seçili filtre kategorisi (Tümü, Görevler, vb.).
 * @param selectedTag Seçili etiket filtresi (opsiyonel).
 * @param filteredTasks Arama kriterlerine uyan görevler.
 * @param filteredEvents Arama kriterlerine uyan etkinlikler.
 * @param filteredNotes Arama kriterlerine uyan notlar.
 * @param filteredRooms Arama kriterlerine uyan odalar.
 * @param availableTags Kullanıcının içeriklerinde yer alan tüm benzersiz etiketler.
 * @param isLoading Verilerin yüklenme durumu.
 */
data class GlobalSearchState(
    val query: String = "",
    val selectedCategory: SearchCategory = SearchCategory.ALL,
    val selectedTag: String? = null,
    val filteredTasks: List<TaskDto> = emptyList(),
    val filteredEvents: List<TaskDto> = emptyList(),
    val filteredNotes: List<TaskDto> = emptyList(),
    val filteredRooms: List<PlanRoomDto> = emptyList(),
    val availableTags: List<String> = emptyList(),
    val isLoading: Boolean = false
) : UiState {
    val totalResultsCount: Int
        get() = when (selectedCategory) {
            SearchCategory.ALL -> filteredTasks.size + filteredEvents.size + filteredNotes.size + filteredRooms.size
            SearchCategory.TASKS -> filteredTasks.size
            SearchCategory.EVENTS -> filteredEvents.size
            SearchCategory.NOTES -> filteredNotes.size
            SearchCategory.ROOMS -> filteredRooms.size
        }

    val isSearchActive: Boolean
        get() = query.isNotBlank() || selectedTag != null
}

/**
 * Genel arama ekranında kullanıcının tetiklediği aksiyonlar (UiEvent).
 */
sealed interface GlobalSearchEvent : UiEvent {
    data class OnQueryChanged(val query: String) : GlobalSearchEvent
    data class OnCategoryChanged(val category: SearchCategory) : GlobalSearchEvent
    data class OnTagSelected(val tag: String?) : GlobalSearchEvent
    data object OnClearSearch : GlobalSearchEvent
    data class OnToggleTaskStatus(val task: TaskDto) : GlobalSearchEvent
    data class OnTaskClicked(val task: TaskDto) : GlobalSearchEvent
    data class OnRoomClicked(val roomId: String) : GlobalSearchEvent
    data object OnBackClicked : GlobalSearchEvent
}

/**
 * Genel arama ekranının tek seferlik yan etkileri (UiEffect).
 */
sealed interface GlobalSearchEffect : UiEffect {
    data object NavigateBack : GlobalSearchEffect
    data class NavigateToTaskEditor(val taskId: String) : GlobalSearchEffect
    data class NavigateToEventEditor(val eventId: String) : GlobalSearchEffect
    data class NavigateToNoteEditor(val noteId: String) : GlobalSearchEffect
    data class NavigateToRoomDetail(val roomId: String) : GlobalSearchEffect
}
