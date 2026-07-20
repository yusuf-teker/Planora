package com.yusufteker.pulse.feature.home.presentation.notes

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
import com.yusufteker.pulse.core.base.CollectEffect
import com.yusufteker.pulse.core.navigation.LocalNavigator
import com.yusufteker.pulse.core.navigation.Screen
import com.yusufteker.pulse.feature.home.presentation.components.EmptyStateComponent
import com.yusufteker.pulse.shared.api.TaskDto
import org.jetbrains.compose.resources.stringResource
import pulsy.core.generated.resources.Res
import pulsy.core.generated.resources.*

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
                        Text(
                            text = stringResource(Res.string.title_notes),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
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
                
                // Modern Search Bar
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = { viewModel.onEvent(NotesEvent.SearchQueryChanged(it)) },
                    placeholder = { Text("Notlarda ara...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Ara") },
                    trailingIcon = {
                        if (state.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onEvent(NotesEvent.SearchQueryChanged("")) }) {
                                Icon(Icons.Default.Close, contentDescription = "Temizle")
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
                shape = RoundedCornerShape(16.dp)
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
            // Folders List
            if (state.folders.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = state.selectedFolderId == null,
                            onClick = { viewModel.onEvent(NotesEvent.FolderSelected(null)) },
                            label = { Text("Tümü", fontWeight = FontWeight.Bold) },
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                    items(state.folders, key = { it.id }) { folder ->
                        FilterChip(
                            selected = state.selectedFolderId == folder.id,
                            onClick = { viewModel.onEvent(NotesEvent.FolderSelected(folder.id)) },
                            label = { Text(folder.title, fontWeight = FontWeight.Medium) },
                            leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            shape = RoundedCornerShape(16.dp)
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
                    Text("Sonuç bulunamadı.", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                    text = "SABİTLENENLER",
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
                                        text = "DİĞER NOTLAR",
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
                                    text = "SABİTLENENLER",
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
                                        text = "DİĞER NOTLAR",
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
    val cardColor = MaterialTheme.colorScheme.surfaceVariant
    val containerModifier = if (note.isPinned) {
        Modifier.background(
            brush = Brush.linearGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    MaterialTheme.colorScheme.surfaceVariant
                )
            )
        )
    } else {
        Modifier.background(cardColor)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { viewModel.onEvent(NotesEvent.NoteClicked(note.id)) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = containerModifier.fillMaxWidth().padding(16.dp)) {
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
                        contentDescription = "Sabitle",
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
                        text = "Sabitlendi",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }else{
                    Spacer(modifier = Modifier.weight(1f))
                }

                
                if (!note.isSynced) {
                    Icon(
                        imageVector = Icons.Default.CloudOff,
                        contentDescription = "Senkronize Edilmedi",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
