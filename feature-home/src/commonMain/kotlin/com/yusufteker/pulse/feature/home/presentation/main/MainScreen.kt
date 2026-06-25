package com.yusufteker.pulse.feature.home.presentation.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.yusufteker.pulse.core.navigation.Screen.MainDestination
import org.jetbrains.compose.resources.stringResource
import pulse.core.generated.resources.Res
import pulse.core.generated.resources.*
import pulse.core.generated.resources.tab_settings
import com.yusufteker.pulse.feature.home.presentation.home.HomeScreen
import com.yusufteker.pulse.feature.home.presentation.home.HomeViewModel
import com.yusufteker.pulse.feature.home.presentation.profile.ProfileScreen
import com.yusufteker.pulse.feature.home.presentation.profile.ProfileViewModel
import com.yusufteker.pulse.feature.home.presentation.settings.SettingsScreen
import com.yusufteker.pulse.feature.home.presentation.settings.SettingsViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun MainScreen(
    onNavigateToLogin: () -> Unit
) {
    // Nested back stack for the bottom navigation
    val backStack = remember { mutableStateListOf<MainDestination>(MainDestination.Home) }

    val currentDestination = backStack.lastOrNull() ?: MainDestination.Home

    val navigateToTab: (MainDestination) -> Unit = { destination ->
        if (currentDestination != destination) {
            backStack.clear()
            backStack.add(MainDestination.Home)
            if (destination != MainDestination.Home) {
                backStack.add(destination)
            }
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                // Home Tab
                val isHomeSelected = currentDestination is MainDestination.Home
                NavigationBarItem(
                    selected = isHomeSelected,
                    onClick = { navigateToTab(MainDestination.Home) },
                    icon = {
                        Icon(
                            imageVector = if (isHomeSelected) Icons.Filled.Home else Icons.Outlined.Home,
                            contentDescription = "Home"
                        )
                    },
                    label = { Text(stringResource(Res.string.tab_home)) }
                )

                // Profile Tab
                val isProfileSelected = currentDestination is MainDestination.Profile
                NavigationBarItem(
                    selected = isProfileSelected,
                    onClick = { navigateToTab(MainDestination.Profile) },
                    icon = {
                        Icon(
                            imageVector = if (isProfileSelected) Icons.Filled.Person else Icons.Outlined.Person,
                            contentDescription = "Profil"
                        )
                    },
                    label = { Text(stringResource(Res.string.tab_profile)) }
                )

                // Settings Tab
                val isSettingsSelected = currentDestination is MainDestination.Settings
                NavigationBarItem(
                    selected = isSettingsSelected,
                    onClick = { navigateToTab(MainDestination.Settings) },
                    icon = {
                        Icon(
                            imageVector = if (isSettingsSelected) Icons.Filled.Settings else Icons.Outlined.Settings,
                            contentDescription = "Ayarlar"
                        )
                    },
                    label = { Text(stringResource(Res.string.tab_settings)) }
                )
            }
        }
    ) { paddingValues ->
        NavDisplay(
            backStack = backStack,
            onBack = { 
                if (backStack.size > 1) {
                    backStack.removeLastOrNull() 
                }
            },
            modifier = Modifier.padding(paddingValues),
            entryProvider = entryProvider {
                entry<MainDestination.Home> {
                    val viewModel = koinViewModel<HomeViewModel>()
                    HomeScreen(
                        viewModel = viewModel,
                        onNavigateToProfile = { navigateToTab(MainDestination.Profile) },
                        onNavigateToSettings = { navigateToTab(MainDestination.Settings) }
                    )
                }

                entry<MainDestination.Profile> {
                    val viewModel = koinViewModel<ProfileViewModel>()
                    ProfileScreen(
                        viewModel = viewModel,
                        onNavigateBack = {
                            backStack.removeLastOrNull()
                        }
                    )
                }

                entry<MainDestination.Settings> {
                    val viewModel = koinViewModel<SettingsViewModel>()
                    SettingsScreen(
                        viewModel = viewModel,
                        onNavigateBack = {
                            backStack.removeLastOrNull()
                        },
                        onNavigateToLogin = onNavigateToLogin
                    )
                }
            }
        )
    }
}
