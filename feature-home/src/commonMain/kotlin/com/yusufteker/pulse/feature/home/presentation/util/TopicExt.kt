package com.yusufteker.pulse.feature.home.presentation.util

import androidx.compose.ui.graphics.Color
import com.yusufteker.pulse.feature.home.domain.model.Topic

val Topic.color: Color
    get() = when (this) {
        Topic.GENERAL -> Color(0xFF607D8B)
        Topic.TECHNOLOGY -> Color(0xFF2196F3)
        Topic.SOFTWARE -> Color(0xFF3F51B5)
        Topic.DESIGN -> Color(0xFFE91E63)
        Topic.NEWS -> Color(0xFFF44336)
        Topic.SPORTS -> Color(0xFFFF9800)
        Topic.ENTERTAINMENT -> Color(0xFF9C27B0)
        Topic.FINANCE -> Color(0xFF4CAF50)
        Topic.HEALTH -> Color(0xFF009688)
        Topic.ART -> Color(0xFFFBC02D) // Darker yellow for better contrast
        Topic.SCIENCE -> Color(0xFF00BCD4)
        Topic.TRAVEL -> Color(0xFF795548)
        Topic.FOOD -> Color(0xFFFF5722)
        Topic.MUSIC -> Color(0xFF673AB7)
        Topic.GAMING -> Color(0xFF8BC34A)
    }
