package com.yusufteker.pulse.feature.home.presentation.home.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yusufteker.pulse.core.utils.TimelineViewOption
import com.yusufteker.pulse.feature.home.presentation.home.HomeEvent
import com.yusufteker.pulse.feature.home.presentation.home.HomeState
import com.yusufteker.pulse.shared.api.TaskDto
import com.yusufteker.pulse.shared.api.TaskStatus
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun CalendarSection(
    state: HomeState,
    onEvent: (HomeEvent) -> Unit
) {

    if (state.viewOption != TimelineViewOption.CALENDAR) return

    val tasksByDate = remember(
        state.allFetchedTasks,
        state.filterOptions
    ) {

        var filteredTasks = state.allFetchedTasks

        if (!state.filterOptions.showCompleted) {
            filteredTasks =
                filteredTasks.filter {
                    it.status != TaskStatus.COMPLETED
                }
        }

        if (state.filterOptions.showOnlyNextRecurring) {

            val uniqueTasks = mutableListOf<TaskDto>()
            val seenRecurringBaseIds = mutableSetOf<String>()

            for (task in filteredTasks) {

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

            filteredTasks = uniqueTasks
        }

        filteredTasks.groupBy { task ->
            Instant
                .fromEpochMilliseconds(task.startTime)
                .toLocalDateTime(TimeZone.currentSystemDefault())
                .date
        }
    }

    val sharedTasksByDate = remember(state.sharedTasksByUser) {
        state.sharedTasksByUser.mapValues { (_, tasks) ->
            tasks.groupBy { task ->
                Instant.fromEpochMilliseconds(task.startTime)
                    .toLocalDateTime(TimeZone.currentSystemDefault())
                    .date
            }
        }
    }

    val sharedUserColors = remember(state.accessibleUsers) {
        state.accessibleUsers.associate { it.userId to it.color }
    }

    Column(modifier = Modifier.fillMaxWidth()) {

        CalendarView(
            tasksByDate = tasksByDate,
            sharedTasksByDate = sharedTasksByDate,
            sharedUserColors = sharedUserColors,
            accessibleUsers = state.accessibleUsers,
            selectedSharedUserIds = state.selectedSharedUserIds,
            selectedDate = state.selectedCalendarDate,
            visibleMonth = state.visibleCalendarMonth,
            upcomingTasks = state.upcomingTasks,
            hasLoadedTasks = state.hasLoadedTasks,
            onDateSelected = {
                onEvent(HomeEvent.CalendarDateSelected(it))
            },
            onMonthChanged = {
                onEvent(HomeEvent.CalendarMonthChanged(it))
            },
            onTaskClick = { task ->
                onEvent(HomeEvent.TimelineItemClicked(task))
            }
        )
    }
}