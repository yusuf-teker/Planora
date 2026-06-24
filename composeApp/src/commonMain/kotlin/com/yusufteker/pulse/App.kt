package com.yusufteker.pulse

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.yusufteker.pulse.core.navigation.Screen
import com.yusufteker.pulse.core.preferences.ThemePreferences
import com.yusufteker.pulse.core.theme.PulseTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.isSystemInDarkTheme
import org.koin.compose.koinInject
import com.yusufteker.pulse.feature.auth.presentation.login.LoginScreen
import com.yusufteker.pulse.feature.auth.presentation.login.LoginViewModel
import com.yusufteker.pulse.feature.auth.presentation.onboarding.OnboardingScreen
import com.yusufteker.pulse.feature.auth.presentation.onboarding.OnboardingViewModel
import com.yusufteker.pulse.feature.auth.presentation.register.RegisterScreen
import com.yusufteker.pulse.feature.auth.presentation.register.RegisterViewModel
import com.yusufteker.pulse.feature.auth.presentation.splash.SplashScreen
import com.yusufteker.pulse.feature.auth.presentation.splash.SplashViewModel
import com.yusufteker.pulse.feature.home.presentation.home.HomeScreen
import com.yusufteker.pulse.feature.home.presentation.home.HomeViewModel
import com.yusufteker.pulse.feature.home.presentation.profile.ProfileScreen
import com.yusufteker.pulse.feature.home.presentation.profile.ProfileViewModel
import com.yusufteker.pulse.feature.home.presentation.settings.SettingsScreen
import com.yusufteker.pulse.feature.home.presentation.settings.SettingsViewModel
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
    val isDark = isDarkModePref ?: isSystemInDarkTheme()

    PulseTheme(darkTheme = isDark) {
        val backStack = remember { mutableStateListOf<Screen>(Screen.Splash) }

        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            entryProvider = entryProvider {
                // ── Splash Graph ─────────────────────────
                entry<Screen.Splash> {
                    val viewModel = koinViewModel<SplashViewModel>()
                    SplashScreen(
                        viewModel = viewModel,
                        onNavigateToOnboarding = {
                            backStack.clear()
                            backStack.add(Screen.Onboarding)
                        },
                        onNavigateToHome = {
                            backStack.clear()
                            backStack.add(Screen.Home)
                        }
                    )
                }

                entry<Screen.Onboarding> {
                    val viewModel = koinViewModel<OnboardingViewModel>()
                    OnboardingScreen(
                        viewModel = viewModel,
                        onNavigateToLogin = {
                            backStack.clear()
                            backStack.add(Screen.Login)
                        }
                    )
                }

                // ── Auth Graph ───────────────────────────
                entry<Screen.Login> {
                    val viewModel = koinViewModel<LoginViewModel>()
                    LoginScreen(
                        viewModel = viewModel,
                        onNavigateToHome = {
                            backStack.clear()
                            backStack.add(Screen.Home)
                        },
                        onNavigateToRegister = {
                            backStack.add(Screen.Register)
                        }
                    )
                }

                entry<Screen.Register> {
                    val viewModel = koinViewModel<RegisterViewModel>()
                    RegisterScreen(
                        viewModel = viewModel,
                        onNavigateToHome = {
                            backStack.clear()
                            backStack.add(Screen.Home)
                        },
                        onNavigateBack = {
                            backStack.removeLastOrNull()
                        }
                    )
                }

                // ── Home Graph ───────────────────────────
                entry<Screen.Home> {
                    val viewModel = koinViewModel<HomeViewModel>()
                    HomeScreen(
                        viewModel = viewModel,
                        onNavigateToProfile = {
                            backStack.add(Screen.Profile)
                        },
                        onNavigateToSettings = {
                            backStack.add(Screen.Settings)
                        }
                    )
                }

                entry<Screen.Profile> {
                    val viewModel = koinViewModel<ProfileViewModel>()
                    ProfileScreen(
                        viewModel = viewModel,
                        onNavigateBack = {
                            backStack.removeLastOrNull()
                        }
                    )
                }

                entry<Screen.Settings> {
                    val viewModel = koinViewModel<SettingsViewModel>()
                    SettingsScreen(
                        viewModel = viewModel,
                        onNavigateBack = {
                            backStack.removeLastOrNull()
                        },
                        onNavigateToLogin = {
                            backStack.clear()
                            backStack.add(Screen.Login)
                        }
                    )
                }
            }
        )
    }
}
