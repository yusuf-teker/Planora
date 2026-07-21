package com.yusufteker.pulse.feature.home.presentation.plan_room_detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import com.yusufteker.pulse.shared.api.UserProfileResponse
import androidx.compose.animation.AnimatedVisibility
import org.jetbrains.compose.resources.stringResource
import pulsy.core.generated.resources.Res
import pulsy.core.generated.resources.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.lazy.LazyRow
import com.yusufteker.pulse.feature.home.presentation.plan_room_detail.components.FeedTimelineComponent
import com.yusufteker.pulse.core.ui.components.AvatarImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanRoomDetailScreen(
    viewModel: PlanRoomDetailViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToCreateTask: (String) -> Unit = {},
    onNavigateToCreateEvent: (String) -> Unit = {},
    onNavigateToTaskEditor: (String) -> Unit = {},
    onNavigateToEventDetail: (String) -> Unit = {},
    onNavigateToNoteEditor: (String) -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    
    LaunchedEffect(Unit) {
        val scope = this
        viewModel.effect.collect { effect ->
            when (effect) {
                is PlanRoomDetailEffect.NavigateBack -> onNavigateBack()
                is PlanRoomDetailEffect.NavigateToCreateTask -> onNavigateToCreateTask(effect.roomId)
                is PlanRoomDetailEffect.NavigateToCreateEvent -> onNavigateToCreateEvent(effect.roomId)
                is PlanRoomDetailEffect.NavigateToTaskEditor -> onNavigateToTaskEditor(effect.taskId)
                is PlanRoomDetailEffect.NavigateToEventDetail -> onNavigateToEventDetail(effect.eventId)
                is PlanRoomDetailEffect.NavigateToNoteEditor -> onNavigateToNoteEditor(effect.noteId)
                is PlanRoomDetailEffect.ShowToast -> {
                    scope.launch {
                        snackbarHostState.showSnackbar(effect.message)
                    }
                }
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.roomName.ifBlank { "Oda Detayı" }) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.onEvent(PlanRoomDetailEvent.OnBackClick) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.onEvent(PlanRoomDetailEvent.OnInviteUserClick) }) {
                        Icon(Icons.Default.PersonAdd, contentDescription = "Kişi Davet Et")
                    }
                    var showMenu by remember { mutableStateOf(false) }
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = stringResource(Res.string.action_more_options))
                    }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.action_edit_room)) },
                                onClick = {
                                    showMenu = false
                                    viewModel.onEvent(PlanRoomDetailEvent.OnEditRoomClick)
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Edit, contentDescription = null)
                                }
                            )
                        if (state.isRoomCreator) {
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.action_delete_room), color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showMenu = false
                                    viewModel.onEvent(PlanRoomDetailEvent.OnDeleteRoomClick)
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                }
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            var isFabExpanded by remember { mutableStateOf(false) }
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(bottom = 80.dp)) {
                AnimatedVisibility(
                    visible = isFabExpanded,
                    enter = fadeIn() + slideInVertically { it / 2 },
                    exit = fadeOut() + slideOutVertically { it / 2 }
                ) {
                    Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(bottom = 16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                            Text(stringResource(Res.string.action_add_task), modifier = Modifier.padding(end = 8.dp), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            SmallFloatingActionButton(
                                onClick = { 
                                    isFabExpanded = false
                                    viewModel.onEvent(PlanRoomDetailEvent.OnCreateTaskClick) 
                                },
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                shape = androidx.compose.foundation.shape.CircleShape
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = stringResource(Res.string.action_add_task))
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(Res.string.action_add_event), modifier = Modifier.padding(end = 8.dp), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            SmallFloatingActionButton(
                                onClick = { 
                                    isFabExpanded = false
                                    viewModel.onEvent(PlanRoomDetailEvent.OnCreateEventClick) 
                                },
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                shape = androidx.compose.foundation.shape.CircleShape
                            ) {
                                Icon(Icons.Default.Event, contentDescription = stringResource(Res.string.action_add_event))
                            }
                        }
                    }
                }
                FloatingActionButton(
                    onClick = { isFabExpanded = !isFabExpanded },
                    containerColor = MaterialTheme.colorScheme.primary,
                    shape = androidx.compose.foundation.shape.CircleShape
                ) {
                    Icon(if (isFabExpanded) Icons.Default.Close else Icons.Default.Add, contentDescription = "Expand")
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = "Oda Üyeleri",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                    )
                    
                    if (state.memberProfiles.isEmpty()) {
                        Text(
                            text = "Bu odada henüz üye bulunmuyor.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    } else {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy((-16).dp)
                        ) {
                            items(state.memberProfiles.values.toList(), key = { it.id }) { user ->
                                AvatarImage(
                                    avatarId = user.avatarId,
                                    profileImageUrl = user.profileImageUrl,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .border(2.dp, MaterialTheme.colorScheme.background, CircleShape)
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        text = "Görevler ve Etkinlikler",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                    )
                    
                    if (state.roomTasks.isEmpty()) {
                        com.yusufteker.pulse.feature.home.presentation.components.EmptyStateComponent(
                            icon = androidx.compose.material.icons.Icons.Default.DateRange,
                            title = "Henüz bir görev veya etkinlik yok",
                            description = "Bu odada henüz paylaşılan bir plan bulunmuyor.",
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                            FeedTimelineComponent(
                                tasks = state.roomTasks,
                                memberProfiles = state.memberProfiles,
                                onTaskClick = { task -> viewModel.onEvent(PlanRoomDetailEvent.OnTaskClick(task)) }
                            )
                        }
                    }
                }
            }
        }
    }
    
    if (state.isInviteDialogOpen) {
        Dialog(onDismissRequest = { viewModel.onEvent(PlanRoomDetailEvent.OnDismissInviteDialog) }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(Res.string.action_invite_person),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = { viewModel.onEvent(PlanRoomDetailEvent.OnSearchQueryChange(it)) },
                        placeholder = { Text(stringResource(Res.string.search_following_placeholder)) },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        singleLine = true
                    )
                    
                    if (state.isFollowingLoading) {
                        Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else if (state.inviteError != null) {
                        Text(
                            text = state.inviteError!!,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    } else if (state.followingUsers.isEmpty()) {
                        Text(
                            text = stringResource(Res.string.error_no_users_found),
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 300.dp)
                        ) {
                            items(state.followingUsers, key = { it.id }) { user ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.onEvent(PlanRoomDetailEvent.OnUserSelectToInvite(user.id)) }
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = user.name.take(1).uppercase(),
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.width(12.dp))
                                    
                                    Column {
                                        Text(
                                            text = user.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "@${user.username}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    TextButton(
                        onClick = { viewModel.onEvent(PlanRoomDetailEvent.OnDismissInviteDialog) },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(stringResource(Res.string.action_close))
                    }
                }
            }
        }
    }
    
    if (state.isRenameDialogOpen) {
        Dialog(onDismissRequest = { viewModel.onEvent(PlanRoomDetailEvent.OnDismissRenameDialog) }) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(Res.string.action_rename_room),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    OutlinedTextField(
                        value = state.renameRoomName,
                        onValueChange = { viewModel.onEvent(PlanRoomDetailEvent.OnRenameRoomNameChange(it)) },
                        label = { Text(stringResource(Res.string.room_name_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { viewModel.onEvent(PlanRoomDetailEvent.OnDismissRenameDialog) }) {
                            Text(stringResource(Res.string.cancel))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { viewModel.onEvent(PlanRoomDetailEvent.OnRenameRoomSubmit) },
                            enabled = state.renameRoomName.isNotBlank() && !state.isLoading
                        ) {
                            Text(stringResource(Res.string.save))
                        }
                    }
                }
            }
        }
    }
}
