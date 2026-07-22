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
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.foundation.lazy.LazyRow
import com.yusufteker.pulse.feature.home.presentation.plan_room_detail.components.FeedTimelineComponent
import com.yusufteker.pulse.feature.home.presentation.plan_room_detail.components.CalendarComponent
import com.yusufteker.pulse.core.ui.components.AvatarImage

import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PersonRemove
import coil3.compose.AsyncImage
import com.yusufteker.pulse.core.ui.components.getOptimizedCloudinaryUrl
import com.yusufteker.pulse.core.ui.components.rememberAppImagePickerLauncher
import kotlinx.datetime.toLocalDateTime

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
    
    val filteredTasks = remember(state.roomTasks, state.selectedFilter, state.taskSearchQuery, state.selectedMemberUserIdFilter) {
        state.roomTasks.filter { task ->
            val matchesType = when (state.selectedFilter) {
                RoomTaskFilter.ALL -> true
                RoomTaskFilter.TASKS -> task.type == com.yusufteker.pulse.shared.api.TaskType.TASK
                RoomTaskFilter.EVENTS -> task.type == com.yusufteker.pulse.shared.api.TaskType.EVENT
                RoomTaskFilter.NOTES -> task.type == com.yusufteker.pulse.shared.api.TaskType.NOTE
            }
            val matchesQuery = state.taskSearchQuery.isBlank() ||
                task.title.contains(state.taskSearchQuery, ignoreCase = true) ||
                (task.description?.contains(state.taskSearchQuery, ignoreCase = true) == true)
                
            val matchesMember = state.selectedMemberUserIdFilter == null ||
                task.creatorId == state.selectedMemberUserIdFilter ||
                task.participants.any { it.userId == state.selectedMemberUserIdFilter }
                
            matchesType && matchesQuery && matchesMember
        }
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, state.roomId) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                if (state.roomId.isNotBlank()) {
                    viewModel.onEvent(PlanRoomDetailEvent.LoadRoom(state.roomId))
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

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
    
    val roomImagePickerLauncher = rememberAppImagePickerLauncher(
        onResult = { bytes ->
            if (bytes != null) {
                viewModel.onEvent(PlanRoomDetailEvent.OnRoomImageSelected(bytes))
            }
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .clickable { roomImagePickerLauncher.launch() }
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!state.roomImageUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = getOptimizedCloudinaryUrl(state.roomImageUrl!!),
                                    contentDescription = "Oda Resmi",
                                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = stringResource(Res.string.action_change_room_image),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            if (state.isUploadingImage) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.4f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        strokeWidth = 2.dp
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = state.roomName.ifBlank { stringResource(Res.string.room_detail_default_title) },
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.onEvent(PlanRoomDetailEvent.OnBackClick) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
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
                            text = { Text(stringResource(Res.string.action_change_room_image)) },
                            onClick = {
                                showMenu = false
                                roomImagePickerLauncher.launch()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.CameraAlt, contentDescription = null)
                            }
                        )
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
                                text = { Text(stringResource(Res.string.title_delete_room_confirm), color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showMenu = false
                                    viewModel.onEvent(PlanRoomDetailEvent.OnDeleteRoomClick)
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                }
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.action_leave_room), color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showMenu = false
                                    viewModel.onEvent(PlanRoomDetailEvent.OnLeaveRoomClick)
                                },
                                leadingIcon = {
                                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = MaterialTheme.colorScheme.error)
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
                        modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 8.dp)
                    )
                    
                    if (state.isMembersLoading) {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(4) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                )
                            }
                        }
                    } else if (state.memberProfiles.isEmpty()) {
                        Text(
                            text = "Bu odada henüz üye bulunmuyor.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    } else {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            item {
                                FilterChip(
                                    selected = state.selectedMemberUserIdFilter == null,
                                    onClick = { viewModel.onEvent(PlanRoomDetailEvent.OnMemberFilterSelected(null)) },
                                    label = { Text(stringResource(Res.string.filter_member_all)) }
                                )
                            }
                            items(state.memberProfiles.values.toList(), key = { it.id }) { user ->
                                val isSelected = state.selectedMemberUserIdFilter == user.id
                                var showMemberMenu by remember { mutableStateOf(false) }

                                Box {
                                    AvatarImage(
                                        avatarId = user.avatarId,
                                        profileImageUrl = user.profileImageUrl,
                                        modifier = Modifier
                                            .size(44.dp)
                                            .border(
                                                width = if (isSelected) 3.dp else 1.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                                shape = CircleShape
                                            )
                                            .clickable { 
                                                if (state.isRoomCreator && user.id.toString() != state.myUserId) {
                                                    showMemberMenu = true
                                                } else {
                                                    viewModel.onEvent(PlanRoomDetailEvent.OnMemberFilterSelected(user.id))
                                                }
                                            }
                                    )

                                    DropdownMenu(
                                        expanded = showMemberMenu,
                                        onDismissRequest = { showMemberMenu = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text(user.name, fontWeight = FontWeight.Bold) },
                                            onClick = {
                                                showMemberMenu = false
                                                viewModel.onEvent(PlanRoomDetailEvent.OnMemberFilterSelected(user.id))
                                            }
                                        )
                                        if (state.isRoomCreator && user.id.toString() != state.myUserId) {
                                            DropdownMenuItem(
                                                text = { Text(stringResource(Res.string.action_remove_member), color = MaterialTheme.colorScheme.error) },
                                                onClick = {
                                                    showMemberMenu = false
                                                    viewModel.onEvent(PlanRoomDetailEvent.OnRemoveMemberClick(user))
                                                },
                                                leadingIcon = {
                                                    Icon(Icons.Default.PersonRemove, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // --- TAB BAR (FEED / CALENDAR) ---
                    SecondaryTabRow(
                        selectedTabIndex = state.selectedTab.ordinal,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    ) {
                        Tab(
                            selected = state.selectedTab == RoomDetailTab.FEED,
                            onClick = { viewModel.onEvent(PlanRoomDetailEvent.OnTabSelected(RoomDetailTab.FEED)) },
                            text = { Text(stringResource(Res.string.tab_feed)) }
                        )
                        Tab(
                            selected = state.selectedTab == RoomDetailTab.CALENDAR,
                            onClick = { viewModel.onEvent(PlanRoomDetailEvent.OnTabSelected(RoomDetailTab.CALENDAR)) },
                            text = { Text(stringResource(Res.string.tab_calendar)) }
                        )
                    }
                    
                    // --- FILTER CHIPS & SEARCH BAR ---
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = state.taskSearchQuery,
                            onValueChange = { viewModel.onEvent(PlanRoomDetailEvent.OnTaskSearchQueryChange(it)) },
                            placeholder = { Text(stringResource(Res.string.search_room_tasks_placeholder), style = MaterialTheme.typography.bodySmall) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                    
                    LazyRow(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = state.selectedFilter == RoomTaskFilter.ALL,
                                onClick = { viewModel.onEvent(PlanRoomDetailEvent.OnFilterSelected(RoomTaskFilter.ALL)) },
                                label = { Text(stringResource(Res.string.filter_all)) }
                            )
                        }
                        item {
                            FilterChip(
                                selected = state.selectedFilter == RoomTaskFilter.TASKS,
                                onClick = { viewModel.onEvent(PlanRoomDetailEvent.OnFilterSelected(RoomTaskFilter.TASKS)) },
                                label = { Text(stringResource(Res.string.filter_tasks)) }
                            )
                        }
                        item {
                            FilterChip(
                                selected = state.selectedFilter == RoomTaskFilter.EVENTS,
                                onClick = { viewModel.onEvent(PlanRoomDetailEvent.OnFilterSelected(RoomTaskFilter.EVENTS)) },
                                label = { Text(stringResource(Res.string.filter_events)) }
                            )
                        }
                        item {
                            FilterChip(
                                selected = state.selectedFilter == RoomTaskFilter.NOTES,
                                onClick = { viewModel.onEvent(PlanRoomDetailEvent.OnFilterSelected(RoomTaskFilter.NOTES)) },
                                label = { Text(stringResource(Res.string.filter_notes)) }
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // --- TAB CONTENT ---
                    if (state.selectedTab == RoomDetailTab.FEED) {
                        if (filteredTasks.isEmpty()) {
                            com.yusufteker.pulse.feature.home.presentation.components.EmptyStateComponent(
                                icon = Icons.Default.DateRange,
                                title = "Henüz bir görev veya etkinlik yok",
                                description = "Bu odada filtrenize uygun bir plan bulunmuyor.",
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                                FeedTimelineComponent(
                                    tasks = filteredTasks,
                                    memberProfiles = state.memberProfiles,
                                    onTaskClick = { task -> viewModel.onEvent(PlanRoomDetailEvent.OnTaskClick(task)) }
                                )
                            }
                        }
                    } else {
                        // CALENDAR TAB
                        val month = state.calendarCurrentMonth ?: kotlinx.datetime.LocalDate(2026, 7, 1)
                        Column(modifier = Modifier.fillMaxWidth().weight(1f)) {
                            CalendarComponent(
                                currentMonth = month,
                                selectedDate = state.calendarSelectedDate,
                                tasks = filteredTasks,
                                memberProfiles = state.memberProfiles,
                                onDateSelected = { date -> viewModel.onEvent(PlanRoomDetailEvent.OnCalendarDateSelected(date)) },
                                onPreviousMonth = { viewModel.onEvent(PlanRoomDetailEvent.OnCalendarPreviousMonth) },
                                onNextMonth = { viewModel.onEvent(PlanRoomDetailEvent.OnCalendarNextMonth) }
                            )
                            
                            val selectedDate = state.calendarSelectedDate
                            if (selectedDate != null) {
                                val dayTasks = filteredTasks.filter { task ->
                                    val taskDate = kotlinx.datetime.Instant.fromEpochMilliseconds(task.startTime)
                                        .toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).date
                                    taskDate == selectedDate
                                }
                                Text(
                                    text = "${selectedDate.dayOfMonth} ${selectedDate.month.name}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                                )
                                if (dayTasks.isEmpty()) {
                                    Text(
                                        text = stringResource(Res.string.empty_events_today),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                    )
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(dayTasks, key = { it.id }) { task ->
                                            val profile = state.memberProfiles[task.creatorId]
                                            val hasParticipants = task.participants.isNotEmpty()
                                            com.yusufteker.pulse.feature.home.presentation.home.components.TimelineTaskCard(
                                                task = task,
                                                showDate = false,
                                                sharedUserAvatar = if (hasParticipants) null else profile?.avatarId,
                                                sharedUserColor = null,
                                                sharedUserProfileImageUrl = if (hasParticipants) null else profile?.profileImageUrl,
                                                onClick = { viewModel.onEvent(PlanRoomDetailEvent.OnTaskClick(task)) }
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
                    
                    OutlinedButton(
                        onClick = { viewModel.onEvent(PlanRoomDetailEvent.OnCopyInviteLinkClick) },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(Res.string.action_copy_invite_link))
                    }
                    
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

    if (state.isRemoveMemberDialogOpen && state.memberToRemove != null) {
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(PlanRoomDetailEvent.OnDismissRemoveMemberDialog) },
            title = { Text(stringResource(Res.string.confirm_remove_member_title)) },
            text = { Text(stringResource(Res.string.confirm_remove_member_msg, state.memberToRemove?.name ?: "")) },
            confirmButton = {
                Button(
                    onClick = { viewModel.onEvent(PlanRoomDetailEvent.OnConfirmRemoveMember) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(Res.string.action_remove_member))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onEvent(PlanRoomDetailEvent.OnDismissRemoveMemberDialog) }) {
                    Text(stringResource(Res.string.cancel))
                }
            }
        )
    }
}


