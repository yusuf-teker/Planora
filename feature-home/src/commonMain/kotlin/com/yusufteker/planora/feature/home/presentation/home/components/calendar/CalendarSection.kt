package com.yusufteker.planora.feature.home.presentation.home.components.calendar

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.yusufteker.planora.core.utils.TimelineViewOption
import com.yusufteker.planora.feature.home.presentation.home.HomeEvent
import com.yusufteker.planora.feature.home.presentation.home.HomeState
import com.yusufteker.planora.shared.api.TaskDto
import com.yusufteker.planora.shared.api.TaskStatus
import com.yusufteker.planora.shared.api.extractBaseTaskId
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

/**
 * Planora Ana Takvim Alanı (CalendarSection).
 *
 * Ana ekranda seçili filtilere göre görevleri haritalandırıp
 * [PlanoraCalendarView] bileşenine aktarır.
 *
 * @param state Ana ekran UI durumu
 * @param onEvent Olay tetikleyici
 * @param modifier Dış düzenleyici
 */
@Composable
fun CalendarSection(
    state: HomeState,
    onEvent: (HomeEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    if (state.viewOption != TimelineViewOption.CALENDAR) return

    val tasksByDate = remember(
        state.allFetchedTasks,
        state.filterOptions,
        state.isMyTasksSelected
    ) {
        var filteredTasks = if (state.isMyTasksSelected) {
            state.allFetchedTasks
        } else {
            state.allFetchedTasks.filter { it.participants.size > 1 || it.sharedRoomIds.isNotEmpty() }
        }

        if (!state.filterOptions.showCompleted) {
            filteredTasks = filteredTasks.filter { it.status != TaskStatus.COMPLETED }
        }

        if (state.filterOptions.showOnlyNextRecurring) {
            val uniqueTasks = mutableListOf<TaskDto>()
            val seenRecurringBaseIds = mutableSetOf<String>()

            for (task in filteredTasks) {
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
            filteredTasks = uniqueTasks
        }

        val tasksMap = mutableMapOf<LocalDate, MutableList<TaskDto>>()
        for (task in filteredTasks) {
            val effectiveTime = (task.specificDetails as? com.yusufteker.planora.shared.api.ItemDetails.Task)?.deadline ?: task.startTime
            val startDate = Instant.fromEpochMilliseconds(effectiveTime)
                .toLocalDateTime(TimeZone.currentSystemDefault()).date
            val endDate = task.endTime?.let {
                Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.currentSystemDefault()).date
            } ?: startDate

            var current = startDate
            while (current <= endDate) {
                tasksMap.getOrPut(current) { mutableListOf() }.add(task)
                current = current.plus(1, kotlinx.datetime.DateTimeUnit.DAY)
            }
        }
        tasksMap
    }

    val sharedTasksByDate = remember(
        state.sharedTasksByUser,
        state.filterOptions
    ) {
        val result = mutableMapOf<Int, Map<LocalDate, List<TaskDto>>>()
        for ((userId, rawUserTasks) in state.sharedTasksByUser) {
            var filteredUserTasks = rawUserTasks.filter {
                it.type != com.yusufteker.planora.shared.api.TaskType.NOTE &&
                it.type != com.yusufteker.planora.shared.api.TaskType.FOLDER &&
                it.parentId == null
            }

            if (!state.filterOptions.showCompleted) {
                filteredUserTasks = filteredUserTasks.filter { it.status != TaskStatus.COMPLETED }
            }

            if (state.filterOptions.showOnlyNextRecurring) {
                val uniqueTasks = mutableListOf<TaskDto>()
                val seenRecurringBaseIds = mutableSetOf<String>()

                for (task in filteredUserTasks) {
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
                filteredUserTasks = uniqueTasks
            }

            val tasksMap = mutableMapOf<LocalDate, MutableList<TaskDto>>()
            for (task in filteredUserTasks) {
                val effectiveTime = (task.specificDetails as? com.yusufteker.planora.shared.api.ItemDetails.Task)?.deadline ?: task.startTime
                val startDate = Instant.fromEpochMilliseconds(effectiveTime)
                    .toLocalDateTime(TimeZone.currentSystemDefault()).date
                val endDate = task.endTime?.let {
                    Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.currentSystemDefault()).date
                } ?: startDate

                var current = startDate
                while (current <= endDate) {
                    tasksMap.getOrPut(current) { mutableListOf() }.add(task)
                    current = current.plus(1, kotlinx.datetime.DateTimeUnit.DAY)
                }
            }
            result[userId] = tasksMap
        }
        result
    }

    val sharedUserColors = remember(state.accessibleUsers) {
        state.accessibleUsers.associate { user -> user.userId to user.color }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        PlanoraCalendarView(
            tasksByDate = tasksByDate,
            sharedTasksByDate = sharedTasksByDate,
            sharedUserColors = sharedUserColors,
            selectedSharedUserIds = state.selectedSharedUserIds,
            isMyTasksSelected = state.isMyTasksSelected,
            accessibleUsers = state.accessibleUsers,
            selectedDate = state.selectedCalendarDate,
            visibleMonth = state.visibleCalendarMonth,
            holidays = state.holidays,
            upcomingTasks = state.upcomingTasks,
            onDateSelected = { date ->
                onEvent(HomeEvent.CalendarDateSelected(date))
            },
            onMonthChanged = { month ->
                onEvent(HomeEvent.CalendarMonthChanged(month))
            },
            onTaskClick = { task ->
                onEvent(HomeEvent.TimelineItemClicked(task))
            },
            modifier = modifier
        )
    }
}
