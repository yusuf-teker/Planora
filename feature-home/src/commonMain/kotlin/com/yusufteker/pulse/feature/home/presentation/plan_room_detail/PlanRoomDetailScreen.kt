package com.yusufteker.pulse.feature.home.presentation.plan_room_detail

import androidx.compose.foundation.background
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanRoomDetailScreen(
    viewModel: PlanRoomDetailViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToCreateTask: (String) -> Unit = {},
    onNavigateToCreateEvent: (String) -> Unit = {}
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
                    if (state.isRoomCreator) {
                        var showMenu by remember { mutableStateOf(false) }
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More options")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Odayı Düzenle") },
                                onClick = {
                                    showMenu = false
                                    viewModel.onEvent(PlanRoomDetailEvent.OnEditRoomClick)
                                },
                                leadingIcon = {
                                    Icon(androidx.compose.material.icons.Icons.Default.Edit, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Odayı Sil", color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showMenu = false
                                    viewModel.onEvent(PlanRoomDetailEvent.OnDeleteRoomClick)
                                },
                                leadingIcon = {
                                    Icon(androidx.compose.material.icons.Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
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
            Column(horizontalAlignment = Alignment.End) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = isFabExpanded,
                    enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInVertically { it / 2 },
                    exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.slideOutVertically { it / 2 }
                ) {
                    Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(bottom = 16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                            Text("Görev Ekle", modifier = Modifier.padding(end = 8.dp), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            androidx.compose.material3.SmallFloatingActionButton(
                                onClick = { 
                                    isFabExpanded = false
                                    viewModel.onEvent(PlanRoomDetailEvent.OnCreateTaskClick) 
                                },
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "Add Task")
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Etkinlik Ekle", modifier = Modifier.padding(end = 8.dp), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            androidx.compose.material3.SmallFloatingActionButton(
                                onClick = { 
                                    isFabExpanded = false
                                    viewModel.onEvent(PlanRoomDetailEvent.OnCreateEventClick) 
                                },
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Icon(Icons.Default.Event, contentDescription = "Add Event")
                            }
                        }
                    }
                }
                androidx.compose.material3.FloatingActionButton(
                    onClick = { isFabExpanded = !isFabExpanded },
                    containerColor = MaterialTheme.colorScheme.primary
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
                        androidx.compose.foundation.lazy.LazyRow(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.memberProfiles.values.toList()) { user ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = user.name.take(1).uppercase(),
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = user.name.split(" ").first(),
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1
                                    )
                                }
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
                        Box(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Henüz bir görev veya etkinlik yok",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                    } else {
                        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                            com.yusufteker.pulse.feature.home.presentation.plan_room_detail.components.FeedTimelineComponent(
                                tasks = state.roomTasks,
                                memberProfiles = state.memberProfiles
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
                        text = "Kişi Davet Et",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = { viewModel.onEvent(PlanRoomDetailEvent.OnSearchQueryChange(it)) },
                        placeholder = { Text("Takip ettiklerin arasında ara...") },
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
                            text = "Kişi bulunamadı.",
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 300.dp)
                        ) {
                            items(state.followingUsers) { user ->
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
                        Text("Kapat")
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
                        text = "Oda Adını Değiştir",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    OutlinedTextField(
                        value = state.renameRoomName,
                        onValueChange = { viewModel.onEvent(PlanRoomDetailEvent.OnRenameRoomNameChange(it)) },
                        label = { Text("Oda Adı") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { viewModel.onEvent(PlanRoomDetailEvent.OnDismissRenameDialog) }) {
                            Text("İptal")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { viewModel.onEvent(PlanRoomDetailEvent.OnRenameRoomSubmit) },
                            enabled = state.renameRoomName.isNotBlank() && !state.isLoading
                        ) {
                            Text("Kaydet")
                        }
                    }
                }
            }
        }
    }
}
