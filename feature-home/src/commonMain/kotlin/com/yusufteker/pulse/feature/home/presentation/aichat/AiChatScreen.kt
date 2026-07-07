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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yusufteker.pulse.shared.ai.AiAvailabilityState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatScreen(
    viewModel: AiChatViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            viewModel.onEvent(AiChatEvent.ClearChat)
        }
    }

    // ── İndirme İstek Dialogu ──
    if (state.showDownloadPrompt) {
        DownloadModelPromptDialog(
            progress = state.downloadProgress,
            onDownload = { viewModel.onEvent(AiChatEvent.RequestModelDownload) },
            onDismiss = { viewModel.onEvent(AiChatEvent.DismissDownloadPrompt) }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("AI Asistan")
                        Spacer(modifier = Modifier.width(8.dp))
                        // ── Kullanılabilirlik göstergesi ──
                        AiAvailabilityDot(state.aiAvailability)
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
                            modifier = Modifier.weight(1f),
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
// AI Kullanılabilirlik Göstergesi
// ═══════════════════════════════════════════════════════════

@Composable
fun AiAvailabilityDot(state: AiAvailabilityState) {
    val color = when (state) {
        AiAvailabilityState.AVAILABLE -> Color(0xFF4CAF50)      // yeşil
        AiAvailabilityState.BASIC_ONLY -> Color(0xFFFFA726)     // turuncu
        AiAvailabilityState.DOWNLOADING -> Color(0xFF42A5F5)    // mavi
        AiAvailabilityState.PROMPT_DOWNLOAD -> Color(0xFFEF5350) // kırmızı
        AiAvailabilityState.ERROR -> Color(0xFFBDBDBD)           // gri
    }
    val label = when (state) {
        AiAvailabilityState.AVAILABLE -> "Cihaz Üstü AI Aktif"
        AiAvailabilityState.BASIC_ONLY -> "Temel AI Modu"
        AiAvailabilityState.DOWNLOADING -> "İndiriliyor..."
        AiAvailabilityState.PROMPT_DOWNLOAD -> "AI İndirilebilir"
        AiAvailabilityState.ERROR -> "AI Kullanılamıyor"
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ═══════════════════════════════════════════════════════════
// Model İndirme İstek Dialogu
// ═══════════════════════════════════════════════════════════

@Composable
fun DownloadModelPromptDialog(
    progress: Float?,
    onDownload: () -> Unit,
    onDismiss: () -> Unit
) {
    val isDownloading = progress != null && progress < 1.0f
    val isComplete = progress != null && progress >= 1.0f

    AlertDialog(
        onDismissRequest = { if (!isDownloading) onDismiss() },
        title = {
            Text(
                if (isComplete) "İndirme Tamamlandı!"
                else if (isDownloading) "İndiriliyor..."
                else "Yapay Zeka Modeli İndirilsin mi?"
            )
        },
        text = {
            Column {
                if (isComplete) {
                    Text("AI model başarıyla indirildi. Artık daha akıllı yanıtlar alabilirsin.")
                } else if (isDownloading) {
                    Text("Model indiriliyor, lütfen bekleyin...")
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { progress!! },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "%${(progress!! * 100).toInt()}",
                        style = MaterialTheme.typography.labelMedium
                    )
                } else {
                    Text(
                        "Daha iyi yanıtlar için cihazına Gemini Nano AI modeli indirilecek " +
                                "(~1.5 GB). İndirmeden de temel özellikleri kullanmaya devam edebilirsin."
                    )
                }
            }
        },
        confirmButton = {
            when {
                isComplete -> {
                    TextButton(onClick = onDismiss) { Text("Tamam") }
                }
                isDownloading -> {
                    // İndirme devam ederken buton gösterilmez
                }
                else -> {
                    TextButton(onClick = onDownload) { Text("İndir") }
                }
            }
        },
        dismissButton = {
            if (!isDownloading && !isComplete) {
                TextButton(onClick = onDismiss) { Text("Şimdi Değil") }
            }
        }
    )
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
