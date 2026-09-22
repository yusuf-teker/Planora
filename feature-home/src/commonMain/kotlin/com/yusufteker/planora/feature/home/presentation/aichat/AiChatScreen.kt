package com.yusufteker.planora.feature.home.presentation.aichat

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import planora.core.generated.resources.Res
import planora.core.generated.resources.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatScreen(
    viewModel: AiChatViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToPremium: () -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val focusRequester = androidx.compose.runtime.remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

    var textFieldValue by remember { mutableStateOf(TextFieldValue(state.inputText)) }

    LaunchedEffect(state.inputText) {
        if (state.inputText != textFieldValue.text) {
            textFieldValue = TextFieldValue(
                text = state.inputText,
                selection = TextRange(state.inputText.length)
            )
        }
    }

    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            keyboardController?.hide()
            focusManager.clearFocus()
        }
    }

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
                        Text(stringResource(Res.string.title_ai_assistant))
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
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.back))
                    }
                },
                actions = {
                    Surface(
                        color = if (state.quota.isPremium) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .clickable { onNavigateToPremium() }
                    ) {
                        val badgeText = if (state.quota.isPremium) {
                            stringResource(Res.string.ai_quota_badge_premium, state.quota.dailyRemaining, state.quota.dailyLimit)
                        } else {
                            stringResource(
                                Res.string.ai_quota_badge_free,
                                state.quota.dailyRemaining,
                                state.quota.dailyLimit,
                                state.quota.weeklyRemaining,
                                state.quota.weeklyLimit
                            )
                        }
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (state.quota.isPremium) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
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
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (com.yusufteker.planora.shared.getPlatformName().contains("Android"))
                            Modifier.navigationBarsPadding()
                        else
                            Modifier
                    )
                    .imePadding()
            ) {
                Column(modifier = Modifier.fillMaxWidth().animateContentSize()) {
                    // ── Hızlı Eylem Çipleri ──
                    if (state.messages.size <= 1) {
                        SuggestedActionChips(
                            onChipClick = { template ->
                                viewModel.onEvent(AiChatEvent.InputTextChanged(template))
                            }
                        )
                    }

                    AnimatedVisibility(visible = state.isLoading) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 2.dp
                                )
                                Text(
                                    text = stringResource(Res.string.ai_thinking),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = { viewModel.onEvent(AiChatEvent.CancelGeneration) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "İptal",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = textFieldValue,
                            onValueChange = { newValue ->
                                textFieldValue = newValue
                                viewModel.onEvent(AiChatEvent.InputTextChanged(newValue.text))
                            },
                            placeholder = { Text(stringResource(Res.string.ai_chat_placeholder)) },
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
                                    contentDescription = stringResource(Res.string.action_send),
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
        stringResource(Res.string.ai_chip_add_task_label) to stringResource(Res.string.ai_chip_add_task_value),
        stringResource(Res.string.ai_chip_plan_meeting_label) to stringResource(Res.string.ai_chip_plan_meeting_value),
        stringResource(Res.string.ai_chip_create_room_label) to stringResource(Res.string.ai_chip_create_room_value),
        stringResource(Res.string.ai_chip_take_note_label) to stringResource(Res.string.ai_chip_take_note_value)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
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
                    Text(stringResource(Res.string.ai_thinking), color = textColor, style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                Text(
                    text = message.text.asString(),
                    color = textColor,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
