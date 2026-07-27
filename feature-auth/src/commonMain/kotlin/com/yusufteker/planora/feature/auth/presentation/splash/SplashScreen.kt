package com.yusufteker.planora.feature.auth.presentation.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yusufteker.planora.core.base.CollectEffect
import com.yusufteker.planora.core.navigation.LocalNavigator
import com.yusufteker.planora.core.navigation.Screen
import org.jetbrains.compose.resources.painterResource
import planora.core.generated.resources.Res
import planora.core.generated.resources.app_icon_dark
import planora.core.generated.resources.app_icon_light

import org.koin.compose.koinInject
import com.yusufteker.planora.core.preferences.ThemePreferences

/**
 * Splash screen composable.
 *
 * Displays the Planora brand icon with dark and light mode support and a smooth entrance animation.
 */
@Composable
fun SplashScreen(
    viewModel: SplashViewModel
) {
    val navigator = LocalNavigator.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val themePreferences: ThemePreferences = koinInject()
    val isDarkModePref by themePreferences.isDarkMode.collectAsStateWithLifecycle(initialValue = null)
    val isDark = isDarkModePref ?: isSystemInDarkTheme()

    viewModel.effect.CollectEffect { effect ->
        when (effect) {
            is SplashEffect.NavigateToOnboarding -> navigator.setRoot(Screen.Onboarding)
            is SplashEffect.NavigateToHome -> navigator.setRoot(Screen.Main())
            is SplashEffect.NavigateToLogin -> navigator.setRoot(Screen.Login)
        }
    }

    val scale = remember { Animatable(0.85f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(durationMillis = 700)
        )
    }

    LaunchedEffect(Unit) {
        alpha.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(durationMillis = 700)
        )
    }

    val backgroundColor = if (isDark) Color(0xFF000000) else Color(0xFFFFFFFF)
    val logoResource = if (isDark) Res.drawable.app_icon_dark else Res.drawable.app_icon_light
    val textColor = if (isDark) Color.White else Color(0xFF1E1E1E)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .scale(scale.value)
                .alpha(alpha.value)
        ) {
            Image(
                painter = painterResource(logoResource),
                contentDescription = "Planora Logo",
                modifier = Modifier.size(160.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Planora",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                ),
                color = textColor
            )
        }
    }
}

