    package com.yusufteker.planora

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
import androidx.compose.ui.input.pointer.pointerInput
import io.github.aakira.napier.Napier
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.platform.LocalFocusManager
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.yusufteker.planora.core.domain.usecase.RegisterFcmTokenUseCase
import com.yusufteker.planora.core.navigation.LocalNavigator
import com.yusufteker.planora.core.navigation.Navigator
import com.yusufteker.planora.core.navigation.Screen
import com.yusufteker.planora.core.navigation.DeepLinkManager
import com.yusufteker.planora.core.preferences.SessionPreferences
import com.yusufteker.planora.core.preferences.ThemePreferences
import com.yusufteker.planora.core.snackbar.SnackbarManager
import com.yusufteker.planora.core.theme.PlanoraTheme
import com.yusufteker.planora.feature.auth.presentation.login.LoginScreen
import com.yusufteker.planora.feature.auth.presentation.login.LoginViewModel
import com.yusufteker.planora.feature.auth.presentation.onboarding.OnboardingScreen
import com.yusufteker.planora.feature.auth.presentation.onboarding.OnboardingViewModel
import com.yusufteker.planora.feature.auth.presentation.register.RegisterScreen
import com.yusufteker.planora.feature.auth.presentation.register.RegisterViewModel
import com.yusufteker.planora.feature.auth.presentation.splash.SplashScreen
import com.yusufteker.planora.feature.auth.presentation.splash.SplashViewModel
import com.yusufteker.planora.feature.home.presentation.create_post.CreatePostScreen
import com.yusufteker.planora.feature.home.presentation.create_post.CreatePostViewModel
import com.yusufteker.planora.feature.home.presentation.event_detail.EventDetailEvent
import com.yusufteker.planora.feature.home.presentation.event_detail.EventDetailScreen
import com.yusufteker.planora.feature.home.presentation.event_detail.EventDetailViewModel
import com.yusufteker.planora.feature.home.presentation.main.MainScreen
import com.yusufteker.planora.feature.home.presentation.pending_posts.PendingPostsScreen
import com.yusufteker.planora.feature.home.presentation.pending_posts.PendingPostsViewModel
import com.yusufteker.planora.feature.home.presentation.plan_room_detail.PlanRoomDetailEvent
import com.yusufteker.planora.feature.home.presentation.plan_room_detail.PlanRoomDetailScreen
import com.yusufteker.planora.feature.home.presentation.plan_room_detail.PlanRoomDetailViewModel
import com.yusufteker.planora.feature.home.presentation.search.SearchUsersScreen
import com.yusufteker.planora.feature.home.presentation.search.SearchUsersViewModel
import com.yusufteker.planora.feature.home.presentation.task_editor.TaskEditorEvent
import com.yusufteker.planora.feature.home.presentation.task_editor.TaskEditorScreen
import com.yusufteker.planora.feature.home.presentation.task_editor.TaskEditorViewModel
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.time.Duration.Companion.milliseconds

    /**
 * Root composable for the Planora application.
 *
 * Sets up:
 * - PlanoraTheme (with automatic dark mode)
 * - Navigation 3 (NavDisplay with user-owned back stack)
 * - Screen routing to feature composables
 */
@OptIn(FlowPreview::class)
@Composable
fun App() {
    val themePreferences = koinInject<ThemePreferences>()
    val isDarkModePref by themePreferences.isDarkMode.collectAsStateWithLifecycle(initialValue = null)
    val themeColorPref by themePreferences.themeColor.collectAsStateWithLifecycle(initialValue = com.yusufteker.planora.core.preferences.ThemeColor.DEFAULT)
    val isDark = isDarkModePref ?: isSystemInDarkTheme()

    PlanoraTheme(themeColor = themeColorPref, darkTheme = isDark) {
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
                val screenName = currentScreen::class.simpleName ?: "UnknownScreen"
                Napier.d("SCREEN: $screenName açıldı")
            }
        }

        val registerFcmTokenUseCase = koinInject<RegisterFcmTokenUseCase>()
        val reminderManager = koinInject<com.yusufteker.planora.core.reminder.ReminderManager>()
        val planRepository = koinInject<com.yusufteker.planora.feature.home.domain.repository.PlanRepository>()
        val processDeepLinkUseCase = koinInject<com.yusufteker.planora.feature.home.domain.use_case.ProcessDeepLinkUseCase>()

        LaunchedEffect(userId) {
            if (userId != null) {
                registerFcmTokenUseCase()
                com.yusufteker.planora.core.utils.NotificationSyncBridge.setSyncHandler {
                    planRepository.fetchMyTasks()
                }
            } else {
                val currentScreen = navigator.backStack.lastOrNull()
                if (currentScreen != null && 
                    currentScreen !is Screen.Splash && 
                    currentScreen !is Screen.Onboarding && 
                    currentScreen !is Screen.Login && 
                    currentScreen !is Screen.Register) {
                    navigator.setRoot(Screen.Login)
                }
            }
        }

        // Observe all tasks and reschedule alarms whenever the task list changes
        LaunchedEffect(userId) {
            if (userId != null) {
                // Performans: debounce ile hızlı ardışık DB değişikliklerinde
                // gereksiz cancel+reschedule döngüsünü önle
                planRepository.observeAllTasks()
                    .debounce(1000L.milliseconds)
                    .collect { tasks ->
                        reminderManager.scheduleAllReminders(tasks)
                    }
            } else {
                // Logged out — cancel all reminders
                reminderManager.cancelAllReminders()
            }
        }

        val deepLinkUrl by DeepLinkManager.deepLinkFlow.collectAsStateWithLifecycle(initialValue = "")
        val currentScreen = navigator.backStack.lastOrNull()
        LaunchedEffect(deepLinkUrl, userId, currentScreen) {
            Napier.d("DEEPLINK DEBUG: deepLinkUrl='$deepLinkUrl', userId='$userId', currentScreen='$currentScreen'")
            if (deepLinkUrl.isNotEmpty() && userId != null && currentScreen == Screen.Main) { // Only handle if logged in
                Napier.d("DEEPLINK DEBUG: Conditions met! Parsing URL with UseCase...")
                when (val result = processDeepLinkUseCase(deepLinkUrl)) {
                    is com.yusufteker.planora.feature.home.domain.use_case.DeepLinkResult.NavigateToEvent -> {
                        navigator.navigate(
                            Screen.EventDetail(
                                eventId = result.eventId,
                                planRoomId = result.planRoomId,
                                sharedTitle = result.sharedTitle,
                                sharedNote = result.sharedNote,
                                sharedDate = result.sharedDate,
                                sharedSender = result.sharedSender
                            )
                        )
                    }
                    is com.yusufteker.planora.feature.home.domain.use_case.DeepLinkResult.NavigateToTask -> {
                        navigator.navigate(
                            Screen.TaskEditor(
                                taskId = result.taskId,
                                sharedTitle = result.sharedTitle,
                                sharedNote = result.sharedNote,
                                sharedDate = result.sharedDate,
                                sharedSender = result.sharedSender
                            )
                        )
                    }
                    is com.yusufteker.planora.feature.home.domain.use_case.DeepLinkResult.NavigateToNote -> {
                        navigator.navigate(
                            Screen.NoteEditor(
                                noteId = result.noteId,
                                sharedNote = result.sharedNote,
                                sharedSender = result.sharedSender
                            )
                        )
                    }
                    is com.yusufteker.planora.feature.home.domain.use_case.DeepLinkResult.NavigateToRoom -> {
                        navigator.navigate(
                            Screen.PlanRoomDetail(
                                roomId = result.roomId
                            )
                        )
                    }
                    com.yusufteker.planora.feature.home.domain.use_case.DeepLinkResult.InvalidOrIgnored -> {
                        Napier.d("DEEPLINK DEBUG: Invalid or Ignored deep link")
                    }
                }
                DeepLinkManager.consumeLink()
            }
        }

        CompositionLocalProvider(LocalNavigator provides navigator) {
            val focusManager = LocalFocusManager.current
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = {
                            focusManager.clearFocus()
                        })
                    }
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

                    entry<Screen.ForgotPassword> {
                        val viewModel = koinViewModel<com.yusufteker.planora.feature.auth.presentation.forgot_password.ForgotPasswordViewModel>()
                        com.yusufteker.planora.feature.auth.presentation.forgot_password.ForgotPasswordScreen(
                            viewModel = viewModel
                        )
                    }

                    // ── Main Graph (Container for Bottom Navigation) ─────────

                    entry<Screen.Main> {
                        MainScreen()
                    }

                    entry<Screen.AiChat> {
                        val viewModel = koinViewModel<com.yusufteker.planora.feature.home.presentation.aichat.AiChatViewModel>(key = vmKey)
                        com.yusufteker.planora.feature.home.presentation.aichat.AiChatScreen(
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
                        val viewModel = koinViewModel<com.yusufteker.planora.feature.home.presentation.follow_list.FollowListViewModel>(key = vmKey)
                        com.yusufteker.planora.feature.home.presentation.follow_list.FollowListScreen(
                            viewModel = viewModel,
                            initialTab = screen.initialTab
                        )
                    }

                    entry<Screen.Profile> { screen ->
                        val viewModel = koinViewModel<com.yusufteker.planora.feature.home.presentation.profile.ProfileViewModel>(
                            key = "${vmKey}_profile_${screen.userId ?: "me"}"
                        )
                        com.yusufteker.planora.feature.home.presentation.profile.ProfileScreen(
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
                            onNavigateToCreateEvent = { roomId -> navigator.navigate(Screen.EventDetail(planRoomId = roomId)) },
                            onNavigateToTaskEditor = { taskId -> navigator.navigate(Screen.TaskEditor(taskId = taskId, planRoomId = screen.roomId)) },
                            onNavigateToEventDetail = { eventId -> navigator.navigate(Screen.EventDetail(eventId = eventId, planRoomId = screen.roomId)) },
                            onNavigateToNoteEditor = { noteId -> navigator.navigate(Screen.NoteEditor(noteId = noteId, planRoomId = screen.roomId)) }
                        )
                    }


                    entry<Screen.TaskEditor> { screen ->
                        val viewModel = koinViewModel<TaskEditorViewModel>(
                            key = "task_editor_${screen.taskId}_${screen.planRoomId}_${screen.parentId}"
                        )
                        LaunchedEffect(screen.taskId, screen.planRoomId, screen.parentId, screen.sharedTitle) {
                            viewModel.onEvent(TaskEditorEvent.OnLoadTask(
                                taskId = screen.taskId, 
                                planRoomId = screen.planRoomId, 
                                parentId = screen.parentId,
                                sharedTitle = screen.sharedTitle,
                                sharedNote = screen.sharedNote,
                                sharedDate = screen.sharedDate,
                                sharedSender = screen.sharedSender
                            ))
                        }
                        TaskEditorScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navigator.pop() },
                            onNavigateToFocus = { id -> navigator.navigate(Screen.Focus(taskId = id)) },
                            onNavigateToCreateNote = { parentId -> navigator.navigate(Screen.NoteEditor(parentId = parentId, planRoomId = screen.planRoomId)) },
                            onNavigateToEditNote = { noteId -> navigator.navigate(Screen.NoteEditor(noteId = noteId, planRoomId = screen.planRoomId)) },
                            onNavigateToPlanRoom = { roomId -> navigator.navigate(Screen.PlanRoomDetail(roomId = roomId)) }
                        )
                    }

                    entry<Screen.NoteEditor> { screen ->
                        val viewModel = koinViewModel<com.yusufteker.planora.feature.home.presentation.note_editor.NoteEditorViewModel>(
                            key = screen.noteId ?: "new_note_${screen.parentId}",
                            parameters = {parametersOf(screen.noteId, screen.planRoomId, screen.parentId) }
                        )
                        androidx.compose.runtime.LaunchedEffect(screen) {
                            viewModel.onEvent(com.yusufteker.planora.feature.home.presentation.note_editor.NoteEditorEvent.OnLoadNote(
                                noteId = screen.noteId, 
                                planRoomId = screen.planRoomId, 
                                parentId = screen.parentId,
                                sharedNote = screen.sharedNote,
                                sharedSender = screen.sharedSender
                            ))
                        }
                        com.yusufteker.planora.feature.home.presentation.note_editor.NoteEditorScreen(
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
                            viewModel.onEvent(EventDetailEvent.OnLoadEvent(
                                eventId = screen.eventId, 
                                planRoomId = screen.planRoomId,
                                sharedTitle = screen.sharedTitle,
                                sharedNote = screen.sharedNote,
                                sharedDate = screen.sharedDate,
                                sharedSender = screen.sharedSender
                            ))
                        }
                        EventDetailScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navigator.pop() },
                            onNavigateToCreateTask = { parentId -> navigator.navigate(Screen.TaskEditor(parentId = parentId, planRoomId = screen.planRoomId)) },
                            onNavigateToCreateNote = { parentId -> navigator.navigate(Screen.NoteEditor(parentId = parentId, planRoomId = screen.planRoomId)) },
                            onNavigateToEditTask = { taskId -> navigator.navigate(Screen.TaskEditor(taskId = taskId, planRoomId = screen.planRoomId)) },
                            onNavigateToEditNote = { noteId -> navigator.navigate(Screen.NoteEditor(noteId = noteId, planRoomId = screen.planRoomId)) },
                            onNavigateToPlanRoom = { roomId -> navigator.navigate(Screen.PlanRoomDetail(roomId = roomId)) }
                        )
                    }

                    entry<Screen.Focus> { screen ->
                        val viewModel = koinViewModel<com.yusufteker.planora.feature.home.presentation.focus.FocusViewModel>(key = vmKey)
                        com.yusufteker.planora.feature.home.presentation.focus.FocusScreen(
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
