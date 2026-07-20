package com.yusufteker.pulse.feature.home.presentation.event_detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
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
import com.yusufteker.pulse.feature.home.presentation.components.DateTimePickerSheet
import com.yusufteker.pulse.feature.home.presentation.components.FormRow
import com.yusufteker.pulse.feature.home.presentation.components.FormSection
import com.yusufteker.pulse.feature.home.presentation.components.RepeatPickerSheet
import com.yusufteker.pulse.feature.home.presentation.components.ReminderPickerSheet
import com.yusufteker.pulse.feature.home.presentation.components.ParticipantPickerSheet
import com.yusufteker.pulse.core.utils.formatShortDate
import com.yusufteker.pulse.core.utils.formatTime
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import pulsy.core.generated.resources.Res
import pulsy.core.generated.resources.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(
    viewModel: EventDetailViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToCreateTask: (String) -> Unit = {},
    onNavigateToCreateNote: (String) -> Unit = {},
    onNavigateToEditTask: (String) -> Unit = {},
    onNavigateToEditNote: (String) -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()
    val shareManager = org.koin.compose.koinInject<com.yusufteker.pulse.core.share.ShareManager>()

    val startDateSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val endDateSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val repeatSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val reminderSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val participantSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(Unit) {
        val scope = this
        viewModel.effect.collect { effect ->
            when (effect) {
                is EventDetailEffect.NavigateBack -> onNavigateBack()
                is EventDetailEffect.ShowToast -> {
                    scope.launch {
                        snackbarHostState.showSnackbar(effect.message)
                    }
                }
                is EventDetailEffect.ShareItem -> {
                    shareManager.shareText(effect.url, state.title.ifBlank { "Event" })
                }
            }
        }
    }

    val isTimeOnly = state.isRecurring && state.recurrenceRule != null && 
        (state.recurrenceRule is com.yusufteker.pulse.shared.api.RecurrenceRule.Daily || 
         state.recurrenceRule is com.yusufteker.pulse.shared.api.RecurrenceRule.Weekly || 
         (state.recurrenceRule as? com.yusufteker.pulse.shared.api.RecurrenceRule.Monthly)?.isLastDay == true)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.id == null) stringResource(Res.string.title_new_event) else stringResource(Res.string.action_edit), fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.onEvent(EventDetailEvent.OnBackClick) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.back))
                    }
                },
                actions = {
                    if (state.id != null) {
                        IconButton(onClick = { viewModel.onEvent(EventDetailEvent.OnShareClick) }) {
                            Icon(Icons.Default.Share, contentDescription = "Paylaş", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    
                    IconButton(onClick = { viewModel.onEvent(EventDetailEvent.OnSaveClick) }) {
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
                                        viewModel.onEvent(EventDetailEvent.OnDeleteClick)
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Title, Description, Location Section
                FormSection {
                    TextField(
                        value = state.title,
                        onValueChange = { viewModel.onEvent(EventDetailEvent.OnTitleChange(it)) },
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
                        value = state.location,
                        onValueChange = { viewModel.onEvent(EventDetailEvent.OnLocationChange(it)) },
                        placeholder = { Text(stringResource(Res.string.event_location_optional)) },
                        modifier = Modifier.fillMaxWidth(),
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
                        onValueChange = { viewModel.onEvent(EventDetailEvent.OnDescriptionChange(it)) },
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

                // Date, Time, and Repetition Section


                FormSection {
                    val startText = if (isTimeOnly) formatTime(state.startDateTimeMs) else "${formatShortDate(state.startDateTimeMs)} ${formatTime(state.startDateTimeMs)}"
                    val endText = if (isTimeOnly) formatTime(state.endDateTimeMs) else "${formatShortDate(state.endDateTimeMs)} ${formatTime(state.endDateTimeMs)}"

                    FormRow(
                        label = if (isTimeOnly) stringResource(Res.string.event_start_time_label) else stringResource(Res.string.event_start_label),
                        value = startText,
                        onClick = { viewModel.onEvent(EventDetailEvent.OnStartPickerVisibilityChanged(true)) }
                    )
                    
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 16.dp))
                    
                    FormRow(
                        label = if (isTimeOnly) stringResource(Res.string.event_end_time_label) else stringResource(Res.string.event_end_label),
                        value = endText,
                        onClick = { viewModel.onEvent(EventDetailEvent.OnEndPickerVisibilityChanged(true)) }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 16.dp))
                    
                    val repeatText = when (val rule = state.recurrenceRule) {
                        null -> stringResource(Res.string.repeat_none)
                        is com.yusufteker.pulse.shared.api.RecurrenceRule.Daily -> stringResource(Res.string.repeat_daily)
                        is com.yusufteker.pulse.shared.api.RecurrenceRule.Weekly -> stringResource(Res.string.repeat_weekly_pattern, rule.daysOfWeek.size.toString())
                        is com.yusufteker.pulse.shared.api.RecurrenceRule.Monthly -> if (rule.isLastDay) stringResource(Res.string.repeat_monthly_last_day) else stringResource(Res.string.repeat_monthly)
                        is com.yusufteker.pulse.shared.api.RecurrenceRule.Yearly -> stringResource(Res.string.repeat_yearly)
                    }

                    FormRow(
                        label = stringResource(Res.string.repeat_label),
                        value = repeatText,
                        onClick = { viewModel.onEvent(EventDetailEvent.OnRepeatPickerVisibilityChanged(true)) }
                    )
                    
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 16.dp))

                    FormRow(
                        label = stringResource(Res.string.reminders_label),
                        value = if (state.reminders.isNotEmpty()) stringResource(Res.string.reminders_selected_count_pattern, state.reminders.size.toString()) else stringResource(Res.string.repeat_none),
                        onClick = { viewModel.onEvent(EventDetailEvent.OnReminderPickerVisibilityChanged(true)) }
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
                            label = stringResource(Res.string.participants_label),
                            value = participantsText,
                            onClick = { 
                                if (state.planRoomId != null) {
                                    viewModel.onEvent(EventDetailEvent.OnParticipantPickerVisibilityChanged(true)) 
                                }
                            }
                        )
                    }
                }
                // Sub-items (Tasks and Notes)
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
                                text = "Alt Öğeler",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Row {
                                TextButton(onClick = { onNavigateToCreateTask(state.id!!) }) {
                                    Text("Görev Ekle")
                                }
                                TextButton(onClick = { onNavigateToCreateNote(state.id!!) }) {
                                    Text("Not Ekle")
                                }
                            }
                        }
                        
                        if (state.subItems.isEmpty()) {
                            Text(
                                text = "Henüz bir alt öğe eklenmemiş.",
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
                                            if (subItem.type == com.yusufteker.pulse.shared.api.TaskType.NOTE) {
                                                onNavigateToEditNote(subItem.id)
                                            } else {
                                                onNavigateToEditTask(subItem.id)
                                            }
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

    if (state.isStartPickerOpen) {
        DateTimePickerSheet(
            initialTimeMs = state.startDateTimeMs,
            timeOnly = isTimeOnly,
            sheetState = startDateSheetState,
            onDismissRequest = { viewModel.onEvent(EventDetailEvent.OnStartPickerVisibilityChanged(false)) },
            onDateTimeSelected = { ms -> viewModel.onEvent(EventDetailEvent.OnStartDateTimeSelected(ms)) }
        )
    }

    if (state.isEndPickerOpen) {
        DateTimePickerSheet(
            initialTimeMs = state.endDateTimeMs,
            timeOnly = isTimeOnly,
            sheetState = endDateSheetState,
            onDismissRequest = { viewModel.onEvent(EventDetailEvent.OnEndPickerVisibilityChanged(false)) },
            onDateTimeSelected = { ms -> viewModel.onEvent(EventDetailEvent.OnEndDateTimeSelected(ms)) }
        )
    }

    if (state.isRepeatPickerOpen) {
        RepeatPickerSheet(
            initialRule = state.recurrenceRule,
            startDateMs = state.startDateTimeMs,
            sheetState = repeatSheetState,
            onDismissRequest = { 
                viewModel.onEvent(EventDetailEvent.OnRepeatPickerVisibilityChanged(false))
            },
            onRuleSelected = { rule -> 
                viewModel.onEvent(EventDetailEvent.OnRecurrenceRuleChanged(rule))
            }
        )
    }

    if (state.isReminderPickerVisible) {
        ReminderPickerSheet(
            selectedReminders = state.reminders,
            sheetState = reminderSheetState,
            onDismissRequest = { viewModel.onEvent(EventDetailEvent.OnReminderPickerVisibilityChanged(false)) },
            onReminderToggled = { min -> viewModel.onEvent(EventDetailEvent.OnReminderToggled(min)) }
        )
    }

    if (state.isParticipantPickerVisible) {
        ParticipantPickerSheet(
            title = stringResource(Res.string.participants_label),
            roomMembers = state.roomMembers,
            selectedParticipantIds = state.participants.keys,
            sheetState = participantSheetState,
            onDismissRequest = { viewModel.onEvent(EventDetailEvent.OnParticipantPickerVisibilityChanged(false)) },
            onParticipantToggled = { id -> viewModel.onEvent(EventDetailEvent.OnParticipantToggled(id)) }
        )
    }
}
