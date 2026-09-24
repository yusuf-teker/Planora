package com.yusufteker.planora.feature.home.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yusufteker.planora.core.base.CollectEffect
import com.yusufteker.planora.core.navigation.LocalMainNavigator
import com.yusufteker.planora.core.navigation.Screen
import com.yusufteker.planora.core.navigation.Screen.MainDestination
import com.yusufteker.planora.core.ui.components.ShimmerLoadingItem
import com.yusufteker.planora.core.utils.TimelineViewOption
import com.yusufteker.planora.core.utils.getTodayWithOriginalTime
import com.yusufteker.planora.feature.home.presentation.components.DateTimePickerSheet
import com.yusufteker.planora.feature.home.presentation.home.components.calendar.CalendarSection
import com.yusufteker.planora.feature.home.presentation.home.components.FilterBottomSheetComponent
import com.yusufteker.planora.feature.home.presentation.home.components.HomeFabMenu
import com.yusufteker.planora.feature.home.presentation.home.components.HomeTopBar
import com.yusufteker.planora.feature.home.presentation.home.components.SharedTaskDetailDialog
import com.yusufteker.planora.feature.home.presentation.home.components.SharedUserChipRow
import com.yusufteker.planora.feature.home.presentation.home.components.TasksHubSection
import com.yusufteker.planora.feature.home.presentation.home.components.TimelineSection
import com.yusufteker.planora.feature.home.presentation.home.components.TypeFilterChipRow
import com.yusufteker.planora.shared.api.ItemDetails

/**
 * Home screen composable.
 *
 * Displays the main feed with top app bar for navigation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel
) {
    val mainNavigator = LocalMainNavigator.current
    val rootNavigator = com.yusufteker.planora.core.navigation.LocalNavigator.current
    val state by viewModel.state.collectAsStateWithLifecycle()

    viewModel.effect.CollectEffect { effect ->
        when (effect) {
            is HomeEffect.NavigateToProfile -> {
                mainNavigator.setRoot(MainDestination.Home)
                mainNavigator.navigate(MainDestination.Profile)
            }

            is HomeEffect.NavigateToSettings -> {
                mainNavigator.setRoot(MainDestination.Home)
                mainNavigator.navigate(MainDestination.Settings)
            }

            is HomeEffect.NavigateToCreateTask -> {
                rootNavigator.navigate(Screen.TaskEditor(taskId = null, sharedDate = effect.initialDateMs))
            }

            is HomeEffect.NavigateToCreateEvent -> {
                rootNavigator.navigate(Screen.EventEditor(eventId = null, sharedDate = effect.initialDateMs))
            }

            is HomeEffect.NavigateToTaskDetail -> {
                rootNavigator.navigate(Screen.TaskDetail(taskId = effect.taskId))
            }

            is HomeEffect.NavigateToTaskEditor -> {
                rootNavigator.navigate(Screen.TaskEditor(taskId = effect.taskId))
            }

            is HomeEffect.NavigateToEventDetail -> {
                rootNavigator.navigate(Screen.EventDetail(eventId = effect.eventId))
            }

            is HomeEffect.NavigateToEventEditor -> {
                rootNavigator.navigate(Screen.EventEditor(eventId = effect.eventId))
            }

            is HomeEffect.NavigateToNoteEditor -> {
                rootNavigator.navigate(Screen.NoteEditor(noteId = effect.noteId))
            }
        }
    }

    var isFabExpanded by remember { mutableStateOf(false) }
    var quickDuplicateTask by remember { mutableStateOf<com.yusufteker.planora.shared.api.TaskDto?>(null) }

    if (state.isPreferencesLoading || !state.hasLoadedTasks) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            ShimmerLoadingItem(height = 48.dp)
            Spacer(modifier = Modifier.height(20.dp))
            ShimmerLoadingItem(height = 100.dp)
            Spacer(modifier = Modifier.height(16.dp))
            repeat(4) {
                ShimmerLoadingItem(height = 84.dp)
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
        return
    }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            HomeFabMenu(
                isExpanded = isFabExpanded,
                onExpandedChange = { isFabExpanded = it },
                onCreateTask = { viewModel.onEvent(HomeEvent.CreateTaskClicked) },
                onCreateEvent = { viewModel.onEvent(HomeEvent.CreateEventClicked) },
                modifier = Modifier.padding(bottom = 84.dp)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.Start,
            ) {
                // Header (Planora Top Bar)
                HomeTopBar(
                    state = state, onEvent = viewModel::onEvent
                )

                // Shared Users row
                SharedUserChipRow(
                    accessibleUsers = state.accessibleUsers,
                    selectedUserIds = state.selectedSharedUserIds,
                    isMyTasksSelected = state.isMyTasksSelected,
                    currentUserAvatarId = state.currentUserAvatarId,
                    currentUserProfileImageUrl = state.currentUserProfileImageUrl,
                    onToggleMyUser = { viewModel.onEvent(HomeEvent.ToggleMyUser) },
                    onToggleUser = { viewModel.onEvent(HomeEvent.ToggleSharedUser(it)) }
                )

                Spacer(modifier = Modifier.height(4.dp))

                // 1-Dokunuş Hızlı Tip Filtresi (Tümü / Etkinlikler / Görevler) - Akış ve Takvim modlarında aktif
                if (state.viewOption == TimelineViewOption.DATE || state.viewOption == TimelineViewOption.CALENDAR) {
                    TypeFilterChipRow(
                        selectedFilter = state.typeFilter,
                        onFilterSelected = { viewModel.onEvent(HomeEvent.TypeFilterChanged(it)) }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // View Option Switch
                when (state.viewOption) {
                    TimelineViewOption.CALENDAR -> {
                        CalendarSection(
                            state = state, 
                            onEvent = viewModel::onEvent,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    TimelineViewOption.TASKS -> {
                        TasksHubSection(
                            state = state,
                            onTaskClick = { viewModel.onEvent(HomeEvent.TimelineItemClicked(it)) },
                            onTaskToggleStatus = { viewModel.onEvent(HomeEvent.ToggleTaskCompletion(it)) },
                            onTaskDelete = { viewModel.onEvent(HomeEvent.OnDeleteTask(it)) },
                            onQuickCreateTask = { viewModel.onEvent(HomeEvent.QuickCreateTask(it)) },
                            onTaskTogglePin = { taskId, isPinned -> viewModel.onEvent(HomeEvent.ToggleTaskPin(taskId, isPinned)) },
                            onTaskChangePriority = { task, newPriority -> viewModel.onEvent(HomeEvent.ChangeTaskPriority(task, newPriority)) },
                            onMoveTaskOrder = { task, isUp -> viewModel.onEvent(HomeEvent.MoveTaskOrder(task, isUp)) },
                            modifier = Modifier.weight(1f).fillMaxWidth()
                        )
                    }
                    TimelineViewOption.DATE -> {
                        TimelineSection(
                            state = state, 
                            onTaskClick = {
                                viewModel.onEvent(HomeEvent.TimelineItemClicked(it))
                            },
                            onTaskDelete = { taskId ->
                                viewModel.onEvent(HomeEvent.OnDeleteTask(taskId))
                            },
                            onTaskToggleStatus = { task ->
                                viewModel.onEvent(HomeEvent.ToggleTaskCompletion(task))
                            },
                            onTaskQuickDuplicate = { task ->
                                quickDuplicateTask = task
                            },
                            onLoadMore = {
                                viewModel.onEvent(HomeEvent.LoadMoreFutureTasks)
                            },
                            modifier = Modifier.weight(1f).fillMaxWidth()
                        )
                    }
                }
            }
        }
    }

    if (state.isFilterSheetVisible) {
        FilterBottomSheetComponent(
            filterOptions = state.filterOptions,
            onDismiss = { viewModel.onEvent(HomeEvent.ToggleFilterSheet(false)) },
            onFilterOptionsChanged = { viewModel.onEvent(HomeEvent.FilterOptionChanged(it)) })
    }

    state.selectedSharedTask?.let { task ->
        SharedTaskDetailDialog(
            task = task,
            onDismiss = { viewModel.onEvent(HomeEvent.DismissSharedTaskDetail) }
        )
    }

    quickDuplicateTask?.let { task ->
        val rawTimeMs = (task.specificDetails as? ItemDetails.Task)?.deadline ?: task.endTime ?: task.startTime
        val initialMs = remember(task.id) {
            getTodayWithOriginalTime(rawTimeMs)
        }
        DateTimePickerSheet(
            initialTimeMs = initialMs,
            sheetState = rememberModalBottomSheetState(),
            onDismissRequest = { quickDuplicateTask = null },
            onDateTimeSelected = { selectedMs ->
                quickDuplicateTask = null
                viewModel.onEvent(HomeEvent.QuickDuplicateTask(task, selectedMs))
            }
        )
    }
}

