package com.yusufteker.planora.feature.home.presentation.global_search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yusufteker.planora.core.theme.PlanoraColors
import com.yusufteker.planora.core.ui.components.clearFocusOnTap
import com.yusufteker.planora.core.utils.formatShortDate
import com.yusufteker.planora.core.utils.formatTime
import com.yusufteker.planora.feature.home.presentation.components.EmptyStateComponent
import com.yusufteker.planora.feature.home.presentation.home.components.RoundCheckbox
import com.yusufteker.planora.shared.api.ItemDetails
import com.yusufteker.planora.shared.api.PlanRoomDto
import com.yusufteker.planora.shared.api.TaskDto
import com.yusufteker.planora.shared.api.TaskStatus
import org.jetbrains.compose.resources.stringResource
import planora.core.generated.resources.Res
import planora.core.generated.resources.back
import planora.core.generated.resources.search_empty_desc
import planora.core.generated.resources.search_empty_title
import planora.core.generated.resources.search_filter_all
import planora.core.generated.resources.search_filter_events
import planora.core.generated.resources.search_filter_notes
import planora.core.generated.resources.search_filter_rooms
import planora.core.generated.resources.search_filter_tasks
import planora.core.generated.resources.search_global_placeholder
import planora.core.generated.resources.search_no_results_found
import planora.core.generated.resources.search_results_count

/**
 * Genel arama ekranı.
 * Kullanıcının görev, etkinlik, not, plan odaları ve etiketlerini tek bir yerden
 * aramasını ve filtrelemesini sağlar.
 */
@Composable
fun GlobalSearchScreen(
    viewModel: GlobalSearchViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToTaskEditor: (String) -> Unit,
    onNavigateToEventEditor: (String) -> Unit,
    onNavigateToNoteEditor: (String) -> Unit,
    onNavigateToRoomDetail: (String) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                GlobalSearchEffect.NavigateBack -> onNavigateBack()
                is GlobalSearchEffect.NavigateToTaskEditor -> onNavigateToTaskEditor(effect.taskId)
                is GlobalSearchEffect.NavigateToEventEditor -> onNavigateToEventEditor(effect.eventId)
                is GlobalSearchEffect.NavigateToNoteEditor -> onNavigateToNoteEditor(effect.noteId)
                is GlobalSearchEffect.NavigateToRoomDetail -> onNavigateToRoomDetail(effect.roomId)
            }
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Üst Başlık & Geri Butonu
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.onEvent(GlobalSearchEvent.OnBackClicked) },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.back),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = stringResource(Res.string.search_empty_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Kategori Filtre Çipleri
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = state.selectedCategory == SearchCategory.ALL,
                            onClick = { viewModel.onEvent(GlobalSearchEvent.OnCategoryChanged(SearchCategory.ALL)) },
                            label = { Text(stringResource(Res.string.search_filter_all)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                selectedLabelColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                    item {
                        FilterChip(
                            selected = state.selectedCategory == SearchCategory.TASKS,
                            onClick = { viewModel.onEvent(GlobalSearchEvent.OnCategoryChanged(SearchCategory.TASKS)) },
                            label = { Text(stringResource(Res.string.search_filter_tasks)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PlanoraColors.Secondary.copy(alpha = 0.15f),
                                selectedLabelColor = PlanoraColors.Secondary
                            )
                        )
                    }
                    item {
                        FilterChip(
                            selected = state.selectedCategory == SearchCategory.EVENTS,
                            onClick = { viewModel.onEvent(GlobalSearchEvent.OnCategoryChanged(SearchCategory.EVENTS)) },
                            label = { Text(stringResource(Res.string.search_filter_events)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PlanoraColors.Primary.copy(alpha = 0.15f),
                                selectedLabelColor = PlanoraColors.Primary
                            )
                        )
                    }
                    item {
                        FilterChip(
                            selected = state.selectedCategory == SearchCategory.NOTES,
                            onClick = { viewModel.onEvent(GlobalSearchEvent.OnCategoryChanged(SearchCategory.NOTES)) },
                            label = { Text(stringResource(Res.string.search_filter_notes)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                                selectedLabelColor = MaterialTheme.colorScheme.tertiary
                            )
                        )
                    }
                    item {
                        FilterChip(
                            selected = state.selectedCategory == SearchCategory.ROOMS,
                            onClick = { viewModel.onEvent(GlobalSearchEvent.OnCategoryChanged(SearchCategory.ROOMS)) },
                            label = { Text(stringResource(Res.string.search_filter_rooms)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                                selectedLabelColor = MaterialTheme.colorScheme.secondary
                            )
                        )
                    }
                }

                // Etiket Filtreleri (Varsa)
                if (state.availableTags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(state.availableTags) { tag ->
                            val isSelected = state.selectedTag.equals(tag, ignoreCase = true)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                },
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { viewModel.onEvent(GlobalSearchEvent.OnTagSelected(tag)) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Sell,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "#$tag",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = state.query,
                        onValueChange = { viewModel.onEvent(GlobalSearchEvent.OnQueryChanged(it)) },
                        placeholder = {
                            Text(
                                text = stringResource(Res.string.search_global_placeholder),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = {
                            if (state.query.isNotEmpty()) {
                                IconButton(onClick = { viewModel.onEvent(GlobalSearchEvent.OnClearSearch) }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clearFocusOnTap()
                .padding(padding)
        ) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                !state.isSearchActive -> {
                    EmptyStateComponent(
                        icon = Icons.Default.Search,
                        title = stringResource(Res.string.search_empty_title),
                        description = stringResource(Res.string.search_empty_desc),
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                state.totalResultsCount == 0 -> {
                    EmptyStateComponent(
                        icon = Icons.Default.FindInPage,
                        title = stringResource(Res.string.search_no_results_found, state.query.ifBlank { state.selectedTag ?: "" }),
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 48.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Sonuç Sayacı
                        item(key = "results_counter") {
                            Text(
                                text = stringResource(Res.string.search_results_count, state.totalResultsCount),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }

                        // Görevler
                        if (state.selectedCategory == SearchCategory.ALL || state.selectedCategory == SearchCategory.TASKS) {
                            if (state.filteredTasks.isNotEmpty()) {
                                item(key = "header_tasks") {
                                    SearchSectionHeader(
                                        title = stringResource(Res.string.search_filter_tasks),
                                        count = state.filteredTasks.size,
                                        color = PlanoraColors.Secondary
                                    )
                                }
                                items(state.filteredTasks, key = { "task_${it.id}" }) { task ->
                                    SearchResultTaskCard(
                                        task = task,
                                        onToggleStatus = { viewModel.onEvent(GlobalSearchEvent.OnToggleTaskStatus(task)) },
                                        onClick = { viewModel.onEvent(GlobalSearchEvent.OnTaskClicked(task)) }
                                    )
                                }
                            }
                        }

                        // Etkinlikler
                        if (state.selectedCategory == SearchCategory.ALL || state.selectedCategory == SearchCategory.EVENTS) {
                            if (state.filteredEvents.isNotEmpty()) {
                                item(key = "header_events") {
                                    SearchSectionHeader(
                                        title = stringResource(Res.string.search_filter_events),
                                        count = state.filteredEvents.size,
                                        color = PlanoraColors.Primary
                                    )
                                }
                                items(state.filteredEvents, key = { "event_${it.id}" }) { eventItem ->
                                    SearchResultEventCard(
                                        event = eventItem,
                                        onClick = { viewModel.onEvent(GlobalSearchEvent.OnTaskClicked(eventItem)) }
                                    )
                                }
                            }
                        }

                        // Notlar
                        if (state.selectedCategory == SearchCategory.ALL || state.selectedCategory == SearchCategory.NOTES) {
                            if (state.filteredNotes.isNotEmpty()) {
                                item(key = "header_notes") {
                                    SearchSectionHeader(
                                        title = stringResource(Res.string.search_filter_notes),
                                        count = state.filteredNotes.size,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                                items(state.filteredNotes, key = { "note_${it.id}" }) { note ->
                                    SearchResultNoteCard(
                                        note = note,
                                        onClick = { viewModel.onEvent(GlobalSearchEvent.OnTaskClicked(note)) }
                                    )
                                }
                            }
                        }

                        // Plan Odaları
                        if (state.selectedCategory == SearchCategory.ALL || state.selectedCategory == SearchCategory.ROOMS) {
                            if (state.filteredRooms.isNotEmpty()) {
                                item(key = "header_rooms") {
                                    SearchSectionHeader(
                                        title = stringResource(Res.string.search_filter_rooms),
                                        count = state.filteredRooms.size,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                                items(state.filteredRooms, key = { "room_${it.id}" }) { room ->
                                    SearchResultRoomCard(
                                        room = room,
                                        onClick = { viewModel.onEvent(GlobalSearchEvent.OnRoomClicked(room.id)) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchSectionHeader(
    title: String,
    count: Int,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "($count)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SearchResultTaskCard(
    task: TaskDto,
    onToggleStatus: () -> Unit,
    onClick: () -> Unit
) {
    val isCompleted = task.status == TaskStatus.COMPLETED
    val taskDetails = task.specificDetails as? ItemDetails.Task
    val deadline = taskDetails?.deadline

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RoundCheckbox(
                isChecked = isCompleted,
                onCheckedChange = onToggleStatus
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    fontWeight = FontWeight.SemiBold,
                    color = if (isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (!task.description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = task.description ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (deadline != null || task.tags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (deadline != null) {
                            Text(
                                text = "${formatShortDate(deadline)} ${formatTime(deadline)}",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }

                        task.tags.take(2).forEach { tag ->
                            Text(
                                text = "#$tag",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultEventCard(
    event: TaskDto,
    onClick: () -> Unit
) {
    val eventDetails = event.specificDetails as? ItemDetails.Event
    val location = eventDetails?.location

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(PlanoraColors.Primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Event,
                    contentDescription = null,
                    tint = PlanoraColors.Primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))
                val timeText = if (event.endTime != null) {
                    "${formatShortDate(event.startTime)} ${formatTime(event.startTime)} - ${formatTime(event.endTime!!)}"
                } else {
                    "${formatShortDate(event.startTime)} ${formatTime(event.startTime)}"
                }
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.bodySmall,
                    color = PlanoraColors.Primary,
                    fontWeight = FontWeight.Medium
                )

                if (!location.isNullOrBlank() || event.tags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (!location.isNullOrBlank()) {
                            Text(
                                text = location,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        event.tags.take(2).forEach { tag ->
                            Text(
                                text = "#$tag",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = PlanoraColors.Primary,
                                modifier = Modifier
                                    .background(PlanoraColors.Primary.copy(alpha = 0.08f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultNoteCard(
    note: TaskDto,
    onClick: () -> Unit
) {
    val noteDetails = note.specificDetails as? ItemDetails.Note
    val content = noteDetails?.content.orEmpty().ifBlank { note.description.orEmpty() }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = note.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (content.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = content,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (note.tags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        note.tags.take(3).forEach { tag ->
                            Text(
                                text = "#$tag",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.08f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultRoomCard(
    room: PlanRoomDto,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = room.name.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = room.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Group,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${room.members.size} üye",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}
