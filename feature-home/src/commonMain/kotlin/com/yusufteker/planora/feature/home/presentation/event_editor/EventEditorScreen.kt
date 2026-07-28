package com.yusufteker.planora.feature.home.presentation.event_editor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.yusufteker.planora.feature.home.presentation.components.RepeatPickerSheet
import com.yusufteker.planora.feature.home.presentation.components.ReminderPickerSheet
import com.yusufteker.planora.feature.home.presentation.components.ParticipantPickerSheet
import com.yusufteker.planora.core.utils.formatShortDate
import com.yusufteker.planora.core.utils.formatTime
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import planora.core.generated.resources.Res
import planora.core.generated.resources.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventEditorScreen(
    viewModel: EventEditorViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToCreateTask: (String) -> Unit = {},
    onNavigateToCreateNote: (String) -> Unit = {},
    onNavigateToEditTask: (String) -> Unit = {},
    onNavigateToEditNote: (String) -> Unit = {},
    onNavigateToPlanRoom: (String) -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()
    val shareManager = org.koin.compose.koinInject<com.yusufteker.planora.core.share.ShareManager>()

    val startDateSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val endDateSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val repeatSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val reminderSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val participantSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(Unit) {
        val scope = this
        viewModel.effect.collect { effect ->
            when (effect) {
                is EventEditorEffect.NavigateBack -> onNavigateBack()
                is EventEditorEffect.ShowToast -> {
                    scope.launch {
                        snackbarHostState.showSnackbar(effect.message)
                    }
                }
                is EventEditorEffect.ShareItem -> {
                    shareManager.shareText(effect.url, state.title.ifBlank { "Event" })
                }
            }
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
                            state.isCopyMode -> stringResource(Res.string.title_copy_event)
                            state.id == null -> stringResource(Res.string.title_new_event)
                            else -> stringResource(Res.string.title_event_editor)
                        }, 
                        fontWeight = FontWeight.SemiBold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.onEvent(EventEditorEvent.OnBackClick) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.back))
                    }
                },
                actions = {
                    if (state.id != null) {
                        IconButton(onClick = { viewModel.onEvent(EventEditorEvent.OnShareClick) }) {
                            Icon(Icons.Default.Share, contentDescription = "Paylaş", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    
                    IconButton(onClick = { viewModel.onEvent(EventEditorEvent.OnSaveClick) }) {
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
                                        viewModel.onEvent(EventEditorEvent.OnDeleteClick)
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
                // Shared Room Banner if event belongs to a plan room
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
                                .padding(horizontal = 16.dp, vertical = 12.dp),
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
                                        text = "Ortak Oda Etkinliği",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                                    )
                                    Text(
                                        text = state.planRoomName ?: "Bağlı Oda",
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

                // Title, Description, Location Section
                FormSection {
                    TextField(
                        value = state.title,
                        onValueChange = { viewModel.onEvent(EventEditorEvent.OnTitleChange(it)) },
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
                        onValueChange = { viewModel.onEvent(EventEditorEvent.OnLocationChange(it)) },
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
                        onValueChange = { viewModel.onEvent(EventEditorEvent.OnDescriptionChange(it)) },
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
                        onClick = { viewModel.onEvent(EventEditorEvent.OnStartPickerVisibilityChanged(true)) }
                    )
                    
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 16.dp))
                    
                    FormRow(
                        label = if (isTimeOnly) stringResource(Res.string.event_end_time_label) else stringResource(Res.string.event_end_label),
                        value = endText,
                        onClick = { viewModel.onEvent(EventEditorEvent.OnEndPickerVisibilityChanged(true)) }
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
                        onClick = { viewModel.onEvent(EventEditorEvent.OnRepeatPickerVisibilityChanged(true)) }
                    )
                }

                // Reminders Section
                FormSection {
                    val reminderSummary = if (state.reminders.isNotEmpty()) {
                        stringResource(Res.string.reminders_selected_count_pattern, state.reminders.size.toString())
                    } else {
                        stringResource(Res.string.repeat_none)
                    }

                    FormRow(
                        label = stringResource(Res.string.reminders_label),
                        value = reminderSummary,
                        onClick = { viewModel.onEvent(EventEditorEvent.OnReminderPickerVisibilityChanged(true)) }
                    )
                }

                // Participants Section (if inside plan room)
                if (state.planRoomId != null || state.participants.isNotEmpty()) {
                    FormSection {
                        val names = state.participants.values.joinToString(", ")
                        val participantSummary = if (names.isBlank()) stringResource(Res.string.option_not_selected) else names
                        FormRow(
                            label = stringResource(Res.string.assignees_label),
                            value = participantSummary,
                            onClick = { viewModel.onEvent(EventEditorEvent.OnParticipantPickerVisibilityChanged(true)) }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // Bottom Sheet Pickers
    if (state.isStartPickerOpen) {
        DateTimePickerSheet(
            initialTimeMs = state.startDateTimeMs,
            timeOnly = isTimeOnly,
            sheetState = startDateSheetState,
            onDismissRequest = { viewModel.onEvent(EventEditorEvent.OnStartPickerVisibilityChanged(false)) },
            onDateTimeSelected = { ms ->
                viewModel.onEvent(EventEditorEvent.OnStartDateTimeSelected(ms))
            }
        )
    }

    if (state.isEndPickerOpen) {
        DateTimePickerSheet(
            initialTimeMs = state.endDateTimeMs,
            timeOnly = isTimeOnly,
            sheetState = endDateSheetState,
            onDismissRequest = { viewModel.onEvent(EventEditorEvent.OnEndPickerVisibilityChanged(false)) },
            onDateTimeSelected = { ms ->
                viewModel.onEvent(EventEditorEvent.OnEndDateTimeSelected(ms))
            }
        )
    }

    if (state.isRepeatPickerOpen) {
        RepeatPickerSheet(
            initialRule = state.recurrenceRule,
            startDateMs = state.startDateTimeMs,
            sheetState = repeatSheetState,
            onDismissRequest = { viewModel.onEvent(EventEditorEvent.OnRepeatPickerVisibilityChanged(false)) },
            onRuleSelected = { rule ->
                viewModel.onEvent(EventEditorEvent.OnRecurrenceRuleChanged(rule))
            }
        )
    }

    if (state.isReminderPickerVisible) {
        ReminderPickerSheet(
            selectedReminders = state.reminders,
            sheetState = reminderSheetState,
            onDismissRequest = { viewModel.onEvent(EventEditorEvent.OnReminderPickerVisibilityChanged(false)) },
            onReminderToggled = { min ->
                viewModel.onEvent(EventEditorEvent.OnReminderToggled(min))
            }
        )
    }

    if (state.isParticipantPickerVisible) {
        ParticipantPickerSheet(
            title = stringResource(Res.string.assignees_label),
            roomMembers = state.roomMembers,
            selectedParticipantIds = state.participants.keys,
            sheetState = participantSheetState,
            onDismissRequest = { viewModel.onEvent(EventEditorEvent.OnParticipantPickerVisibilityChanged(false)) },
            onParticipantToggled = { userId ->
                viewModel.onEvent(EventEditorEvent.OnParticipantToggled(userId))
            }
        )
    }
}
