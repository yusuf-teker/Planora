package com.yusufteker.planora.feature.home.presentation.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yusufteker.planora.core.base.CollectEffect
import com.yusufteker.planora.core.navigation.LocalMainNavigator
import com.yusufteker.planora.core.navigation.Screen
import com.yusufteker.planora.core.navigation.Screen.MainDestination
import com.yusufteker.planora.core.ui.components.AvatarImage
import com.yusufteker.planora.core.ui.components.GradientText
import com.yusufteker.planora.core.ui.components.rememberAppImagePickerLauncher
import org.jetbrains.compose.resources.stringResource
import planora.core.generated.resources.Res
import planora.core.generated.resources.action_accept
import planora.core.generated.resources.action_add_friend
import planora.core.generated.resources.action_decline
import planora.core.generated.resources.action_follow
import planora.core.generated.resources.action_login
import planora.core.generated.resources.action_logout
import planora.core.generated.resources.action_requested
import planora.core.generated.resources.action_see_all
import planora.core.generated.resources.action_unfollow
import planora.core.generated.resources.cancel
import planora.core.generated.resources.profile_calendar_access_requests
import planora.core.generated.resources.profile_calendar_request_sent
import planora.core.generated.resources.profile_calendar_viewers
import planora.core.generated.resources.profile_followers_label
import planora.core.generated.resources.profile_following_label
import planora.core.generated.resources.profile_guest_login_prompt
import planora.core.generated.resources.profile_leave_calendar
import planora.core.generated.resources.profile_posts_label
import planora.core.generated.resources.profile_premium_badge
import planora.core.generated.resources.profile_tier_free
import planora.core.generated.resources.profile_upgrade_banner_desc
import planora.core.generated.resources.profile_action_upgrade
import planora.core.generated.resources.profile_tier_premium
import planora.core.generated.resources.profile_premium_expires_format
import planora.core.generated.resources.profile_premium_lifetime
import planora.core.generated.resources.profile_action_manage_premium
import planora.core.generated.resources.profile_request_calendar_access
import planora.core.generated.resources.settings_title
import planora.core.generated.resources.title_notifications
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Formats an ISO-8601 date string to DD.MM.YYYY for subscription expiry display.
 */
private fun formatSubscriptionDate(isoDate: String?): String? {
    if (isoDate.isNullOrBlank()) return null
    return try {
        val instant = Instant.parse(isoDate)
        val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        val day = local.dayOfMonth.toString().padStart(2, '0')
        val month = local.monthNumber.toString().padStart(2, '0')
        val year = local.year
        "$day.$month.$year"
    } catch (_: Exception) {
        if (isoDate.length >= 10) isoDate.substring(0, 10) else isoDate
    }
}

/**
 * Profile screen composable.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel, userId: Int? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val mainNavigator = if (userId == null) LocalMainNavigator.current else null
    val state by viewModel.state.collectAsStateWithLifecycle()

    val rootNavigator = com.yusufteker.planora.core.navigation.LocalNavigator.current

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

        onPauseOrDispose {}
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).statusBarsPadding()
                .safeContentPadding()
        ) {
            var pendingCropImageBytes by remember { mutableStateOf<ByteArray?>(null) }

            val imagePickerLauncher = rememberAppImagePickerLauncher(
                onResult = { imageBytes ->
                    if (imageBytes != null) {
                        pendingCropImageBytes = imageBytes
                    }
                })

            pendingCropImageBytes?.let { rawBytes ->
                com.yusufteker.planora.core.ui.components.ImageCropDialog(
                    imageBytes = rawBytes,
                    isCircular = true,
                    onImageCropped = { croppedBytes ->
                        pendingCropImageBytes = null
                        viewModel.onEvent(ProfileEvent.ProfileImageSelected(croppedBytes))
                    },
                    onDismiss = {
                        pendingCropImageBytes = null
                    })
            }

            val avatarList = List(10) { "avatar_${it + 1}" }
            val currentAvatarId = if (state.avatarId in avatarList) state.avatarId else "avatar_1"

            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 0.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                if (!state.isMyProfile) {
                    Row(
                        modifier = Modifier.fillMaxWidth()
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
                    com.yusufteker.planora.core.ui.components.GlassCard(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        shape = RoundedCornerShape(28.dp),
                        borderGradient = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {

                            // Centered Avatar with gradient ring border (Gold if Premium)
                            val avatarBorderBrush = if (state.isPremium) {
                                Brush.linearGradient(
                                    colors = listOf(
                                        androidx.compose.ui.graphics.Color(0xFFFFD700),
                                        androidx.compose.ui.graphics.Color(0xFFFFA500),
                                        androidx.compose.ui.graphics.Color(0xFFFF8C00)
                                    )
                                )
                            } else {
                                Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.tertiary,
                                        MaterialTheme.colorScheme.secondary
                                    )
                                )
                            }

                            Box(
                                modifier = Modifier.size(96.dp).clip(CircleShape)
                                    .clickable(enabled = state.isMyProfile) {
                                        imagePickerLauncher.launch()
                                    }.border(
                                        width = 3.dp, brush = avatarBorderBrush, shape = CircleShape
                                    ).background(MaterialTheme.colorScheme.surface).padding(3.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                AvatarImage(
                                    avatarId = currentAvatarId,
                                    profileImageUrl = state.profileImageUrl,
                                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                                )
                                if (state.isUploadingImage) {
                                    Box(
                                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                                            .background(
                                                androidx.compose.ui.graphics.Color.Black.copy(
                                                    alpha = 0.4f
                                                )
                                            ), contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Name & Username with Gradient
                            GradientText(
                                text = state.name.ifBlank { "User" },
                                colors = com.yusufteker.planora.core.theme.PlanoraColors.GradientPrimary,
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Black
                                )
                            )


                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "@${state.username}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                            )

                            // Premium Badge or Free Tier Tag
                            if (state.isPremium) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = androidx.compose.ui.graphics.Color(0xFFFFD700).copy(alpha = 0.15f),
                                    border = BorderStroke(
                                        width = 1.dp,
                                        brush = Brush.horizontalGradient(
                                            listOf(
                                                androidx.compose.ui.graphics.Color(0xFFFFD700),
                                                androidx.compose.ui.graphics.Color(0xFFFFA500)
                                            )
                                        )
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = androidx.compose.ui.graphics.Color(0xFFFFD700),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = stringResource(Res.string.profile_premium_badge),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
                                            color = androidx.compose.ui.graphics.Color(0xFFFFD700)
                                        )
                                    }
                                }
                            } else if (state.isMyProfile) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                    )
                                ) {
                                    Text(
                                        text = stringResource(Res.string.profile_tier_free),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                    )
                                }
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
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                            alpha = 0.7f
                                        ),
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                                    )
                                }

                                // Divider
                                Box(
                                    modifier = Modifier.width(1.dp).height(24.dp).background(
                                        MaterialTheme.colorScheme.outlineVariant.copy(
                                            alpha = 0.4f
                                        )
                                    )
                                )

                                // Followers
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                                        .clickable(enabled = state.isMyProfile) {
                                            viewModel.onEvent(ProfileEvent.NavigateToFollowList(0))
                                        }.padding(vertical = 4.dp)
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
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                            alpha = 0.7f
                                        ),
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                                    )
                                }

                                // Divider
                                Box(
                                    modifier = Modifier.width(1.dp).height(24.dp).background(
                                        MaterialTheme.colorScheme.outlineVariant.copy(
                                            alpha = 0.4f
                                        )
                                    )
                                )

                                // Following
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                                        .clickable(enabled = state.isMyProfile) {
                                            viewModel.onEvent(ProfileEvent.NavigateToFollowList(1))
                                        }.padding(vertical = 4.dp)
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
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                            alpha = 0.7f
                                        ),
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    if (state.isMyProfile) {
                        // Prominent Membership Tier Card (Directly visible without opening Settings)
                        if (state.isPremium) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                                    .clickable { rootNavigator.navigate(Screen.Premium) },
                                shape = RoundedCornerShape(22.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = androidx.compose.ui.graphics.Color(0xFFFFD700).copy(alpha = 0.12f)
                                ),
                                border = BorderStroke(
                                    width = 1.2.dp,
                                    brush = Brush.horizontalGradient(
                                        listOf(
                                            androidx.compose.ui.graphics.Color(0xFFFFD700),
                                            androidx.compose.ui.graphics.Color(0xFFFFA500)
                                        )
                                    )
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(
                                                Brush.linearGradient(
                                                    listOf(
                                                        androidx.compose.ui.graphics.Color(0xFFFFD700),
                                                        androidx.compose.ui.graphics.Color(0xFFFF8C00)
                                                    )
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = androidx.compose.ui.graphics.Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = stringResource(Res.string.profile_tier_premium),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
                                            color = androidx.compose.ui.graphics.Color(0xFFFFD700)
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        val formattedDate = formatSubscriptionDate(state.premiumUntil)
                                        Text(
                                            text = if (formattedDate != null) {
                                                stringResource(Res.string.profile_premium_expires_format, formattedDate)
                                            } else {
                                                stringResource(Res.string.profile_premium_lifetime)
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = androidx.compose.ui.graphics.Color(0xFFFFD700).copy(alpha = 0.2f),
                                        border = BorderStroke(1.dp, androidx.compose.ui.graphics.Color(0xFFFFD700).copy(alpha = 0.5f))
                                    ) {
                                        Text(
                                            text = stringResource(Res.string.profile_action_manage_premium),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                            color = androidx.compose.ui.graphics.Color(0xFFFFD700),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        } else {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                                    .clickable { rootNavigator.navigate(Screen.Premium) },
                                shape = RoundedCornerShape(22.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                                ),
                                border = BorderStroke(
                                    width = 1.dp,
                                    brush = Brush.horizontalGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f)
                                        )
                                    )
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(
                                                Brush.linearGradient(
                                                    listOf(
                                                        MaterialTheme.colorScheme.primary,
                                                        MaterialTheme.colorScheme.tertiary
                                                    )
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = androidx.compose.ui.graphics.Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = stringResource(Res.string.profile_tier_free),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = stringResource(Res.string.profile_upgrade_banner_desc),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    androidx.compose.material3.Button(
                                        onClick = { rootNavigator.navigate(Screen.Premium) },
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        ),
                                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                                    ) {
                                        Text(
                                            text = stringResource(Res.string.profile_action_upgrade),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Quick Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Add Friend pill button
                            androidx.compose.material3.Button(
                                onClick = { rootNavigator.navigate(Screen.SearchUsers) },
                                modifier = Modifier.weight(1f).height(50.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
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
                                modifier = Modifier.size(50.dp).clip(RoundedCornerShape(16.dp))
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
                                modifier = Modifier.size(50.dp).clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f))
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(16.dp)
                                    ).clickable { viewModel.onEvent(ProfileEvent.LogoutClicked) },
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
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(24.dp)
                                    ).border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                        shape = RoundedCornerShape(24.dp)
                                    ).padding(16.dp)
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
                                                modifier = Modifier.size(20.dp).background(
                                                    MaterialTheme.colorScheme.primary, CircleShape
                                                ), contentAlignment = Alignment.Center
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
                                                onClick = {
                                                    viewModel.onEvent(
                                                        ProfileEvent.NavigateToFollowList(
                                                            2
                                                        )
                                                    )
                                                }, contentPadding = PaddingValues(
                                                    horizontal = 8.dp, vertical = 4.dp
                                                )
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
                                                modifier = Modifier.fillMaxWidth().background(
                                                    color = MaterialTheme.colorScheme.surface.copy(
                                                        alpha = 0.7f
                                                    ), shape = RoundedCornerShape(16.dp)
                                                ).border(
                                                    width = 1.dp,
                                                    color = MaterialTheme.colorScheme.outlineVariant.copy(
                                                        alpha = 0.3f
                                                    ),
                                                    shape = RoundedCornerShape(16.dp)
                                                ).padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                AvatarImage(
                                                    avatarId = request.requesterAvatarId,
                                                    profileImageUrl = request.requesterProfileImageUrl,
                                                    modifier = Modifier.size(44.dp)
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
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                                alpha = 0.8f
                                                            )
                                                        )
                                                    }
                                                }

                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    androidx.compose.material3.Button(
                                                        onClick = {
                                                            viewModel.onEvent(
                                                                ProfileEvent.AcceptRequestClicked(
                                                                    request.id
                                                                )
                                                            )
                                                        },
                                                        shape = RoundedCornerShape(12.dp),
                                                        contentPadding = PaddingValues(
                                                            horizontal = 12.dp, vertical = 6.dp
                                                        ),
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
                                                        onClick = {
                                                            viewModel.onEvent(
                                                                ProfileEvent.RejectRequestClicked(
                                                                    request.id
                                                                )
                                                            )
                                                        },
                                                        shape = RoundedCornerShape(12.dp),
                                                        contentPadding = PaddingValues(
                                                            horizontal = 12.dp, vertical = 6.dp
                                                        ),
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
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(24.dp)
                                    ).border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                        shape = RoundedCornerShape(24.dp)
                                    ).padding(16.dp)
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
                                            modifier = Modifier.fillMaxWidth()
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
                                                    modifier = Modifier.size(40.dp)
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
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                            alpha = 0.7f
                                                        )
                                                    )
                                                }
                                            }

                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                androidx.compose.material3.Button(
                                                    onClick = {
                                                        viewModel.onEvent(
                                                            ProfileEvent.AcceptCalendarRequestClicked(
                                                                request.id
                                                            )
                                                        )
                                                    },
                                                    shape = RoundedCornerShape(12.dp),
                                                    contentPadding = PaddingValues(
                                                        horizontal = 12.dp, vertical = 6.dp
                                                    ),
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
                                                    onClick = {
                                                        viewModel.onEvent(
                                                            ProfileEvent.RejectCalendarRequestClicked(
                                                                request.id
                                                            )
                                                        )
                                                    },
                                                    shape = RoundedCornerShape(12.dp),
                                                    contentPadding = PaddingValues(
                                                        horizontal = 12.dp, vertical = 6.dp
                                                    ),
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
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(24.dp)
                                    ).border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                        shape = RoundedCornerShape(24.dp)
                                    ).padding(16.dp)
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
                                            modifier = Modifier.fillMaxWidth()
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
                                                    modifier = Modifier.size(40.dp)
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
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                            alpha = 0.7f
                                                        )
                                                    )
                                                }
                                            }

                                            androidx.compose.material3.OutlinedButton(
                                                onClick = {
                                                    viewModel.onEvent(
                                                        ProfileEvent.RevokeCalendarGrantClicked(
                                                            grant.userId
                                                        )
                                                    )
                                                },
                                                shape = RoundedCornerShape(12.dp),
                                                contentPadding = PaddingValues(
                                                    horizontal = 12.dp, vertical = 6.dp
                                                ),
                                                modifier = Modifier.height(34.dp),
                                                colors = ButtonDefaults.outlinedButtonColors(
                                                    contentColor = MaterialTheme.colorScheme.error
                                                ),
                                                border = androidx.compose.foundation.BorderStroke(
                                                    width = 1.dp,
                                                    color = MaterialTheme.colorScheme.error.copy(
                                                        alpha = 0.5f
                                                    )
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
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
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
                                modifier = Modifier.fillMaxWidth().height(50.dp),
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
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
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
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
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
                                modifier = Modifier.fillMaxWidth().height(50.dp),
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
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                },
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
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
                        modifier = Modifier.fillMaxWidth().padding(32.dp).background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(28.dp)
                        ).border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(28.dp)
                        ).padding(32.dp), contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier.size(100.dp).clip(CircleShape).border(
                                    width = 3.dp, brush = Brush.linearGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.outlineVariant,
                                            MaterialTheme.colorScheme.outline
                                        )
                                    ), shape = CircleShape
                                ).background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(4.dp), contentAlignment = Alignment.Center
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
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}