package com.yusufteker.planora.feature.home.presentation.plan_rooms

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import org.jetbrains.compose.resources.stringResource
import planora.core.generated.resources.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yusufteker.planora.shared.api.PlanRoomDto
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import coil3.compose.AsyncImage
import com.yusufteker.planora.core.ui.components.AvatarImage
import com.yusufteker.planora.core.ui.components.getOptimizedCloudinaryUrl


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanRoomsScreen(
    state: PlanRoomsState,
    effectFlow: kotlinx.coroutines.flow.Flow<PlanRoomsEffect>,
    onEvent: (PlanRoomsEvent) -> Unit,
    onNavigateToRoomDetail: (String) -> Unit,
    onNavigateToCreateTask: () -> Unit,
    onNavigateToCreateEvent: () -> Unit,
    onShowSnackbar: (String) -> Unit
) {
    LaunchedEffect(effectFlow) {
        effectFlow.collectLatest { effect ->
            when (effect) {
                is PlanRoomsEffect.ShowToast -> onShowSnackbar(effect.message)
                is PlanRoomsEffect.NavigateToRoomDetail -> onNavigateToRoomDetail(effect.roomId)
                is PlanRoomsEffect.NavigateToCreateTask -> onNavigateToCreateTask()
                is PlanRoomsEffect.NavigateToCreateEvent -> onNavigateToCreateEvent()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Plan Odaları", 
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                    ) 
                },
                actions = {
                    BadgedBox(
                        badge = {
                            if (state.pendingInvitations.isNotEmpty()) {
                                Badge { Text(state.pendingInvitations.size.toString()) }
                            }
                        },
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .clickable { onEvent(PlanRoomsEvent.OnInvitationsClick(true)) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Davetler"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onEvent(PlanRoomsEvent.OnCreateRoomClick(true)) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = androidx.compose.foundation.shape.CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(Res.string.fab_new_room)
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (state.rooms.isEmpty() && !state.isLoading) {
                com.yusufteker.planora.feature.home.presentation.components.EmptyStateComponent(
                    icon = androidx.compose.material.icons.Icons.Default.DateRange,
                    title = "Plan Odası Yok",
                    description = "Henüz bir plan odasında değilsiniz.\nYeni bir oda kurun veya davetleri kontrol edin.",
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.rooms, key = { it.id }) { room ->
                        RoomItem(
                            room = room,
                            memberProfiles = state.memberProfiles,
                            onClick = { onEvent(PlanRoomsEvent.OnRoomClick(room.id)) }
                        )
                    }
                }
            }

            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }

    // --- DIALOGS ---

    // 1. Create Room Dialog
    if (state.isCreateRoomDialogVisible) {
        AlertDialog(
            onDismissRequest = { onEvent(PlanRoomsEvent.OnCreateRoomClick(false)) },
            title = { Text(stringResource(Res.string.title_new_plan_room)) },
            text = {
                OutlinedTextField(
                    value = state.createRoomNameInput,
                    onValueChange = { onEvent(PlanRoomsEvent.OnCreateRoomNameChanged(it)) },
                    label = { Text(stringResource(Res.string.room_name_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = { onEvent(PlanRoomsEvent.SubmitCreateRoom) }) {
                    Text(stringResource(Res.string.action_create))
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(PlanRoomsEvent.OnCreateRoomClick(false)) }) {
                    Text(stringResource(Res.string.cancel))
                }
            }
        )
    }

    // 2. Invitations Dialog
    if (state.isInvitationsDialogVisible) {
        AlertDialog(
            onDismissRequest = { onEvent(PlanRoomsEvent.OnInvitationsClick(false)) },
            title = { 
                Text(
                    stringResource(Res.string.title_incoming_invitations),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                ) 
            },
            text = {
                if (state.pendingInvitations.isEmpty()) {
                    Text(
                        stringResource(Res.string.empty_invitations_prompt),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.pendingInvitations, key = { it.id }) { invite ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        text = invite.name, 
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        OutlinedButton(
                                            onClick = { onEvent(PlanRoomsEvent.RespondToInvite(invite.id, false)) },
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                contentColor = MaterialTheme.colorScheme.error
                                            ),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                                        ) {
                                            Text(stringResource(Res.string.action_decline))
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Button(
                                            onClick = { onEvent(PlanRoomsEvent.RespondToInvite(invite.id, true)) },
                                        ) {
                                            Text(stringResource(Res.string.action_accept))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { onEvent(PlanRoomsEvent.OnInvitationsClick(false)) }) {
                    Text(stringResource(Res.string.action_close))
                }
            },
            shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
        )
    }
}

@Composable
fun RoomItem(
    room: PlanRoomDto,
    memberProfiles: Map<Int, com.yusufteker.planora.shared.api.UserProfileResponse>,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Room Image / Cover Avatar
            val imageUrl = room.imageUrl
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.tertiaryContainer
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (!imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = getOptimizedCloudinaryUrl(imageUrl),
                        contentDescription = room.name,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = room.name.take(1).uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Room Title & Overlapping Member Avatars
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = room.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(Res.string.onboarding_members_count, room.members.size),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (room.members.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(12.dp))

                        // Overlapping Member Avatars Row (up to 3 members)
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val maxVisible = 3
                            val displayedMembers = room.members.take(maxVisible)
                            val remainingCount = room.members.size - displayedMembers.size

                            displayedMembers.forEachIndexed { index, member ->
                                val profile = memberProfiles[member.userId]
                                Box(
                                    modifier = Modifier
                                        .offset(x = (-8 * index).dp)
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
                                        .background(MaterialTheme.colorScheme.secondaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AvatarImage(
                                        avatarId = profile?.avatarId ?: "avatar_1",
                                        profileImageUrl = profile?.profileImageUrl,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }

                            if (remainingCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .offset(x = (-8 * displayedMembers.size).dp)
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "+$remainingCount",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "Detay",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}
