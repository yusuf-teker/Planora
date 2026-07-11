package com.yusufteker.pulse.feature.home.presentation.home.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yusufteker.pulse.core.ui.text.UiText
import com.yusufteker.pulse.core.utils.TimelineViewOption
import com.yusufteker.pulse.core.utils.formatDayName
import com.yusufteker.pulse.core.utils.formatShortDate
import com.yusufteker.pulse.core.utils.getRelativeTimeBucket
import com.yusufteker.pulse.core.utils.isToday
import com.yusufteker.pulse.core.utils.isTomorrow
import com.yusufteker.pulse.feature.home.presentation.components.EmptyStateComponent
import com.yusufteker.pulse.feature.home.presentation.home.HomeState
import com.yusufteker.pulse.shared.api.ItemDetails
import com.yusufteker.pulse.shared.api.TaskDto
import org.jetbrains.compose.resources.stringResource
import pulsy.core.generated.resources.Res
import pulsy.core.generated.resources.chat_with_ai
import pulsy.core.generated.resources.no_tasks
import pulsy.core.generated.resources.today
import pulsy.core.generated.resources.tomorrow

@Composable
fun TimelineSection(
    state: HomeState,
    onTaskClick: (TaskDto) -> Unit,
    modifier: Modifier = Modifier
) {

    val grouped = state.upcomingTasks.groupBy<TaskDto, String> { task ->

        val time =
            (task.specificDetails as? ItemDetails.Task)?.deadline
                ?: task.endTime
                ?: task.startTime

        when (state.viewOption) {

            TimelineViewOption.DATE -> {
                when {
                    isToday(time) -> stringResource(Res.string.today)
                    isTomorrow(time) -> stringResource(Res.string.tomorrow)
                    else -> "${formatDayName(time)}, ${formatShortDate(time)}"
                }
            }

            TimelineViewOption.CALENDAR -> {
                if (state.selectedCalendarDate != null) {
                    formatShortDate(time)
                } else {
                    "${formatDayName(time)}, ${formatShortDate(time)}"
                }
            }

            TimelineViewOption.RELATIVE -> {
                getRelativeTimeBucket(time)
            }
        }
    }

    // Don't show empty state until the first data load has completed.
    // After hasLoadedTasks is true, the state is reliable.
    if (!state.hasLoadedTasks && state.upcomingTasks.isEmpty()) {
        return
    }

    if (state.upcomingTasks.isEmpty()) {

        EmptyStateComponent(
            icon = Icons.Default.CalendarToday,
            title = stringResource(Res.string.no_tasks),
            description = stringResource(Res.string.chat_with_ai),
            modifier = modifier.fillMaxSize()

        )

        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            ,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {

        grouped.forEach { (dayLabel, tasks) ->

            item(
                key = "header_$dayLabel"
            ) {
                DayHeader(dayLabel)
            }

            items(
                items = tasks,
                key = { it.id }
            ) { task ->
                val isMine = state.allFetchedTasks.any { it.id == task.id }
                val creatorUser = if (!isMine) state.accessibleUsers.find { it.userId == task.creatorId } else null
                val creatorColor = creatorUser?.color?.let { 
                    try { androidx.compose.ui.graphics.Color(it.removePrefix("#").toLong(16) or 0x00000000FF000000) } catch (e: Exception) { null } 
                }

                TimelineTaskCard(
                    task = task,
                    showDate = state.viewOption == TimelineViewOption.RELATIVE,
                    sharedUserAvatar = creatorUser?.avatarId,
                    sharedUserColor = creatorColor,
                    onClick = {
                        onTaskClick(task)
                    }
                )
            }
        }
    }
}