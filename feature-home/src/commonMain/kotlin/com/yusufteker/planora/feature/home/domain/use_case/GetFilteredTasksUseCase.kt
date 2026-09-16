package com.yusufteker.planora.feature.home.domain.use_case

import com.yusufteker.planora.core.utils.TimelineTypeFilter
import com.yusufteker.planora.core.utils.TimelineViewOption
import com.yusufteker.planora.feature.home.presentation.home.TimelineFilterOptions
import com.yusufteker.planora.shared.api.ItemDetails
import com.yusufteker.planora.shared.api.TaskDto
import com.yusufteker.planora.shared.api.TaskStatus
import com.yusufteker.planora.shared.api.TaskType
import com.yusufteker.planora.shared.api.extractBaseTaskId
import com.yusufteker.planora.shared.api.isUnscheduled
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Görev ve etkinlik listesini kullanıcının seçtiği görünüm modu ([TimelineViewOption]),
 * tür filtresi ([TimelineTypeFilter]), oda filtreleri ve takvim tarihlerine göre
 * süzen use case sınıfı.
 */
class GetFilteredTasksUseCase {

    operator fun invoke(
        myTasks: List<TaskDto>,
        sharedTasksMap: Map<Int, List<TaskDto>>,
        selectedUsers: Set<Int>,
        isIncludeMyTasks: Boolean = true,
        options: TimelineFilterOptions,
        viewOption: TimelineViewOption,
        typeFilter: TimelineTypeFilter = TimelineTypeFilter.ALL,
        selectedCalendarDate: LocalDate?,
        visibleCalendarMonth: LocalDate?
    ): List<TaskDto> {
        // 1. Combine tasks
        val sharedTasks = selectedUsers.flatMap { userId ->
            sharedTasksMap[userId] ?: emptyList()
        }
        val myTasksToInclude = if (isIncludeMyTasks) {
            myTasks
        } else {
            // Sadece bireysel (kişisel) görevleri hariç tut, ortak görevler kalsın
            myTasks.filter { it.participants.size > 1 || it.sharedRoomIds.isNotEmpty() }
        }
        var filtered = (myTasksToInclude + sharedTasks)
            .distinctBy { it.id }
            .filter { it.parentId == null }
            .sortedBy { task ->
                (task.specificDetails as? ItemDetails.Task)?.deadline ?: task.startTime
            }

        // 2. Type filter (Tümü / Sadece Etkinlik / Sadece Görev)
        // Eğer TASKS görünümündeysek otomatik olarak sadece TASK tipi gösterilir.
        if (viewOption == TimelineViewOption.TASKS) {
            filtered = filtered.filter { it.type == TaskType.TASK }
        } else {
            when (typeFilter) {
                TimelineTypeFilter.ALL -> Unit
                TimelineTypeFilter.EVENTS_ONLY -> filtered = filtered.filter { it.type == TaskType.EVENT }
                TimelineTypeFilter.TASKS_ONLY -> filtered = filtered.filter { it.type == TaskType.TASK }
            }
        }

        // 3. Room tasks filter
        if (!options.showRoomTasks) {
            filtered = filtered.filter { it.sharedRoomIds.isEmpty() }
        }

        // 4. Completed filter
        // Görevler (TASKS) hub'ında tamamlananlar ayrı açılır/kapanır bölümde gösterileceği için orada filtrelenmez.
        if (!options.showCompleted && viewOption != TimelineViewOption.TASKS) {
            filtered = filtered.filter { it.status != TaskStatus.COMPLETED }
        }

        // 5. Recurring next-only filter
        if (options.showOnlyNextRecurring) {
            val uniqueTasks = mutableListOf<TaskDto>()
            val seenRecurringBaseIds = mutableSetOf<String>()

            for (task in filtered) {
                if (task.isRecurring) {
                    val baseId = task.id.extractBaseTaskId()
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

        // 6. Calendar filtering
        if (viewOption == TimelineViewOption.CALENDAR) {
            filtered = filtered.filter { task ->
                // Zamansız to-do'lar takvim ızgarasında gösterilmez
                if (task.isUnscheduled()) return@filter false

                val effectiveTime = (task.specificDetails as? ItemDetails.Task)?.deadline ?: task.startTime
                val taskStartDate = Instant.fromEpochMilliseconds(effectiveTime)
                    .toLocalDateTime(TimeZone.currentSystemDefault()).date
                
                val taskEndDate = task.endTime?.let { 
                    Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.currentSystemDefault()).date
                } ?: taskStartDate

                if (selectedCalendarDate != null) {
                    selectedCalendarDate in taskStartDate..taskEndDate
                } else if (visibleCalendarMonth != null) {
                    val startYearMonth = taskStartDate.year * 12 + taskStartDate.monthNumber
                    val endYearMonth = taskEndDate.year * 12 + taskEndDate.monthNumber
                    val targetYearMonth = visibleCalendarMonth.year * 12 + visibleCalendarMonth.monthNumber
                    
                    targetYearMonth in startYearMonth..endYearMonth
                } else {
                    true
                }
            }
        }

        // 7. Akış (DATE) görünümünde zamansız görevler (to-do backlog) yer almaz, sadece planlanmış tarihli ögeler gösterilir.
        if (viewOption == TimelineViewOption.DATE) {
            filtered = filtered.filter { !it.isUnscheduled() }
        }

        return filtered
    }
}

/**
 * Calculates the earliest date within the given [monthDate] that contains at least one event/task.
 *
 * Iterates through [tasks], determines the effective date range for each task (considering start time
 * and deadline/end time), and checks if it falls within the specified target month. Returns the earliest
 * date in that month with a task, or null if no tasks exist in that month.
 *
 * @param tasks List of [TaskDto] to inspect for dates.
 * @param monthDate A [LocalDate] representing the target month (e.g., year and monthNumber).
 * @return The earliest [LocalDate] in [monthDate] with an event, or null if none.
 */
fun getEarliestEventDateInMonth(
    tasks: List<TaskDto>,
    monthDate: LocalDate
): LocalDate? {
    val monthStart = LocalDate(monthDate.year, monthDate.monthNumber, 1)
    val targetYearMonth = monthDate.year * 12 + monthDate.monthNumber

    var earliest: LocalDate? = null

    for (task in tasks) {
        val effectiveTime = (task.specificDetails as? ItemDetails.Task)?.deadline ?: task.startTime
        val taskStartDate = Instant.fromEpochMilliseconds(effectiveTime)
            .toLocalDateTime(TimeZone.currentSystemDefault()).date

        val taskEndDate = task.endTime?.let {
            Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.currentSystemDefault()).date
        } ?: taskStartDate

        val startYearMonth = taskStartDate.year * 12 + taskStartDate.monthNumber
        val endYearMonth = taskEndDate.year * 12 + taskEndDate.monthNumber

        if (targetYearMonth in startYearMonth..endYearMonth) {
            val taskEarliestInMonth = if (startYearMonth == targetYearMonth) {
                taskStartDate
            } else {
                monthStart
            }
            if (earliest == null || taskEarliestInMonth < earliest) {
                earliest = taskEarliestInMonth
            }
        }
    }
    return earliest
}

