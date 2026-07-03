package com.yusufteker.pulse.feature.home.presentation.event_detail

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
import com.yusufteker.pulse.feature.home.presentation.components.ParticipantPickerSheet
import com.yusufteker.pulse.core.utils.formatShortDate
import com.yusufteker.pulse.core.utils.formatTime
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(
    viewModel: EventDetailViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()

    val startDateSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val endDateSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val repeatSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
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
                title = { Text(if (state.id == null) "Yeni Etkinlik" else "Düzenle", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.onEvent(EventDetailEvent.OnBackClick) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (state.id != null) {
                        IconButton(onClick = { viewModel.onEvent(EventDetailEvent.OnDeleteClick) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Sil", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    TextButton(onClick = { viewModel.onEvent(EventDetailEvent.OnSaveClick) }) {
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
                    .padding(horizontal = 16.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Title, Description, Location Section
                FormSection {
                    TextField(
                        value = state.title,
                        onValueChange = { viewModel.onEvent(EventDetailEvent.OnTitleChange(it)) },
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
                        value = state.location,
                        onValueChange = { viewModel.onEvent(EventDetailEvent.OnLocationChange(it)) },
                        placeholder = { Text("Konum (Opsiyonel)") },
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

                // Date, Time, and Repetition Section


                FormSection {
                    val startText = if (isTimeOnly) formatTime(state.startDateTimeMs) else "${formatShortDate(state.startDateTimeMs)} ${formatTime(state.startDateTimeMs)}"
                    val endText = if (isTimeOnly) formatTime(state.endDateTimeMs) else "${formatShortDate(state.endDateTimeMs)} ${formatTime(state.endDateTimeMs)}"

                    FormRow(
                        label = if (isTimeOnly) "Başlangıç Saati" else "Başlangıç",
                        value = startText,
                        onClick = { viewModel.onEvent(EventDetailEvent.OnStartPickerVisibilityChanged(true)) }
                    )
                    
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 16.dp))
                    
                    FormRow(
                        label = if (isTimeOnly) "Bitiş Saati" else "Bitiş",
                        value = endText,
                        onClick = { viewModel.onEvent(EventDetailEvent.OnEndPickerVisibilityChanged(true)) }
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
                        onClick = { viewModel.onEvent(EventDetailEvent.OnRepeatPickerVisibilityChanged(true)) }
                    )
                }

                // Context specific: Plan Room Participants
                if (state.planRoomId != null) {
                    FormSection {
                        FormRow(
                            label = "Katılımcılar",
                            value = "${state.participants.size} Kişi",
                            onClick = { viewModel.onEvent(EventDetailEvent.OnParticipantPickerVisibilityChanged(true)) }
                        )
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

    if (state.isParticipantPickerVisible) {
        ParticipantPickerSheet(
            title = "Katılımcılar",
            roomMembers = state.roomMembers,
            selectedParticipantIds = state.participants.keys,
            sheetState = participantSheetState,
            onDismissRequest = { viewModel.onEvent(EventDetailEvent.OnParticipantPickerVisibilityChanged(false)) },
            onParticipantToggled = { id -> viewModel.onEvent(EventDetailEvent.OnParticipantToggled(id)) }
        )
    }
}
