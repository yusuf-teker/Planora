package com.yusufteker.pulse.feature.home.presentation.home.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.material.icons.filled.Delete
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
import pulsy.core.generated.resources.no_tasks
import pulsy.core.generated.resources.today
import pulsy.core.generated.resources.tomorrow
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import kotlinx.coroutines.launch

import com.yusufteker.pulse.core.ui.components.SwipeToDeleteWrapper
import androidx.compose.foundation.shape.RoundedCornerShape

@Composable
fun TimelineSection(
    state: HomeState,
    onTaskClick: (TaskDto) -> Unit,
    onTaskDelete: (String) -> Unit = {},
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
            modifier = modifier.fillMaxSize()
        )

        return
    }

    var firstTodayIndex = -1
    var currentIndex = 0
    for ((_, tasks) in grouped) {
        val hasTodayTask = tasks.any { task ->
            val time = (task.specificDetails as? ItemDetails.Task)?.deadline ?: task.endTime ?: task.startTime
            isToday(time)
        }
        if (hasTodayTask && firstTodayIndex == -1) {
            firstTodayIndex = currentIndex
        }
        currentIndex += 1 // Header
        currentIndex += tasks.size // Items
    }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val showFab by remember(firstTodayIndex) {
        derivedStateOf {
            if (firstTodayIndex == -1) return@derivedStateOf false
            val visibleItemsInfo = listState.layoutInfo.visibleItemsInfo
            if (visibleItemsInfo.isEmpty()) return@derivedStateOf false
            
            val firstVisible = visibleItemsInfo.first().index
            val lastVisible = visibleItemsInfo.last().index
            
            firstTodayIndex < firstVisible || firstTodayIndex > lastVisible
        }
    }

    val isTodayAbove by remember(firstTodayIndex) {
        derivedStateOf {
            if (firstTodayIndex == -1) return@derivedStateOf false
            val visibleItemsInfo = listState.layoutInfo.visibleItemsInfo
            if (visibleItemsInfo.isEmpty()) return@derivedStateOf false
            
            val firstVisible = visibleItemsInfo.first().index
            firstTodayIndex < firstVisible
        }
    }

    Box(modifier = modifier) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth(),
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

                if (isMine) {
                    SwipeToDeleteWrapper(
                        shape = RoundedCornerShape(24.dp),
                        onDelete = { onTaskDelete(task.id) }
                    ) {
                        TimelineTaskCard(
                            task = task,
                            showDate = state.viewOption == TimelineViewOption.RELATIVE,
                            sharedUserAvatar = creatorUser?.avatarId,
                            sharedUserColor = creatorColor,
                            sharedUserProfileImageUrl = creatorUser?.profileImageUrl,
                            onClick = { onTaskClick(task) }
                        )
                    }
                } else {
                    TimelineTaskCard(
                        task = task,
                        showDate = state.viewOption == TimelineViewOption.RELATIVE,
                        sharedUserAvatar = creatorUser?.avatarId,
                        sharedUserColor = creatorColor,
                        sharedUserProfileImageUrl = creatorUser?.profileImageUrl,
                        onClick = { onTaskClick(task) }
                    )
                }
            }
        }
    }

    if (showFab) {
        FloatingActionButton(
            onClick = {
                coroutineScope.launch {
                    if (firstTodayIndex != -1) {
                        listState.animateScrollToItem(firstTodayIndex)
                    }
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 88.dp, bottom = 16.dp),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shape = androidx.compose.foundation.shape.CircleShape
        ) {
            Icon(
                imageVector = if (isTodayAbove) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                contentDescription = "Go to Today"
            )
        }
    }
}
}