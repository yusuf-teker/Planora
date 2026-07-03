package com.yusufteker.pulse.feature.home.presentation.task_editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
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
import com.yusufteker.pulse.feature.home.presentation.components.FormSwitchRow
import com.yusufteker.pulse.feature.home.presentation.components.RepeatPickerSheet
import com.yusufteker.pulse.feature.home.presentation.components.ReminderPickerSheet
import com.yusufteker.pulse.feature.home.presentation.components.ParticipantPickerSheet
import com.yusufteker.pulse.core.utils.formatShortDate
import com.yusufteker.pulse.core.utils.formatTime
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditorScreen(
    viewModel: TaskEditorViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val dateSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val repeatSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val reminderSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val participantSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
                title = { Text(if (state.id == null) "Yeni Görev" else "Düzenle", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.onEvent(TaskEditorEvent.OnBackClick) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (state.id != null) {
                        IconButton(onClick = { viewModel.onEvent(TaskEditorEvent.DeleteClicked) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Sil", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    TextButton(onClick = { viewModel.onEvent(TaskEditorEvent.SaveClicked) }) {
                        Text("Kaydet", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
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
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Title and Description Section
                FormSection {
                    TextField(
                        value = state.title,
                        onValueChange = { viewModel.onEvent(TaskEditorEvent.TitleChanged(it)) },
                        placeholder = { Text("Başlık", style = MaterialTheme.typography.titleLarge) },
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
                        placeholder = { Text("Açıklama (Opsiyonel)") },
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
                                is com.yusufteker.pulse.shared.api.RecurrenceRule.Yearly -> "${formatShortDate(ms)} ${formatTime(ms)}"
                                is com.yusufteker.pulse.shared.api.RecurrenceRule.Monthly -> if (rule.isLastDay) formatTime(ms) else "${formatShortDate(ms)} ${formatTime(ms)}"
                                else -> formatTime(ms)
                            }
                        } else {
                            "${formatShortDate(ms)} ${formatTime(ms)}"
                        }
                    } ?: "Seçilmedi"

                    FormRow(
                        label = if (isTimeOnly) "Saat" else "Bitiş (Deadline)",
                        value = deadlineText,
                        onClick = { viewModel.onEvent(TaskEditorEvent.OnDeadlinePickerVisibilityChanged(true)) }
                    )
                    
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 16.dp))
                    
                    val repeatText = when (val rule = state.recurrenceRule) {
                        null -> "Yok"
                        is com.yusufteker.pulse.shared.api.RecurrenceRule.Daily -> "Her Gün"
                        is com.yusufteker.pulse.shared.api.RecurrenceRule.Weekly -> "Haftada ${rule.daysOfWeek.size} Gün"
                        is com.yusufteker.pulse.shared.api.RecurrenceRule.Monthly -> if (rule.isLastDay) "Her Ay (Son Gün)" else "Her Ay"
                        is com.yusufteker.pulse.shared.api.RecurrenceRule.Yearly -> "Her Yıl"
                    }

                    FormRow(
                        label = "Tekrar",
                        value = repeatText,
                        onClick = { viewModel.onEvent(TaskEditorEvent.OnRepeatPickerVisibilityChanged(true)) }
                    )
                }

                // Options Section
                FormSection {
                    if (state.id != null) {
                        FormSwitchRow(
                            label = "Tamamlandı",
                            checked = state.status == com.yusufteker.pulse.shared.api.TaskStatus.COMPLETED,
                            onCheckedChange = { viewModel.onEvent(TaskEditorEvent.StatusChanged(it)) }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 16.dp))
                    }

                    FormSwitchRow(
                        label = "Opsiyonel",
                        checked = state.isOptional,
                        onCheckedChange = { viewModel.onEvent(TaskEditorEvent.OnIsOptionalChanged(it)) }
                    )
                    
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 16.dp))

                    FormRow(
                        label = "Hatırlatıcılar",
                        value = if (state.reminders.isNotEmpty()) "${state.reminders.size} Seçildi" else "Yok",
                        onClick = { viewModel.onEvent(TaskEditorEvent.OnReminderPickerVisibilityChanged(true)) }
                    )
                }

                // Context specific: Plan Room Participants
                if (state.planRoomId != null) {
                    FormSection {
                        FormRow(
                            label = "Sorumlular",
                            value = "${state.participants.size} Kişi",
                            onClick = { viewModel.onEvent(TaskEditorEvent.OnParticipantPickerVisibilityChanged(true)) }
                        )
                    }
                }
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
            startDateMs = state.deadlineDateMs ?: com.yusufteker.pulse.core.utils.getCurrentTimeMs(),
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
            title = "Sorumlular",
            roomMembers = state.roomMembers,
            selectedParticipantIds = state.participants.keys,
            sheetState = participantSheetState,
            onDismissRequest = { viewModel.onEvent(TaskEditorEvent.OnParticipantPickerVisibilityChanged(false)) },
            onParticipantToggled = { id -> viewModel.onEvent(TaskEditorEvent.OnParticipantToggled(id)) }
        )
    }
}
