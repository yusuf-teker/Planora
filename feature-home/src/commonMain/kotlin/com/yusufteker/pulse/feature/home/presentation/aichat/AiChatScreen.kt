package com.yusufteker.pulse.feature.home.presentation.aichat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatScreen(
    viewModel: AiChatViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val focusRequester = androidx.compose.runtime.remember { FocusRequester() }

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            viewModel.onEvent(AiChatEvent.ClearChat)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("AI Asistan")
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "BETA",
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().imePadding()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // ── Hızlı Eylem Çipleri ──
                    if (state.messages.size <= 1) {
                        SuggestedActionChips(
                            onChipClick = { template ->
                                viewModel.onEvent(AiChatEvent.InputTextChanged(template))
                            }
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = state.inputText,
                            onValueChange = { viewModel.onEvent(AiChatEvent.InputTextChanged(it)) },
                            placeholder = { Text("Görev veya not yazın...") },
                            modifier = Modifier.weight(1f).focusRequester(focusRequester),
                            shape = RoundedCornerShape(24.dp),
                            maxLines = 3,
                            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = { viewModel.onEvent(AiChatEvent.SendMessage) }),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            )
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = { viewModel.onEvent(AiChatEvent.SendMessage) },
                            enabled = state.inputText.isNotBlank() && !state.isLoading,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(
                                    if (state.inputText.isNotBlank()) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                        ) {
                            if (state.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Gönder",
                                    tint = if (state.inputText.isNotBlank())
                                        MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(state.messages, key = { it.id }) { message ->
                MessageBubble(message)
            }
        }
    }
}



// ═══════════════════════════════════════════════════════════
// Hızlı Eylem Çipleri
// ═══════════════════════════════════════════════════════════

@Composable
fun SuggestedActionChips(onChipClick: (String) -> Unit) {
    val suggestions = listOf(
        "📋 Görev ekle" to "yapmam lazım: ",
        "📅 Toplantı planla" to "yarın saat 'te toplantı ",
        "📝 Not al" to "not al: "
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        suggestions.forEach { (label, template) ->
            SuggestionChip(
                onClick = { onChipClick(template) },
                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════
// Mesaj Balonu
// ═══════════════════════════════════════════════════════════

@Composable
fun MessageBubble(message: AiChatMessage) {
    val alignment = if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart
    val backgroundColor = if (message.isUser) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.secondaryContainer
    val textColor = if (message.isUser) MaterialTheme.colorScheme.onPrimary
    else MaterialTheme.colorScheme.onSecondaryContainer
    val shape = if (message.isUser) {
        RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp)
    } else {
        RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(shape)
                .background(backgroundColor)
                .padding(12.dp)
        ) {
            if (message.isLoading) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = textColor,
                        strokeWidth = 2.dp
                    )
                    Text("Düşünüyor...", color = textColor, style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                Text(
                    text = message.text,
                    color = textColor,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
