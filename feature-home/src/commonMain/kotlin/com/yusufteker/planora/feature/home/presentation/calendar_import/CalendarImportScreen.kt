package com.yusufteker.planora.feature.home.presentation.calendar_import

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yusufteker.planora.core.base.CollectEffect
import com.yusufteker.planora.core.calendar.CalendarImportDateRange
import com.yusufteker.planora.core.calendar.rememberCalendarPermissionLauncher
import com.yusufteker.planora.feature.home.presentation.calendar_import.components.CalendarImportEditDialog
import com.yusufteker.planora.feature.home.presentation.calendar_import.components.CalendarImportItemCard
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import planora.core.generated.resources.Res
import planora.core.generated.resources.back
import planora.core.generated.resources.calendar_import_button
import planora.core.generated.resources.calendar_import_deselect_all
import planora.core.generated.resources.calendar_import_empty_desc
import planora.core.generated.resources.calendar_import_empty_title
import planora.core.generated.resources.calendar_import_filter_all
import planora.core.generated.resources.calendar_import_grant_permission
import planora.core.generated.resources.calendar_import_in_progress
import planora.core.generated.resources.calendar_import_loading
import planora.core.generated.resources.calendar_import_permission_desc
import planora.core.generated.resources.calendar_import_permission_title
import planora.core.generated.resources.calendar_import_select_all
import planora.core.generated.resources.calendar_import_selected_count
import planora.core.generated.resources.calendar_import_title

/**
 * Dedicated Review and Import screen for migrating events from native system calendars
 * (Google Calendar on Android/iOS, Apple Calendar on iOS) into Planora.
 *
 * Provides a clean overview of detected events with selection checkboxes,
 * instant type switching between Event and Task, detailed event editing, and batch importing.
 *
 * @param viewModel Instance of [CalendarImportViewModel].
 * @param onNavigateBack Callback invoked when navigating back.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarImportScreen(
    viewModel: CalendarImportViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val permissionLauncher = rememberCalendarPermissionLauncher { isGranted ->
        viewModel.onEvent(CalendarImportUiEvent.PermissionResult(isGranted))
    }

    viewModel.effect.CollectEffect { effect ->
        when (effect) {
            is CalendarImportEffect.NavigateBack -> onNavigateBack()
            is CalendarImportEffect.ShowSnackbar -> {
                coroutineScope.launch {
                    val msg = if (effect.count != null) {
                        getString(effect.messageRes, effect.count)
                    } else {
                        getString(effect.messageRes)
                    }
                    snackbarHostState.showSnackbar(msg)
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.calendar_import_title),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.onEvent(CalendarImportUiEvent.NavigateBack) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.back),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    if (state.hasPermission && state.filteredEvents.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                viewModel.onEvent(CalendarImportUiEvent.ToggleSelectAll(!state.isAllSelected))
                            }
                        ) {
                            Text(
                                text = if (state.isAllSelected) stringResource(Res.string.calendar_import_deselect_all)
                                else stringResource(Res.string.calendar_import_select_all),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            AnimatedVisibility(
                visible = state.hasPermission && state.filteredEvents.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = stringResource(
                                    Res.string.calendar_import_selected_count,
                                    state.selectedCount,
                                    state.totalCount
                                ),
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Button(
                            onClick = { viewModel.onEvent(CalendarImportUiEvent.ImportSelectedEvents) },
                            enabled = state.selectedCount > 0 && !state.isImporting,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                        ) {
                            if (state.isImporting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(Res.string.calendar_import_in_progress),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Upload,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(Res.string.calendar_import_button, state.selectedCount),
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Date Range Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CalendarImportDateRange.entries.forEach { range ->
                    val isSelected = state.selectedDateRange == range
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.onEvent(CalendarImportUiEvent.SelectDateRange(range)) },
                        label = { Text(stringResource(range.labelRes)) },
                        shape = RoundedCornerShape(12.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            // Calendar Source Filters (if multiple calendars exist)
            if (state.availableCalendars.size > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = state.selectedCalendarFilter == null,
                        onClick = { viewModel.onEvent(CalendarImportUiEvent.SelectCalendarFilter(null)) },
                        label = { Text(stringResource(Res.string.calendar_import_filter_all)) },
                        shape = RoundedCornerShape(10.dp)
                    )

                    state.availableCalendars.forEach { calName ->
                        FilterChip(
                            selected = state.selectedCalendarFilter == calName,
                            onClick = { viewModel.onEvent(CalendarImportUiEvent.SelectCalendarFilter(calName)) },
                            label = { Text(calName) },
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Main Content Area
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when {
                    // Permission Required State
                    !state.hasPermission || state.isPermissionDenied -> {
                        PermissionRequiredCard(
                            onGrantPermission = { permissionLauncher.launch() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                                .align(Alignment.Center)
                        )
                    }

                    // Loading State
                    state.isLoading -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(36.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 3.dp
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = stringResource(Res.string.calendar_import_loading),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Empty State
                    state.filteredEvents.isEmpty() -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EventBusy,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(Res.string.calendar_import_empty_title),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = stringResource(Res.string.calendar_import_empty_desc),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // Events List
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(
                                items = state.filteredEvents,
                                key = { it.id }
                            ) { item ->
                                CalendarImportItemCard(
                                    item = item,
                                    onToggleSelect = {
                                        viewModel.onEvent(CalendarImportUiEvent.ToggleEventSelection(item.id))
                                    },
                                    onToggleType = {
                                        viewModel.onEvent(CalendarImportUiEvent.ToggleTargetType(item.id))
                                    },
                                    onEditClick = {
                                        viewModel.onEvent(CalendarImportUiEvent.StartEditItem(item))
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Edit Item Modal Dialog
    if (state.editingItem != null) {
        CalendarImportEditDialog(
            item = state.editingItem!!,
            onSave = { updated -> viewModel.onEvent(CalendarImportUiEvent.SaveEditedItem(updated)) },
            onDismiss = { viewModel.onEvent(CalendarImportUiEvent.DismissEdit) }
        )
    }
}

/**
 * Beautiful permission prompt card displayed when calendar access has not yet been authorized.
 */
@Composable
private fun PermissionRequiredCard(
    onGrantPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(Res.string.calendar_import_permission_title),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(Res.string.calendar_import_permission_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onGrantPermission,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text(
                    text = stringResource(Res.string.calendar_import_grant_permission),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}
