package com.yusufteker.pulse.feature.home.presentation.event_detail

import com.yusufteker.pulse.shared.api.RecurrenceRule
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

sealed interface EventDetailEvent {
    data class OnLoadEvent(val eventId: String?, val planRoomId: String? = null) : EventDetailEvent
    data class OnTitleChange(val title: String) : EventDetailEvent
    data class OnDescriptionChange(val description: String) : EventDetailEvent
    data class OnLocationChange(val location: String) : EventDetailEvent
    
    // Dates & Times
    data class OnStartDateTimeSelected(val dateMs: Long) : EventDetailEvent
    data class OnEndDateTimeSelected(val dateMs: Long) : EventDetailEvent
    
    // Dialog Toggles
    data class OnStartPickerVisibilityChanged(val isVisible: Boolean) : EventDetailEvent
    data class OnEndPickerVisibilityChanged(val isVisible: Boolean) : EventDetailEvent
    
    // Recurrence
    data class OnToggleRecurring(val isRecurring: Boolean) : EventDetailEvent
    data class OnRecurrenceRuleChanged(val rule: RecurrenceRule?) : EventDetailEvent
    data class OnRepeatPickerVisibilityChanged(val isVisible: Boolean) : EventDetailEvent
    
    data class OnReminderPickerVisibilityChanged(val isVisible: Boolean) : EventDetailEvent
    data class OnReminderToggled(val minutes: Int) : EventDetailEvent
    
    object OnSaveClick : EventDetailEvent
    object OnDeleteClick : EventDetailEvent
    object OnBackClick : EventDetailEvent
    data class OnParticipantPickerVisibilityChanged(val isVisible: Boolean) : EventDetailEvent
    data class OnParticipantToggled(val userId: Int) : EventDetailEvent
}
