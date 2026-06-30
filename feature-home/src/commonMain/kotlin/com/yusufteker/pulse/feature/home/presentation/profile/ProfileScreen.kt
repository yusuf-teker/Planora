package com.yusufteker.pulse.feature.home.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yusufteker.pulse.core.base.CollectEffect
import com.yusufteker.pulse.core.navigation.LocalMainNavigator
import org.jetbrains.compose.resources.stringResource
import pulse.core.generated.resources.Res
import pulse.core.generated.resources.*
import androidx.compose.foundation.Image
import kotlin.collections.getOrNull
import com.yusufteker.pulse.core.ui.components.AvatarImage
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import com.yusufteker.pulse.core.navigation.Screen.MainDestination

/**
 * Profile screen composable.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel
) {
    val mainNavigator = LocalMainNavigator.current
    val state by viewModel.state.collectAsStateWithLifecycle()

    val rootNavigator = com.yusufteker.pulse.core.navigation.LocalNavigator.current

    viewModel.effect.CollectEffect { effect ->
        when (effect) {
            is ProfileEffect.NavigateBack -> mainNavigator.pop()
            is ProfileEffect.NavigateToLogin -> rootNavigator.setRoot(com.yusufteker.pulse.core.navigation.Screen.Login)
        }
    }

    androidx.lifecycle.compose.LifecycleResumeEffect(Unit) {
        io.github.aakira.napier.Napier.d(
            tag = "Screen",
            message = { ">>> ProfileScreen resumed | state.name=${state.name}" })
        viewModel.onEvent(ProfileEvent.LoadProfile(state.profileId))
        
        onPauseOrDispose {
        }
    }

    Scaffold(
        topBar = {
            if (!state.isEditing && state.isMyProfile) {
                TopAppBar(
                    title = {},
                    actions = {},
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .safeContentPadding()
        ) {

            val avatarList = List(10) { "avatar_${it + 1}" }
            val currentAvatarId = if (state.avatarId in avatarList) state.avatarId else "avatar_1"

            if (state.isEditing) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(Res.string.profile_choose_avatar),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(5),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth().height(180.dp)
                    ) {
                        items(avatarList.size) { index ->
                            val avatarName = avatarList[index]
                            val isSelected = state.avatarId == avatarName
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .border(
                                        width = if (isSelected) 3.dp else 0.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        viewModel.onEvent(ProfileEvent.AvatarSelected(avatarName))
                                    }
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                AvatarImage(
                                    avatarId = avatarName,
                                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    androidx.compose.material3.OutlinedTextField(
                        value = state.name,
                        onValueChange = { /* Disabled by user request */ },
                        label = { Text(stringResource(Res.string.profile_username)) },
                        singleLine = true,
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    androidx.compose.material3.Button(
                        onClick = { viewModel.onEvent(ProfileEvent.SaveClicked) },
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    ) {
                        Text(stringResource(Res.string.save))
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        AvatarImage(
                            avatarId = currentAvatarId,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = state.name,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    if (state.username.isNotBlank()) {
                        Text(
                            text = "@${state.username}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    if (state.isLoggedIn) {
                        // Stats Row
                        androidx.compose.foundation.layout.Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = state.postsCount.toString(), style = MaterialTheme.typography.titleLarge)
                                Text(text = "Posts", style = MaterialTheme.typography.bodyMedium)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = state.followersCount.toString(),
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Text(text = "Followers", style = MaterialTheme.typography.bodyMedium)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = state.followingCount.toString(),
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Text(text = "Following", style = MaterialTheme.typography.bodyMedium)
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        if (state.isMyProfile) {
                            androidx.compose.foundation.layout.Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Add Friend Button
                                Box(
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(androidx.compose.ui.graphics.Color.Green.copy(alpha = 0.2f))
                                        .clickable { rootNavigator.navigate(com.yusufteker.pulse.core.navigation.Screen.SearchUsers) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Filled.PersonAdd,
                                        contentDescription = "Add Friend",
                                        tint = androidx.compose.ui.graphics.Color.Green
                                    )
                                }
                                
                                // Settings Button
                                Box(
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { mainNavigator.navigate(MainDestination.Settings) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Filled.Settings,
                                        contentDescription = "Settings",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                
                                // Logout Button
                                Box(
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(androidx.compose.ui.graphics.Color.Red.copy(alpha = 0.2f))
                                        .clickable { viewModel.onEvent(ProfileEvent.LogoutClicked) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Logout,
                                        contentDescription = "Logout",
                                        tint = androidx.compose.ui.graphics.Color.Red
                                    )
                                }
                            }
                        } else {
                            androidx.compose.material3.Button(
                                onClick = { viewModel.onEvent(ProfileEvent.ToggleFollowClicked) }
                            ) {
                                Text(text = if (state.isFollowedByMe) "Unfollow" else "Follow")
                            }
                        }
                    } else {
                        // Not logged in
                        Spacer(modifier = Modifier.height(16.dp))
                        androidx.compose.material3.Button(onClick = { rootNavigator.navigate(com.yusufteker.pulse.core.navigation.Screen.Login) }) {
                            Text("Giriş Yap")
                        }
                    }
                }
            }
        }
    }
}