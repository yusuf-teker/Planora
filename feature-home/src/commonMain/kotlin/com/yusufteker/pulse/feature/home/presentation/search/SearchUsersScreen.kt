package com.yusufteker.pulse.feature.home.presentation.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yusufteker.pulse.core.base.CollectEffect
import com.yusufteker.pulse.core.navigation.LocalNavigator
import com.yusufteker.pulse.core.navigation.Screen
import com.yusufteker.pulse.core.ui.components.AvatarImage
import com.yusufteker.pulse.shared.api.UserProfileResponse
import org.jetbrains.compose.resources.stringResource
import pulsy.core.generated.resources.Res
import pulsy.core.generated.resources.*
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchUsersScreen(
    viewModel: SearchUsersViewModel
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val navigator = LocalNavigator.current

    viewModel.effect.CollectEffect { effect ->
        when (effect) {
            is SearchUsersEffect.NavigateBack -> navigator.pop()
            is SearchUsersEffect.NavigateToProfile -> {
                // If we want to navigate to another user's profile, we can push it to main navigator.
                // Currently ProfileScreen might need modifications to accept userId from navigation args.
                // For now, just as an example.
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    TextField(
                        value = state.query,
                        onValueChange = { viewModel.onEvent(SearchUsersEvent.OnQueryChanged(it)) },
                        placeholder = { Text(stringResource(Res.string.search_users_placeholder)) },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                            unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(CircleShape),
                        trailingIcon = {
                            if (state.query.isNotEmpty()) {
                                IconButton(onClick = { viewModel.onEvent(SearchUsersEvent.OnQueryChanged("")) }) {
                                    Icon(Icons.Rounded.Clear, contentDescription = "Clear")
                                }
                            } else {
                                Icon(Icons.Rounded.Search, contentDescription = "Search")
                            }
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.onEvent(SearchUsersEvent.OnBackClicked) }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (state.isLoading && state.results.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (state.query.isNotBlank() && state.results.isEmpty() && !state.isLoading) {
                Text(
                    text = stringResource(Res.string.search_no_results),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(state.results) { user ->
                        UserListItem(
                            user = user,
                            onClick = { viewModel.onEvent(SearchUsersEvent.OnUserClicked(user.id)) },
                            onFollowClick = { viewModel.onEvent(SearchUsersEvent.OnToggleFollow(user.id)) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UserListItem(
    user: UserProfileResponse,
    onClick: () -> Unit,
    onFollowClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        AvatarImage(
            avatarId = user.avatarId,
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (user.username.isNotBlank()) {
                Text(
                    text = "@${user.username}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Button(
            onClick = onFollowClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (user.isFollowedByMe) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary,
                contentColor = if (user.isFollowedByMe) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimary
            ),
            shape = CircleShape
        ) {
            Text(if (user.isFollowedByMe) stringResource(Res.string.action_following) else stringResource(Res.string.action_follow))
        }
    }
}
