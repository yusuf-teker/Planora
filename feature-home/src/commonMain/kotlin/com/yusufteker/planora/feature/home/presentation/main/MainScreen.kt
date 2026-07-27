package com.yusufteker.planora.feature.home.presentation.main

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.yusufteker.planora.core.navigation.Screen.MainDestination
import com.yusufteker.planora.core.navigation.Navigator
import com.yusufteker.planora.core.navigation.LocalMainNavigator
import com.yusufteker.planora.core.navigation.LocalNavigator
import com.yusufteker.planora.core.navigation.Screen
import org.jetbrains.compose.resources.stringResource
import planora.core.generated.resources.Res
import planora.core.generated.resources.*
import planora.core.generated.resources.tab_settings
import planora.core.generated.resources.tab_social
import com.yusufteker.planora.feature.home.presentation.home.HomeScreen
import com.yusufteker.planora.feature.home.presentation.home.HomeViewModel
import com.yusufteker.planora.feature.home.presentation.social.SocialScreen
import com.yusufteker.planora.feature.home.presentation.social.SocialViewModel
import com.yusufteker.planora.feature.home.presentation.profile.ProfileScreen
import com.yusufteker.planora.feature.home.presentation.profile.ProfileViewModel
import com.yusufteker.planora.feature.home.presentation.notes.NotesScreen
import com.yusufteker.planora.feature.home.presentation.notes.NotesViewModel
import com.yusufteker.planora.feature.home.presentation.settings.SettingsScreen
import com.yusufteker.planora.feature.home.presentation.settings.SettingsViewModel
import com.yusufteker.planora.feature.home.presentation.plan_rooms.PlanRoomsScreen
import com.yusufteker.planora.feature.home.presentation.plan_rooms.PlanRoomsViewModel
import org.koin.compose.viewmodel.koinViewModel

import com.yusufteker.planora.core.theme.LocalIsDarkTheme

import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.yusufteker.planora.feature.home.presentation.components.PlanoraBottomBar
import org.koin.compose.koinInject

@Composable
fun MainScreen(initialDestination: String? = null) {
    val rootNavigator = LocalNavigator.current
    // Nested back stack for the bottom navigation, saved across compositions
    val backStack = rememberSaveable(
        saver = listSaver(
            save = { list ->
                list.map { dest ->
                    when (dest) {
                        MainDestination.Home -> "Home"
// MainDestination.Social -> "Social"
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
// "Social" -> list.add(MainDestination.Social)
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
        val initial = when (initialDestination) {
            "PlanRooms" -> MainDestination.PlanRooms
            "Notes" -> MainDestination.Notes
            "Profile" -> MainDestination.Profile
            "Settings" -> MainDestination.Settings
            else -> MainDestination.Home
        }
        mutableStateListOf<MainDestination>(initial)
    }
    val navigator = remember { Navigator(backStack) }

    val currentDestination = backStack.lastOrNull() ?: MainDestination.Home

    LaunchedEffect(initialDestination) {
        if (!initialDestination.isNullOrEmpty()) {
            val targetDest = when (initialDestination) {
                "PlanRooms" -> MainDestination.PlanRooms
                "Notes" -> MainDestination.Notes
                "Profile" -> MainDestination.Profile
                "Settings" -> MainDestination.Settings
                else -> MainDestination.Home
            }
            if (backStack.lastOrNull() != targetDest) {
                while (backStack.size > 1) {
                    backStack.removeLastOrNull()
                }
                if (targetDest != MainDestination.Home) {
                    backStack.add(targetDest)
                }
            }
        }
    }

    LaunchedEffect(currentDestination) {
        val screenName = currentDestination::class.simpleName ?: "UnknownScreen"
        io.github.aakira.napier.Napier.d("SCREEN: $screenName açıldı")
    }

    val isDark = LocalIsDarkTheme.current
    
    val sessionPreferences = koinInject<com.yusufteker.planora.core.preferences.SessionPreferences>()
    val userId by sessionPreferences.userIdFlow.collectAsStateWithLifecycle(initialValue = null)
    
    val pendingFollowRequestsCount by sessionPreferences.pendingFollowRequestsCountFlow.collectAsStateWithLifecycle(initialValue = 0)
    val pendingCalendarRequestsCount by sessionPreferences.pendingCalendarRequestsCountFlow.collectAsStateWithLifecycle(initialValue = 0)
    val pendingRequestsCount = pendingFollowRequestsCount + pendingCalendarRequestsCount

    val vmKey = userId ?: "guest"

    val navigateToTab: (MainDestination) -> Unit = { destination ->
        if (currentDestination != destination) {
            if (destination == MainDestination.Home) {
                while (backStack.size > 1) {
                    navigator.pop()
                }
            } else {
                navigator.setRoot(MainDestination.Home)
                navigator.navigate(destination)
            }
        }
    }

    // Create a ViewModelStore for the MainScreen. 
    // When MainScreen is removed from composition (logout), it will be cleared.
    val viewModelStoreOwner = remember {
        object : ViewModelStoreOwner {
            override val viewModelStore = ViewModelStore()
        }
    }
    
    // Clear the ViewModelStore when MainScreen leaves the composition
    DisposableEffect(Unit) {
        onDispose {
            viewModelStoreOwner.viewModelStore.clear()
        }
    }

    // Create a cache of ViewModelStores for each tab to prevent recreation during transitions
    val viewModelStores = remember { mutableMapOf<String, ViewModelStore>() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        CompositionLocalProvider(
            LocalMainNavigator provides navigator,
            LocalViewModelStoreOwner provides viewModelStoreOwner
        ) {
            NavDisplay(
                backStack = navigator.backStack,
                onBack = { navigator.pop() },
                modifier = Modifier.fillMaxSize(),
                entryProvider = entryProvider {
                    entry<MainDestination.Home> {
                        val storeOwner = remember {
                            object : ViewModelStoreOwner {
                                override val viewModelStore = viewModelStores.getOrPut("Home") { ViewModelStore() }
                            }
                        }
                        val viewModel = koinViewModel<HomeViewModel>(
                            viewModelStoreOwner = storeOwner,
                            key = vmKey
                        )
                        HomeScreen(viewModel = viewModel)
                    }

/*
                    entry<MainDestination.Social> {
                        val storeOwner = remember {
                            object : ViewModelStoreOwner {
                                override val viewModelStore = viewModelStores.getOrPut("Social") { ViewModelStore() }
                            }
                        }
                        val viewModel = koinViewModel<SocialViewModel>(
                            viewModelStoreOwner = storeOwner,
                            key = vmKey
                        )
                        SocialScreen(viewModel = viewModel)
                    }
                    */

                    entry<MainDestination.Profile> {
                        val storeOwner = remember {
                            object : ViewModelStoreOwner {
                                override val viewModelStore = viewModelStores.getOrPut("Profile") { ViewModelStore() }
                            }
                        }
                        val viewModel = koinViewModel<ProfileViewModel>(
                            viewModelStoreOwner = storeOwner,
                            key = vmKey
                        )
                        ProfileScreen(viewModel = viewModel)
                    }
                    
                    entry<MainDestination.Notes> {
                        val storeOwner = remember {
                            object : ViewModelStoreOwner {
                                override val viewModelStore = viewModelStores.getOrPut("Notes") { ViewModelStore() }
                            }
                        }
                        val viewModel = koinViewModel<NotesViewModel>(
                            viewModelStoreOwner = storeOwner,
                            key = vmKey
                        )
                        NotesScreen(viewModel = viewModel)
                    }

                    entry<MainDestination.PlanRooms> {
                        val storeOwner = remember {
                            object : ViewModelStoreOwner {
                                override val viewModelStore = viewModelStores.getOrPut("PlanRooms") { ViewModelStore() }
                            }
                        }
                        val viewModel = koinViewModel<PlanRoomsViewModel>(
                            viewModelStoreOwner = storeOwner,
                            key = vmKey
                        )
                        val state by viewModel.state.collectAsStateWithLifecycle()
                        val rootNav = com.yusufteker.planora.core.navigation.LocalNavigator.current
                        PlanRoomsScreen(
                            state = state,
                            effectFlow = viewModel.effect,
                            onEvent = viewModel::onEvent,
                            onNavigateToRoomDetail = { roomId -> 
                                rootNav.navigate(Screen.PlanRoomDetail(roomId))
                            },
                            onNavigateToCreateTask = {
                                rootNav.navigate(Screen.TaskEditor(taskId = null))
                            },
                            onNavigateToCreateEvent = {
                                rootNav.navigate(Screen.EventEditor(eventId = null))
                            },
                            onShowSnackbar = { /* TODO */ }
                        )
                    }

                    entry<MainDestination.Settings> {
                        val storeOwner = remember {
                            object : ViewModelStoreOwner {
                                override val viewModelStore = viewModelStores.getOrPut("Settings") { ViewModelStore() }
                            }
                        }
                        val viewModel = koinViewModel<SettingsViewModel>(
                            viewModelStoreOwner = storeOwner,
                            key = vmKey
                        )
                        SettingsScreen(viewModel = viewModel)
                    }
                }
            )
        }

        PlanoraBottomBar(
            currentDestination = currentDestination,
            isDark = isDark,
            onNavigate = { destination ->
                navigateToTab(destination)
            },
            onAiButtonClick = {
                rootNavigator.navigate(Screen.AiChat)
            },
            pendingRequestsCount = pendingRequestsCount,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
