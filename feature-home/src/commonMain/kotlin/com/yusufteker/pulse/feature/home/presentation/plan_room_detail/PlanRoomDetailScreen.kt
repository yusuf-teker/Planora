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
import com.yusufteker.pulse.feature.home.presentation.plan_room_detail.components.CalendarComponent
import com.yusufteker.pulse.feature.home.presentation.plan_room_detail.components.FeedTimelineComponent
import com.yusufteker.pulse.feature.home.presentation.plan_room_detail.components.TaskTimelineComponent
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanRoomDetailScreen(
    viewModel: PlanRoomDetailViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    
    LaunchedEffect(Unit) {
        val scope = this
        viewModel.effect.collect { effect ->
            when (effect) {
                is PlanRoomDetailEffect.NavigateBack -> onNavigateBack()
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
                            Icon(androidx.compose.material.icons.Icons.Default.MoreVert, contentDescription = "Daha Fazla")
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
        snackbarHost = { SnackbarHost(snackbarHostState) }
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
                    // View Mode Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        SingleChoiceSegmentedButtonRow {
                            SegmentedButton(
                                selected = state.viewMode == PlanRoomViewMode.CALENDAR,
                                onClick = { viewModel.onEvent(PlanRoomDetailEvent.OnViewModeChange(PlanRoomViewMode.CALENDAR)) },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                            ) { Text("Takvim") }
                            SegmentedButton(
                                selected = state.viewMode == PlanRoomViewMode.FEED,
                                onClick = { viewModel.onEvent(PlanRoomDetailEvent.OnViewModeChange(PlanRoomViewMode.FEED)) },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                            ) { Text("Akış") }
                        }
                    }

                    if (state.viewMode == PlanRoomViewMode.CALENDAR) {
                        CalendarComponent(
                            currentMonth = state.currentMonth,
                            selectedDate = state.selectedDate,
                            tasks = state.tasks,
                            memberProfiles = state.memberProfiles,
                            onDateSelected = { viewModel.onEvent(PlanRoomDetailEvent.OnDateSelected(it)) },
                            onPreviousMonth = { viewModel.onEvent(PlanRoomDetailEvent.OnPreviousMonth) },
                            onNextMonth = { viewModel.onEvent(PlanRoomDetailEvent.OnNextMonth) }
                        )
                        
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        
                        Box(modifier = Modifier.weight(1f)) {
                            TaskTimelineComponent(
                                selectedDate = state.selectedDate,
                                tasks = state.tasks,
                                memberProfiles = state.memberProfiles
                            )
                        }
                    } else {
                        Box(modifier = Modifier.weight(1f)) {
                            FeedTimelineComponent(
                                tasks = state.tasks,
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
