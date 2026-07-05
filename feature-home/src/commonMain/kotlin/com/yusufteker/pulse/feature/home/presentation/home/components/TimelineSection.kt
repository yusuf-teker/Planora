package com.yusufteker.pulse.feature.home.presentation.home.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
                    isToday(time) -> "Bugün"
                    isTomorrow(time) -> "Yarın"
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

    if (state.upcomingTasks.isEmpty()) {

        EmptyStateComponent(
            icon = Icons.Default.CalendarToday,
            title = "Henüz görevin yok",
            description = "Aşağıdan AI'a bir şey söyle!",
            modifier = modifier.fillMaxWidth()

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

                TimelineTaskCard(
                    task = task,
                    showDate = state.viewOption == TimelineViewOption.RELATIVE,
                    onClick = {
                        onTaskClick(task)
                    }
                )
            }
        }
    }
}