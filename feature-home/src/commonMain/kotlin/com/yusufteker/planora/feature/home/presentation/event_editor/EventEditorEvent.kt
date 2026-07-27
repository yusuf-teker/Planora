package com.yusufteker.planora.feature.home.presentation.event_editor

import com.yusufteker.planora.shared.api.RecurrenceRule

sealed interface EventEditorEvent {
    data class OnLoadEvent(
        val eventId: String?, 
        val planRoomId: String? = null,
        val sharedTitle: String? = null,
        val sharedNote: String? = null,
        val sharedDate: Long? = null,
        val sharedSender: String? = null
    ) : EventEditorEvent
    data class OnTitleChange(val title: String) : EventEditorEvent
    data class OnDescriptionChange(val description: String) : EventEditorEvent
    data class OnLocationChange(val location: String) : EventEditorEvent
    
    // Dates & Times
    data class OnStartDateTimeSelected(val dateMs: Long) : EventEditorEvent
    data class OnEndDateTimeSelected(val dateMs: Long) : EventEditorEvent
    
    // Dialog Toggles
    data class OnStartPickerVisibilityChanged(val isVisible: Boolean) : EventEditorEvent
    data class OnEndPickerVisibilityChanged(val isVisible: Boolean) : EventEditorEvent
    
    // Recurrence
    data class OnToggleRecurring(val isRecurring: Boolean) : EventEditorEvent
    data class OnRecurrenceRuleChanged(val rule: RecurrenceRule?) : EventEditorEvent
    data class OnRepeatPickerVisibilityChanged(val isVisible: Boolean) : EventEditorEvent
    
    data class OnReminderPickerVisibilityChanged(val isVisible: Boolean) : EventEditorEvent
    data class OnReminderToggled(val minutes: Int) : EventEditorEvent
    
    object OnSaveClick : EventEditorEvent
    object OnDeleteClick : EventEditorEvent
    object OnBackClick : EventEditorEvent
    data class OnParticipantPickerVisibilityChanged(val isVisible: Boolean) : EventEditorEvent
    data class OnParticipantToggled(val userId: Int) : EventEditorEvent
    object OnShareClick : EventEditorEvent
}
