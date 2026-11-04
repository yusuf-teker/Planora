package com.yusufteker.planora.feature.home.presentation.plan_room_detail.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yusufteker.planora.core.theme.PlanoraColors
import com.yusufteker.planora.core.ui.components.GlassCard
import com.yusufteker.planora.core.ui.components.clearFocusOnTap
import com.yusufteker.planora.feature.home.presentation.components.EmptyStateComponent
import com.yusufteker.planora.feature.home.presentation.home.components.RoundCheckbox
import com.yusufteker.planora.shared.api.ItemDetails
import com.yusufteker.planora.shared.api.TaskDto
import com.yusufteker.planora.shared.api.TaskPriority
import com.yusufteker.planora.shared.api.TaskStatus
import com.yusufteker.planora.shared.api.TaskType
import com.yusufteker.planora.shared.api.UserProfileResponse
import com.yusufteker.planora.shared.api.isUnscheduled
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import planora.core.generated.resources.Res
import planora.core.generated.resources.*

/**
 * Shared Plan Room to-dos component.
 *
 * Displays unscheduled (timeline-free / backlog) to-dos and scheduled to-dos belonging to the room,
 * with quick inline to-do creation, round checkbox status toggling, and completion tracking.
 *
 * @param tasks All tasks associated with this room.
 * @param memberProfiles Map of user profiles by member user ID.
 * @param taskSearchQuery Optional query string to filter to-dos.
 * @param selectedMemberUserIds Filter set of member IDs to show to-dos for.
 * @param onTaskClick Callback when a to-do is clicked.
 * @param onToggleTaskStatus Callback when the checkbox is clicked to toggle completion.
 * @param onQuickCreateTask Callback to quickly create an unscheduled to-do for this room.
 * @param modifier Layout modifier.
 */
@Composable
fun RoomTodosComponent(
    tasks: List<TaskDto>,
    memberProfiles: Map<Int, UserProfileResponse>,
    taskSearchQuery: String = "",
    selectedMemberUserIds: Set<Int> = emptySet(),
    onTaskClick: (TaskDto) -> Unit,
    onToggleTaskStatus: (TaskDto) -> Unit,
    onQuickCreateTask: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var quickTodoText by rememberSaveable { mutableStateOf("") }

    // Filter tasks: must be TaskType.TASK
    val roomTodoTasks = remember(tasks, taskSearchQuery, selectedMemberUserIds) {
        tasks.filter { it.type == TaskType.TASK }.filter { task ->
            if (taskSearchQuery.isBlank()) {
                true
            } else {
                task.title.contains(taskSearchQuery, ignoreCase = true) ||
                    (task.description?.contains(taskSearchQuery, ignoreCase = true) == true) ||
                    task.tags.any { it.contains(taskSearchQuery, ignoreCase = true) }
            }
        }.filter { task ->
            if (selectedMemberUserIds.isEmpty()) {
                true
            } else {
                selectedMemberUserIds.contains(task.creatorId) ||
                    task.participants.any { selectedMemberUserIds.contains(it.userId) }
            }
        }
    }

    val unscheduledTasks = remember(roomTodoTasks) {
        roomTodoTasks.filter { it.isUnscheduled() && it.status != TaskStatus.COMPLETED }
    }

    val scheduledTasks = remember(roomTodoTasks) {
        roomTodoTasks.filter { !it.isUnscheduled() && it.status != TaskStatus.COMPLETED }
            .sortedBy { (it.specificDetails as? ItemDetails.Task)?.deadline ?: it.startTime }
    }

    val completedTasks = remember(roomTodoTasks) {
        roomTodoTasks.filter { it.status == TaskStatus.COMPLETED }
    }

    val hasAnyTasks = unscheduledTasks.isNotEmpty() || scheduledTasks.isNotEmpty() || completedTasks.isNotEmpty()

    Column(
        modifier = modifier
            .fillMaxSize()
            .clearFocusOnTap()
            .padding(horizontal = 16.dp)
    ) {
        // --- Quick Add To-Do Bar ---
        RoomQuickAddBar(
            value = quickTodoText,
            onValueChange = { quickTodoText = it },
            onAdd = {
                if (quickTodoText.isNotBlank()) {
                    onQuickCreateTask(quickTodoText)
                    quickTodoText = ""
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )

        if (!hasAnyTasks) {
            EmptyStateComponent(
                icon = Icons.Default.CheckCircleOutline,
                title = stringResource(Res.string.room_todos_empty_title),
                description = stringResource(Res.string.room_todos_empty_desc),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. Unscheduled To-Dos Section (Backlog / Timeline-free)
                if (unscheduledTasks.isNotEmpty()) {
                    item(key = "section_header_unscheduled") {
                        RoomTodoSectionHeader(
                            title = stringResource(Res.string.room_todos_unscheduled_section),
                            count = unscheduledTasks.size,
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    }

                    items(unscheduledTasks, key = { it.id }) { task ->
                        RoomTodoItemCard(
                            task = task,
                            memberProfiles = memberProfiles,
                            onTaskClick = { onTaskClick(task) },
                            onToggleStatus = { onToggleTaskStatus(task) }
                        )
                    }
                }

                // 2. Scheduled Tasks Section
                if (scheduledTasks.isNotEmpty()) {
                    item(key = "section_header_scheduled") {
                        RoomTodoSectionHeader(
                            title = stringResource(Res.string.room_todos_scheduled_section),
                            count = scheduledTasks.size,
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                            contentColor = MaterialTheme.colorScheme.secondary
                        )
                    }

                    items(scheduledTasks, key = { it.id }) { task ->
                        RoomTodoItemCard(
                            task = task,
                            memberProfiles = memberProfiles,
                            onTaskClick = { onTaskClick(task) },
                            onToggleStatus = { onToggleTaskStatus(task) }
                        )
                    }
                }

                // 3. Completed Section (Always visible directly at bottom)
                if (completedTasks.isNotEmpty()) {
                    item(key = "section_header_completed") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp, bottom = 4.dp, start = 4.dp, end = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(Res.string.room_todos_completed_section),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(22.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = completedTasks.size.toString(),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    items(completedTasks, key = { it.id }) { task ->
                        RoomTodoItemCard(
                            task = task,
                            memberProfiles = memberProfiles,
                            onTaskClick = { onTaskClick(task) },
                            onToggleStatus = { onToggleTaskStatus(task) },
                            isCompleted = true
                        )
                    }
                }
            }
        }
    }
}

/**
 * Header banner for to-do sections.
 */
@Composable
private fun RoomTodoSectionHeader(
    title: String,
    count: Int,
    containerColor: Color,
    contentColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = containerColor,
            modifier = Modifier.padding(start = 8.dp)
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}

/**
 * Single card representing a room to-do item.
 */
@Composable
private fun RoomTodoItemCard(
    task: TaskDto,
    memberProfiles: Map<Int, UserProfileResponse>,
    onTaskClick: () -> Unit,
    onToggleStatus: () -> Unit,
    isCompleted: Boolean = false
) {
    val taskDetails = task.specificDetails as? ItemDetails.Task
    val priority = taskDetails?.priority ?: TaskPriority.MEDIUM
    val deadline = taskDetails?.deadline

    val deadlineText = remember(deadline) {
        if (deadline != null && deadline > 0) {
            val date = Instant.fromEpochMilliseconds(deadline)
                .toLocalDateTime(TimeZone.currentSystemDefault()).date
            "${date.dayOfMonth}/${date.monthNumber}/${date.year}"
        } else {
            null
        }
    }

    val creatorProfile = memberProfiles[task.creatorId]

    GlassCard(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onTaskClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox
            RoundCheckbox(
                isChecked = isCompleted,
                onCheckedChange = onToggleStatus
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Body
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isCompleted) FontWeight.Normal else FontWeight.Medium,
                        textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    color = if (isCompleted) {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (!task.description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = task.description!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Tags & Deadline & Assignee row
                val hasTags = task.tags.isNotEmpty()
                val hasDeadline = deadlineText != null

                if (hasTags || hasDeadline || creatorProfile != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (hasDeadline) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    modifier = Modifier.size(11.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = deadlineText!!,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Tag chips (first 2)
                        task.tags.take(2).forEach { tag ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            ) {
                                Text(
                                    text = "#$tag",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        if (task.tags.size > 2) {
                            Text(
                                text = "+${task.tags.size - 2}",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        if (creatorProfile != null) {
                            Text(
                                text = creatorProfile.name,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Inline quick-add bar for room to-dos.
 */
@Composable
private fun RoomQuickAddBar(
    value: String,
    onValueChange: (String) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = {
                    Text(
                        text = stringResource(Res.string.room_quick_add_todo_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                },
                modifier = Modifier.weight(1f),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onAdd() })
            )

            IconButton(
                onClick = onAdd,
                enabled = value.isNotBlank(),
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (value.isNotBlank()) MaterialTheme.colorScheme.primary else Color.Transparent
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(Res.string.action_add_task),
                    tint = if (value.isNotBlank()) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            }
        }
    }
}
