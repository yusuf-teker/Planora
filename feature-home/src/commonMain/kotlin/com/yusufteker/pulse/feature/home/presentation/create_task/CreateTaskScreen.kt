package com.yusufteker.pulse.feature.home.presentation.create_task

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import pulse.core.generated.resources.*
import com.yusufteker.pulse.shared.api.TaskType
import com.yusufteker.pulse.shared.api.TaskVisibility
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import com.yusufteker.pulse.core.ui.components.WheelTimePickerBottomSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTaskScreen(
    viewModel: CreateTaskViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    
    val startDatePickerState = rememberDatePickerState(initialSelectedDateMillis = state.startTime)
    val endDatePickerState = rememberDatePickerState(initialSelectedDateMillis = state.endTime)
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.onEvent(CreateTaskEvent.OnClearState)
        viewModel.effect.collect { effect ->
            when (effect) {
                is CreateTaskEffect.NavigateBack -> onNavigateBack()
                is CreateTaskEffect.ShowToast -> { }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.create_task_title)) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.onEvent(CreateTaskEvent.OnBackClick) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Geri")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // TabRow for TaskType
            val tabs = TaskType.entries
            val selectedTabIndex = tabs.indexOf(state.taskType)
            
            TabRow(selectedTabIndex = selectedTabIndex) {
                tabs.forEachIndexed { index, type ->
                    val title = when (type) {
                        TaskType.NOTE -> "Not"
                        TaskType.TASK -> "Görev"
                        TaskType.EVENT -> "Etkinlik"
                    }
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { viewModel.onEvent(CreateTaskEvent.OnTaskTypeChange(type)) },
                        text = { Text(title, style = MaterialTheme.typography.titleSmall) }
                    )
                }
            }
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Ortak Alanlar: Başlık ve Açıklama
                OutlinedTextField(
                    value = state.title,
                    onValueChange = { viewModel.onEvent(CreateTaskEvent.OnTitleChange(it)) },
                    label = { Text(stringResource(Res.string.task_title_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = state.description,
                    onValueChange = { viewModel.onEvent(CreateTaskEvent.OnDescriptionChange(it)) },
                    label = { Text(stringResource(Res.string.task_desc_label)) },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    shape = RoundedCornerShape(12.dp)
                )

                // Tipe Göre Form Alanları
                Crossfade(targetState = state.taskType) { type ->
                    when (type) {
                        TaskType.NOTE -> {
                            // Not için tarih/saat seçimine gerek yok, temiz bir görünüm.
                        }
                        TaskType.TASK -> {
                            TaskForm(
                                state = state,
                                onEvent = viewModel::onEvent,
                                onShowStartDatePicker = { showStartDatePicker = true },
                                onShowStartTimePicker = { showStartTimePicker = true }
                            )
                        }
                        TaskType.EVENT -> {
                            EventForm(
                                state = state,
                                onEvent = viewModel::onEvent,
                                onShowStartDatePicker = { showStartDatePicker = true },
                                onShowStartTimePicker = { showStartTimePicker = true },
                                onShowEndDatePicker = { showEndDatePicker = true },
                                onShowEndTimePicker = { showEndTimePicker = true }
                            )
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Görünürlük (Odalar)
                VisibilitySection(state, viewModel::onEvent)

                Spacer(modifier = Modifier.weight(1f, fill = false))
                Spacer(modifier = Modifier.height(16.dp))

                // Oluştur Butonu
                Button(
                    onClick = { viewModel.onEvent(CreateTaskEvent.Submit) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    enabled = !state.isLoading
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                    } else {
                        Text(stringResource(Res.string.action_create), style = MaterialTheme.typography.titleMedium)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
        
        // Diyaloglar (Pickers)
        if (showStartDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showStartDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        startDatePickerState.selectedDateMillis?.let {
                            viewModel.onEvent(CreateTaskEvent.OnStartTimeChange(it))
                        }
                        showStartDatePicker = false
                    }) { Text("Tamam") }
                },
                dismissButton = {
                    TextButton(onClick = { showStartDatePicker = false }) { Text(stringResource(Res.string.cancel)) }
                }
            ) { DatePicker(state = startDatePickerState) }
        }
        
        if (showEndDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showEndDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        endDatePickerState.selectedDateMillis?.let {
                            viewModel.onEvent(CreateTaskEvent.OnEndTimeChange(it))
                        }
                        showEndDatePicker = false
                    }) { Text("Tamam") }
                },
                dismissButton = {
                    TextButton(onClick = { showEndDatePicker = false }) { Text(stringResource(Res.string.cancel)) }
                }
            ) { DatePicker(state = endDatePickerState) }
        }

        if (showStartTimePicker) {
            WheelTimePickerBottomSheet(
                onDismissRequest = { showStartTimePicker = false },
                onTimeSelected = { h, m ->
                    viewModel.onEvent(CreateTaskEvent.OnStartTimeUpdate(h, m))
                    showStartTimePicker = false
                }
            )
        }

        if (showEndTimePicker) {
            WheelTimePickerBottomSheet(
                onDismissRequest = { showEndTimePicker = false },
                onTimeSelected = { h, m ->
                    viewModel.onEvent(CreateTaskEvent.OnEndTimeUpdate(h, m))
                    showEndTimePicker = false
                }
            )
        }
    }
}

@Composable
private fun TaskForm(
    state: CreateTaskState,
    onEvent: (CreateTaskEvent) -> Unit,
    onShowStartDatePicker: () -> Unit,
    onShowStartTimePicker: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Tekrarlanan Görev mi?", style = MaterialTheme.typography.titleMedium)
            Switch(
                checked = state.isRecurring,
                onCheckedChange = { onEvent(CreateTaskEvent.OnRecurringToggle(it)) }
            )
        }

        if (state.isRecurring) {
            Text(stringResource(Res.string.task_days), style = MaterialTheme.typography.bodyMedium)
            val days = listOf(
                1 to Res.string.task_day_mo, 2 to Res.string.task_day_tu, 3 to Res.string.task_day_we, 
                4 to Res.string.task_day_th, 5 to Res.string.task_day_fr, 6 to Res.string.task_day_sa, 7 to Res.string.task_day_su
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                days.forEach { (dayInt, resString) ->
                    val isSelected = state.selectedDaysOfWeek.contains(dayInt)
                    FilterChip(
                        selected = isSelected,
                        onClick = { onEvent(CreateTaskEvent.OnDayOfWeekToggle(dayInt)) },
                        label = { Text(stringResource(resString), style = MaterialTheme.typography.bodySmall) }
                    )
                }
            }
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Hatırlatma Saati", style = MaterialTheme.typography.bodyLarge)
                val startTimeText = state.startTime?.let { formatTimeOnly(it) } ?: "Saat Seç"
                OutlinedButton(
                    onClick = onShowStartTimePicker,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(startTimeText)
                }
            }
        } else {
            val startDateText = state.startTime?.let { formatDateOnly(it) } ?: "Tarih Seç"
            OutlinedButton(
                onClick = onShowStartDatePicker,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(startDateText)
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Tüm Gün", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = state.isAllDay,
                        onCheckedChange = { onEvent(CreateTaskEvent.OnAllDayToggle(it)) }
                    )
                }

                // Hatırlatma saati tüm gün olsa bile seçilebilir.
                val startTimeText = state.startTime?.let { formatTimeOnly(it) } ?: "Saat Seç"
                OutlinedButton(
                    onClick = onShowStartTimePicker,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(startTimeText)
                }
            }
        }
    }
}

@Composable
private fun EventForm(
    state: CreateTaskState,
    onEvent: (CreateTaskEvent) -> Unit,
    onShowStartDatePicker: () -> Unit,
    onShowStartTimePicker: () -> Unit,
    onShowEndDatePicker: () -> Unit,
    onShowEndTimePicker: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            Text("Tüm Gün", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.width(8.dp))
            Switch(
                checked = state.isAllDay,
                onCheckedChange = { onEvent(CreateTaskEvent.OnAllDayToggle(it)) }
            )
        }
        
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Başlangıç:", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(0.3f))
            
            Row(modifier = Modifier.weight(0.7f), horizontalArrangement = Arrangement.End) {
                val startDateText = state.startTime?.let { formatDateOnly(it) } ?: "Tarih"
                OutlinedButton(onClick = onShowStartDatePicker, shape = RoundedCornerShape(12.dp)) {
                    Text(startDateText)
                }

                if (!state.isAllDay) {
                    Spacer(modifier = Modifier.width(8.dp))
                    val startTimeText = state.startTime?.let { formatTimeOnly(it) } ?: "Saat"
                    OutlinedButton(onClick = onShowStartTimePicker, shape = RoundedCornerShape(12.dp)) {
                        Text(startTimeText)
                    }
                }
            }
        }
        
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Bitiş:", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(0.3f))
            
            Row(modifier = Modifier.weight(0.7f), horizontalArrangement = Arrangement.End) {
                val endDateText = state.endTime?.let { formatDateOnly(it) } ?: "Tarih"
                OutlinedButton(onClick = onShowEndDatePicker, shape = RoundedCornerShape(12.dp)) {
                    Text(endDateText)
                }

                if (!state.isAllDay) {
                    Spacer(modifier = Modifier.width(8.dp))
                    val endTimeText = state.endTime?.let { formatTimeOnly(it) } ?: "Saat"
                    OutlinedButton(onClick = onShowEndTimePicker, shape = RoundedCornerShape(12.dp)) {
                        Text(endTimeText)
                    }
                }
            }
        }
    }
}

@Composable
private fun VisibilitySection(
    state: CreateTaskState,
    onEvent: (CreateTaskEvent) -> Unit
) {
    Column {
        Text(stringResource(Res.string.task_visibility_label), style = MaterialTheme.typography.titleMedium)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = state.visibility == TaskVisibility.PRIVATE,
                onClick = { onEvent(CreateTaskEvent.OnVisibilityChange(TaskVisibility.PRIVATE)) }
            )
            Text(stringResource(Res.string.task_visibility_private), modifier = Modifier.clickable { onEvent(CreateTaskEvent.OnVisibilityChange(TaskVisibility.PRIVATE)) })
            
            Spacer(modifier = Modifier.width(16.dp))
            
            RadioButton(
                selected = state.visibility == TaskVisibility.ROOM_SHARED,
                onClick = { onEvent(CreateTaskEvent.OnVisibilityChange(TaskVisibility.ROOM_SHARED)) }
            )
            Text(stringResource(Res.string.task_visibility_shared), modifier = Modifier.clickable { onEvent(CreateTaskEvent.OnVisibilityChange(TaskVisibility.ROOM_SHARED)) })
        }

        AnimatedVisibility(
            visible = state.visibility == TaskVisibility.ROOM_SHARED,
            enter = expandVertically(animationSpec = tween(300)),
            exit = shrinkVertically(animationSpec = tween(300))
        ) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(Res.string.task_rooms_title), style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (state.availableRooms.isEmpty()) {
                        Text(stringResource(Res.string.task_rooms_empty), style = MaterialTheme.typography.bodyMedium)
                    } else {
                        state.availableRooms.forEach { room ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val isSelected = state.selectedRoomIds.contains(room.id)
                                        onEvent(CreateTaskEvent.OnRoomToggle(room.id, !isSelected))
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = state.selectedRoomIds.contains(room.id),
                                    onCheckedChange = { isChecked ->
                                        onEvent(CreateTaskEvent.OnRoomToggle(room.id, isChecked))
                                    }
                                )
                                Text(room.name, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatDateOnly(millis: Long): String {
    val localDateTime = Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.currentSystemDefault())
    return "${localDateTime.dayOfMonth}/${localDateTime.monthNumber}/${localDateTime.year}"
}

private fun formatTimeOnly(millis: Long): String {
    val localDateTime = Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.currentSystemDefault())
    return "${localDateTime.hour}:${localDateTime.minute.toString().padStart(2, '0')}"
}
