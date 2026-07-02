package com.yusufteker.pulse.feature.home.presentation.event_detail

import com.yusufteker.pulse.core.utils.getCurrentTimeMs
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

data class EventDetailState(
    val id: String? = null,
    val title: String = "",
    val description: String = "",
    val location: String = "",
    
    val startDateTimeMs: Long = getCurrentTimeMs(),
    val endDateTimeMs: Long = getCurrentTimeMs() + 3600000L, // +1 hour default
    val isEndTimeManuallyChanged: Boolean = false,
    
    // Context
    val planRoomId: String? = null,
    val participants: Map<Int, String> = emptyMap(),
    
    // Recurrence
    val isRecurring: Boolean = false,
    val selectedDaysOfWeek: Set<Int> = emptySet(), // 1=Monday...7=Sunday
    
    val isLoading: Boolean = false,
    val error: String? = null,
    
    // Dialog states
    val isStartPickerOpen: Boolean = false,
    val isEndPickerOpen: Boolean = false,
    val isRepeatPickerOpen: Boolean = false
)



private fun LocalTime.plusHours(hours: Int): LocalTime {
    val totalMinutes = this.hour * 60 + this.minute + hours * 60
    val newHour = (totalMinutes / 60) % 24
    val newMinute = totalMinutes % 60
    return LocalTime(newHour, newMinute)
}
