package com.yusufteker.pulse

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.yusufteker.pulse.core.navigation.LocalNavigator
import com.yusufteker.pulse.core.navigation.Navigator
import com.yusufteker.pulse.core.navigation.Screen
import com.yusufteker.pulse.core.preferences.ThemePreferences
import com.yusufteker.pulse.core.theme.PulseTheme
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
import com.yusufteker.pulse.feature.home.presentation.main.MainScreen
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/**
 * Root composable for the Pulse application.
 *
 * Sets up:
 * - PulseTheme (with automatic dark mode)
 * - Navigation 3 (NavDisplay with user-owned back stack)
 * - Screen routing to feature composables
 */
@Composable
fun App() {
    val themePreferences = koinInject<ThemePreferences>()
    val isDarkModePref by themePreferences.isDarkMode.collectAsState(initial = null)
    val themeColorPref by themePreferences.themeColor.collectAsState(initial = com.yusufteker.pulse.core.preferences.ThemeColor.BLUE)
    val isDark = isDarkModePref ?: isSystemInDarkTheme()

    PulseTheme(themeColor = themeColorPref, darkTheme = isDark) {
        val snackbarManager = koinInject<com.yusufteker.pulse.core.snackbar.SnackbarManager>()
        val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }
        val activeMessage by snackbarManager.messages.collectAsState()

        androidx.compose.runtime.LaunchedEffect(activeMessage) {
            activeMessage?.let { msg ->
                snackbarHostState.showSnackbar(msg.message)
                snackbarManager.clearMessage(msg.id)
            }
        }

        val backStack = remember { mutableStateListOf<Screen>(Screen.Splash) }
        val navigator = remember { Navigator(backStack) }

        CompositionLocalProvider(LocalNavigator provides navigator) {
            androidx.compose.material3.Scaffold(
                snackbarHost = { androidx.compose.material3.SnackbarHost(snackbarHostState) },
                modifier = androidx.compose.ui.Modifier.fillMaxSize()
            ) { _ -> // paddingValues kullanılmıyor, iç sayfalarda insets kendileri hesaplanıyor
                NavDisplay(
                    modifier = androidx.compose.ui.Modifier.fillMaxSize(),
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

                    entry<Screen.PendingPosts> {
                        val viewModel = koinViewModel<com.yusufteker.pulse.feature.home.presentation.pending_posts.PendingPostsViewModel>()
                        com.yusufteker.pulse.feature.home.presentation.pending_posts.PendingPostsScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navigator.pop() },
                            onNavigateToEdit = { postId -> navigator.navigate(Screen.CreatePost(postId)) }
                        )
                    }

                    entry<Screen.CreatePost> { screen ->
                        val viewModel = koinViewModel<CreatePostViewModel>(
                            parameters = { org.koin.core.parameter.parametersOf(screen.postId) }
                        )
                        CreatePostScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navigator.pop() }
                        )
                    }
                }
            )
            } // Close Scaffold
        }
    }
}
