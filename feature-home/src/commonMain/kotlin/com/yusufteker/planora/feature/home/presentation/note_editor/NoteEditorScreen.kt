package com.yusufteker.planora.feature.home.presentation.note_editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import planora.core.generated.resources.Res
import planora.core.generated.resources.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    viewModel: NoteEditorViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val contentFocusRequester = remember { FocusRequester() }
    val shareManager = org.koin.compose.koinInject<com.yusufteker.planora.core.share.ShareManager>()
    
    var showFolderDialog by remember { mutableStateOf(false) }
    var showFolderDropdown by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }

    if (showFolderDialog) {
        AlertDialog(
            onDismissRequest = { showFolderDialog = false },
            title = { Text(stringResource(Res.string.title_new_folder)) },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    label = { Text(stringResource(Res.string.folder_name_label)) },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newFolderName.isNotBlank()) {
                        viewModel.onEvent(NoteEditorEvent.OnCreateFolderClick(newFolderName))
                    }
                    showFolderDialog = false
                    newFolderName = ""
                }) {
                    Text(stringResource(Res.string.action_create))
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showFolderDialog = false 
                    newFolderName = ""
                }) {
                    Text(stringResource(Res.string.cancel))
                }
            }
        )
    }

    LaunchedEffect(state.id) {
        if (state.id == null && !state.isLoading) {
            contentFocusRequester.requestFocus()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.onEvent(NoteEditorEvent.OnDispose)
        }
    }

    LaunchedEffect(Unit) {
        val scope = this
        viewModel.effect.collect { effect ->
            when (effect) {
                is NoteEditorEffect.NavigateBack -> onNavigateBack()
                is NoteEditorEffect.ShowToast -> {
                    scope.launch {
                        snackbarHostState.showSnackbar(effect.message)
                    }
                }
                is NoteEditorEffect.ShareItem -> {
                    shareManager.shareText(effect.url, state.title.ifBlank { "Note" })
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { }, // Empty title for a clean look
                navigationIcon = {
                    IconButton(onClick = { viewModel.onEvent(NoteEditorEvent.OnBackClick) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showFolderDropdown = true }) {
                            Icon(
                                Icons.Default.Folder, 
                                contentDescription = "Klasör", 
                                tint = if (state.parentId != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        DropdownMenu(
                            expanded = showFolderDropdown,
                            onDismissRequest = { showFolderDropdown = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.folder_none)) },
                                onClick = { 
                                    viewModel.onEvent(NoteEditorEvent.OnFolderSelected(null))
                                    showFolderDropdown = false
                                }
                            )
                            state.folders.forEach { folder ->
                                DropdownMenuItem(
                                    text = { Text(folder.title) },
                                    trailingIcon = if (state.parentId == folder.id) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null,
                                    onClick = {
                                        viewModel.onEvent(NoteEditorEvent.OnFolderSelected(folder.id))
                                        showFolderDropdown = false
                                    }
                                )
                            }
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.action_add_folder)) },
                                leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp)) },
                                onClick = {
                                    showFolderDropdown = false
                                    showFolderDialog = true
                                }
                            )
                        }
                    }
                    if (state.id != null) {
                        IconButton(onClick = { viewModel.onEvent(NoteEditorEvent.OnShareClick) }) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    
                    IconButton(onClick = { viewModel.onEvent(NoteEditorEvent.OnSaveClick) }) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }

                    if (state.id != null) {
                        var showNoteMenu by remember { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = { showNoteMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            DropdownMenu(expanded = showNoteMenu, onDismissRequest = { showNoteMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(Res.string.action_delete), color = MaterialTheme.colorScheme.error) },
                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                    onClick = { 
                                        showNoteMenu = false
                                        viewModel.onEvent(NoteEditorEvent.OnDeleteClick) 
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (state.isLoading && state.id != null && state.title.isBlank()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {

                    // Title
                    TextField(
                        value = state.title,
                        onValueChange = { viewModel.onEvent(NoteEditorEvent.OnTitleChange(it)) },
                        placeholder = { 
                            Text(
                                "Başlık", 
                                style = MaterialTheme.typography.displaySmall.copy(
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                                    fontWeight = FontWeight.Bold
                                )
                            ) 
                        },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 0.dp),
                        textStyle = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        ),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = MaterialTheme.colorScheme.primary
                        ),
                        singleLine = false
                    )

                    // Date
                    if (state.dateText.isNotBlank()) {
                        Text(
                            text = state.dateText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                            modifier = Modifier.padding(bottom = 8.dp, start = 16.dp)
                        )
                    }
                    
                    // Content
                    TextField(
                        value = state.content,
                        onValueChange = { viewModel.onEvent(NoteEditorEvent.OnContentChange(it)) },
                        placeholder = { 
                            Text(
                                "Yazmaya başla", 
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                                )
                            ) 
                        },
                        modifier = Modifier.fillMaxWidth().focusRequester(contentFocusRequester),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f)
                        ),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Checklist
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    state.checklist.forEach { item ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = item.isDone,
                                onCheckedChange = { viewModel.onEvent(NoteEditorEvent.OnToggleChecklistItem(item.id)) }
                            )
                            
                            TextField(
                                value = item.title,
                                onValueChange = { viewModel.onEvent(NoteEditorEvent.OnUpdateChecklistItem(item.id, it)) },
                                modifier = Modifier.weight(1f),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                textStyle = if (item.isDone) {
                                    MaterialTheme.typography.bodyLarge.copy(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough, color = Color.Gray)
                                } else {
                                    MaterialTheme.typography.bodyLarge
                                }
                            )
                            
                            IconButton(onClick = { viewModel.onEvent(NoteEditorEvent.OnDeleteChecklistItem(item.id)) }) {
                                Icon(Icons.Default.Close, contentDescription = "Sil", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    
                    var newItemTitle by remember { mutableStateOf("") }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.Add, 
                            contentDescription = null, 
                            modifier = Modifier.padding(start = 12.dp, end = 12.dp), 
                            tint = MaterialTheme.colorScheme.primary
                        )
                        TextField(
                            value = newItemTitle,
                            onValueChange = { newItemTitle = it },
                            placeholder = { Text(stringResource(Res.string.add_checklist_item_placeholder)) },
                            modifier = Modifier.weight(1f),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            )
                        )
                        if (newItemTitle.isNotBlank()) {
                            IconButton(onClick = {
                                viewModel.onEvent(NoteEditorEvent.OnAddChecklistItem(newItemTitle))
                                newItemTitle = ""
                            }) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                }
                
                // AI Alanı - Profesyonel Alt Bar Tasarımı
                var aiQuery by remember { mutableStateOf("") }
                
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    // AI Suggestion Chips
                    if (!state.isAiLoading) {
                        val suggestions = listOf(
                            stringResource(Res.string.ai_chip_summarize),
                            stringResource(Res.string.ai_chip_fix_grammar),
                            stringResource(Res.string.ai_chip_bullet_points),
                            stringResource(Res.string.ai_chip_professional)
                        )
                        LazyRow(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(suggestions) { suggestion ->
                                SuggestionChip(
                                    onClick = { aiQuery = suggestion },
                                    label = { Text(suggestion, style = MaterialTheme.typography.labelSmall) },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    ),
                                    border = SuggestionChipDefaults.suggestionChipBorder(
                                        enabled = true,
                                        borderColor = Color.Transparent
                                    )
                                )
                            }
                        }
                    }

                    // Yükleme Göstergesi
                    if (state.isAiLoading) {
                        Row(
                            modifier = Modifier.padding(start = 16.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(Res.string.ai_note_editing),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        OutlinedTextField(
                            value = aiQuery,
                            onValueChange = { aiQuery = it },
                            placeholder = { Text(stringResource(Res.string.note_ai_placeholder)) },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        if (state.isAiLoading) {
                                            viewModel.onEvent(NoteEditorEvent.OnAiCancelClick)
                                        } else {
                                            if (aiQuery.isNotBlank()) {
                                                viewModel.onEvent(NoteEditorEvent.OnAiActionClick(aiQuery))
                                                aiQuery = ""
                                            }
                                        }
                                    },
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = if (state.isAiLoading) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = if (state.isAiLoading) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                ) {
                                    Icon(
                                        imageVector = if (state.isAiLoading) Icons.Default.Stop else Icons.AutoMirrored.Filled.Send, 
                                        contentDescription = null
                                    )
                                }
                            },
                            shape = RoundedCornerShape(24.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            maxLines = 3,
                            enabled = !state.isAiLoading
                        )
                    }
                }
            }
        }
    }

    if (state.aiPreviewTitle != null) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.onEvent(NoteEditorEvent.OnAiPreviewReject) },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = "AI",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(Res.string.ai_suggestion_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = state.aiPreviewTitle ?: "",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = state.aiPreviewContent ?: "",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { viewModel.onEvent(NoteEditorEvent.OnAiPreviewReject) }) {
                        Text(stringResource(Res.string.cancel))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { viewModel.onEvent(NoteEditorEvent.OnAiPreviewAccept) }) {
                        Text(stringResource(Res.string.action_apply))
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
