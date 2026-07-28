package com.yusufteker.planora.feature.home.presentation.task_editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yusufteker.planora.feature.home.presentation.components.DateTimePickerSheet
import com.yusufteker.planora.feature.home.presentation.components.FormRow
import com.yusufteker.planora.feature.home.presentation.components.FormSection
import com.yusufteker.planora.feature.home.presentation.components.FormSwitchRow
import com.yusufteker.planora.feature.home.presentation.components.RepeatPickerSheet
import com.yusufteker.planora.feature.home.presentation.components.ReminderPickerSheet
import com.yusufteker.planora.feature.home.presentation.components.ParticipantPickerSheet
import com.yusufteker.planora.feature.home.presentation.components.PriorityPickerSheet
import com.yusufteker.planora.core.utils.formatShortDate
import com.yusufteker.planora.core.utils.formatTime
import com.yusufteker.planora.shared.api.TaskStatus
import com.yusufteker.planora.shared.api.TaskPriority
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import planora.core.generated.resources.Res
import planora.core.generated.resources.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditorScreen(
    viewModel: TaskEditorViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToFocus: (String) -> Unit = {},
    onNavigateToCreateNote: (String) -> Unit = {},
    onNavigateToEditNote: (String) -> Unit = {},
    onNavigateToPlanRoom: (String) -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val shareManager = org.koin.compose.koinInject<com.yusufteker.planora.core.share.ShareManager>()

    val dateSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val repeatSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val reminderSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val participantSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val prioritySheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(Unit) {
        val scope = this
        viewModel.effect.collect { effect ->
            when (effect) {
                is TaskEditorEffect.NavigateBack -> onNavigateBack()
                is TaskEditorEffect.ShowSnackbar -> {
                    scope.launch {
                        snackbarHostState.showSnackbar(effect.message)
                    }
                }
                is TaskEditorEffect.ShareItem -> {
                    shareManager.shareText(effect.url, state.title.ifBlank { "Task" })
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.onEvent(TaskEditorEvent.OnDispose)
        }
    }

    val isTimeOnly = state.isRecurring && state.recurrenceRule != null && 
        (state.recurrenceRule is com.yusufteker.planora.shared.api.RecurrenceRule.Daily || 
         state.recurrenceRule is com.yusufteker.planora.shared.api.RecurrenceRule.Weekly || 
         (state.recurrenceRule as? com.yusufteker.planora.shared.api.RecurrenceRule.Monthly)?.isLastDay == true)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = when {
                            state.isCopyMode -> stringResource(Res.string.title_copy_task)
                            state.id == null -> stringResource(Res.string.title_new_task)
                            else -> stringResource(Res.string.action_edit)
                        }, 
                        fontWeight = FontWeight.SemiBold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.onEvent(TaskEditorEvent.OnBackClick) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(Res.string.back))
                    }
                },
                actions = {
                    if (state.id != null) {
                        IconButton(onClick = { onNavigateToFocus(state.id!!) }) {
                            Icon(
                                painterResource(Res.drawable.focus_planora),
                                contentDescription = "Odaklan",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        
                        IconButton(onClick = { viewModel.onEvent(TaskEditorEvent.OnShareClick) }) {
                            Icon(Icons.Default.Share, contentDescription = "Paylaş", tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    IconButton(onClick = { viewModel.onEvent(TaskEditorEvent.SaveClicked) }) {
                        Icon(Icons.Default.Check, contentDescription = stringResource(Res.string.save), tint = MaterialTheme.colorScheme.primary)
                    }

                    if (state.id != null) {
                        var showMenu by remember { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(
                                    Icons.Default.MoreVert,
                                    contentDescription = "Daha Fazla",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
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
                                        viewModel.onEvent(TaskEditorEvent.DeleteClicked)
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (state.isLoading && state.id != null && state.title.isBlank()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {

            val scrollState = rememberScrollState()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Shared Room Banner if task belongs to a plan room
                if (state.planRoomId != null || state.planRoomName != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(enabled = state.planRoomId != null) {
                                state.planRoomId?.let { roomId ->
                                    onNavigateToPlanRoom(roomId)
                                }
                            },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Group,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = stringResource(Res.string.shared_room_task_label),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                                    )
                                    Text(
                                        text = state.planRoomName ?: stringResource(Res.string.connected_room_label),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                            }
                            if (state.planRoomId != null) {
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = "Odaya Git",
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
                // Title and Description Section
                FormSection {
                    TextField(
                        value = state.title,
                        onValueChange = { viewModel.onEvent(TaskEditorEvent.TitleChanged(it)) },
                        placeholder = { Text(stringResource(Res.string.task_title_label), style = MaterialTheme.typography.titleLarge) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        singleLine = true
                    )
                    
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 16.dp))

                    TextField(
                        value = state.description,
                        onValueChange = { viewModel.onEvent(TaskEditorEvent.DescriptionChanged(it)) },
                        placeholder = { Text(stringResource(Res.string.task_desc_label)) },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )
                }

                FormSection {
                    val deadlineText = state.deadlineDateMs?.let { ms ->
                        if (state.isRecurring && state.recurrenceRule != null) {
                            when (val rule = state.recurrenceRule) {
                                is com.yusufteker.planora.shared.api.RecurrenceRule.Yearly -> "${formatShortDate(ms)} ${formatTime(ms)}"
                                is com.yusufteker.planora.shared.api.RecurrenceRule.Monthly -> if (rule.isLastDay) formatTime(ms) else "${formatShortDate(ms)} ${formatTime(ms)}"
                                else -> formatTime(ms)
                            }
                        } else {
                            "${formatShortDate(ms)} ${formatTime(ms)}"
                        }
                    } ?: stringResource(Res.string.option_not_selected)

                    FormRow(
                        label = if (isTimeOnly) stringResource(Res.string.time_label) else stringResource(Res.string.deadline_label),
                        value = deadlineText,
                        onClick = { viewModel.onEvent(TaskEditorEvent.OnDeadlinePickerVisibilityChanged(true)) }
                    )
                    
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 16.dp))
                    
                    val repeatText = when (val rule = state.recurrenceRule) {
                        null -> stringResource(Res.string.repeat_none)
                        is com.yusufteker.planora.shared.api.RecurrenceRule.Daily -> stringResource(Res.string.repeat_daily)
                        is com.yusufteker.planora.shared.api.RecurrenceRule.Weekly -> stringResource(Res.string.repeat_weekly_pattern, rule.daysOfWeek.size.toString())
                        is com.yusufteker.planora.shared.api.RecurrenceRule.Monthly -> if (rule.isLastDay) stringResource(Res.string.repeat_monthly_last_day) else stringResource(Res.string.repeat_monthly)
                        is com.yusufteker.planora.shared.api.RecurrenceRule.Yearly -> stringResource(Res.string.repeat_yearly)
                    }

                    FormRow(
                        label = stringResource(Res.string.repeat_label),
                        value = repeatText,
                        onClick = { viewModel.onEvent(TaskEditorEvent.OnRepeatPickerVisibilityChanged(true)) }
                    )
                }

                // Options Section
                FormSection {
                    if (state.id != null) {
                        FormSwitchRow(
                            label = stringResource(Res.string.task_completed_label),
                            checked = state.status == TaskStatus.COMPLETED,
                            onCheckedChange = { viewModel.onEvent(TaskEditorEvent.StatusChanged(it)) }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 16.dp))
                    }

                    FormSwitchRow(
                        label = stringResource(Res.string.task_optional_label),
                        checked = state.isOptional,
                        onCheckedChange = { viewModel.onEvent(TaskEditorEvent.OnIsOptionalChanged(it)) }
                    )
                    
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 16.dp))

                    FormRow(
                        label = stringResource(Res.string.reminders_label),
                        value = if (state.reminders.isNotEmpty()) stringResource(Res.string.reminders_selected_count_pattern, state.reminders.size.toString()) else stringResource(Res.string.repeat_none),
                        onClick = { viewModel.onEvent(TaskEditorEvent.OnReminderPickerVisibilityChanged(true)) }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 16.dp))

                    val priorityText = when (state.priority) {
                        TaskPriority.LOW -> stringResource(Res.string.priority_low)
                        TaskPriority.MEDIUM -> stringResource(Res.string.priority_medium)
                        TaskPriority.HIGH -> stringResource(Res.string.priority_high)
                        TaskPriority.URGENT -> stringResource(Res.string.priority_urgent)
                    }
                    FormRow(
                        label = stringResource(Res.string.priority_label),
                        value = priorityText,
                        onClick = { viewModel.onEvent(TaskEditorEvent.OnPriorityPickerVisibilityChanged(true)) }
                    )
                }

                // Context specific: Plan Room Participants
                if (state.planRoomId != null || state.participants.isNotEmpty()) {
                    FormSection {
                        val participantsText = if (state.participants.isEmpty()) {
                            stringResource(Res.string.option_not_selected)
                        } else {
                            state.participants.values.joinToString(", ")
                        }
                        FormRow(
                            label = stringResource(Res.string.assignees_label),
                            value = participantsText,
                            onClick = { 
                                if (state.planRoomId != null) {
                                    viewModel.onEvent(TaskEditorEvent.OnParticipantPickerVisibilityChanged(true)) 
                                }
                            }
                        )
                    }
                }
                // Sub-items (Notes)
                if (state.id != null) {
                    FormSection {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(Res.string.sub_notes_label),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(onClick = { onNavigateToCreateNote(state.id!!) }) {
                                Text(stringResource(Res.string.action_create_note))
                            }
                        }
                        
                        if (state.subItems.isEmpty()) {
                            Text(
                                text = stringResource(Res.string.no_sub_notes_added),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        } else {
                            state.subItems.forEach { subItem ->
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 16.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onNavigateToEditNote(subItem.id)
                                        }
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = subItem.title,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (state.isDeadlinePickerVisible) {
        DateTimePickerSheet(
            initialTimeMs = state.deadlineDateMs,
            timeOnly = isTimeOnly,
            sheetState = dateSheetState,
            onDismissRequest = { viewModel.onEvent(TaskEditorEvent.OnDeadlinePickerVisibilityChanged(false)) },
            onDateTimeSelected = { ms -> viewModel.onEvent(TaskEditorEvent.OnDeadlineSelected(ms)) }
        )
    }

    if (state.isRepeatPickerVisible) {
        RepeatPickerSheet(
            initialRule = state.recurrenceRule,
            startDateMs = state.deadlineDateMs ?: com.yusufteker.planora.core.utils.getCurrentTimeMs(),
            sheetState = repeatSheetState,
            onDismissRequest = { 
                viewModel.onEvent(TaskEditorEvent.OnRepeatPickerVisibilityChanged(false))
            },
            onRuleSelected = { rule -> 
                viewModel.onEvent(TaskEditorEvent.OnRecurrenceRuleChanged(rule))
            }
        )
    }

    if (state.isReminderPickerVisible) {
        ReminderPickerSheet(
            selectedReminders = state.reminders,
            sheetState = reminderSheetState,
            onDismissRequest = { viewModel.onEvent(TaskEditorEvent.OnReminderPickerVisibilityChanged(false)) },
            onReminderToggled = { min -> viewModel.onEvent(TaskEditorEvent.OnReminderToggled(min)) }
        )
    }

    if (state.isParticipantPickerVisible) {
        ParticipantPickerSheet(
            title = stringResource(Res.string.assignees_label),
            roomMembers = state.roomMembers,
            selectedParticipantIds = state.participants.keys,
            isLoading = state.isRoomMembersLoading,
            sheetState = participantSheetState,
            onDismissRequest = { viewModel.onEvent(TaskEditorEvent.OnParticipantPickerVisibilityChanged(false)) },
            onParticipantToggled = { id -> viewModel.onEvent(TaskEditorEvent.OnParticipantToggled(id)) }
        )
    }

    if (state.isPriorityPickerVisible) {
        PriorityPickerSheet(
            currentPriority = state.priority,
            sheetState = prioritySheetState,
            onDismissRequest = { viewModel.onEvent(TaskEditorEvent.OnPriorityPickerVisibilityChanged(false)) },
            onPrioritySelected = { priority -> viewModel.onEvent(TaskEditorEvent.OnPriorityChanged(priority)) }
        )
    }
}
