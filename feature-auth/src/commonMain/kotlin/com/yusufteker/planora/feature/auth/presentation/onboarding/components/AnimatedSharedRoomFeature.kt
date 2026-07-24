package com.yusufteker.planora.feature.auth.presentation.onboarding.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import planora.core.generated.resources.Res
import planora.core.generated.resources.*

@Composable
fun AnimatedSharedRoomFeature() {
    val infiniteTransition = rememberInfiniteTransition()
    
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    // 0 -> 20: show rooms list, cursor moves to first room
    // 20 -> 25: room card clicked (scale)
    // 25 -> 40: room details slides in from right, list slides left out
    // 40 -> 60: inside room, avatars pop in, tasks show up
    // 60 -> 80: stay in room
    // 80 -> 100: slide back to list

    val cursorOffsetX = when {
        progress < 20f -> 100f - (progress / 20f) * 40f
        progress < 25f -> 60f
        else -> -100f // hidden
    }
    
    val cursorOffsetY = when {
        progress < 20f -> 120f - (progress / 20f) * 60f
        progress < 25f -> 60f
        else -> 120f
    }
    
    val cursorAlpha = when {
        progress < 25f -> 1f
        progress < 30f -> 1f - (progress - 25f) / 5f
        else -> 0f
    }

    val roomCardScale = when {
        progress in 20f..25f -> 0.95f
        else -> 1f
    }

    val listOffsetX = when {
        progress < 25f -> 0f
        progress < 40f -> -((progress - 25f) / 15f) * 280f
        progress < 80f -> -280f
        progress < 95f -> -280f + ((progress - 80f) / 15f) * 280f
        else -> 0f
    }
    
    val detailsOffsetX = when {
        progress < 25f -> 280f
        progress < 40f -> 280f - ((progress - 25f) / 15f) * 280f
        progress < 80f -> 0f
        progress < 95f -> ((progress - 80f) / 15f) * 280f
        else -> 280f
    }

    val avatar1Scale = when {
        progress < 45f -> 0f
        progress < 50f -> (progress - 45f) / 5f
        else -> 1f
    }
    val avatar2Scale = when {
        progress < 50f -> 0f
        progress < 55f -> (progress - 50f) / 5f
        else -> 1f
    }
    val sharedTaskScale = when {
        progress < 55f -> 0f
        progress < 60f -> (progress - 55f) / 5f
        else -> 1f
    }

    Box(
        modifier = Modifier
            .size(280.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
    ) {
        // List Screen
        Box(modifier = Modifier.fillMaxSize().offset(x = listOffsetX.dp)) {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Top Bar
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(Res.string.onboarding_title_2), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                    Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(24.dp))
                }
                
                // Room Card 1
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .scale(if (progress < 40f) roomCardScale else 1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(16.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(stringResource(Res.string.onboarding_design_team), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(stringResource(Res.string.onboarding_members_count, "4"), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                        }
                    }
                }
                // Room Card 2
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(16.dp)
                ) {
                    Column {
                        Text(stringResource(Res.string.onboarding_family), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(stringResource(Res.string.onboarding_members_count, "3"), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    }
                }
            }
        }

        // Details Screen
        Box(modifier = Modifier.fillMaxSize().offset(x = detailsOffsetX.dp)) {
             Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                // Top Bar
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
                    Text(stringResource(Res.string.onboarding_design_team), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                }
                
                // Avatars
                Row(horizontalArrangement = Arrangement.spacedBy((-12).dp)) {
                    Box(modifier = Modifier.size(40.dp).scale(avatar1Scale).clip(CircleShape).background(MaterialTheme.colorScheme.secondary), contentAlignment = Alignment.Center) {
                        Text("Y", color = MaterialTheme.colorScheme.onSecondary, fontWeight = FontWeight.Bold)
                    }
                    Box(modifier = Modifier.size(40.dp).scale(avatar2Scale).clip(CircleShape).background(MaterialTheme.colorScheme.tertiary), contentAlignment = Alignment.Center) {
                        Text("A", color = MaterialTheme.colorScheme.onTertiary, fontWeight = FontWeight.Bold)
                    }
                }

                // Shared tasks
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Task 1
                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(12.dp)) {
                         Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                              Box(modifier = Modifier.size(28.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondary), contentAlignment = Alignment.Center) {
                                  Text("Y", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSecondary)
                              }
                              Spacer(modifier = Modifier.width(12.dp))
                              Column {
                                  Text(stringResource(Res.string.onboarding_logo_revision), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                  Text("10:00 - Yusuf", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                              }
                         }
                    }
                    // Task 2 (pops in)
                    Box(modifier = Modifier.fillMaxWidth().scale(sharedTaskScale).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(12.dp)) {
                         Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                              Box(modifier = Modifier.size(28.dp).clip(CircleShape).background(MaterialTheme.colorScheme.tertiary), contentAlignment = Alignment.Center) {
                                  Text("A", fontSize = 10.sp, color = MaterialTheme.colorScheme.onTertiary)
                              }
                              Spacer(modifier = Modifier.width(12.dp))
                              Column {
                                  Text(stringResource(Res.string.onboarding_ui_review), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                  Text("14:00 - Ali", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                              }
                         }
                    }
                }
             }
        }

        // Cursor
        Box(
            modifier = Modifier
                .offset(x = cursorOffsetX.dp, y = cursorOffsetY.dp)
                .size(36.dp)
                .alpha(cursorAlpha)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Box(modifier = Modifier.size(18.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)))
        }
    }
}
