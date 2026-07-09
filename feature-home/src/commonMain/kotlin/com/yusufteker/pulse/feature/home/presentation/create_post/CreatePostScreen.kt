package com.yusufteker.pulse.feature.home.presentation.create_post

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mikepenz.markdown.m3.Markdown
import com.yusufteker.pulse.core.base.CollectEffect
import org.koin.compose.viewmodel.koinViewModel

import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.launch
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditor
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import com.yusufteker.pulse.core.navigation.LocalNavigator
import com.yusufteker.pulse.core.navigation.Screen
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import com.yusufteker.pulse.feature.home.domain.model.Topic
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.yusufteker.pulse.feature.home.presentation.util.color
import com.yusufteker.pulse.feature.home.presentation.util.titleRes
import org.jetbrains.compose.resources.stringResource
import pulsy.core.generated.resources.Res
import pulsy.core.generated.resources.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreatePostScreen(
    onNavigateBack: () -> Unit,
    viewModel: CreatePostViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    viewModel.effect.CollectEffect { effect ->
        when (effect) {
            is CreatePostEffect.NavigateBack -> onNavigateBack()
            is CreatePostEffect.ShowError -> {} // handled globally now via snackbar manager
        }
    }

    val rootNavigator = LocalNavigator.current
    val richTextState = rememberRichTextState()

    LaunchedEffect(state.isEditing, state.content) {
        if (state.isEditing && richTextState.toMarkdown().isBlank() && state.content.isNotBlank()) {
            richTextState.setMarkdown(state.content)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditing) stringResource(Res.string.title_edit_post) else stringResource(Res.string.title_create_post)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.back))
                    }
                },
                actions = {
                    Button(
                        onClick = { viewModel.onEvent(CreatePostEvent.OnPost(richTextState.toMarkdown())) },
                        enabled = richTextState.annotatedString.text.isNotBlank() && !state.isSaving
                    ) {
                        Text(stringResource(Res.string.action_share))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Butonlar Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { rootNavigator.navigate(Screen.PendingPosts) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(Res.string.title_my_drafts))
                }
                OutlinedButton(
                    onClick = { viewModel.onEvent(CreatePostEvent.OnSaveDraft(richTextState.toMarkdown())) },
                    enabled = richTextState.annotatedString.text.isNotBlank() && !state.isSaving,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(Res.string.action_save_draft))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Araç Çubuğu (Toolbar)
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    IconButton(onClick = { richTextState.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold)) }) {
                        Icon(imageVector = Icons.Default.FormatBold, contentDescription = stringResource(Res.string.action_bold))
                    }
                }
                item {
                    IconButton(onClick = { richTextState.toggleSpanStyle(SpanStyle(fontStyle = FontStyle.Italic)) }) {
                        Icon(imageVector = Icons.Default.FormatItalic, contentDescription = stringResource(Res.string.action_italic))
                    }
                }
                item {
                    IconButton(onClick = { richTextState.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) }) {
                        Icon(imageVector = Icons.Default.FormatStrikethrough, contentDescription = stringResource(Res.string.action_strikethrough))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            var isTopicsExpanded by remember { mutableStateOf(false) }

            // Topic Selector (Konu Seçimi)
            Text(
                text = stringResource(Res.string.select_topic_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            val topics = Topic.entries
            val displayTopics = if (isTopicsExpanded) topics else topics.take(6)
            
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                displayTopics.forEach { topic ->
                    val isSelected = state.selectedTopic == topic.id
                    val chipColor = topic.color
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.onEvent(CreatePostEvent.OnTopicSelected(topic.id)) },
                        label = { Text(stringResource(topic.titleRes)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = chipColor.copy(alpha = 0.2f),
                            selectedLabelColor = chipColor
                        )
                    )
                }
                
                if (!isTopicsExpanded && topics.size > 6) {
                    FilterChip(
                        selected = false,
                        onClick = { isTopicsExpanded = true },
                        label = { Text(stringResource(Res.string.action_show_more)) }
                    )
                } else if (isTopicsExpanded) {
                    FilterChip(
                        selected = false,
                        onClick = { isTopicsExpanded = false },
                        label = { Text(stringResource(Res.string.action_hide)) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Metin Editörü
            RichTextEditor(
                state = richTextState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 8.dp),
                placeholder = { Text(stringResource(Res.string.create_post_placeholder)) }
            )
        }
    }
}
