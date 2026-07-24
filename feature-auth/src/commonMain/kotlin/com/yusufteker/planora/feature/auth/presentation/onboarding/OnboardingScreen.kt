package com.yusufteker.planora.feature.auth.presentation.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.animation.Crossfade
import com.yusufteker.planora.core.base.CollectEffect
import com.yusufteker.planora.core.navigation.LocalNavigator
import com.yusufteker.planora.core.navigation.Screen
import com.yusufteker.planora.feature.auth.presentation.onboarding.components.*
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import planora.core.generated.resources.Res
import planora.core.generated.resources.*

/**
 * Onboarding screen composable.
 *
 * Displays a multi-page onboarding flow with:
 * - Page content (title, description)
 * - Page indicators
 * - Skip/Next/Get Started buttons
 */
@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel
) {
    val navigator = LocalNavigator.current
    val state by viewModel.state.collectAsStateWithLifecycle()

    viewModel.effect.CollectEffect { effect ->
        when (effect) {
            is OnboardingEffect.NavigateToLogin -> navigator.setRoot(Screen.Login)
            is OnboardingEffect.NavigateToHome -> navigator.setRoot(Screen.Main)
        }
    }

    val pages = listOf(
        OnboardingPage(
            title = Res.string.onboarding_title_1,
            description = Res.string.onboarding_desc_1
        ),
        OnboardingPage(
            title = Res.string.onboarding_title_2,
            description = Res.string.onboarding_desc_2
        ),
        OnboardingPage(
            title = Res.string.onboarding_title_3,
            description = Res.string.onboarding_desc_3
        ),
        OnboardingPage(
            title = Res.string.onboarding_title_4,
            description = Res.string.onboarding_desc_4
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeContentPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Skip button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            if (state.currentPage < state.totalPages - 1) {
                TextButton(onClick = { viewModel.onEvent(OnboardingEvent.SkipClicked) }) {
                    Text(
                        text = stringResource(Res.string.action_skip),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(48.dp)) // Reserve space when skip button is hidden
            }
        }

        // Animated illustration area
        Box(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Crossfade(
                targetState = state.currentPage,
                animationSpec = androidx.compose.animation.core.tween(500)
            ) { page ->
                when (page) {
                    0 -> AnimatedCalendarFeature()
                    1 -> AnimatedSharedRoomFeature()
                    2 -> AnimatedFriendCalendarFeature()
                    3 -> AnimatedAIFeature()
                }
            }
        }

        Text(
            text = stringResource(pages[state.currentPage].title),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(pages[state.currentPage].description),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )

        Spacer(modifier = Modifier.weight(1f))

        // Page indicators
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 32.dp)
        ) {
            repeat(state.totalPages) { index ->
                Box(
                    modifier = Modifier
                        .size(if (index == state.currentPage) 12.dp else 8.dp)
                        .clip(CircleShape)
                        .background(
                            if (index == state.currentPage) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            }
                        )
                )
            }
        }

        // Action button
        Button(
            onClick = {
                if (state.currentPage == state.totalPages - 1) {
                    viewModel.onEvent(OnboardingEvent.GetStartedClicked)
                } else {
                    viewModel.onEvent(OnboardingEvent.NextClicked)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Text(
                text = if (state.currentPage == state.totalPages - 1) 
                    stringResource(Res.string.action_start) 
                else 
                    stringResource(Res.string.action_next),
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

/**
 * Data class representing a single onboarding page.
 */
private data class OnboardingPage(
    val title: StringResource,
    val description: StringResource
)
