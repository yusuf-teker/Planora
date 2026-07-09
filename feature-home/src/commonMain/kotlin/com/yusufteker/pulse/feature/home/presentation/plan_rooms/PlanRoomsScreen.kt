package com.yusufteker.pulse.feature.home.presentation.plan_rooms

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
import pulsy.core.generated.resources.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yusufteker.pulse.shared.api.PlanRoomDto
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.animation.AnimatedVisibility


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
            FloatingActionButton(onClick = { onEvent(PlanRoomsEvent.OnCreateRoomClick(true)) }) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(Res.string.fab_new_room)
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (state.rooms.isEmpty() && !state.isLoading) {
                com.yusufteker.pulse.feature.home.presentation.components.EmptyStateComponent(
                    icon = androidx.compose.material.icons.Icons.Default.DateRange,
                    title = "Plan Odası Yok",
                    description = "Henüz bir plan odasında değilsiniz.\nYeni bir oda kurun veya davetleri kontrol edin.",
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.rooms) { room ->
                        RoomItem(
                            room = room,
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
                        items(state.pendingInvitations) { invite ->
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
    onClick: () -> Unit
) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onClick() },
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = room.name, 
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${room.members.size} üye",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "Detay",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}
