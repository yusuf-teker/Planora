package com.yusufteker.pulse.feature.home.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.zIndex
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.SyncProblem
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import kotlinx.datetime.toLocalDateTime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material3.Surface
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yusufteker.pulse.core.base.CollectEffect
import com.yusufteker.pulse.core.navigation.LocalMainNavigator
import com.yusufteker.pulse.feature.home.presentation.components.EmptyStateComponent
import com.yusufteker.pulse.feature.home.presentation.home.components.DayHeader
import com.yusufteker.pulse.feature.home.presentation.home.components.TimelineTaskCard
import com.yusufteker.pulse.feature.home.presentation.home.components.FilterBottomSheetComponent
import com.yusufteker.pulse.core.navigation.Screen
import com.yusufteker.pulse.core.navigation.Screen.MainDestination
import com.yusufteker.pulse.core.utils.formatDayName
import com.yusufteker.pulse.core.utils.formatShortDate
import com.yusufteker.pulse.core.utils.formatTime
import com.yusufteker.pulse.core.utils.isToday
import com.yusufteker.pulse.core.utils.isTomorrow
import com.yusufteker.pulse.core.utils.rotateVertically
import com.yusufteker.pulse.shared.api.TaskDto
import com.yusufteker.pulse.shared.api.TaskType
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.yusufteker.pulse.core.utils.getRelativeTimeBucket
import com.yusufteker.pulse.feature.home.presentation.home.components.CalendarView

/**
 * Home screen composable.
 *
 * Displays the main feed with top app bar for navigation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel
) {
    val mainNavigator = LocalMainNavigator.current
    val rootNavigator = com.yusufteker.pulse.core.navigation.LocalNavigator.current
    val state by viewModel.state.collectAsStateWithLifecycle()

    viewModel.effect.CollectEffect { effect ->
        when (effect) {
            is HomeEffect.NavigateToProfile -> {
                mainNavigator.setRoot(MainDestination.Home)
                mainNavigator.navigate(MainDestination.Profile)
            }
            is HomeEffect.NavigateToSettings -> {
                mainNavigator.setRoot(MainDestination.Home)
                mainNavigator.navigate(MainDestination.Settings)
            }
            is HomeEffect.NavigateToCreateTask -> {
                rootNavigator.navigate(Screen.TaskEditor(taskId = null))
            }
            is HomeEffect.NavigateToCreateEvent -> {
                rootNavigator.navigate(Screen.EventDetail(eventId = null))
            }
            is HomeEffect.NavigateToTaskEditor -> {
                rootNavigator.navigate(Screen.TaskEditor(taskId = effect.taskId))
            }
            is HomeEffect.NavigateToEventDetail -> {
                rootNavigator.navigate(Screen.EventDetail(eventId = effect.eventId))
            }
            is HomeEffect.NavigateToNoteEditor -> {
                rootNavigator.navigate(Screen.NoteEditor(noteId = effect.noteId))
            }
        }
    }

    var isFabExpanded by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                AnimatedVisibility(
                    visible = isFabExpanded,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                    exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Text("Task Ekle", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.bodyMedium)
                            }
                            SmallFloatingActionButton(
                                onClick = { 
                                    isFabExpanded = false
                                    viewModel.onEvent(HomeEvent.CreateTaskClicked) 
                                },
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "Add Task")
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Text("Etkinlik Ekle", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.bodyMedium)
                            }
                            SmallFloatingActionButton(
                                onClick = { 
                                    isFabExpanded = false
                                    viewModel.onEvent(HomeEvent.CreateEventClicked) 
                                },
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Icon(Icons.Default.Event, contentDescription = "Add Event")
                            }
                        }
                    }
                }
                FloatingActionButton(
                    onClick = { isFabExpanded = !isFabExpanded },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(if (isFabExpanded) Icons.Default.Close else Icons.Default.Add, contentDescription = "Expand")
                }
            }
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .imePadding(),
            horizontalAlignment = Alignment.Start
        ) {
            // Header (Pulsy Top Bar)
            
            // Header (Pulsy Top Bar)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Pulsy",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // View Option Toggle
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isDate = state.viewOption == TimelineViewOption.DATE
                    val isRelative = state.viewOption == TimelineViewOption.RELATIVE
                    val isCalendar = state.viewOption == TimelineViewOption.CALENDAR
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDate) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { viewModel.onEvent(HomeEvent.ViewOptionChanged(TimelineViewOption.DATE)) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Tarih",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isDate) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isRelative) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { viewModel.onEvent(HomeEvent.ViewOptionChanged(TimelineViewOption.RELATIVE)) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Periyot",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isRelative) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isCalendar) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { viewModel.onEvent(HomeEvent.ViewOptionChanged(TimelineViewOption.CALENDAR)) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Takvim",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isCalendar) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    }
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Filter Button
                IconButton(
                    onClick = { viewModel.onEvent(HomeEvent.ToggleFilterSheet(true)) },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (state.filterOptions.showOnlyNextRecurring || state.filterOptions.showCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                        .size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Filtreler",
                        tint = if (state.filterOptions.showOnlyNextRecurring || !state.filterOptions.showCompleted) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

        Spacer(modifier = Modifier.height(16.dp))

        // Calendar View
        if (state.viewOption == TimelineViewOption.CALENDAR) {
            val tasksByDate = remember(state.allFetchedTasks, state.filterOptions) {
                var filteredForCalendar = state.allFetchedTasks
                if (!state.filterOptions.showCompleted) {
                    filteredForCalendar = filteredForCalendar.filter { it.status != com.yusufteker.pulse.shared.api.TaskStatus.COMPLETED }
                }
                if (state.filterOptions.showOnlyNextRecurring) {
                    val uniqueTasks = mutableListOf<com.yusufteker.pulse.shared.api.TaskDto>()
                    val seenRecurringBaseIds = mutableSetOf<String>()
                    for (task in filteredForCalendar) {
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
                    filteredForCalendar = uniqueTasks
                }
                
                filteredForCalendar.groupBy { task ->
                    kotlinx.datetime.Instant.fromEpochMilliseconds(task.startTime)
                        .toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).date
                }
            }
            
            CalendarView(
                tasksByDate = tasksByDate,
                selectedDate = state.selectedCalendarDate,
                visibleMonth = state.visibleCalendarMonth,
                onDateSelected = { viewModel.onEvent(HomeEvent.CalendarDateSelected(it)) },
                onMonthChanged = { viewModel.onEvent(HomeEvent.CalendarMonthChanged(it)) }
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            if (state.selectedCalendarDate != null && state.upcomingTasks.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("Bugün etkinlik yok.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else if (state.selectedCalendarDate == null && state.upcomingTasks.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("Bu ay etkinlik yok.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        


        // Timeline
        val grouped = state.upcomingTasks.groupBy<com.yusufteker.pulse.shared.api.TaskDto, String> { task ->
            val time = (task.specificDetails as? com.yusufteker.pulse.shared.api.ItemDetails.Task)?.deadline ?: task.endTime ?: task.startTime
            
            if (state.viewOption == TimelineViewOption.DATE) {
                when {
                    isToday(time) -> "Bugün"
                    isTomorrow(time) -> "Yarın"
                    else -> "${formatDayName(time)}, ${formatShortDate(time)}"
                }
            } else if (state.viewOption == TimelineViewOption.CALENDAR) {
                if (state.selectedCalendarDate != null) {
                    formatShortDate(time)
                } else {
                    "${formatDayName(time)}, ${formatShortDate(time)}"
                }
            }
            else {
                getRelativeTimeBucket(time)
            }
        }

        if (state.upcomingTasks.isEmpty()) {
            EmptyStateComponent(
                icon = Icons.Default.CalendarToday,
                title = "Henüz görevin yok",
                description = "Aşağıdan AI'a bir şey söyle!",
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                grouped.forEach { (dayLabel, tasks) ->
                    // Day header
                    item(key = "header_$dayLabel") {
                        DayHeader(dayLabel = dayLabel)
                    }
                    // Task cards for that day
                    items(tasks, key = { it.id }) { task ->
                        TimelineTaskCard(
                            task = task,
                            showDate = state.viewOption == TimelineViewOption.RELATIVE,
                            onClick = { viewModel.onEvent(HomeEvent.TimelineItemClicked(task)) }
                        )
                    }
                }
            }
        }

    }
    }
    
    if (state.isFilterSheetVisible) {
        FilterBottomSheetComponent(
            filterOptions = state.filterOptions,
            onDismiss = { viewModel.onEvent(HomeEvent.ToggleFilterSheet(false)) },
            onFilterOptionsChanged = { viewModel.onEvent(HomeEvent.FilterOptionChanged(it)) }
        )
    }
}

