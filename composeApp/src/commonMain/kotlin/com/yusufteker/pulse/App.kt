package com.yusufteker.pulse

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.yusufteker.pulse.core.domain.usecase.RegisterFcmTokenUseCase
import com.yusufteker.pulse.core.navigation.LocalNavigator
import com.yusufteker.pulse.core.navigation.Navigator
import com.yusufteker.pulse.core.navigation.Screen
import com.yusufteker.pulse.core.preferences.SessionPreferences
import com.yusufteker.pulse.core.preferences.ThemePreferences
import com.yusufteker.pulse.core.snackbar.SnackbarManager
import com.yusufteker.pulse.core.theme.PulsyTheme
import com.yusufteker.pulse.feature.auth.presentation.login.LoginScreen
import com.yusufteker.pulse.feature.auth.presentation.login.LoginViewModel
import com.yusufteker.pulse.feature.auth.presentation.onboarding.OnboardingScreen
import com.yusufteker.pulse.feature.auth.presentation.onboarding.OnboardingViewModel
import com.yusufteker.pulse.feature.auth.presentation.register.RegisterScreen
import com.yusufteker.pulse.feature.auth.presentation.register.RegisterViewModel
import com.yusufteker.pulse.feature.auth.presentation.splash.SplashScreen
import com.yusufteker.pulse.feature.auth.presentation.splash.SplashViewModel
import com.yusufteker.pulse.feature.home.presentation.create_post.CreatePostScreen
import com.yusufteker.pulse.feature.home.presentation.create_post.CreatePostViewModel
import com.yusufteker.pulse.feature.home.presentation.event_detail.EventDetailEvent
import com.yusufteker.pulse.feature.home.presentation.event_detail.EventDetailScreen
import com.yusufteker.pulse.feature.home.presentation.event_detail.EventDetailViewModel
import com.yusufteker.pulse.feature.home.presentation.main.MainScreen
import com.yusufteker.pulse.feature.home.presentation.pending_posts.PendingPostsScreen
import com.yusufteker.pulse.feature.home.presentation.pending_posts.PendingPostsViewModel
import com.yusufteker.pulse.feature.home.presentation.plan_room_detail.PlanRoomDetailEvent
import com.yusufteker.pulse.feature.home.presentation.plan_room_detail.PlanRoomDetailScreen
import com.yusufteker.pulse.feature.home.presentation.plan_room_detail.PlanRoomDetailViewModel
import com.yusufteker.pulse.feature.home.presentation.search.SearchUsersScreen
import com.yusufteker.pulse.feature.home.presentation.search.SearchUsersViewModel
import com.yusufteker.pulse.feature.home.presentation.task_editor.TaskEditorEvent
import com.yusufteker.pulse.feature.home.presentation.task_editor.TaskEditorScreen
import com.yusufteker.pulse.feature.home.presentation.task_editor.TaskEditorViewModel
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Root composable for the Pulsy application.
 *
 * Sets up:
 * - PulsyTheme (with automatic dark mode)
 * - Navigation 3 (NavDisplay with user-owned back stack)
 * - Screen routing to feature composables
 */
@Composable
fun App() {
    val themePreferences = koinInject<ThemePreferences>()
    val isDarkModePref by themePreferences.isDarkMode.collectAsStateWithLifecycle(initialValue = null)
    val themeColorPref by themePreferences.themeColor.collectAsStateWithLifecycle(initialValue = com.yusufteker.pulse.core.preferences.ThemeColor.DEFAULT)
    val isDark = isDarkModePref ?: isSystemInDarkTheme()

    PulsyTheme(themeColor = themeColorPref, darkTheme = isDark) {
        val snackbarManager = koinInject<SnackbarManager>()
        val snackbarHostState = remember { SnackbarHostState() }
        val activeMessage by snackbarManager.messages.collectAsStateWithLifecycle()

        LaunchedEffect(activeMessage) {
            activeMessage?.let { msg ->
                snackbarHostState.showSnackbar(msg.message)
                snackbarManager.clearMessage(msg.id)
            }
        }

        val sessionPreferences = koinInject<SessionPreferences>()
        val userId by sessionPreferences.userIdFlow.collectAsStateWithLifecycle(initialValue = null)
        val vmKey = userId ?: "guest"

        val backStack = remember { mutableStateListOf<Screen>(Screen.Splash) }
        val navigator = remember { Navigator(backStack) }

        LaunchedEffect(navigator.backStack.lastOrNull()) {
            val currentScreen = navigator.backStack.lastOrNull()
            if (currentScreen != null) {
                var screenName = currentScreen::class.simpleName ?: "UnknownScreen"
                println("SCREEN: $screenName açıldı")
            }
        }

        val registerFcmTokenUseCase = koinInject<RegisterFcmTokenUseCase>()
        val reminderManager = koinInject<com.yusufteker.pulse.core.reminder.ReminderManager>()
        val planRepository = koinInject<com.yusufteker.pulse.feature.home.domain.repository.PlanRepository>()

        LaunchedEffect(userId) {
            if (userId != null) {
                registerFcmTokenUseCase()
            }
        }

        // Observe all tasks and reschedule alarms whenever the task list changes
        LaunchedEffect(userId) {
            if (userId != null) {
                planRepository.observeAllTasks().collect { tasks ->
                    reminderManager.scheduleAllReminders(tasks)
                }
            } else {
                // Logged out — cancel all reminders
                reminderManager.cancelAllReminders()
            }
        }

        CompositionLocalProvider(LocalNavigator provides navigator) {
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                modifier = Modifier.fillMaxSize()
            ) { _ -> // paddingValues kullanılmıyor, iç sayfalarda insets kendileri hesaplanıyor
                NavDisplay(
                    modifier = Modifier.fillMaxSize(),
                    backStack = navigator.backStack,
                    onBack = { navigator.pop() },
                    entryProvider = entryProvider {
                    // ── Splash Graph ─────────────────────────
                    entry<Screen.Splash> {
                        val viewModel = koinViewModel<SplashViewModel>()
                        SplashScreen(
                            viewModel = viewModel
                        )
                    }

                    entry<Screen.Onboarding> {
                        val viewModel = koinViewModel<OnboardingViewModel>()
                        OnboardingScreen(
                            viewModel = viewModel
                        )
                    }

                    // ── Auth Graph ───────────────────────────
                    entry<Screen.Login> {
                        val viewModel = koinViewModel<LoginViewModel>()
                        LoginScreen(
                            viewModel = viewModel
                        )
                    }

                    entry<Screen.Register> {
                        val viewModel = koinViewModel<RegisterViewModel>()
                        RegisterScreen(
                            viewModel = viewModel
                        )
                    }

                    // ── Main Graph (Container for Bottom Navigation) ─────────
                    entry<Screen.Main> {
                        MainScreen()
                    }

                    entry<Screen.AiChat> {
                        val viewModel = koinViewModel<com.yusufteker.pulse.feature.home.presentation.aichat.AiChatViewModel>(key = vmKey)
                        com.yusufteker.pulse.feature.home.presentation.aichat.AiChatScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navigator.pop() }
                        )
                    }

                    entry<Screen.PendingPosts> {
                        val viewModel = koinViewModel<PendingPostsViewModel>(key = vmKey)
                        PendingPostsScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navigator.pop() },
                            onNavigateToEdit = { postId -> navigator.navigate(Screen.CreatePost(postId)) }
                        )
                    }

                    entry<Screen.SearchUsers> {
                        val viewModel = koinViewModel<SearchUsersViewModel>(key = vmKey)
                        SearchUsersScreen(viewModel)
                    }

                    entry<Screen.FollowList> { screen ->
                        val viewModel = koinViewModel<com.yusufteker.pulse.feature.home.presentation.follow_list.FollowListViewModel>(key = vmKey)
                        com.yusufteker.pulse.feature.home.presentation.follow_list.FollowListScreen(
                            viewModel = viewModel,
                            initialTab = screen.initialTab
                        )
                    }

                    entry<Screen.Profile> { screen ->
                        val viewModel = koinViewModel<com.yusufteker.pulse.feature.home.presentation.profile.ProfileViewModel>(
                            key = "${vmKey}_profile_${screen.userId ?: "me"}"
                        )
                        com.yusufteker.pulse.feature.home.presentation.profile.ProfileScreen(
                            viewModel = viewModel,
                            userId = screen.userId
                        )
                    }

                    entry<Screen.CreatePost> { screen ->
                        val viewModel = koinViewModel<CreatePostViewModel>(
                            key = screen.id.toString(),
                            parameters = { parametersOf(screen.postId) }
                        )
                        CreatePostScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navigator.pop() }
                        )
                    }
                    
                    entry<Screen.PlanRoomDetail> { screen ->
                        val viewModel = koinViewModel<PlanRoomDetailViewModel>(
                            key = "${vmKey}_${screen.roomId}"
                        )
                        
                        // Set the room ID using a side effect when this composition starts
                        LaunchedEffect(screen.roomId) {
                            viewModel.onEvent(PlanRoomDetailEvent.LoadRoom(screen.roomId))
                        }
                        
                        PlanRoomDetailScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navigator.pop() },
                            onNavigateToCreateTask = { roomId -> navigator.navigate(Screen.TaskEditor(planRoomId = roomId)) },
                            onNavigateToCreateEvent = { roomId -> navigator.navigate(Screen.EventDetail(planRoomId = roomId)) }
                        )
                    }


                    entry<Screen.TaskEditor> { screen ->
                        val viewModel = koinViewModel<TaskEditorViewModel>(
                            key = "task_editor_${screen.taskId}_${screen.planRoomId}_${screen.parentId}"
                        )
                        LaunchedEffect(screen.taskId, screen.planRoomId, screen.parentId) {
                            viewModel.onEvent(TaskEditorEvent.OnLoadTask(screen.taskId, screen.planRoomId, screen.parentId))
                        }
                        TaskEditorScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navigator.pop() },
                            onNavigateToFocus = { id -> navigator.navigate(Screen.Focus(taskId = id)) },
                            onNavigateToCreateNote = { parentId -> navigator.navigate(Screen.NoteEditor(parentId = parentId, planRoomId = screen.planRoomId)) }
                        )
                    }

                    entry<Screen.NoteEditor> { screen ->
                        val viewModel = koinViewModel<com.yusufteker.pulse.feature.home.presentation.note_editor.NoteEditorViewModel>(
                            key = screen.noteId ?: "new_note_${screen.parentId}",
                            parameters = { org.koin.core.parameter.parametersOf(screen.noteId) }
                        )
                        androidx.compose.runtime.LaunchedEffect(screen) {
                            viewModel.onEvent(com.yusufteker.pulse.feature.home.presentation.note_editor.NoteEditorEvent.OnLoadNote(screen.noteId, screen.planRoomId, screen.parentId))
                        }
                        com.yusufteker.pulse.feature.home.presentation.note_editor.NoteEditorScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navigator.pop() }
                        )
                    }

                    entry<Screen.EventDetail> { screen ->
                        val viewModel = koinViewModel<EventDetailViewModel>(
                            key = screen.eventId ?: "new_event",
                            parameters = { parametersOf(screen.eventId) }
                        )
                        LaunchedEffect(screen) {
                            viewModel.onEvent(EventDetailEvent.OnLoadEvent(screen.eventId, screen.planRoomId))
                        }
                        EventDetailScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navigator.pop() },
                            onNavigateToCreateTask = { parentId -> navigator.navigate(Screen.TaskEditor(parentId = parentId, planRoomId = screen.planRoomId)) },
                            onNavigateToCreateNote = { parentId -> navigator.navigate(Screen.NoteEditor(parentId = parentId, planRoomId = screen.planRoomId)) }
                        )
                    }

                    entry<Screen.Focus> { screen ->
                        val viewModel = koinViewModel<com.yusufteker.pulse.feature.home.presentation.focus.FocusViewModel>(key = vmKey)
                        com.yusufteker.pulse.feature.home.presentation.focus.FocusScreen(
                            viewModel = viewModel,
                            taskId = screen.taskId,
                            onNavigateBack = { navigator.pop() }
                        )
                    }
                } // closes entryProvider
            )
            } // Close Scaffold
        }
    }
}
