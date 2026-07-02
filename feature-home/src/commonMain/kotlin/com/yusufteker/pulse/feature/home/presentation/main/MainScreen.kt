package com.yusufteker.pulse.feature.home.presentation.main

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
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.clickable
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.yusufteker.pulse.core.navigation.Screen.MainDestination
import com.yusufteker.pulse.core.navigation.Navigator
import com.yusufteker.pulse.core.navigation.LocalMainNavigator
import com.yusufteker.pulse.core.navigation.Screen
import org.jetbrains.compose.resources.stringResource
import pulse.core.generated.resources.Res
import pulse.core.generated.resources.*
import pulse.core.generated.resources.tab_settings
import pulse.core.generated.resources.tab_social
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


@Composable
fun MainScreen() {
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
    val userId by sessionPreferences.userIdFlow.collectAsState(null)
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
            Surface(
                color = if (isDark) Color.Black else MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    // Subtle top border
                    androidx.compose.material3.Divider(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                        thickness = 1.dp
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .height(56.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Home Tab
                    val isHomeSelected = currentDestination is MainDestination.Home
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .clickable(
                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                indication = null
                            ) { navigateToTab(MainDestination.Home) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isHomeSelected) Icons.Filled.Home else Icons.Outlined.Home,
                            contentDescription = "Home",
                            tint = if (isHomeSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Social Tab
                    val isSocialSelected = currentDestination is MainDestination.Social
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .clickable(
                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                indication = null
                            ) { navigateToTab(MainDestination.Social) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isSocialSelected) Icons.Filled.People else Icons.Outlined.People,
                            contentDescription = "Social",
                            tint = if (isSocialSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // PlanRooms Tab
                    val isPlanRoomsSelected = currentDestination is MainDestination.PlanRooms
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .clickable(
                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                indication = null
                            ) { navigateToTab(MainDestination.PlanRooms) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlanRoomsSelected) Icons.Filled.DateRange else Icons.Outlined.DateRange,
                            contentDescription = "Plans",
                            tint = if (isPlanRoomsSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Notes Tab
                    val isNotesSelected = currentDestination is MainDestination.Notes
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .clickable(
                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                indication = null
                            ) { navigateToTab(MainDestination.Notes) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isNotesSelected) Icons.Filled.Edit else Icons.Outlined.Edit,
                            contentDescription = "Notes",
                            tint = if (isNotesSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Profile Tab
                    val isProfileSelected = currentDestination is MainDestination.Profile || currentDestination is MainDestination.Settings
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .clickable(
                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                indication = null
                            ) { navigateToTab(MainDestination.Profile) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isProfileSelected) Icons.Filled.Person else Icons.Outlined.Person,
                            contentDescription = "Profile",
                            tint = if (isProfileSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                }
            }
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
                        val viewModel = koinViewModel<HomeViewModel>(key = vmKey)
                        HomeScreen(viewModel = viewModel)
                    }

                    entry<MainDestination.Social> {
                        val viewModel = koinViewModel<SocialViewModel>(key = vmKey)
                        SocialScreen(viewModel = viewModel)
                    }

                    entry<MainDestination.Profile> {
                        val viewModel = koinViewModel<ProfileViewModel>(key = vmKey)
                        ProfileScreen(viewModel = viewModel)
                    }
                    
                    entry<MainDestination.Notes> {
                        val viewModel = koinViewModel<NotesViewModel>(key = vmKey)
                        NotesScreen(viewModel = viewModel)
                    }

                    entry<MainDestination.PlanRooms> {
                        val viewModel = koinViewModel<PlanRoomsViewModel>(key = vmKey)
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
                        val viewModel = koinViewModel<SettingsViewModel>(key = vmKey)
                        SettingsScreen(viewModel = viewModel)
                    }
                }
            )
        }
    }
}
