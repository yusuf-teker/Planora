package com.yusufteker.planora.feature.home.presentation.util

import androidx.compose.ui.graphics.Color
import com.yusufteker.planora.feature.home.domain.model.Topic
import org.jetbrains.compose.resources.StringResource
import planora.core.generated.resources.Res
import planora.core.generated.resources.*

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

val Topic.titleRes: StringResource
    get() = when (this) {
        Topic.GENERAL -> Res.string.topic_general
        Topic.TECHNOLOGY -> Res.string.topic_technology
        Topic.SOFTWARE -> Res.string.topic_software
        Topic.DESIGN -> Res.string.topic_design
        Topic.NEWS -> Res.string.topic_news
        Topic.SPORTS -> Res.string.topic_sports
        Topic.ENTERTAINMENT -> Res.string.topic_entertainment
        Topic.FINANCE -> Res.string.topic_finance
        Topic.HEALTH -> Res.string.topic_health
        Topic.ART -> Res.string.topic_art
        Topic.SCIENCE -> Res.string.topic_science
        Topic.TRAVEL -> Res.string.topic_travel
        Topic.FOOD -> Res.string.topic_food
        Topic.MUSIC -> Res.string.topic_music
        Topic.GAMING -> Res.string.topic_gaming
    }
