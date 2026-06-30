package com.yusufteker.pulse.feature.home.presentation.plan_rooms

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yusufteker.pulse.shared.api.PlanRoomDto
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanRoomsScreen(
    state: PlanRoomsState,
    effectFlow: kotlinx.coroutines.flow.Flow<PlanRoomsEffect>,
    onEvent: (PlanRoomsEvent) -> Unit,
    onNavigateToRoomDetail: (String) -> Unit,
    onShowSnackbar: (String) -> Unit
) {
    LaunchedEffect(effectFlow) {
        effectFlow.collectLatest { effect ->
            when (effect) {
                is PlanRoomsEffect.ShowToast -> onShowSnackbar(effect.message)
                is PlanRoomsEffect.NavigateToRoomDetail -> onNavigateToRoomDetail(effect.roomId)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Plan Odaları") },
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
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onEvent(PlanRoomsEvent.OnCreateRoomClick(true)) }) {
                Icon(Icons.Default.Add, contentDescription = "Yeni Oda")
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = room.name, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${room.members.size} üye",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
