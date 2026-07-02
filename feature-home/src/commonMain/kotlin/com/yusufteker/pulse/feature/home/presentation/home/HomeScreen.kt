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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import com.yusufteker.pulse.core.navigation.Screen.MainDestination
import com.yusufteker.pulse.core.utils.formatDayName
import com.yusufteker.pulse.core.utils.formatShortDate
import com.yusufteker.pulse.core.utils.formatTime
import com.yusufteker.pulse.core.utils.isToday
import com.yusufteker.pulse.core.utils.isTomorrow
import com.yusufteker.pulse.shared.api.TaskDto
import com.yusufteker.pulse.shared.api.TaskType

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
                rootNavigator.navigate(com.yusufteker.pulse.core.navigation.Screen.TaskEditor(taskId = null))
            }
            is HomeEffect.NavigateToCreateEvent -> {
                rootNavigator.navigate(com.yusufteker.pulse.core.navigation.Screen.EventDetail(eventId = null))
            }
            is HomeEffect.NavigateToTaskEditor -> {
                rootNavigator.navigate(com.yusufteker.pulse.core.navigation.Screen.TaskEditor(taskId = effect.taskId))
            }
            is HomeEffect.NavigateToEventDetail -> {
                rootNavigator.navigate(com.yusufteker.pulse.core.navigation.Screen.EventDetail(eventId = effect.eventId))
            }
            is HomeEffect.NavigateToNoteEditor -> {
                rootNavigator.navigate(com.yusufteker.pulse.core.navigation.Screen.NoteEditor(noteId = effect.noteId))
            }
        }
    }

    var isFabExpanded by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    androidx.compose.material3.Scaffold(
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = isFabExpanded,
                    enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInVertically(initialOffsetY = { it }),
                    exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.slideOutVertically(targetOffsetY = { it })
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
                            androidx.compose.material3.SmallFloatingActionButton(
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
                            androidx.compose.material3.SmallFloatingActionButton(
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
                androidx.compose.material3.FloatingActionButton(
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
                .windowInsetsPadding(WindowInsets.statusBars)
                .imePadding(),
            horizontalAlignment = Alignment.Start
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            // Header (Pulse Top Bar)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
            Text(
                text = "Pulse",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
            )
            // Optional: Profile or notification icon could go here
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Timeline
        val grouped = state.upcomingTasks.groupBy { task ->
            val time = (task.specificDetails as? com.yusufteker.pulse.shared.api.ItemDetails.Task)?.deadline ?: task.endTime ?: task.startTime
            when {
                isToday(time) -> "Bugün"
                isTomorrow(time) -> "Yarın"
                else -> "${formatDayName(time)}, ${formatShortDate(time)}"
            }
        }

        if (state.upcomingTasks.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Henüz görevin yok",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "Aşağıdan AI'a bir şey söyle!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
            }
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
                            onClick = { viewModel.onEvent(HomeEvent.TimelineItemClicked(task)) }
                        )
                    }
                }
            }
        }

        // Smart Input (bottom)
        OutlinedTextField(
            value = state.smartInputText,
            onValueChange = { viewModel.onEvent(HomeEvent.SmartInputChanged(it)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 8.dp),
            placeholder = { Text("AI'a söyle: Yarın 15:00'te toplantım var...") },
            trailingIcon = {
                IconButton(onClick = { viewModel.onEvent(HomeEvent.SubmitSmartInput) }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send to AI",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            },
            shape = RoundedCornerShape(16.dp),
            singleLine = true
        )
    }
}
}

// ── Day Header ─────────────────────────────────
@Composable
private fun DayHeader(dayLabel: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = dayLabel,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant)
        )
    }
}

// ── Timeline Task Card ─────────────────────────
@Composable
private fun TimelineTaskCard(
    task: TaskDto,
    onClick: () -> Unit = {}
) {
    val typeColor = when (task.type) {
        TaskType.EVENT -> Color(0xFF6366F1)  // Indigo
        TaskType.TASK -> Color(0xFF10B981)   // Emerald
        TaskType.NOTE -> Color(0xFFF59E0B)   // Amber
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Time column (left)
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.width(56.dp)
        ) {
            val primaryTime = (task.specificDetails as? com.yusufteker.pulse.shared.api.ItemDetails.Task)?.deadline ?: task.startTime
            Text(
                text = formatTime(primaryTime),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            // Show endTime only if it's not a Task (since tasks use deadline and their endTime is null or irrelevant here),
            // or if we have an endTime explicitly defined for non-tasks.
            task.endTime?.let {
                if (task.type != TaskType.TASK || it != task.startTime) {
                    Text(
                        text = formatTime(it),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Vertical line indicator
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(40.dp)
                .clip(RoundedCornerShape(percent = 50))
                .background(typeColor.copy(alpha = 0.8f))
        )

        Spacer(modifier = Modifier.width(16.dp))

        // Content column (right)
        Column(modifier = Modifier.weight(1f)) {
            // Title
            Text(
                text = task.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Description
            if (!task.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = task.description ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Footer (Participant count if any)
            if (task.participants.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "\uD83D\uDC65 ${task.participants.size} katılımcı",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}
