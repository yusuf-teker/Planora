package com.yusufteker.pulse.feature.home.presentation.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import org.jetbrains.compose.resources.painterResource
import androidx.compose.foundation.layout.Box

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.clickable
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import androidx.compose.runtime.CompositionLocalProvider

import androidx.compose.runtime.getValue
import com.yusufteker.pulse.core.navigation.Screen.MainDestination
import com.yusufteker.pulse.core.navigation.Navigator
import com.yusufteker.pulse.core.navigation.LocalMainNavigator
import com.yusufteker.pulse.core.navigation.LocalNavigator
import com.yusufteker.pulse.core.navigation.Screen
import org.jetbrains.compose.resources.stringResource
import pulsy.core.generated.resources.Res
import pulsy.core.generated.resources.*
import pulsy.core.generated.resources.tab_settings
import pulsy.core.generated.resources.tab_social
import com.yusufteker.pulse.feature.home.presentation.home.HomeScreen
import com.yusufteker.pulse.feature.home.presentation.home.HomeViewModel
import com.yusufteker.pulse.feature.home.presentation.social.SocialScreen
import com.yusufteker.pulse.feature.home.presentation.social.SocialViewModel
import com.yusufteker.pulse.feature.home.presentation.profile.ProfileScreen
import com.yusufteker.pulse.feature.home.presentation.profile.ProfileViewModel
import com.yusufteker.pulse.feature.home.presentation.notes.NotesScreen
import com.yusufteker.pulse.feature.home.presentation.notes.NotesViewModel
import com.yusufteker.pulse.feature.home.presentation.settings.SettingsScreen
import com.yusufteker.pulse.feature.home.presentation.settings.SettingsViewModel
import com.yusufteker.pulse.feature.home.presentation.plan_rooms.PlanRoomsScreen
import com.yusufteker.pulse.feature.home.presentation.plan_rooms.PlanRoomsViewModel
import org.koin.compose.viewmodel.koinViewModel

import com.yusufteker.pulse.core.theme.LocalIsDarkTheme

import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.LaunchedEffect
import com.yusufteker.pulse.feature.home.presentation.components.PulseBottomBar
import org.jetbrains.compose.resources.vectorResource

@Composable
fun MainScreen() {
    val rootNavigator = LocalNavigator.current
    // Nested back stack for the bottom navigation, saved across compositions
    val backStack = rememberSaveable(
        saver = listSaver(
            save = { list ->
                list.map { dest ->
                    when (dest) {
                        MainDestination.Home -> "Home"
                        MainDestination.Social -> "Social"
                        MainDestination.PlanRooms -> "PlanRooms"
                        MainDestination.Notes -> "Notes"
                        MainDestination.Profile -> "Profile"
                        MainDestination.Settings -> "Settings"
                    }
                }
            },
            restore = { savedList ->
                val list = mutableStateListOf<MainDestination>()
                savedList.forEach { name ->
                    when (name) {
                        "Home" -> list.add(MainDestination.Home)
                        "Social" -> list.add(MainDestination.Social)
                        "PlanRooms" -> list.add(MainDestination.PlanRooms)
                        "Notes" -> list.add(MainDestination.Notes)
                        "Profile" -> list.add(MainDestination.Profile)
                        "Settings" -> list.add(MainDestination.Settings)
                    }
                }
                if (list.isEmpty()) list.add(MainDestination.Home)
                list
            }
        )
    ) {
        mutableStateListOf<MainDestination>(MainDestination.Home)
    }
    val navigator = remember { Navigator(backStack) }

    val currentDestination = backStack.lastOrNull() ?: MainDestination.Home

    LaunchedEffect(currentDestination) {
        val screenName = currentDestination::class.simpleName ?: "UnknownScreen"
        println("SCREEN: $screenName açıldı")
    }

    val isDark = LocalIsDarkTheme.current
    
    val sessionPreferences = org.koin.compose.koinInject<com.yusufteker.pulse.core.preferences.SessionPreferences>()
    val userId by sessionPreferences.userIdFlow.collectAsStateWithLifecycle(initialValue = null)
    
    val pendingFollowRequestsCount by sessionPreferences.pendingFollowRequestsCountFlow.collectAsStateWithLifecycle(initialValue = 0)
    val pendingCalendarRequestsCount by sessionPreferences.pendingCalendarRequestsCountFlow.collectAsStateWithLifecycle(initialValue = 0)
    val pendingRequestsCount = pendingFollowRequestsCount + pendingCalendarRequestsCount

    val vmKey = userId ?: "guest"

    val navigateToTab: (MainDestination) -> Unit = { destination ->
        if (currentDestination != destination) {
            navigator.setRoot(MainDestination.Home)
            if (destination != MainDestination.Home) {
                navigator.navigate(destination)
            }
        }
    }

    Scaffold(
        bottomBar = {
            PulseBottomBar(
                currentDestination = currentDestination,
                isDark = isDark,
                onNavigate = { destination ->
                    navigateToTab(destination)
                },
                onAiButtonClick = {
                    rootNavigator.navigate(com.yusufteker.pulse.core.navigation.Screen.AiChat)
                },
                pendingRequestsCount = pendingRequestsCount
            )
        }
    ) { paddingValues ->
        // Create a ViewModelStore for the MainScreen. 
        // When MainScreen is removed from composition (logout), it will be cleared.
        val viewModelStoreOwner = remember {
            object : androidx.lifecycle.ViewModelStoreOwner {
                override val viewModelStore = androidx.lifecycle.ViewModelStore()
            }
        }
        
        // Clear the ViewModelStore when MainScreen leaves the composition
        androidx.compose.runtime.DisposableEffect(Unit) {
            onDispose {
                viewModelStoreOwner.viewModelStore.clear()
            }
        }

        CompositionLocalProvider(
            LocalMainNavigator provides navigator,
            androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner provides viewModelStoreOwner
        ) {
            NavDisplay(
                backStack = navigator.backStack,
                onBack = { navigator.pop() },
                modifier = Modifier.padding(bottom = paddingValues.calculateBottomPadding()),
                entryProvider = entryProvider {
                    entry<MainDestination.Home> {
                        val viewModel = koinViewModel<HomeViewModel>(
                            viewModelStoreOwner = viewModelStoreOwner,
                            key = vmKey
                        )
                        HomeScreen(viewModel = viewModel)
                    }

                    entry<MainDestination.Social> {
                        val viewModel = koinViewModel<SocialViewModel>(
                            viewModelStoreOwner = viewModelStoreOwner,
                            key = vmKey
                        )
                        SocialScreen(viewModel = viewModel)
                    }

                    entry<MainDestination.Profile> {
                        val viewModel = koinViewModel<ProfileViewModel>(
                            viewModelStoreOwner = viewModelStoreOwner,
                            key = vmKey
                        )
                        ProfileScreen(viewModel = viewModel)
                    }
                    
                    entry<MainDestination.Notes> {
                        val viewModel = koinViewModel<NotesViewModel>(
                            viewModelStoreOwner = viewModelStoreOwner,
                            key = vmKey
                        )
                        NotesScreen(viewModel = viewModel)
                    }

                    entry<MainDestination.PlanRooms> {
                        val viewModel = koinViewModel<PlanRoomsViewModel>(
                            viewModelStoreOwner = viewModelStoreOwner,
                            key = vmKey
                        )
                        val state = viewModel.state.collectAsStateWithLifecycle().value
                        val rootNavigator = com.yusufteker.pulse.core.navigation.LocalNavigator.current
                        PlanRoomsScreen(
                            state = state,
                            effectFlow = viewModel.effect,
                            onEvent = viewModel::onEvent,
                            onNavigateToRoomDetail = { roomId -> 
                                rootNavigator.navigate(Screen.PlanRoomDetail(roomId))
                            },
                            onNavigateToCreateTask = {
                                rootNavigator.navigate(Screen.TaskEditor(taskId = null))
                            },
                            onNavigateToCreateEvent = {
                                rootNavigator.navigate(Screen.EventDetail(eventId = null))
                            },
                            onShowSnackbar = { /* TODO */ }
                        )
                    }

                    entry<MainDestination.Settings> {
                        val viewModel = koinViewModel<SettingsViewModel>(
                            viewModelStoreOwner = viewModelStoreOwner,
                            key = vmKey
                        )
                        SettingsScreen(viewModel = viewModel)
                    }
                }
            )
        }
    }
}

val PulseIcon: ImageVector
    get() {
        if (_pulseIcon != null) {
            return _pulseIcon!!
        }
        _pulseIcon = ImageVector.Builder(
            name = "PulseIcon",
            defaultWidth = 100.dp,
            defaultHeight = 100.dp,
            viewportWidth = 100f,
            viewportHeight = 100f
        ).apply {
            group {
                // 1. Dark Filled Background
                path(
                    fill = SolidColor(Color(0xFF05050A))
                ) {
                    moveTo(50f, 5f)
                    curveTo(74.85f, 5f, 95f, 25.15f, 95f, 50f)
                    curveTo(95f, 74.85f, 74.85f, 95f, 50f, 95f)
                    curveTo(25.15f, 95f, 5f, 74.85f, 5f, 50f)
                    curveTo(5f, 25.15f, 25.15f, 5f, 50f, 5f)
                    close()
                }

                // 2. Glow for Circle
                path(
                    stroke = SolidColor(Color(0x8063CFF1)),
                    strokeLineWidth = 6f
                ) {
                    moveTo(50f, 5f)
                    curveTo(74.85f, 5f, 95f, 25.15f, 95f, 50f)
                    curveTo(95f, 74.85f, 74.85f, 95f, 50f, 95f)
                    curveTo(25.15f, 95f, 5f, 74.85f, 5f, 50f)
                    curveTo(5f, 25.15f, 25.15f, 5f, 50f, 5f)
                    close()
                }

                // 3. Core of Circle
                path(
                    stroke = Brush.linearGradient(
                        colorStops = arrayOf(
                            0.0f to Color(0xFF63CFF1),
                            1.0f to Color(0xFF268DDF)
                        ),
                        start = Offset(50f, 5f),
                        end = Offset(50f, 95f)
                    ),
                    strokeLineWidth = 2f
                ) {
                    moveTo(50f, 5f)
                    curveTo(74.85f, 5f, 95f, 25.15f, 95f, 50f)
                    curveTo(95f, 74.85f, 74.85f, 95f, 50f, 95f)
                    curveTo(25.15f, 95f, 5f, 74.85f, 5f, 50f)
                    curveTo(5f, 25.15f, 25.15f, 5f, 50f, 5f)
                    close()
                }

                // 4. Glow for Pulse (Taller)
                path(
                    stroke = SolidColor(Color(0x8063CFF1)),
                    strokeLineWidth = 8f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round
                ) {
                    moveTo(25f, 50f)
                    lineTo(38f, 50f)
                    lineTo(47f, 22f)
                    lineTo(58f, 78f)
                    lineTo(66f, 40f)
                    lineTo(70f, 50f)
                    lineTo(83f, 50f)
                }

                // 5. Core of Pulse (Taller)
                path(
                    stroke = Brush.linearGradient(
                        colorStops = arrayOf(
                            0.0f to Color(0xFF63CFF1),
                            1.0f to Color(0xFF268DDF)
                        ),
                        start = Offset(50f, 22f),
                        end = Offset(50f, 78f)
                    ),
                    strokeLineWidth = 3f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round
                ) {
                    moveTo(25f, 50f)
                    lineTo(38f, 50f)
                    lineTo(47f, 22f)
                    lineTo(58f, 78f)
                    lineTo(66f, 40f)
                    lineTo(70f, 50f)
                    lineTo(83f, 50f)
                }
            }
        }.build()
        return _pulseIcon!!
    }

private var _pulseIcon: ImageVector? = null
