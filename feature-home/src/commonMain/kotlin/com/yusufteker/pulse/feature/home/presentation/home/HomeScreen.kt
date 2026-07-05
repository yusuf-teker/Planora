package com.yusufteker.pulse.feature.home.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.zIndex
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.SyncProblem
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import kotlinx.datetime.toLocalDateTime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material3.Surface
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yusufteker.pulse.core.base.CollectEffect
import com.yusufteker.pulse.core.navigation.LocalMainNavigator
import com.yusufteker.pulse.feature.home.presentation.components.EmptyStateComponent
import com.yusufteker.pulse.feature.home.presentation.home.components.DayHeader
import com.yusufteker.pulse.feature.home.presentation.home.components.TimelineTaskCard
import com.yusufteker.pulse.feature.home.presentation.home.components.FilterBottomSheetComponent
import com.yusufteker.pulse.core.navigation.Screen
import com.yusufteker.pulse.core.navigation.Screen.MainDestination
import com.yusufteker.pulse.core.utils.formatDayName
import com.yusufteker.pulse.core.utils.formatShortDate
import com.yusufteker.pulse.core.utils.formatTime
import com.yusufteker.pulse.core.utils.isToday
import com.yusufteker.pulse.core.utils.isTomorrow
import com.yusufteker.pulse.core.utils.rotateVertically
import com.yusufteker.pulse.shared.api.TaskDto
import com.yusufteker.pulse.shared.api.TaskType
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.yusufteker.pulse.core.utils.TimelineViewOption
import com.yusufteker.pulse.core.utils.getRelativeTimeBucket
import com.yusufteker.pulse.feature.home.presentation.home.components.CalendarSection
import com.yusufteker.pulse.feature.home.presentation.home.components.HomeFabMenu
import com.yusufteker.pulse.feature.home.presentation.home.components.HomeTopBar
import com.yusufteker.pulse.feature.home.presentation.home.components.TimelineSection

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
    val rootNavigator = com.yusufteker.pulse.core.navigation.LocalNavigator.current
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
                rootNavigator.navigate(Screen.TaskEditor(taskId = null))
            }
            is HomeEffect.NavigateToCreateEvent -> {
                rootNavigator.navigate(Screen.EventDetail(eventId = null))
            }
            is HomeEffect.NavigateToTaskEditor -> {
                rootNavigator.navigate(Screen.TaskEditor(taskId = effect.taskId))
            }
            is HomeEffect.NavigateToEventDetail -> {
                rootNavigator.navigate(Screen.EventDetail(eventId = effect.eventId))
            }
            is HomeEffect.NavigateToNoteEditor -> {
                rootNavigator.navigate(Screen.NoteEditor(noteId = effect.noteId))
            }
        }
    }

    var isFabExpanded by remember { mutableStateOf(false) }

    if (state.isPreferencesLoading) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
    return
}
Scaffold(
        floatingActionButton = {
            HomeFabMenu(
                isExpanded = isFabExpanded,
                onExpandedChange = {
                    isFabExpanded = it
                },
                onCreateTask = {
                    viewModel.onEvent(HomeEvent.CreateTaskClicked)
                },
                onCreateEvent = {
                    viewModel.onEvent(HomeEvent.CreateEventClicked)
                }
            )
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .imePadding(),
            horizontalAlignment = Alignment.Start
        ) {
            
            // Header (Pulsy Top Bar)
            HomeTopBar(
                state = state,
                onEvent = viewModel::onEvent
            )

        Spacer(modifier = Modifier.height(16.dp))

        // Calendar View
        if (state.viewOption == TimelineViewOption.CALENDAR) {
            CalendarSection(
                state = state,
                onEvent = viewModel::onEvent
            )
        }
        


        // Timeline
            TimelineSection(
                state = state,
                onTaskClick = {
                    viewModel.onEvent(HomeEvent.TimelineItemClicked(it))
                }
            )

    }
    }
    
    if (state.isFilterSheetVisible) {
        FilterBottomSheetComponent(
            filterOptions = state.filterOptions,
            onDismiss = { viewModel.onEvent(HomeEvent.ToggleFilterSheet(false)) },
            onFilterOptionsChanged = { viewModel.onEvent(HomeEvent.FilterOptionChanged(it)) }
        )
    }
}

