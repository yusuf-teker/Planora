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
            Column(horizontalAlignment = Alignment.End) {
                AnimatedVisibility(visible = state.isFabExpanded) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 16.dp)
                        ) {
                            Surface(
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Text("Yeni Etkinlik", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                            }
                            SmallFloatingActionButton(onClick = { onEvent(PlanRoomsEvent.OnCreateEventClick) }) {
                                Icon(Icons.Default.DateRange, contentDescription = "Yeni Etkinlik")
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 16.dp)
                        ) {
                            Surface(
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Text(stringResource(Res.string.fab_new_task), modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                            }
                            SmallFloatingActionButton(onClick = { onEvent(PlanRoomsEvent.OnCreateTaskClick) }) {
                                Icon(Icons.Default.Edit, contentDescription = stringResource(Res.string.fab_new_task))
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Text(stringResource(Res.string.fab_new_room), modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                            }
                            SmallFloatingActionButton(onClick = { 
                                onEvent(PlanRoomsEvent.ToggleFab)
                                onEvent(PlanRoomsEvent.OnCreateRoomClick(true)) 
                            }) {
                                Icon(Icons.Default.Add, contentDescription = stringResource(Res.string.fab_new_room))
                            }
                        }
                    }
                }
                FloatingActionButton(onClick = { onEvent(PlanRoomsEvent.ToggleFab) }) {
                    Icon(
                        imageVector = if (state.isFabExpanded) Icons.Default.Close else Icons.Default.Add,
                        contentDescription = "Menü"
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (state.rooms.isEmpty() && !state.isLoading) {
                Text(
                    text = "Henüz bir plan odasında değilsiniz.\nYeni bir oda kurun veya davetleri kontrol edin.",
                    modifier = Modifier.align(Alignment.Center).padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
            title = { Text("Yeni Plan Odası") },
            text = {
                OutlinedTextField(
                    value = state.createRoomNameInput,
                    onValueChange = { onEvent(PlanRoomsEvent.OnCreateRoomNameChanged(it)) },
                    label = { Text("Oda İsmi") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = { onEvent(PlanRoomsEvent.SubmitCreateRoom) }) {
                    Text("Oluştur")
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(PlanRoomsEvent.OnCreateRoomClick(false)) }) {
                    Text("İptal")
                }
            }
        )
    }

    // 3. Invitations Dialog
    if (state.isInvitationsDialogVisible) {
        AlertDialog(
            onDismissRequest = { onEvent(PlanRoomsEvent.OnInvitationsClick(false)) },
            title = { Text("Gelen Davetler") },
            text = {
                if (state.pendingInvitations.isEmpty()) {
                    Text("Bekleyen davetiniz bulunmuyor.")
                } else {
                    LazyColumn {
                        items(state.pendingInvitations) { invite ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(invite.name, style = MaterialTheme.typography.bodyLarge)
                                Row {
                                    TextButton(onClick = { onEvent(PlanRoomsEvent.RespondToInvite(invite.id, false)) }) {
                                        Text("Reddet", color = MaterialTheme.colorScheme.error)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(onClick = { onEvent(PlanRoomsEvent.RespondToInvite(invite.id, true)) }) {
                                        Text("Kabul Et")
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { onEvent(PlanRoomsEvent.OnInvitationsClick(false)) }) {
                    Text("Kapat")
                }
            }
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
