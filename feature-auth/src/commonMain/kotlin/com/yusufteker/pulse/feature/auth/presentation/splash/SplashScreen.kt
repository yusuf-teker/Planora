package com.yusufteker.pulse.feature.auth.presentation.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Color

/**
 * Splash screen composable.
 *
 * Displays the Pulsy brand with a scale + fade animation,
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    )
}
