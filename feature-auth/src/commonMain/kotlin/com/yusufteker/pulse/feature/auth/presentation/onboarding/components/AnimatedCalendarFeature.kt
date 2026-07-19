package com.yusufteker.pulse.feature.auth.presentation.onboarding.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AnimatedCalendarFeature() {
    val infiniteTransition = rememberInfiniteTransition()
    
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    // Progress timelines:
    // 0 -> 20: Cursor moves from bottom to FAB
    // 20 -> 25: FAB pressed (scale down)
    // 25 -> 40: Dialog slides up
    // 40 -> 60: Cursor moves to dialog save button
    // 60 -> 65: Save button pressed
    // 65 -> 80: Dialog slides down
    // 80 -> 100: New task pops in list

    val cursorOffsetX = when {
        progress < 20f -> 140f - (progress / 20f) * 45f // moves slightly left
        progress < 40f -> 95f
        progress < 60f -> 95f - ((progress - 40f) / 20f) * 35f 
        progress < 80f -> 60f
        else -> 140f
    }
    
    val cursorOffsetY = when {
        progress < 20f -> 140f - (progress / 20f) * 45f // moves up
        progress < 40f -> 95f
        progress < 60f -> 95f - ((progress - 40f) / 20f) * 35f 
        progress < 80f -> 60f
        else -> 140f
    }
    
    val cursorAlpha = when {
        progress < 80f -> 1f
        progress < 90f -> 1f - (progress - 80f) / 10f
        else -> 0f
    }

    val fabScale = when {
        progress in 20f..25f -> 0.8f
        else -> 1f
    }

    val sheetOffsetY = when {
        progress < 25f -> 240f
        progress < 40f -> 240f - ((progress - 25f) / 15f) * 240f
        progress < 65f -> 0f
        progress < 80f -> ((progress - 65f) / 15f) * 240f
        else -> 240f
    }

    val saveScale = when {
        progress in 60f..65f -> 0.8f
        else -> 1f
    }

    val newTaskScale = when {
        progress < 80f -> 0f
        progress < 90f -> ((progress - 80f) / 10f)
        else -> 1f
    }

    Box(
        modifier = Modifier
            .size(280.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
    ) {
        // Mock App UI
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Ekim 2026", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text("Bugün, 14 Çarşamba", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            // Calendar Days Row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                val days = listOf("Pzt" to "12", "Sal" to "13", "Çar" to "14", "Per" to "15", "Cum" to "16")
                days.forEachIndexed { i, day ->
                    val isSelected = i == 2
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .padding(vertical = 8.dp, horizontal = 4.dp)
                    ) {
                        Text(day.first, fontSize = 10.sp, color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(day.second, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            // Tasks List
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                // Existing Task
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.secondaryContainer).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Tasarım Toplantısı", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        Text("10:00 - 11:30", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
                    }
                }
                
                // New Task (Animated)
                Row(
                    modifier = Modifier.fillMaxWidth().scale(newTaskScale).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primaryContainer).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Pulsy Güncellemesi", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("14:00 - 15:00", fontSize = 10.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                    }
                }
            }
        }

        // FAB
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 8.dp, end = 8.dp)
                .size(48.dp)
                .scale(fabScale)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
        }

        // Dialog / Bottom Sheet
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(180.dp)
                .offset(y = sheetOffsetY.dp)
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Yeni Görev", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                
                // Text Field Mock
                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.background).padding(12.dp)) {
                    Text("Pulsy Güncellemesi", fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground)
                }
                
                // Time selector mock
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.background).padding(8.dp), contentAlignment = Alignment.Center) {
                        Text("14:00", fontSize = 10.sp)
                    }
                    Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.background).padding(8.dp), contentAlignment = Alignment.Center) {
                        Text("15:00", fontSize = 10.sp)
                    }
                }

                // Save Button
                Box(
                    modifier = Modifier
                        .align(Alignment.End)
                        .clip(RoundedCornerShape(20.dp))
                        .scale(saveScale)
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Kaydet", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
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
