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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yusufteker.planora.core.base.CollectEffect
import com.yusufteker.planora.core.navigation.LocalMainNavigator
import com.yusufteker.planora.core.navigation.Screen
import com.yusufteker.planora.core.navigation.Screen.MainDestination
import com.yusufteker.planora.core.theme.premiumColor
import com.yusufteker.planora.core.ui.components.AvatarImage
import com.yusufteker.planora.core.ui.components.GradientText
import com.yusufteker.planora.core.ui.components.rememberAppImagePickerLauncher
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import planora.core.generated.resources.Res
import planora.core.generated.resources.action_accept
import planora.core.generated.resources.action_decline
import planora.core.generated.resources.action_follow
import planora.core.generated.resources.action_login
import planora.core.generated.resources.action_requested
import planora.core.generated.resources.action_unfollow
import planora.core.generated.resources.back
import planora.core.generated.resources.profile_access_privacy_title
import planora.core.generated.resources.profile_action_find_friends
import planora.core.generated.resources.profile_action_revoke_access
import planora.core.generated.resources.profile_calendar_empty_viewers
import planora.core.generated.resources.profile_calendar_request_sent
import planora.core.generated.resources.profile_calendar_requests_label
import planora.core.generated.resources.profile_calendar_sharing_count
import planora.core.generated.resources.profile_calendar_sharing_title
import planora.core.generated.resources.profile_calendar_viewers_sheet_desc
import planora.core.generated.resources.profile_calendar_viewers_sheet_title
import planora.core.generated.resources.profile_follow_requests_label
import planora.core.generated.resources.profile_followers_label
import planora.core.generated.resources.profile_following_label
import planora.core.generated.resources.profile_guest_login_prompt
import planora.core.generated.resources.profile_incoming_requests_title
import planora.core.generated.resources.profile_leave_calendar
import planora.core.generated.resources.profile_posts_label
import planora.core.generated.resources.profile_request_calendar_access
import planora.core.generated.resources.profile_screen_title
import planora.core.generated.resources.settings_title

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

    var showCalendarViewersSheet by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background, topBar = {
            TopAppBar(
                title = {
                GradientText(
                    text = if (!state.isMyProfile) "@${state.username}" else stringResource(Res.string.profile_screen_title),
                    colors = listOf(
                        MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary
                    ),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold
                    )
                )
            }, navigationIcon = {
                if (!state.isMyProfile) {
                    IconButton(onClick = { viewModel.onEvent(ProfileEvent.BackClicked) }) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(Res.string.back),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }, actions = {
                if (state.isMyProfile) {
                    IconButton(onClick = { mainNavigator?.navigate(MainDestination.Settings) }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(Res.string.settings_title),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }, colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
            )
        }) { paddingValues ->
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
            modifier = Modifier.fillMaxSize().padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            if (state.isLoggedIn) {
                // Main Profile Card
                com.yusufteker.planora.core.ui.components.GlassCard(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(28.dp),
                    borderGradient = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
                    )
                ) {
                    Box() {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {

                            // Centered Avatar with gradient ring border matching theme
                            val avatarBorderBrush = Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                )
                            )

                            Box(
                                modifier = Modifier.size(96.dp).clip(CircleShape)
                                .clickable(enabled = state.isMyProfile) {
                                    imagePickerLauncher.launch()
                                }.border(
                                    width = 3.dp, brush = avatarBorderBrush, shape = CircleShape
                                ).background(MaterialTheme.colorScheme.surface).padding(3.dp),
                                contentAlignment = Alignment.Center) {
                                AvatarImage(
                                    avatarId = currentAvatarId,
                                    profileImageUrl = state.profileImageUrl,
                                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                                )
                                if (state.isUploadingImage) {
                                    Box(
                                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.4f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }

                            }

                            // Name & Username matching theme
                            Row(
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                GradientText(
                                    text = state.name.ifBlank { "User" }, colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.secondary
                                    ), style = MaterialTheme.typography.headlineSmall.copy(
                                        fontWeight = FontWeight.Black
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "@${state.username}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                    fontWeight = FontWeight.Medium
                                )

                            }


                            Spacer(modifier = Modifier.height(18.dp))

                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                thickness = 1.dp
                            )

                            Spacer(modifier = Modifier.height(14.dp))

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
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = stringResource(Res.string.profile_posts_label).uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                            alpha = 0.7f
                                        ),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                // Divider
                                Box(
                                    modifier = Modifier.width(1.dp).height(24.dp).background(
                                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
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
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = stringResource(Res.string.profile_followers_label).uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                            alpha = 0.7f
                                        ),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                // Divider
                                Box(
                                    modifier = Modifier.width(1.dp).height(24.dp).background(
                                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
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
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = stringResource(Res.string.profile_following_label).uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                            alpha = 0.7f
                                        ),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                        }
                        if (state.isPremium) {
                            Surface(
                                modifier = Modifier.align(Alignment.TopEnd).padding(24.dp),
                                shape = RoundedCornerShape(20.dp),
                                color = premiumColor.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, premiumColor.copy(alpha = 0.35f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(
                                        horizontal = 12.dp, vertical = 5.dp
                                    ),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = premiumColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                if (state.isMyProfile) {
                    Spacer(modifier = Modifier.height(8.dp))

                    // Single, sleek quick action button: Find Friends
                    Button(
                        onClick = { rootNavigator.navigate(Screen.SearchUsers) },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                            .height(48.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PersonAdd,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(Res.string.profile_action_find_friends),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // ── Erişim ve Gizlilik (Access & Privacy) ──
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = stringResource(Res.string.profile_access_privacy_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            modifier = Modifier.padding(start = 4.dp)
                        )

                        // 1. Gelen İstekler Kartı (Takip veya Takvim isteği varsa)
                        if (state.pendingRequests.isNotEmpty() || state.pendingCalendarRequests.isNotEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(
                                        alpha = 0.25f
                                    )
                                ),
                                border = BorderStroke(
                                    1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Notifications,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(
                                                text = stringResource(Res.string.profile_incoming_requests_title),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onBackground
                                            )
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        ) {
                                            Text(
                                                text = (state.pendingRequests.size + state.pendingCalendarRequests.size).toString(),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.ExtraBold,
                                                modifier = Modifier.padding(
                                                    horizontal = 8.dp, vertical = 2.dp
                                                )
                                            )
                                        }
                                    }

                                    // Takip İstekleri
                                    if (state.pendingRequests.isNotEmpty()) {
                                        Text(
                                            text = stringResource(Res.string.profile_follow_requests_label),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            state.pendingRequests.take(4).forEach { request ->
                                                Row(
                                                    modifier = Modifier.fillMaxWidth().background(
                                                            color = MaterialTheme.colorScheme.surface.copy(
                                                                alpha = 0.7f
                                                            ), shape = RoundedCornerShape(14.dp)
                                                        ).border(
                                                            width = 1.dp,
                                                            color = MaterialTheme.colorScheme.outlineVariant.copy(
                                                                alpha = 0.3f
                                                            ),
                                                            shape = RoundedCornerShape(14.dp)
                                                        ).padding(10.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    AvatarImage(
                                                        avatarId = request.requesterAvatarId,
                                                        profileImageUrl = request.requesterProfileImageUrl,
                                                        modifier = Modifier.size(36.dp)
                                                            .clip(CircleShape)
                                                    )
                                                    Spacer(modifier = Modifier.width(10.dp))
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = request.requesterName,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onBackground,
                                                            maxLines = 1
                                                        )
                                                        if (request.requesterUsername.isNotBlank()) {
                                                            Text(
                                                                text = "@${request.requesterUsername}",
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                                    alpha = 0.8f
                                                                ),
                                                                maxLines = 1
                                                            )
                                                        }
                                                    }
                                                    Row(
                                                        horizontalArrangement = Arrangement.spacedBy(
                                                            6.dp
                                                        )
                                                    ) {
                                                        Button(
                                                            onClick = {
                                                                viewModel.onEvent(
                                                                    ProfileEvent.AcceptRequestClicked(
                                                                        request.id
                                                                    )
                                                                )
                                                            },
                                                            shape = RoundedCornerShape(10.dp),
                                                            contentPadding = PaddingValues(
                                                                horizontal = 10.dp, vertical = 4.dp
                                                            ),
                                                            modifier = Modifier.height(32.dp),
                                                            colors = ButtonDefaults.buttonColors(
                                                                containerColor = MaterialTheme.colorScheme.primary
                                                            )
                                                        ) {
                                                            Text(
                                                                text = stringResource(Res.string.action_accept),
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                        OutlinedButton(
                                                            onClick = {
                                                                viewModel.onEvent(
                                                                    ProfileEvent.RejectRequestClicked(
                                                                        request.id
                                                                    )
                                                                )
                                                            },
                                                            shape = RoundedCornerShape(10.dp),
                                                            contentPadding = PaddingValues(
                                                                horizontal = 10.dp, vertical = 4.dp
                                                            ),
                                                            modifier = Modifier.height(32.dp),
                                                            colors = ButtonDefaults.outlinedButtonColors(
                                                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                                            ),
                                                            border = BorderStroke(
                                                                1.dp,
                                                                MaterialTheme.colorScheme.outlineVariant
                                                            )
                                                        ) {
                                                            Text(
                                                                text = stringResource(Res.string.action_decline),
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Takvim İstekleri
                                    if (state.pendingCalendarRequests.isNotEmpty()) {
                                        Text(
                                            text = stringResource(Res.string.profile_calendar_requests_label),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            state.pendingCalendarRequests.take(4)
                                                .forEach { request ->
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth()
                                                            .background(
                                                                color = MaterialTheme.colorScheme.surface.copy(
                                                                    alpha = 0.7f
                                                                ), shape = RoundedCornerShape(14.dp)
                                                            ).border(
                                                                width = 1.dp,
                                                                color = MaterialTheme.colorScheme.outlineVariant.copy(
                                                                    alpha = 0.3f
                                                                ),
                                                                shape = RoundedCornerShape(14.dp)
                                                            ).padding(10.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        AvatarImage(
                                                            avatarId = request.requesterAvatarId,
                                                            profileImageUrl = request.requesterProfileImageUrl,
                                                            modifier = Modifier.size(36.dp)
                                                                .clip(CircleShape)
                                                        )
                                                        Spacer(modifier = Modifier.width(10.dp))
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(
                                                                text = request.requesterName,
                                                                style = MaterialTheme.typography.bodyMedium,
                                                                fontWeight = FontWeight.Bold,
                                                                color = MaterialTheme.colorScheme.onBackground,
                                                                maxLines = 1
                                                            )
                                                            Text(
                                                                text = "@${request.requesterUsername}",
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                                    alpha = 0.8f
                                                                ),
                                                                maxLines = 1
                                                            )
                                                        }
                                                        Row(
                                                            horizontalArrangement = Arrangement.spacedBy(
                                                                6.dp
                                                            )
                                                        ) {
                                                            Button(
                                                                onClick = {
                                                                    viewModel.onEvent(
                                                                        ProfileEvent.AcceptCalendarRequestClicked(
                                                                            request.id
                                                                        )
                                                                    )
                                                                },
                                                                shape = RoundedCornerShape(10.dp),
                                                                contentPadding = PaddingValues(
                                                                    horizontal = 10.dp,
                                                                    vertical = 4.dp
                                                                ),
                                                                modifier = Modifier.height(32.dp),
                                                                colors = ButtonDefaults.buttonColors(
                                                                    containerColor = MaterialTheme.colorScheme.primary
                                                                )
                                                            ) {
                                                                Text(
                                                                    text = stringResource(Res.string.action_accept),
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    fontWeight = FontWeight.Bold
                                                                )
                                                            }
                                                            OutlinedButton(
                                                                onClick = {
                                                                    viewModel.onEvent(
                                                                        ProfileEvent.RejectCalendarRequestClicked(
                                                                            request.id
                                                                        )
                                                                    )
                                                                },
                                                                shape = RoundedCornerShape(10.dp),
                                                                contentPadding = PaddingValues(
                                                                    horizontal = 10.dp,
                                                                    vertical = 4.dp
                                                                ),
                                                                modifier = Modifier.height(32.dp),
                                                                colors = ButtonDefaults.outlinedButtonColors(
                                                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                                                ),
                                                                border = BorderStroke(
                                                                    1.dp,
                                                                    MaterialTheme.colorScheme.outlineVariant
                                                                )
                                                            ) {
                                                                Text(
                                                                    text = stringResource(Res.string.action_decline),
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    fontWeight = FontWeight.Bold
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                        }
                                    }
                                }
                            }
                        }

                        // 2. Takvim Paylaşımı Yönetim Kartı
                        Card(
                            modifier = Modifier.fillMaxWidth()
                                .clickable { showCalendarViewersSheet = true },
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            ),
                            border = BorderStroke(
                                1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Event,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(Res.string.profile_calendar_sharing_title),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (state.calendarGrants.isEmpty()) {
                                            stringResource(Res.string.profile_calendar_empty_viewers)
                                        } else {
                                            stringResource(
                                                Res.string.profile_calendar_sharing_count,
                                                state.calendarGrants.size
                                            )
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.size(20.dp)
                                )
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

                        Button(
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
                                fontWeight = FontWeight.Bold
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

                        Button(
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
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                // Non-logged in state banner
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
                            ).background(MaterialTheme.colorScheme.surfaceVariant).padding(4.dp),
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
                            fontWeight = FontWeight.ExtraBold,
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
                        Button(
                            onClick = { rootNavigator.navigate(Screen.Login) },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                text = stringResource(Res.string.action_login),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(100.dp))
        }

        // ── Bottom Sheet for Calendar Viewers ──
        if (showCalendarViewersSheet) {
            ModalBottomSheet(
                onDismissRequest = { showCalendarViewersSheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
                        .padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column {
                        Text(
                            text = stringResource(Res.string.profile_calendar_viewers_sheet_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(Res.string.profile_calendar_viewers_sheet_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (state.calendarGrants.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(Res.string.profile_calendar_empty_viewers),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            state.calendarGrants.forEach { grant ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().background(
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                            RoundedCornerShape(16.dp)
                                        ).padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AvatarImage(
                                        avatarId = grant.avatarId,
                                        profileImageUrl = grant.profileImageUrl,
                                        modifier = Modifier.size(40.dp).clip(CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = grant.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "@${grant.username}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            viewModel.onEvent(
                                                ProfileEvent.RevokeCalendarGrantClicked(
                                                    grant.userId
                                                )
                                            )
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(
                                            horizontal = 10.dp, vertical = 4.dp
                                        ),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = MaterialTheme.colorScheme.error
                                        ),
                                        border = BorderStroke(
                                            1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                                        )
                                    ) {
                                        Text(
                                            text = stringResource(Res.string.profile_action_revoke_access),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}