package com.yusufteker.planora.feature.home.presentation.plan_room_detail

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
import androidx.compose.material.icons.filled.Star
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
import com.yusufteker.planora.shared.api.UserProfileResponse
import androidx.compose.animation.AnimatedVisibility
import org.jetbrains.compose.resources.stringResource
import planora.core.generated.resources.Res
import planora.core.generated.resources.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.foundation.lazy.LazyRow
import com.yusufteker.planora.feature.home.presentation.plan_room_detail.components.FeedTimelineComponent
import com.yusufteker.planora.feature.home.presentation.plan_room_detail.components.CalendarComponent
import com.yusufteker.planora.core.ui.components.AvatarImage

import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Search
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import coil3.compose.AsyncImage
import com.yusufteker.planora.core.ui.components.getOptimizedCloudinaryUrl
import com.yusufteker.planora.core.ui.components.rememberAppImagePickerLauncher
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
    
    val filteredTasks = remember(state.roomTasks, state.selectedFilter, state.taskSearchQuery, state.selectedMemberUserIdsFilter) {
        state.roomTasks.filter { task ->
            val matchesType = when (state.selectedFilter) {
                RoomTaskFilter.ALL -> true
                RoomTaskFilter.TASKS -> task.type == com.yusufteker.planora.shared.api.TaskType.TASK
                RoomTaskFilter.EVENTS -> task.type == com.yusufteker.planora.shared.api.TaskType.EVENT
                RoomTaskFilter.NOTES -> task.type == com.yusufteker.planora.shared.api.TaskType.NOTE
            }
            val matchesQuery = state.taskSearchQuery.isBlank() ||
                task.title.contains(state.taskSearchQuery, ignoreCase = true) ||
                (task.description?.contains(state.taskSearchQuery, ignoreCase = true) == true)
                
            val matchesMember = state.selectedMemberUserIdsFilter.isEmpty() ||
                state.selectedMemberUserIdsFilter.contains(task.creatorId) ||
                task.participants.any { state.selectedMemberUserIdsFilter.contains(it.userId) }
                
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
                is PlanRoomDetailEffect.NavigateToTaskDetail -> onNavigateToTaskEditor(effect.taskId)
                is PlanRoomDetailEffect.NavigateToTaskEditor -> onNavigateToTaskEditor(effect.taskId)
                is PlanRoomDetailEffect.NavigateToEventDetail -> onNavigateToEventDetail(effect.eventId)
                is PlanRoomDetailEffect.NavigateToEventEditor -> onNavigateToCreateEvent(effect.eventId)
                is PlanRoomDetailEffect.NavigateToNoteEditor -> onNavigateToNoteEditor(effect.noteId)
                is PlanRoomDetailEffect.ShowToast -> {
                    scope.launch {
                        snackbarHostState.showSnackbar(effect.message)
                    }
                }
            }
        }
    }
    
    var pendingRoomCropImageBytes by remember { mutableStateOf<ByteArray?>(null) }

    val roomImagePickerLauncher = rememberAppImagePickerLauncher(
        onResult = { bytes ->
            if (bytes != null) {
                pendingRoomCropImageBytes = bytes
            }
        }
    )

    pendingRoomCropImageBytes?.let { rawBytes ->
        com.yusufteker.planora.core.ui.components.ImageCropDialog(
            imageBytes = rawBytes,
            isCircular = false,
            onImageCropped = { croppedBytes ->
                pendingRoomCropImageBytes = null
                viewModel.onEvent(PlanRoomDetailEvent.OnRoomImageSelected(croppedBytes))
            },
            onDismiss = {
                pendingRoomCropImageBytes = null
            }
        )
    }

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
                                    contentDescription = stringResource(Res.string.room_image_desc),
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
                        Icon(Icons.Default.PersonAdd, contentDescription = stringResource(Res.string.action_invite_person))
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
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
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
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
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
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = androidx.compose.foundation.shape.CircleShape
                ) {
                    Icon(if (isFabExpanded) Icons.Default.Close else Icons.Default.Add, contentDescription = stringResource(Res.string.action_expand))
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
                        text = stringResource(Res.string.room_members),
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
                            text = stringResource(Res.string.room_no_members_yet),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    } else {
                        val acceptedMembers = remember(state.memberProfiles, state.roomMembers) {
                            val acceptedUserIds = if (state.roomMembers.isEmpty()) {
                                state.memberProfiles.keys
                            } else {
                                state.roomMembers
                                    .filter { it.status == com.yusufteker.planora.shared.api.RoomMemberStatus.ACCEPTED }
                                    .map { it.userId }
                                    .toSet()
                            }

                            acceptedUserIds.mapNotNull { userId ->
                                state.memberProfiles[userId] ?: UserProfileResponse(
                                    id = userId,
                                    name = "Kullanıcı $userId",
                                    username = "",
                                    email = "",
                                    avatarId = "default"
                                )
                            }
                        }

                        LazyRow(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            item {
                                FilterChip(
                                    selected = state.selectedMemberUserIdsFilter.isEmpty(),
                                    onClick = { viewModel.onEvent(PlanRoomDetailEvent.OnMemberFilterSelected(null)) },
                                    label = { Text(stringResource(Res.string.filter_member_all)) }
                                )
                            }
                            items(acceptedMembers, key = { it.id }) { user ->
                                val isSelected = state.selectedMemberUserIdsFilter.contains(user.id)
                                val isCreator = user.id == state.creatorId

                                Box(
                                    modifier = Modifier.clickable { 
                                        viewModel.onEvent(PlanRoomDetailEvent.OnMemberFilterSelected(user.id))
                                    }
                                ) {
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
                                    )

                                    if (isCreator) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier
                                                .size(16.dp)
                                                .align(Alignment.TopEnd)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.Star,
                                                    contentDescription = stringResource(Res.string.role_creator),
                                                    modifier = Modifier.size(10.dp)
                                                )
                                            }
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
                            com.yusufteker.planora.feature.home.presentation.components.EmptyStateComponent(
                                icon = Icons.Default.DateRange,
                                title = stringResource(Res.string.empty_room_tasks_title),
                                description = stringResource(Res.string.empty_room_tasks_desc),
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
                                    text = "${selectedDate.dayOfMonth} ${getMonthNameRes(selectedDate.monthNumber)}",
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
                                            com.yusufteker.planora.feature.home.presentation.home.components.TimelineTaskCard(
                                                modifier = Modifier.padding(vertical = 4.dp),
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

    if (state.isEditRoomBottomSheetOpen) {
        val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
        EditRoomBottomSheet(
            state = state,
            onDismissRequest = { viewModel.onEvent(PlanRoomDetailEvent.OnDismissEditRoomBottomSheet) },
            onRenameNameChange = { viewModel.onEvent(PlanRoomDetailEvent.OnRenameRoomNameChange(it)) },
            onRenameSubmit = { viewModel.onEvent(PlanRoomDetailEvent.OnRenameRoomSubmit) },
            onRoomImageClick = { roomImagePickerLauncher.launch() },
            onSearchQueryChange = { viewModel.onEvent(PlanRoomDetailEvent.OnSearchQueryChange(it)) },
            onUserSelectToInvite = { viewModel.onEvent(PlanRoomDetailEvent.OnUserSelectToInvite(it)) },
            onCopyInviteLinkClick = {
                val inviteUrl = "https://planora.app/room/join?roomId=${state.roomId}"
                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(inviteUrl))
                viewModel.onEvent(PlanRoomDetailEvent.OnCopyInviteLinkClick)
            },
            onRemoveMemberClick = { viewModel.onEvent(PlanRoomDetailEvent.OnRemoveMemberClick(it)) },
            onDeleteRoomClick = {
                viewModel.onEvent(PlanRoomDetailEvent.OnDismissEditRoomBottomSheet)
                viewModel.onEvent(PlanRoomDetailEvent.OnDeleteRoomClick)
            },
            onLeaveRoomClick = {
                viewModel.onEvent(PlanRoomDetailEvent.OnDismissEditRoomBottomSheet)
                viewModel.onEvent(PlanRoomDetailEvent.OnLeaveRoomClick)
            }
        )
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

    if (state.isDeleteConfirmationOpen) {
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(PlanRoomDetailEvent.OnDismissDeleteDialog) },
            title = { Text(stringResource(Res.string.title_delete_room_confirm)) },
            text = { Text(stringResource(Res.string.msg_delete_room_confirm)) },
            confirmButton = {
                Button(
                    onClick = { viewModel.onEvent(PlanRoomDetailEvent.OnConfirmDeleteRoom) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(Res.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onEvent(PlanRoomDetailEvent.OnDismissDeleteDialog) }) {
                    Text(stringResource(Res.string.cancel))
                }
            }
        )
    }

    if (state.isLeaveConfirmationOpen) {
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(PlanRoomDetailEvent.OnDismissLeaveDialog) },
            title = { Text(stringResource(Res.string.title_leave_room_confirm)) },
            text = { Text(stringResource(Res.string.msg_leave_room_confirm)) },
            confirmButton = {
                Button(
                    onClick = { viewModel.onEvent(PlanRoomDetailEvent.OnConfirmLeaveRoom) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(Res.string.action_leave_room))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onEvent(PlanRoomDetailEvent.OnDismissLeaveDialog) }) {
                    Text(stringResource(Res.string.cancel))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditRoomBottomSheet(
    state: PlanRoomDetailState,
    onDismissRequest: () -> Unit,
    onRenameNameChange: (String) -> Unit,
    onRenameSubmit: () -> Unit,
    onRoomImageClick: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onUserSelectToInvite: (Int) -> Unit,
    onCopyInviteLinkClick: () -> Unit,
    onRemoveMemberClick: (UserProfileResponse) -> Unit,
    onDeleteRoomClick: () -> Unit,
    onLeaveRoomClick: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Title Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(Res.string.title_room_settings),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismissRequest) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(Res.string.action_close))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Room Image with Camera Icon Badge
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(84.dp)
            ) {
                AvatarImage(
                    avatarId = null,
                    profileImageUrl = state.roomImageUrl,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .border(2.dp, MaterialTheme.colorScheme.primaryContainer, CircleShape)
                )
                Surface(
                    onClick = onRoomImageClick,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .size(30.dp)
                        .align(Alignment.BottomEnd)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = stringResource(Res.string.action_change_room_image),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 1. Rename Room Section
            Text(
                text = stringResource(Res.string.room_name_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = state.renameRoomName,
                    onValueChange = onRenameNameChange,
                    placeholder = { Text(stringResource(Res.string.room_name_label)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = onRenameSubmit,
                    enabled = state.renameRoomName.isNotBlank() && state.renameRoomName != state.roomName && !state.isLoading,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(Res.string.save))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(16.dp))

            // 2. Room Members Section
            Text(
                text = stringResource(Res.string.room_members_title) + " (${state.memberProfiles.size})",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val allMembersList = remember(state.memberProfiles, state.roomMembers) {
                    if (state.roomMembers.isEmpty()) {
                        state.memberProfiles.values.map { user -> user to com.yusufteker.planora.shared.api.RoomMemberStatus.ACCEPTED }
                    } else {
                        state.roomMembers.mapNotNull { memberDto ->
                            if (memberDto.status == com.yusufteker.planora.shared.api.RoomMemberStatus.DECLINED) return@mapNotNull null
                            val profile = state.memberProfiles[memberDto.userId]
                            if (profile != null) profile to memberDto.status else null
                        }
                    }
                }

                allMembersList.forEach { (user, memberStatus) ->
                    val isCreator = user.id == state.creatorId
                    val isPending = memberStatus == com.yusufteker.planora.shared.api.RoomMemberStatus.PENDING
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AvatarImage(
                            avatarId = user.avatarId,
                            profileImageUrl = user.profileImageUrl,
                            modifier = Modifier.size(40.dp).clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = user.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (isCreator) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            text = stringResource(Res.string.role_creator),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                } else if (isPending) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = MaterialTheme.colorScheme.tertiaryContainer
                                    ) {
                                        Text(
                                            text = stringResource(Res.string.status_invited),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "@${user.username}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (state.isRoomCreator && !isCreator) {
                            IconButton(onClick = { onRemoveMemberClick(user) }) {
                                Icon(
                                    Icons.Default.PersonRemove,
                                    contentDescription = stringResource(Res.string.action_remove_member),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(16.dp))

            // 3. Add / Invite People Section
            Text(
                text = stringResource(Res.string.section_add_members),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onCopyInviteLinkClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(Res.string.action_copy_invite_link))
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text(stringResource(Res.string.search_following_placeholder)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (state.isFollowingLoading) {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (state.inviteError != null) {
                Text(
                    text = state.inviteError,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else if (state.followingUsers.isEmpty()) {
                Text(
                    text = stringResource(Res.string.error_no_users_found),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    state.followingUsers.forEach { user ->
                        val memberRecord = state.roomMembers.find { it.userId == user.id }
                        val isAccepted = memberRecord?.status == com.yusufteker.planora.shared.api.RoomMemberStatus.ACCEPTED
                        val isPending = memberRecord?.status == com.yusufteker.planora.shared.api.RoomMemberStatus.PENDING
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AvatarImage(
                                avatarId = user.avatarId,
                                profileImageUrl = user.profileImageUrl,
                                modifier = Modifier.size(36.dp).clip(CircleShape)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = user.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "@${user.username}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (isAccepted) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Text(
                                        text = stringResource(Res.string.status_member),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            } else if (isPending) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.tertiaryContainer
                                ) {
                                    Text(
                                        text = stringResource(Res.string.status_invited),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            } else {
                                val isInviting = state.invitingUserIds.contains(user.id)
                                FilledTonalButton(
                                    onClick = { onUserSelectToInvite(user.id) },
                                    enabled = !isInviting,
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    if (isInviting) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    } else {
                                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(stringResource(Res.string.action_invite_person), style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(16.dp))

            // Danger Zone
            if (state.isRoomCreator) {
                OutlinedButton(
                    onClick = onDeleteRoomClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(Res.string.action_delete_room))
                }
            } else {
                OutlinedButton(
                    onClick = onLeaveRoomClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(Res.string.action_leave_room))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun getMonthNameRes(month: Int): String {
    return when(month) {
        1 -> stringResource(Res.string.month_jan)
        2 -> stringResource(Res.string.month_feb)
        3 -> stringResource(Res.string.month_mar)
        4 -> stringResource(Res.string.month_apr)
        5 -> stringResource(Res.string.month_may)
        6 -> stringResource(Res.string.month_jun)
        7 -> stringResource(Res.string.month_jul)
        8 -> stringResource(Res.string.month_aug)
        9 -> stringResource(Res.string.month_sep)
        10 -> stringResource(Res.string.month_oct)
        11 -> stringResource(Res.string.month_nov)
        12 -> stringResource(Res.string.month_dec)
        else -> ""
    }
}


