package com.yusufteker.planora.feature.home.presentation.notes

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yusufteker.planora.core.base.CollectEffect
import com.yusufteker.planora.core.navigation.LocalNavigator
import com.yusufteker.planora.core.navigation.Screen
import com.yusufteker.planora.feature.home.presentation.components.EmptyStateComponent
import com.yusufteker.planora.shared.api.TaskDto
import org.jetbrains.compose.resources.stringResource
import planora.core.generated.resources.Res
import planora.core.generated.resources.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    viewModel: NotesViewModel
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val rootNavigator = LocalNavigator.current

    viewModel.effect.CollectEffect { effect ->
        when (effect) {
            is NotesEffect.NavigateToTaskEditor -> {
                rootNavigator.navigate(Screen.NoteEditor(effect.noteId))
            }
            is NotesEffect.NavigateToCreateTask -> {
                rootNavigator.navigate(Screen.NoteEditor(noteId = null))
            }
        }
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
                TopAppBar(
                    title = {
                        com.yusufteker.planora.core.ui.components.GradientText(
                            text = stringResource(Res.string.title_notes),
                            colors = com.yusufteker.planora.core.theme.PlanoraColors.GradientPrimary,
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black)
                        )
                    },
                    actions = {
                        IconButton(onClick = { viewModel.onEvent(NotesEvent.ToggleViewMode) }) {
                            Icon(
                                imageVector = if (state.isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                                contentDescription = "Toggle View"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
                
                // Modern Glass Search Bar
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = { viewModel.onEvent(NotesEvent.SearchQueryChanged(it)) },
                    placeholder = { Text(stringResource(Res.string.notes_search_placeholder)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (state.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onEvent(NotesEvent.SearchQueryChanged("")) }) {
                                Icon(Icons.Default.Close, contentDescription = null)
                            }
                        }
                    },
                    shape = RoundedCornerShape(24.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .height(52.dp)
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.onEvent(NotesEvent.CreateNoteClicked) },
                containerColor = MaterialTheme.colorScheme.primary,
                shape = androidx.compose.foundation.shape.CircleShape
            ) {
                Icon(Icons.Filled.Add, stringResource(Res.string.action_create_note))
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Folders List with GlassChip
            if (state.folders.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        com.yusufteker.planora.core.ui.components.GlassChip(
                            text = stringResource(Res.string.filter_all),
                            isSelected = state.selectedFolderId == null,
                            onClick = { viewModel.onEvent(NotesEvent.FolderSelected(null)) }
                        )
                    }
                    items(state.folders, key = { it.id }) { folder ->
                        com.yusufteker.planora.core.ui.components.GlassChip(
                            text = folder.title,
                            isSelected = state.selectedFolderId == folder.id,
                            onClick = { viewModel.onEvent(NotesEvent.FolderSelected(folder.id)) },
                            icon = { Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                    }
                }
            }

            if (state.notes.isEmpty() && state.searchQuery.isEmpty()) {
                EmptyStateComponent(
                    icon = Icons.Default.Edit,
                    title = stringResource(Res.string.empty_notes_title),
                    description = stringResource(Res.string.empty_notes_desc),
                    modifier = Modifier.weight(1f)
                )
            } else if (state.notes.isEmpty() && state.searchQuery.isNotEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(Res.string.notes_no_results), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                if (state.isGridView) {
                    LazyVerticalStaggeredGrid(
                        columns = StaggeredGridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalItemSpacing = 8.dp,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (state.pinnedNotes.isNotEmpty()) {
                            item(span = androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan.FullLine) {
                                Text(
                                    text = stringResource(Res.string.notes_section_pinned),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp, start = 4.dp)
                                )
                            }
                            items(state.pinnedNotes, key = { it.id }) { note ->
                                NoteCard(note = note, viewModel = viewModel, isGrid = true)
                            }
                            
                            // Workaround for LazyVerticalStaggeredGrid bug: An odd number of items before a FullLine item causes the last item to be hidden.
                            if (state.pinnedNotes.size % 2 != 0) {
                                item { Spacer(modifier = Modifier) }
                            }

                            if (state.unpinnedNotes.isNotEmpty()) {
                                item(span = androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan.FullLine) {
                                    Text(
                                        text = stringResource(Res.string.notes_section_other),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp, start = 4.dp)
                                    )
                                }
                            }
                        }
                        
                        items(state.unpinnedNotes, key = { it.id }) { note ->
                            NoteCard(note = note, viewModel = viewModel, isGrid = true)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (state.pinnedNotes.isNotEmpty()) {
                            item {
                                Text(
                                    text = stringResource(Res.string.notes_section_pinned),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp, start = 4.dp)
                                )
                            }
                            items(state.pinnedNotes, key = { it.id }) { note ->
                                NoteCard(note = note, viewModel = viewModel, isGrid = false)
                            }
                            if (state.unpinnedNotes.isNotEmpty()) {
                                item {
                                    Text(
                                        text = stringResource(Res.string.notes_section_other),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp, start = 4.dp)
                                    )
                                }
                            }
                        }
                        
                        items(state.unpinnedNotes, key = { it.id }) { note ->
                            NoteCard(note = note, viewModel = viewModel, isGrid = false)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NoteCard(note: TaskDto, viewModel: NotesViewModel, isGrid: Boolean) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f

    val borderGradient = if (note.isPinned) {
        listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)
        )
    } else {
        listOf(
            if (isDark) Color.White.copy(alpha = 0.12f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
            if (isDark) Color.White.copy(alpha = 0.03f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
        )
    }

    com.yusufteker.planora.core.ui.components.GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        borderGradient = borderGradient,
        onClick = { viewModel.onEvent(NotesEvent.NoteClicked(note.id)) }
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    text = note.title.ifBlank { stringResource(Res.string.untitled_note_label) },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                IconButton(
                    onClick = { viewModel.onEvent(NotesEvent.TogglePin(note.id)) },
                    modifier = Modifier.size(24.dp).padding(start = 4.dp)
                ) {
                    Icon(
                        imageVector = if (note.isPinned) Icons.Filled.Star else Icons.Filled.StarBorder,
                        contentDescription = null,
                        tint = if (note.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            
            if (!note.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = note.description ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
                    maxLines = if (isGrid) 6 else 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (note.isPinned){
                    Text(
                        text = stringResource(Res.string.note_pinned),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                if (!note.isSynced) {
                    Icon(
                        imageVector = Icons.Default.CloudOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
