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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material.icons.filled.Edit
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
import com.yusufteker.pulse.core.navigation.Screen
import org.jetbrains.compose.resources.stringResource
import pulsy.core.generated.resources.Res
import pulsy.core.generated.resources.*
import androidx.compose.foundation.Image
import com.yusufteker.pulse.core.ui.components.AvatarImage
import com.yusufteker.pulse.core.ui.components.rememberAppImagePickerLauncher
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import com.yusufteker.pulse.core.navigation.Screen.MainDestination
import androidx.compose.ui.graphics.Brush
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton

/**
 * Profile screen composable.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    userId: Int? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val mainNavigator = if (userId == null) LocalMainNavigator.current else null
    val state by viewModel.state.collectAsStateWithLifecycle()

    val rootNavigator = com.yusufteker.pulse.core.navigation.LocalNavigator.current

    viewModel.effect.CollectEffect { effect ->
        when (effect) {
            is ProfileEffect.NavigateBack -> {
                if (state.isMyProfile) {
                    mainNavigator?.pop()
                } else {
                    rootNavigator.pop()
                }
            }
            is ProfileEffect.NavigateToLogin -> rootNavigator.setRoot(Screen.Login)
            is ProfileEffect.NavigateToFollowList -> rootNavigator.navigate(Screen.FollowList(effect.tab))
        }
    }

    androidx.lifecycle.compose.LifecycleResumeEffect(userId) {
        io.github.aakira.napier.Napier.d(
            tag = "Screen",
            message = { ">>> ProfileScreen resumed | state.name=${state.name} | userId=$userId" })
        viewModel.onEvent(ProfileEvent.LoadProfile(userId))
        
        onPauseOrDispose {
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .safeContentPadding()
        ) {
            val imagePickerLauncher = rememberAppImagePickerLauncher(
                onResult = { imageBytes ->
                    if (imageBytes != null) {
                        viewModel.onEvent(ProfileEvent.ProfileImageSelected(imageBytes))
                    }
                }
            )

            val avatarList = List(10) { "avatar_${it + 1}" }
            val currentAvatarId = if (state.avatarId in avatarList) state.avatarId else "avatar_1"

            Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 0.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                ) {
                    if (!state.isMyProfile) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { viewModel.onEvent(ProfileEvent.BackClicked) }) {
                                Icon(
                                    Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = "Back",
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "@${state.username}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }

                    if (state.isLoggedIn) {
                        // Main Profile Card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(24.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(24.dp)
                                )
                                .padding(20.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Centered Avatar with gradient ring border
                                Box(
                                    modifier = Modifier
                                        .size(96.dp)
                                        .clip(CircleShape)
                                        .clickable(enabled = state.isMyProfile) { 
                                            imagePickerLauncher.launch()
                                        }
                                        .border(
                                            width = 3.dp,
                                            brush = Brush.linearGradient(
                                                colors = listOf(
                                                    MaterialTheme.colorScheme.primary,
                                                    MaterialTheme.colorScheme.tertiary,
                                                    MaterialTheme.colorScheme.secondary
                                                )
                                            ),
                                            shape = CircleShape
                                        )
                                        .background(MaterialTheme.colorScheme.surface)
                                        .padding(3.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AvatarImage(
                                        avatarId = currentAvatarId,
                                        profileImageUrl = state.profileImageUrl,
                                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                                    )
                                    if (state.isUploadingImage) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(CircleShape)
                                                .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.4f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(32.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Name & Username
                                Text(
                                    text = state.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )

                                if (state.username.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "@${state.username}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                                    )
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                androidx.compose.material3.HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                    thickness = 1.dp
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                // Stats Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Posts
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = state.postsCount.toString(),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = stringResource(Res.string.profile_posts_label).uppercase(),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                                        )
                                    }

                                    // Divider
                                    Box(
                                        modifier = Modifier
                                            .width(1.dp)
                                            .height(24.dp)
                                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                    )

                                    // Followers
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable(enabled = state.isMyProfile) {
                                                viewModel.onEvent(ProfileEvent.NavigateToFollowList(0))
                                            }
                                            .padding(vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = state.followersCount.toString(),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = stringResource(Res.string.profile_followers_label).uppercase(),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                                        )
                                    }

                                    // Divider
                                    Box(
                                        modifier = Modifier
                                            .width(1.dp)
                                            .height(24.dp)
                                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                    )

                                    // Following
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable(enabled = state.isMyProfile) {
                                                viewModel.onEvent(ProfileEvent.NavigateToFollowList(1))
                                            }
                                            .padding(vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = state.followingCount.toString(),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = stringResource(Res.string.profile_following_label).uppercase(),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (state.isMyProfile) {
                            // Quick Action Buttons
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Add Friend pill button
                                androidx.compose.material3.Button(
                                    onClick = { rootNavigator.navigate(Screen.SearchUsers) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.PersonAdd,
                                        contentDescription = stringResource(Res.string.action_add_friend),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(Res.string.action_add_friend),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                    )
                                }

                                // Settings button capsule
                                Box(
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                        .border(
                                            width = 1.dp,
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                        .clickable { mainNavigator?.navigate(MainDestination.Settings) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Filled.Settings,
                                        contentDescription = stringResource(Res.string.settings_title),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // Logout button capsule
                                Box(
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f))
                                        .border(
                                            width = 1.dp,
                                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.3f),
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                        .clickable { viewModel.onEvent(ProfileEvent.LogoutClicked) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Logout,
                                        contentDescription = stringResource(Res.string.action_logout),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }

                            // PENDING REQUESTS SECTION
                            if (state.pendingRequests.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(24.dp))

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(24.dp)
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                            shape = RoundedCornerShape(24.dp)
                                        )
                                        .padding(16.dp)
                                ) {
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = stringResource(Res.string.title_notifications),
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onBackground
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .size(20.dp)
                                                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = state.pendingRequests.size.toString(),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onPrimary,
                                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                                    )
                                                }
                                            }
                                            if (state.pendingRequests.size > 3) {
                                                androidx.compose.material3.TextButton(
                                                    onClick = { viewModel.onEvent(ProfileEvent.NavigateToFollowList(2)) },
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                                ) {
                                                    Text(
                                                        stringResource(Res.string.action_see_all),
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(16.dp))

                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            state.pendingRequests.take(3).forEach { request ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(
                                                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                                            shape = RoundedCornerShape(16.dp)
                                                        )
                                                        .border(
                                                            width = 1.dp,
                                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                                            shape = RoundedCornerShape(16.dp)
                                                        )
                                                        .padding(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    AvatarImage(
                                                        avatarId = request.requesterAvatarId,
                                                        profileImageUrl = request.requesterProfileImageUrl,
                                                        modifier = Modifier
                                                            .size(44.dp)
                                                            .clip(CircleShape)
                                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                                    )

                                                    Spacer(modifier = Modifier.width(12.dp))

                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = request.requesterName,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onBackground
                                                        )
                                                        if (request.requesterUsername.isNotBlank()) {
                                                            Spacer(modifier = Modifier.height(2.dp))
                                                            Text(
                                                                text = "@${request.requesterUsername}",
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                                            )
                                                        }
                                                    }

                                                    Row(
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        androidx.compose.material3.Button(
                                                            onClick = { viewModel.onEvent(ProfileEvent.AcceptRequestClicked(request.id)) },
                                                            shape = RoundedCornerShape(12.dp),
                                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                                            modifier = Modifier.height(34.dp),
                                                            colors = ButtonDefaults.buttonColors(
                                                                containerColor = MaterialTheme.colorScheme.primary
                                                            )
                                                        ) {
                                                            Text(
                                                                text = stringResource(Res.string.action_accept),
                                                                style = MaterialTheme.typography.labelMedium,
                                                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                                            )
                                                        }
                                                        androidx.compose.material3.OutlinedButton(
                                                            onClick = { viewModel.onEvent(ProfileEvent.RejectRequestClicked(request.id)) },
                                                            shape = RoundedCornerShape(12.dp),
                                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                                            modifier = Modifier.height(34.dp),
                                                            colors = ButtonDefaults.outlinedButtonColors(
                                                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                                            ),
                                                            border = androidx.compose.foundation.BorderStroke(
                                                                width = 1.dp,
                                                                color = MaterialTheme.colorScheme.outlineVariant
                                                            )
                                                        ) {
                                                            Text(
                                                                text = stringResource(Res.string.action_decline),
                                                                style = MaterialTheme.typography.labelMedium,
                                                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // PENDING CALENDAR REQUESTS SECTION
                            if (state.pendingCalendarRequests.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(16.dp))

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(24.dp)
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                            shape = RoundedCornerShape(24.dp)
                                        )
                                        .padding(16.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = stringResource(Res.string.profile_calendar_access_requests),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))

                                        state.pendingCalendarRequests.forEach { request ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 8.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    AvatarImage(
                                                        avatarId = request.requesterAvatarId,
                                                        profileImageUrl = request.requesterProfileImageUrl,
                                                        modifier = Modifier
                                                            .size(40.dp)
                                                            .clip(CircleShape)
                                                    )
                                                    Column {
                                                        Text(
                                                            text = request.requesterName,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onBackground
                                                        )
                                                        Text(
                                                            text = "@${request.requesterUsername}",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                                        )
                                                    }
                                                }

                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    androidx.compose.material3.Button(
                                                        onClick = { viewModel.onEvent(ProfileEvent.AcceptCalendarRequestClicked(request.id)) },
                                                        shape = RoundedCornerShape(12.dp),
                                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                                        modifier = Modifier.height(34.dp),
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = MaterialTheme.colorScheme.primary
                                                        )
                                                    ) {
                                                        Text(
                                                            text = stringResource(Res.string.action_accept),
                                                            style = MaterialTheme.typography.labelMedium,
                                                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                                        )
                                                    }
                                                    androidx.compose.material3.OutlinedButton(
                                                        onClick = { viewModel.onEvent(ProfileEvent.RejectCalendarRequestClicked(request.id)) },
                                                        shape = RoundedCornerShape(12.dp),
                                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                                        modifier = Modifier.height(34.dp),
                                                        colors = ButtonDefaults.outlinedButtonColors(
                                                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                                        ),
                                                        border = androidx.compose.foundation.BorderStroke(
                                                            width = 1.dp,
                                                            color = MaterialTheme.colorScheme.outlineVariant
                                                        )
                                                    ) {
                                                        Text(
                                                            text = stringResource(Res.string.action_decline),
                                                            style = MaterialTheme.typography.labelMedium,
                                                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // CALENDAR GRANTS SECTION (Users who can see my calendar)
                            if (state.calendarGrants.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(16.dp))

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(24.dp)
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                            shape = RoundedCornerShape(24.dp)
                                        )
                                        .padding(16.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = stringResource(Res.string.profile_calendar_viewers),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))

                                        state.calendarGrants.forEach { grant ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 8.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    AvatarImage(
                                                        avatarId = grant.avatarId,
                                                        profileImageUrl = grant.profileImageUrl,
                                                        modifier = Modifier
                                                            .size(40.dp)
                                                            .clip(CircleShape)
                                                    )
                                                    Column {
                                                        Text(
                                                            text = grant.name,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onBackground
                                                        )
                                                        Text(
                                                            text = "@${grant.username}",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                                        )
                                                    }
                                                }

                                                androidx.compose.material3.OutlinedButton(
                                                    onClick = { viewModel.onEvent(ProfileEvent.RevokeCalendarGrantClicked(grant.userId)) },
                                                    shape = RoundedCornerShape(12.dp),
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                                    modifier = Modifier.height(34.dp),
                                                    colors = ButtonDefaults.outlinedButtonColors(
                                                        contentColor = MaterialTheme.colorScheme.error
                                                    ),
                                                    border = androidx.compose.foundation.BorderStroke(
                                                        width = 1.dp,
                                                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                                                    )
                                                ) {
                                                    Text(
                                                        text = stringResource(Res.string.cancel),
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            // Follow Action Button (Foreign Profile)
                            Spacer(modifier = Modifier.height(16.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                val buttonText = when {
                                    state.isFollowedByMe -> stringResource(Res.string.action_unfollow)
                                    state.followRequestStatus == "PENDING" -> stringResource(Res.string.action_requested)
                                    else -> stringResource(Res.string.action_follow)
                                }

                                val isPending = state.followRequestStatus == "PENDING"
                                val isFollowing = state.isFollowedByMe

                                androidx.compose.material3.Button(
                                    onClick = { viewModel.onEvent(ProfileEvent.ToggleFollowClicked) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = when {
                                        isPending -> ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        isFollowing -> ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                        else -> ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        )
                                    },
                                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                                ) {
                                    Text(
                                        text = buttonText,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                    )
                                }
                            }

                            // Calendar Access Action Button (Foreign Profile)
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                val calendarButtonText = when {
                                    state.calendarAccessStatus == "ACCEPTED" -> stringResource(Res.string.profile_leave_calendar)
                                    state.calendarAccessStatus == "PENDING" -> stringResource(Res.string.profile_calendar_request_sent)
                                    else -> stringResource(Res.string.profile_request_calendar_access)
                                }

                                val isCalendarAccepted = state.calendarAccessStatus == "ACCEPTED"
                                val isCalendarPending = state.calendarAccessStatus == "PENDING"

                                androidx.compose.material3.Button(
                                    onClick = { viewModel.onEvent(ProfileEvent.RequestCalendarAccessClicked) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = when {
                                        isCalendarAccepted -> ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                        isCalendarPending -> ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        else -> ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    },
                                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                                ) {
                                    Text(
                                        text = calendarButtonText,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                    )
                                }
                            }
                        }
                    } else {
                        // Premium non-logged in state banner
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(28.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(28.dp)
                                )
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(CircleShape)
                                        .border(
                                            width = 3.dp,
                                            brush = Brush.linearGradient(
                                                colors = listOf(
                                                    MaterialTheme.colorScheme.outlineVariant,
                                                    MaterialTheme.colorScheme.outline
                                                )
                                            ),
                                            shape = CircleShape
                                        )
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .padding(4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AvatarImage(
                                        avatarId = currentAvatarId,
                                        profileImageUrl = state.profileImageUrl,
                                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                                    )
                                }
                                Spacer(modifier = Modifier.height(20.dp))
                                Text(
                                    text = state.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stringResource(Res.string.profile_guest_login_prompt),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(28.dp))
                                androidx.compose.material3.Button(
                                    onClick = { rootNavigator.navigate(Screen.Login) },
                                    modifier = Modifier.fillMaxWidth().height(50.dp),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Text(
                                        text = stringResource(Res.string.action_login),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                    )
                                }
                        }
                    }
                }
            }
        }
    }
}