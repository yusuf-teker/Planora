package com.yusufteker.planora.feature.home.presentation.home.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.material.icons.filled.Delete
import com.yusufteker.planora.core.utils.TimelineViewOption
import com.yusufteker.planora.core.utils.formatDayName
import com.yusufteker.planora.core.utils.formatShortDate
import com.yusufteker.planora.core.utils.getRelativeTimeBucket
import com.yusufteker.planora.core.utils.isToday
import com.yusufteker.planora.core.utils.isTomorrow
import com.yusufteker.planora.feature.home.presentation.components.EmptyStateComponent
import com.yusufteker.planora.feature.home.presentation.home.HomeState
import com.yusufteker.planora.shared.api.ItemDetails
import com.yusufteker.planora.shared.api.TaskDto
import org.jetbrains.compose.resources.stringResource
import planora.core.generated.resources.Res
import planora.core.generated.resources.no_tasks
import planora.core.generated.resources.today
import planora.core.generated.resources.tomorrow
import planora.core.generated.resources.load_more_future_tasks
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.yusufteker.planora.core.utils.getCurrentTimeMs
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import androidx.compose.ui.Alignment
import kotlinx.coroutines.launch

import com.yusufteker.planora.core.ui.components.SwipeToDeleteWrapper
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text

@Composable
fun TimelineSection(
    state: HomeState,
    onTaskClick: (TaskDto) -> Unit,
    onTaskDelete: (String) -> Unit = {},
    onLoadMore: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // Performans: groupBy sonucunu remember ile sarıp her frame'de tekrar hesaplamayı önle
    val todayLabel = stringResource(Res.string.today)
    val tomorrowLabel = stringResource(Res.string.tomorrow)

    val grouped = remember(state.upcomingTasks, state.viewOption, state.selectedCalendarDate) {
        state.upcomingTasks.groupBy<TaskDto, String> { task ->

            val time =
                (task.specificDetails as? ItemDetails.Task)?.deadline
                    ?: task.startTime

            when (state.viewOption) {

                TimelineViewOption.DATE -> {
                    when {
                        isToday(time) -> todayLabel
                        isTomorrow(time) -> tomorrowLabel
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

    // Performans: targetTodayIndex hesaplamasını remember ile sar
    val targetTodayIndex = remember(grouped) {
        val nowMs = getCurrentTimeMs()
        val timeZone = TimeZone.currentSystemDefault()
        val todayDate = Instant.fromEpochMilliseconds(nowMs).toLocalDateTime(timeZone).date

        var todayIndex = -1
        var firstUpcomingIndex = -1
        var currentIndex = 0

        for ((_, tasks) in grouped) {
            var groupHasToday = false
            var groupHasUpcoming = false

            for (task in tasks) {
                val time = (task.specificDetails as? ItemDetails.Task)?.deadline ?: task.startTime
                if (isToday(time)) {
                    groupHasToday = true
                }
                try {
                    val taskDate = Instant.fromEpochMilliseconds(time).toLocalDateTime(timeZone).date
                    if (taskDate >= todayDate) {
                        groupHasUpcoming = true
                    }
                } catch (e: Exception) {
                    // ignore
                }
            }

            if (groupHasToday && todayIndex == -1) {
                todayIndex = currentIndex
            }
            if (groupHasUpcoming && firstUpcomingIndex == -1) {
                firstUpcomingIndex = currentIndex
            }

            currentIndex += 1 // Header
            currentIndex += tasks.size // Items
        }

        if (todayIndex != -1) todayIndex else firstUpcomingIndex
    }

    // Performans: O(n) any{} arama yerine O(1) Set lookup kullan
    val myTaskIds = remember(state.allFetchedTasks) { state.allFetchedTasks.map { it.id }.toHashSet() }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    var hasScrolledToToday by remember(state.viewOption) { mutableStateOf(false) }

    LaunchedEffect(targetTodayIndex, state.hasLoadedTasks) {
        if (!hasScrolledToToday && state.hasLoadedTasks && targetTodayIndex > 0) {
            listState.scrollToItem(targetTodayIndex)
            hasScrolledToToday = true
        }
    }

    val showFab by remember(targetTodayIndex) {
        derivedStateOf {
            if (targetTodayIndex == -1) return@derivedStateOf false
            val visibleItemsInfo = listState.layoutInfo.visibleItemsInfo
            if (visibleItemsInfo.isEmpty()) return@derivedStateOf false
            
            val firstVisible = visibleItemsInfo.first().index
            val lastVisible = visibleItemsInfo.last().index
            
            targetTodayIndex < firstVisible || targetTodayIndex > lastVisible
        }
    }

    val isTodayAbove by remember(targetTodayIndex) {
        derivedStateOf {
            if (targetTodayIndex == -1) return@derivedStateOf false
            val visibleItemsInfo = listState.layoutInfo.visibleItemsInfo
            if (visibleItemsInfo.isEmpty()) return@derivedStateOf false
            
            val firstVisible = visibleItemsInfo.first().index
            targetTodayIndex < firstVisible
        }
    }

    Box(modifier = modifier) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(bottom = 100.dp)
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
                val isMine = myTaskIds.contains(task.id)
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

        if (onLoadMore != null) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.OutlinedButton(
                        onClick = onLoadMore,
                        enabled = !state.isLoadingMoreFutureTasks,
                        shape = RoundedCornerShape(12.dp),
                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        if (state.isLoadingMoreFutureTasks) {
                            androidx.compose.material3.CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        } else {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = stringResource(Res.string.load_more_future_tasks),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }

    if (showFab) {
        FloatingActionButton(
            onClick = {
                coroutineScope.launch {
                    if (targetTodayIndex != -1) {
                        listState.animateScrollToItem(targetTodayIndex)
                    }
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 88.dp, bottom = 100.dp),
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