package com.yusufteker.pulse.shared.api

import kotlinx.serialization.Serializable

@Serializable
data class PostDto(
    val id: String,
    val authorId: String,
    val authorName: String,
    val authorUsername: String,
    val content: String,
    val createdAt: Long,
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val isLikedByMe: Boolean = false
)

@Serializable
data class FeedResponse(
    val posts: List<PostDto>,
    val nextCursor: String?,
    val hasMore: Boolean
)
