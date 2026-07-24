package com.yusufteker.planora.feature.home.presentation.follow_list

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yusufteker.planora.core.base.CollectEffect
import com.yusufteker.planora.core.navigation.LocalNavigator
import com.yusufteker.planora.core.navigation.Screen
import com.yusufteker.planora.core.ui.components.AvatarImage
import com.yusufteker.planora.feature.home.presentation.components.EmptyStateComponent
import com.yusufteker.planora.shared.api.FollowRequestResponse
import com.yusufteker.planora.shared.api.UserProfileResponse
import org.jetbrains.compose.resources.stringResource
import planora.core.generated.resources.Res
import planora.core.generated.resources.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowListScreen(
    viewModel: FollowListViewModel,
    initialTab: Int = 0
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val navigator = LocalNavigator.current

    LaunchedEffect(Unit) {
        viewModel.onEvent(FollowListEvent.TabSelected(initialTab))
        viewModel.onEvent(FollowListEvent.LoadAll)
    }

    viewModel.effect.CollectEffect { effect ->
        when (effect) {
            is FollowListEffect.NavigateBack -> navigator.pop()
        }
    }

    val tabTitles = listOf(
        stringResource(Res.string.tab_followers),
        stringResource(Res.string.tab_following),
        stringResource(Res.string.tab_requests)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.title_follow_list)) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.onEvent(FollowListEvent.BackClicked) }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(Res.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab Row
            TabRow(
                selectedTabIndex = state.selectedTab,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                tabTitles.forEachIndexed { index, title ->
                    val requestCount = if (index == 2) state.requests.size else 0
                    Tab(
                        selected = state.selectedTab == index,
                        onClick = { viewModel.onEvent(FollowListEvent.TabSelected(index)) },
                        text = {
                            if (index == 2 && requestCount > 0) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(title)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.error
                                    ) {
                                        Text(requestCount.toString())
                                    }
                                }
                            } else {
                                Text(title)
                            }
                        }
                    )
                }
            }

            // Tab Content
            when (state.selectedTab) {
                0 -> FollowersTab(
                    followers = state.followers,
                    isLoading = state.isLoadingFollowers,
                    onUserClick = { userId -> navigator.navigate(Screen.Profile(userId)) },
                    onRemoveFollower = { viewModel.onEvent(FollowListEvent.RemoveFollowerClicked(it)) }
                )
                1 -> FollowingTab(
                    following = state.following,
                    isLoading = state.isLoadingFollowing,
                    onUserClick = { userId -> navigator.navigate(Screen.Profile(userId)) },
                    onUnfollow = { viewModel.onEvent(FollowListEvent.UnfollowClicked(it)) }
                )
                2 -> RequestsTab(
                    requests = state.requests,
                    isLoading = state.isLoadingRequests,
                    onUserClick = { userId -> navigator.navigate(Screen.Profile(userId)) },
                    onAccept = { viewModel.onEvent(FollowListEvent.AcceptRequestClicked(it)) },
                    onReject = { viewModel.onEvent(FollowListEvent.RejectRequestClicked(it)) }
                )
            }
        }
    }
}

@Composable
private fun FollowersTab(
    followers: List<UserProfileResponse>,
    isLoading: Boolean,
    onUserClick: (Int) -> Unit,
    onRemoveFollower: (Int) -> Unit
) {
    if (isLoading && followers.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else if (followers.isEmpty()) {
        EmptyStateComponent(
            icon = Icons.Default.Group,
            title = stringResource(Res.string.empty_followers),
            description = "Henüz takipçiniz bulunmuyor.",
            modifier = Modifier.fillMaxSize()
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(followers, key = { it.id }) { user ->
                FollowUserItem(
                    avatarId = user.avatarId,
                    profileImageUrl = user.profileImageUrl,
                    name = user.name,
                    username = user.username,
                    onClick = { onUserClick(user.id) },
                    actionButton = {
                        OutlinedButton(
                            onClick = { onRemoveFollower(user.id) },
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text(stringResource(Res.string.action_remove_follower))
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun FollowingTab(
    following: List<UserProfileResponse>,
    isLoading: Boolean,
    onUserClick: (Int) -> Unit,
    onUnfollow: (Int) -> Unit
) {
    if (isLoading && following.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else if (following.isEmpty()) {
        EmptyStateComponent(
            icon = Icons.Default.PersonSearch,
            title = stringResource(Res.string.empty_following),
            description = "Henüz kimseyi takip etmiyorsunuz.",
            modifier = Modifier.fillMaxSize()
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(following, key = { it.id }) { user ->
                FollowUserItem(
                    avatarId = user.avatarId,
                    profileImageUrl = user.profileImageUrl,
                    name = user.name,
                    username = user.username,
                    onClick = { onUserClick(user.id) },
                    actionButton = {
                        Button(
                            onClick = { onUnfollow(user.id) },
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Text(stringResource(Res.string.action_unfollow))
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun RequestsTab(
    requests: List<FollowRequestResponse>,
    isLoading: Boolean,
    onUserClick: (Int) -> Unit,
    onAccept: (Int) -> Unit,
    onReject: (Int) -> Unit
) {
    if (isLoading && requests.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else if (requests.isEmpty()) {
        EmptyStateComponent(
            icon = Icons.Default.PersonAdd,
            title = stringResource(Res.string.empty_requests),
            description = "Bekleyen takip isteğiniz yok.",
            modifier = Modifier.fillMaxSize()
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(requests, key = { it.id }) { request ->
                FollowUserItem(
                    avatarId = request.requesterAvatarId,
                    profileImageUrl = request.requesterProfileImageUrl,
                    name = request.requesterName,
                    username = request.requesterUsername,
                    onClick = { onUserClick(request.requesterId) },
                    actionButton = {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { onAccept(request.id) },
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text(stringResource(Res.string.action_accept))
                            }
                            OutlinedButton(
                                onClick = { onReject(request.id) },
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Text(stringResource(Res.string.action_decline))
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun FollowUserItem(
    avatarId: String,
    profileImageUrl: String? = null,
    name: String,
    username: String,
    onClick: (() -> Unit)? = null,
    actionButton: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AvatarImage(
            avatarId = avatarId,
            profileImageUrl = profileImageUrl,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (username.isNotBlank()) {
                Text(
                    text = "@$username",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        actionButton()
    }
}
