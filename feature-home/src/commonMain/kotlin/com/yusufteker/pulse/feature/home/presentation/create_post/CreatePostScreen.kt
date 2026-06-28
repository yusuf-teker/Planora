package com.yusufteker.pulse.feature.home.presentation.create_post

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Yeni Post Oluştur") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                actions = {
                    Button(
                        onClick = { viewModel.onEvent(CreatePostEvent.OnPost) },
                        enabled = state.content.isNotBlank() && !state.isSaving
                    ) {
                        Text("Paylaş")
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
            // Editör / Önizleme geçiş butonu
            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { viewModel.onEvent(CreatePostEvent.OnTogglePreview) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (state.showMarkdownPreview) "Editöre Dön" else "Önizlemeyi Gör (Markdown)")
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = { viewModel.onEvent(CreatePostEvent.OnSaveDraft) },
                    enabled = state.content.isNotBlank() && !state.isSaving,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Taslak Kaydet")
                }
            }

            if (state.showMarkdownPreview) {
                // Markdown Önizlemesi
                Markdown(
                    content = state.content,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 16.dp)
                )
            } else {
                // Metin Editörü
                OutlinedTextField(
                    value = state.content,
                    onValueChange = { viewModel.onEvent(CreatePostEvent.OnContentChanged(it)) },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 16.dp),
                    placeholder = { Text("Ne düşünüyorsun? (Markdown destekler: **kalın**, *italik*)") }
                )
            }
        }
    }
}
