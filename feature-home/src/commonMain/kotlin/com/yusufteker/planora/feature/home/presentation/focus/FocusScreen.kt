package com.yusufteker.planora.feature.home.presentation.focus

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yusufteker.planora.core.ui.components.ParticleBurstEffect
import com.yusufteker.planora.core.ui.components.bounceClick
import org.jetbrains.compose.resources.stringResource
import planora.core.generated.resources.Res
import planora.core.generated.resources.*
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusScreen(
    viewModel: FocusViewModel,
    taskId: String?,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(taskId) {
        viewModel.loadTask(taskId)
    }

    var showExitDialog by remember { mutableStateOf(false) }

    val handleBack = {
        if (state.isRunning) {
            showExitDialog = true
        } else {
            onNavigateBack()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is FocusEffect.NavigateBack -> onNavigateBack()
                is FocusEffect.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = handleBack) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Text(
                    text = "Odak Modu",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.width(48.dp)) // for balance
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Task Title
            Text(
                text = state.taskTitle,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))
            
            // Duration Selector with GlassChip
            AnimatedVisibility(visible = !state.isRunning && !state.isFinished) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    val durations = listOf(15, 25, 45, 60)
                    durations.forEach { minutes ->
                        val isSelected = state.selectedDurationMinutes == minutes
                        com.yusufteker.planora.core.ui.components.GlassChip(
                            text = "${minutes}dk",
                            isSelected = isSelected,
                            onClick = { viewModel.setFocusDuration(minutes) },
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Breathing Timer Circle
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(280.dp)
            ) {
                FocusBreathingCircle(isRunning = state.isRunning)
                
                if (state.isFinished) {
                    ParticleBurstEffect(
                        modifier = Modifier.fillMaxSize(),
                        particleColor = MaterialTheme.colorScheme.primary,
                        particleCount = 60
                    )
                }
                
                TimerTextDisplay(timeRemainingSeconds = state.timeRemainingSeconds)
            }

            Spacer(modifier = Modifier.weight(1f))

            // Controls
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = { viewModel.resetTimer() }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(32.dp))

                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .bounceClick()
                        .clickable(onClick = { viewModel.toggleTimer() }),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (state.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Toggle",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(40.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(32.dp))
                
                // Placeholder for balance
                Spacer(modifier = Modifier.width(48.dp)) 
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Complete Task Button with GlowButton
            com.yusufteker.planora.core.ui.components.GlowButton(
                text = stringResource(Res.string.action_complete_task),
                onClick = { viewModel.completeTask() },
                isLoading = state.isCompleting,
                gradientColors = com.yusufteker.planora.core.theme.PlanoraColors.GradientEmeraldTeal,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                icon = {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = androidx.compose.ui.graphics.Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        if (showExitDialog) {
            AlertDialog(
                onDismissRequest = { showExitDialog = false },
                title = { Text(stringResource(Res.string.focus_exit_title)) },
                text = { Text(stringResource(Res.string.focus_exit_desc)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showExitDialog = false
                            viewModel.completeTask()
                        }
                    ) {
                        Text(stringResource(Res.string.action_confirm))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showExitDialog = false
                            viewModel.resetTimer()
                            onNavigateBack()
                        }
                    ) {
                        Text(stringResource(Res.string.focus_exit_and_cancel))
                    }
                }
            )
        }
    }
}

@Composable
fun FocusBreathingCircle(isRunning: Boolean) {
    val infiniteTransition = rememberInfiniteTransition()
    val planoraGlowColor = MaterialTheme.colorScheme.primary

    // Breathing scale animation
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = if (isRunning) 1.15f else 0.95f, // Only breathe if running
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    // Opacity animation
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = if (isRunning) 0.5f else 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val radius = (size.width / 2) * scale
        val centerOffset = Offset(size.width / 2, size.height / 2)

        // Outer glow
        drawCircle(
            color = planoraGlowColor.copy(alpha = alpha),
            radius = radius,
            center = centerOffset
        )
        
        // Inner static border
        drawCircle(
            color = planoraGlowColor.copy(alpha = 0.8f),
            radius = (size.width / 2) * 0.85f,
            center = centerOffset,
            style = Stroke(width = 6.dp.toPx())
        )
    }
}

@Composable
fun TimerTextDisplay(timeRemainingSeconds: Int) {
    val minutes = timeRemainingSeconds / 60
    val seconds = timeRemainingSeconds % 60
    val timeStr = "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"

    Text(
        text = timeStr,
        style = MaterialTheme.typography.displayLarge.copy(
            fontSize = 72.sp,
            fontWeight = FontWeight.Black
        ),
        color = MaterialTheme.colorScheme.onBackground
    )
}

