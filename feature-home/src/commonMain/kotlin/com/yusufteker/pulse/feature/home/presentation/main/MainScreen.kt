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
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import com.yusufteker.pulse.core.navigation.Screen.MainDestination
import com.yusufteker.pulse.core.navigation.Navigator
import com.yusufteker.pulse.core.navigation.LocalMainNavigator
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
import com.yusufteker.pulse.feature.home.presentation.settings.SettingsScreen
import com.yusufteker.pulse.feature.home.presentation.settings.SettingsViewModel
import org.koin.compose.viewmodel.koinViewModel

import com.yusufteker.pulse.core.theme.LocalIsDarkTheme

import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.listSaver

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

    val isDark = LocalIsDarkTheme.current

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
                tonalElevation = if (isDark) 0.dp else 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .height(60.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Home Tab
                    val isHomeSelected = currentDestination is MainDestination.Home
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { navigateToTab(MainDestination.Home) }
                            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 8.dp)
                    ) {
                        Icon(
                            imageVector = if (isHomeSelected) Icons.Filled.Home else Icons.Outlined.Home,
                            contentDescription = "Home",
                            tint = if (isHomeSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (isHomeSelected) {
                            Text(
                                text = stringResource(Res.string.tab_home),
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Social Tab
                    val isSocialSelected = currentDestination is MainDestination.Social
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { navigateToTab(MainDestination.Social) }
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = if (isSocialSelected) Icons.Filled.People else Icons.Outlined.People,
                            contentDescription = "Social",
                            tint = if (isSocialSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (isSocialSelected) {
                            Text(
                                text = stringResource(Res.string.tab_social),
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Profile Tab
                    val isProfileSelected = currentDestination is MainDestination.Profile
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { navigateToTab(MainDestination.Profile) }
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = if (isProfileSelected) Icons.Filled.Person else Icons.Outlined.Person,
                            contentDescription = "Profile",
                            tint = if (isProfileSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (isProfileSelected) {
                            Text(
                                text = stringResource(Res.string.tab_profile),
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Settings Tab
                    val isSettingsSelected = currentDestination is MainDestination.Settings
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { navigateToTab(MainDestination.Settings) }
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = if (isSettingsSelected) Icons.Filled.Settings else Icons.Outlined.Settings,
                            contentDescription = "Settings",
                            tint = if (isSettingsSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (isSettingsSelected) {
                            Text(
                                text = stringResource(Res.string.tab_settings),
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        CompositionLocalProvider(LocalMainNavigator provides navigator) {
            NavDisplay(
                backStack = navigator.backStack,
                onBack = { navigator.pop() },
                modifier = Modifier.padding(bottom = paddingValues.calculateBottomPadding()),
                entryProvider = entryProvider {
                    entry<MainDestination.Home> {
                        val viewModel = koinViewModel<HomeViewModel>()
                        HomeScreen(viewModel = viewModel)
                    }

                    entry<MainDestination.Social> {
                        val viewModel = koinViewModel<SocialViewModel>()
                        SocialScreen(viewModel = viewModel)
                    }

                    entry<MainDestination.Profile> {
                        val viewModel = koinViewModel<ProfileViewModel>()
                        ProfileScreen(viewModel = viewModel)
                    }

                    entry<MainDestination.Settings> {
                        val viewModel = koinViewModel<SettingsViewModel>()
                        SettingsScreen(viewModel = viewModel)
                    }
                }
            )
        }
    }
}
