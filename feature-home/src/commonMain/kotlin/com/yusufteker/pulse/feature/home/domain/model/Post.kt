package com.yusufteker.pulse.feature.home.domain.model

data class Post(
    val id: String,
    val authorId: String,
    val authorName: String,
    val authorUsername: String,
    val content: String,
    val createdAt: Long,
    val likesCount: Int,
    val commentsCount: Int,
    val isLikedByMe: Boolean,
    val isBookmarkedByMe: Boolean,
    val topic: String
)
