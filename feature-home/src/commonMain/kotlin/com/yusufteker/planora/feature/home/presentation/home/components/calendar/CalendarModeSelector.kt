package com.yusufteker.planora.feature.home.presentation.home.components.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarViewDay
import androidx.compose.material.icons.filled.CalendarViewMonth
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import planora.core.generated.resources.Res
import planora.core.generated.resources.calendar_mode_day
import planora.core.generated.resources.calendar_mode_month
import planora.core.generated.resources.calendar_mode_week
import planora.core.generated.resources.calendar_mode_year

/**
 * Planora takvim alt navigasyon mod seçici bileşeni.
 *
 * Yıl, Ay, Hafta ve Gün modları arasında kolay geçiş sağlar.
 *
 * @param selectedMode Mevcut aktif takvim modu
 * @param onModeSelected Mod değişim olayı
 * @param modifier Dış düzenleyici
 */
@Composable
fun CalendarModeSelector(
    selectedMode: CalendarMode,
    onModeSelected: (CalendarMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ModeNavItem(
                label = stringResource(Res.string.calendar_mode_year),
                icon = Icons.Default.GridView,
                isSelected = selectedMode == CalendarMode.YEAR,
                onClick = { onModeSelected(CalendarMode.YEAR) }
            )
            ModeNavItem(
                label = stringResource(Res.string.calendar_mode_month),
                icon = Icons.Default.CalendarViewMonth,
                isSelected = selectedMode == CalendarMode.MONTH,
                onClick = { onModeSelected(CalendarMode.MONTH) }
            )
            ModeNavItem(
                label = stringResource(Res.string.calendar_mode_week),
                icon = Icons.Default.CalendarMonth,
                isSelected = selectedMode == CalendarMode.WEEK,
                onClick = { onModeSelected(CalendarMode.WEEK) }
            )
            ModeNavItem(
                label = stringResource(Res.string.calendar_mode_day),
                icon = Icons.Default.CalendarViewDay,
                isSelected = selectedMode == CalendarMode.DAY,
                onClick = { onModeSelected(CalendarMode.DAY) }
            )
        }
    }
}

@Composable
private fun ModeNavItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
    }

    val bgColor = if (isSelected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    } else {
        Color.Transparent
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            ),
            color = contentColor
        )
    }
}
