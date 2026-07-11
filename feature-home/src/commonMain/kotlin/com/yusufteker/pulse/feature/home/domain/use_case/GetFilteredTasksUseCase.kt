package com.yusufteker.pulse.feature.home.domain.use_case

import com.yusufteker.pulse.core.utils.TimelineViewOption
import com.yusufteker.pulse.feature.home.presentation.home.TimelineFilterOptions
import com.yusufteker.pulse.shared.api.ItemDetails
import com.yusufteker.pulse.shared.api.TaskDto
import com.yusufteker.pulse.shared.api.TaskStatus
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class GetFilteredTasksUseCase {

    operator fun invoke(
        myTasks: List<TaskDto>,
        sharedTasksMap: Map<Int, List<TaskDto>>,
        selectedUsers: Set<Int>,
        options: TimelineFilterOptions,
        viewOption: TimelineViewOption,
        selectedCalendarDate: LocalDate?,
        visibleCalendarMonth: LocalDate?
    ): List<TaskDto> {
        // 1. Combine tasks
        val sharedTasks = selectedUsers.flatMap { userId ->
            sharedTasksMap[userId] ?: emptyList()
        }
        var filtered = (myTasks + sharedTasks).sortedBy { task ->
            (task.specificDetails as? ItemDetails.Task)?.deadline ?: task.startTime
        }

        // 2. Completed filter
        if (!options.showCompleted) {
            filtered = filtered.filter { it.status != TaskStatus.COMPLETED }
        }

        // 3. Recurring next-only filter
        if (options.showOnlyNextRecurring) {
            val uniqueTasks = mutableListOf<TaskDto>()
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

        // 4. Calendar filtering
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
