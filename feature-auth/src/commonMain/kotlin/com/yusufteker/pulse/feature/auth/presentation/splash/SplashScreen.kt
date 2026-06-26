package com.yusufteker.pulse.feature.auth.presentation.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yusufteker.pulse.core.base.CollectEffect
import com.yusufteker.pulse.core.navigation.LocalNavigator
import com.yusufteker.pulse.core.navigation.Screen
import kotlinx.coroutines.delay

/**
 * Splash screen composable.
 *
 * Displays the Pulse brand with a scale + fade animation,
 * then navigates to the next screen.
 */
@Composable
fun SplashScreen(
    viewModel: SplashViewModel
) {
    val navigator = LocalNavigator.current
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Collect side effects
    viewModel.effect.CollectEffect { effect ->
        when (effect) {
            is SplashEffect.NavigateToOnboarding -> navigator.setRoot(Screen.Onboarding)
            is SplashEffect.NavigateToHome -> navigator.setRoot(Screen.Main)
        }
    }

    // Splash animation
    val scale = remember { Animatable(0.3f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800)
        )
        alpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 600)
        )
        delay(500)
        viewModel.onEvent(SplashEvent.AnimationCompleted)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .scale(scale.value)
                .alpha(alpha.value)
        ) {
            Icon(
                imageVector = Icons.Rounded.FavoriteBorder,
                contentDescription = "Pulse Logo",
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Pulse",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}
