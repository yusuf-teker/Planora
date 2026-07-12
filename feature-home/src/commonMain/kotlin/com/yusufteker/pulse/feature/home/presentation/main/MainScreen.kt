package com.yusufteker.pulse.feature.home.presentation.main

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect

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
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.yusufteker.pulse.feature.home.presentation.components.PulseBottomBar
import org.koin.compose.koinInject

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
    
    val sessionPreferences = koinInject<com.yusufteker.pulse.core.preferences.SessionPreferences>()
    val userId by sessionPreferences.userIdFlow.collectAsStateWithLifecycle(initialValue = null)
    
    val pendingFollowRequestsCount by sessionPreferences.pendingFollowRequestsCountFlow.collectAsStateWithLifecycle(initialValue = 0)
    val pendingCalendarRequestsCount by sessionPreferences.pendingCalendarRequestsCountFlow.collectAsStateWithLifecycle(initialValue = 0)
    val pendingRequestsCount = pendingFollowRequestsCount + pendingCalendarRequestsCount

    val vmKey = userId ?: "guest"

    LaunchedEffect(vmKey) {
        println("MAIN_SCREEN vmKey changed to: $vmKey")
    }

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

    Scaffold(
        bottomBar = {
            PulseBottomBar(
                currentDestination = currentDestination,
                isDark = isDark,
                onNavigate = { destination ->
                    navigateToTab(destination)
                },
                onAiButtonClick = {
                    rootNavigator.navigate(Screen.AiChat)
                },
                pendingRequestsCount = pendingRequestsCount
            )
        }
    ) { paddingValues ->
        // Create a ViewModelStore for the MainScreen. 
        // When MainScreen is removed from composition (logout), it will be cleared.
        val viewModelStoreOwner = remember {
            object : ViewModelStoreOwner {
                override val viewModelStore = ViewModelStore()
            }
        }
        
        // Clear the ViewModelStore when MainScreen leaves the composition
        DisposableEffect(Unit) {
            println("MAIN_SCREEN viewModelStoreOwner CREATED: ${viewModelStoreOwner.hashCode()}")

            onDispose {
                println("MAIN_SCREEN viewModelStoreOwner CLEARING: ${viewModelStoreOwner.hashCode()}")

                viewModelStoreOwner.viewModelStore.clear()
            }
        }

        CompositionLocalProvider(
            LocalMainNavigator provides navigator,
           LocalViewModelStoreOwner provides viewModelStoreOwner
        ) {

            NavDisplay(
                backStack = navigator.backStack,
                onBack = { navigator.pop() },
                modifier = Modifier.padding(bottom = paddingValues.calculateBottomPadding()),
                transitionSpec = {
                    ContentTransform(
                        EnterTransition.None,
                        ExitTransition.None
                    )
                },
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
