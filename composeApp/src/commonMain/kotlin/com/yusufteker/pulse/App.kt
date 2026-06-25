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
import com.yusufteker.pulse.feature.home.presentation.main.MainScreen
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
        val backStack = remember { mutableStateListOf<Screen>(Screen.Splash) }

        NavDisplay(
            backStack = backStack,
            onBack = { 
                if (backStack.size > 1) {
                    backStack.removeLastOrNull() 
                }
            },
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
                            backStack.add(Screen.Main)
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
                            backStack.add(Screen.Main)
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
                            backStack.add(Screen.Main)
                        },
                        onNavigateBack = {
                            backStack.removeLastOrNull()
                        }
                    )
                }

                // ── Main Graph (Container for Bottom Navigation) ─────────
                entry<Screen.Main> {
                    MainScreen(
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
