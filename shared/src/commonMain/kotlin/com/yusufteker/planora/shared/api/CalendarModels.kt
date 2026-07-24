package com.yusufteker.planora.shared.api

import kotlinx.serialization.Serializable

/**
 * The response sent back from the server when requesting a list of calendar access requests.
 */
@Serializable
data class CalendarAccessRequestDto(
    val id: Int,
    val requesterId: Int,
    val requesterName: String,
    val requesterUsername: String,
    val requesterAvatarId: String,
    val status: String,
    val requesterProfileImageUrl: String? = null
)

/**
 * The response representing a user who has granted calendar access to me (or I granted to them).
 */
@Serializable
data class CalendarAccessGrantDto(
    val userId: Int,
    val name: String,
    val username: String,
    val avatarId: String,
    val color: String,
    val profileImageUrl: String? = null
)

/**
 * The response containing the shared tasks for a specific user.
 */
@Serializable
data class SharedCalendarTasksResponse(
    val userId: Int,
    val tasks: List<TaskDto>
)
