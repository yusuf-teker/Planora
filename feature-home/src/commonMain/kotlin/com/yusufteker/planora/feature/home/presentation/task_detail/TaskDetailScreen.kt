package com.yusufteker.planora.feature.home.presentation.task_detail

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.yusufteker.planora.core.ui.components.getOptimizedCloudinaryUrl
import com.yusufteker.planora.core.utils.formatShortDate
import com.yusufteker.planora.core.utils.formatTime
import com.yusufteker.planora.shared.api.TaskPriority
import com.yusufteker.planora.shared.api.TaskStatus
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import planora.core.generated.resources.Res
import planora.core.generated.resources.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    viewModel: TaskDetailViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToEditTask: (String, String?) -> Unit = { _, _ -> },
    onNavigateToCopyTask: (String, String?) -> Unit = { _, _ -> },
    onNavigateToFocus: (String) -> Unit = {},
    onNavigateToEditNote: (String) -> Unit = {},
    onNavigateToPlanRoom: (String) -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()
    val shareManager = org.koin.compose.koinInject<com.yusufteker.planora.core.share.ShareManager>()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val scope = this
        viewModel.effect.collect { effect ->
            when (effect) {
                is TaskDetailEffect.NavigateBack -> onNavigateBack()
                is TaskDetailEffect.NavigateToEditTask -> onNavigateToEditTask(effect.taskId, effect.planRoomId)
                is TaskDetailEffect.NavigateToCopyTask -> onNavigateToCopyTask(effect.taskId, effect.planRoomId)
                is TaskDetailEffect.NavigateToFocus -> onNavigateToFocus(effect.taskId)
                is TaskDetailEffect.ShowSnackbar -> {
                    scope.launch {
                        snackbarHostState.showSnackbar(effect.message)
                    }
                }
                is TaskDetailEffect.ShareItem -> {
                    shareManager.shareText(effect.url, state.title.ifBlank { "Task" })
                }
            }
        }
    }

    val isCompleted = state.status == TaskStatus.COMPLETED

    val animatedHeaderBg by animateColorAsState(
        targetValue = if (isCompleted) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.title_task_detail), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.onEvent(TaskDetailEvent.OnBackClick) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.onEvent(TaskDetailEvent.OnShareClick) }) {
                        Icon(Icons.Default.Share, contentDescription = stringResource(Res.string.action_share), tint = MaterialTheme.colorScheme.primary)
                    }

                    var showMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = stringResource(Res.string.action_more_options),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.action_copy)) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.ContentCopy,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    viewModel.onEvent(TaskDetailEvent.OnCopyClick)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.action_delete), color = MaterialTheme.colorScheme.error) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    showDeleteDialog = true
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 12.dp,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    state.taskId?.let { taskId ->
                        OutlinedButton(
                            onClick = { viewModel.onEvent(TaskDetailEvent.OnFocusClick) },
                            modifier = Modifier.weight(1f).height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                                brush = Brush.linearGradient(
                                    listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                                )
                            )
                        ) {
                            Icon(
                                painterResource(Res.drawable.focus_planora),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                stringResource(Res.string.action_focus),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Button(
                        onClick = { viewModel.onEvent(TaskDetailEvent.OnEditClick) },
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(Res.string.action_edit_task), fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (state.isLoading && state.title.isBlank()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Spacer(modifier = Modifier.height(4.dp))

                // Plan Room Banner if associated
                if (state.planRoomId != null || state.planRoomName != null) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .clickable(enabled = state.planRoomId != null) {
                                state.planRoomId?.let { roomId -> onNavigateToPlanRoom(roomId) }
                            },
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        shadowElevation = 1.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                if (!state.planRoomImageUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = getOptimizedCloudinaryUrl(state.planRoomImageUrl!!),
                                        contentDescription = state.planRoomName,
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = state.planRoomName?.take(1)?.uppercase() ?: "O",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = state.planRoomName ?: stringResource(Res.string.connected_room),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            if (state.planRoomId != null) {
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }

                // Hero Task Card with Single Completion Toggle Button
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = animatedHeaderBg),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = BorderStroke(
                        1.dp,
                        if (isCompleted) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(22.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Header Top Row: Status Badge & Priority Flag
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Status Badge
                            Surface(
                                color = if (isCompleted) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (isCompleted) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.TaskAlt,
                                        contentDescription = null,
                                        tint = if (isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = if (isCompleted) stringResource(Res.string.task_status_completed) else stringResource(Res.string.task_status_pending),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Priority Badge
                            val (priorityColor, priorityTextRes) = when (state.priority) {
                                TaskPriority.URGENT -> MaterialTheme.colorScheme.error to Res.string.priority_urgent
                                TaskPriority.HIGH -> Color(0xFFE53935) to Res.string.priority_high
                                TaskPriority.MEDIUM -> Color(0xFFFB8C00) to Res.string.priority_medium
                                TaskPriority.LOW -> MaterialTheme.colorScheme.outline to Res.string.priority_low
                            }
                            Surface(
                                color = priorityColor.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, priorityColor.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Flag,
                                        contentDescription = null,
                                        tint = priorityColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = stringResource(priorityTextRes),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = priorityColor
                                    )
                                }
                            }
                        }

                        // Task Title
                        Text(
                            text = state.title.ifBlank { stringResource(Res.string.untitled_task) },
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-0.5).sp
                            ),
                            textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                            color = if (isCompleted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            else MaterialTheme.colorScheme.onSurface
                        )

                        // SINGLE Primary Toggle Button for Task Completion
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { viewModel.onEvent(TaskDetailEvent.OnToggleStatus) },
                            color = if (isCompleted) MaterialTheme.colorScheme.surface
                            else MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(16.dp),
                            shadowElevation = 2.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = if (isCompleted) Icons.Default.TaskAlt else Icons.Default.Check,
                                    contentDescription = null,
                                    tint = if (isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = if (isCompleted) stringResource(Res.string.action_mark_pending) else stringResource(Res.string.action_mark_completed),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                }

                // Subtasks Card with Animated Progress Bar
                if (state.subtasks.isNotEmpty()) {
                    val completedCount = state.subtasks.count { it.status == TaskStatus.COMPLETED }
                    val progressRatio = if (state.subtasks.isNotEmpty()) completedCount.toFloat() / state.subtasks.size else 0f
                    val animatedProgress by animateFloatAsState(targetValue = progressRatio)

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Text(
                                        text = stringResource(Res.string.label_subtasks),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "$completedCount / ${state.subtasks.size}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            LinearProgressIndicator(
                                progress = { animatedProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )

                            Column(
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                state.subtasks.forEach { subtask ->
                                    val subtaskCompleted = subtask.status == TaskStatus.COMPLETED
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { viewModel.onEvent(TaskDetailEvent.OnToggleSubtask(subtask.id)) },
                                        color = if (subtaskCompleted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = subtaskCompleted,
                                                onCheckedChange = { viewModel.onEvent(TaskDetailEvent.OnToggleSubtask(subtask.id)) },
                                                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = subtask.title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (subtaskCompleted) FontWeight.Normal else FontWeight.Medium,
                                                textDecoration = if (subtaskCompleted) TextDecoration.LineThrough else TextDecoration.None,
                                                color = if (subtaskCompleted) MaterialTheme.colorScheme.onSurfaceVariant
                                                else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Date & Time Details Card (NO Title Header, Icon on Top/Left, Label: "Deadline")
                val dateMs = state.dueDateMs ?: state.startTimeMs
                if (dateMs != null || state.isRecurring || state.reminders.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            if (dateMs != null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CalendarMonth,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = stringResource(Res.string.label_deadline),
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${formatShortDate(dateMs)}  •  ${formatTime(dateMs)}",
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            if (state.isRecurring && state.recurrenceRule != null) {
                                if (dateMs != null) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Repeat,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = stringResource(Res.string.repeat_label),
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        val repeatText = when (val rule = state.recurrenceRule) {
                                            is com.yusufteker.planora.shared.api.RecurrenceRule.Daily -> stringResource(Res.string.repeat_daily)
                                            is com.yusufteker.planora.shared.api.RecurrenceRule.Weekly -> stringResource(Res.string.repeat_weekly_pattern, rule.daysOfWeek.size.toString())
                                            is com.yusufteker.planora.shared.api.RecurrenceRule.Monthly -> if (rule.isLastDay) stringResource(Res.string.repeat_monthly_last_day) else stringResource(Res.string.repeat_monthly)
                                            is com.yusufteker.planora.shared.api.RecurrenceRule.Yearly -> stringResource(Res.string.repeat_yearly)
                                            null -> ""
                                        }
                                        Text(
                                            text = repeatText,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            if (state.reminders.isNotEmpty()) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Notifications,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = stringResource(Res.string.reminders_label),
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.tertiary
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        val remNow = stringResource(Res.string.reminder_time_now)
                                        val rem5 = stringResource(Res.string.reminder_5_min)
                                        val rem15 = stringResource(Res.string.reminder_15_min)
                                        val rem30 = stringResource(Res.string.reminder_30_min)
                                        val rem60 = stringResource(Res.string.reminder_1_hour)
                                        val rem1440 = stringResource(Res.string.reminder_1_day)
                                        val reminderSummary = state.reminders.joinToString(", ") { mins ->
                                            when (mins) {
                                                0 -> remNow
                                                5 -> rem5
                                                15 -> rem15
                                                30 -> rem30
                                                60 -> rem60
                                                1440 -> rem1440
                                                else -> "$mins dk"
                                            }
                                        }
                                        Text(
                                            text = reminderSummary,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Description Card (No "(Optional)", localized label_description)
                if (state.description.isNotBlank()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    text = stringResource(Res.string.label_description),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text(
                                    text = state.description,
                                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                        }
                    }
                }

                // Participants Section (Assignees)
                if (state.participants.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Group,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    text = stringResource(Res.string.assignees_label),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            state.participants.forEach { participant ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        if (!participant.profileImageUrl.isNullOrBlank()) {
                                            AsyncImage(
                                                model = getOptimizedCloudinaryUrl(participant.profileImageUrl!!),
                                                contentDescription = participant.name,
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .clip(CircleShape),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = participant.name.take(1).uppercase(),
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            }
                                        }
                                        Text(
                                            text = participant.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Attached Notes Section
                if (state.notes.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = stringResource(Res.string.label_attached_notes),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            state.notes.forEach { note ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { onNavigateToEditNote(note.id) },
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = note.title.ifBlank { stringResource(Res.string.label_attached_notes) },
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Icon(
                                            Icons.Default.ChevronRight,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(Res.string.delete_confirm_title)) },
            text = { Text(stringResource(Res.string.delete_task_confirm_msg)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.onEvent(TaskDetailEvent.OnDeleteClick)
                    }
                ) {
                    Text(stringResource(Res.string.action_delete), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(Res.string.cancel))
                }
            }
        )
    }
}
