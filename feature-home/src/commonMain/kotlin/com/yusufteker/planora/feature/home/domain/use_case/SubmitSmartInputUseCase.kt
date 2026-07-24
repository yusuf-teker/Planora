package com.yusufteker.planora.feature.home.domain.use_case

import com.yusufteker.planora.core.utils.getCurrentTimeMs
import com.yusufteker.planora.feature.home.domain.repository.PlanRepository
import com.yusufteker.planora.feature.home.domain.repository.ProfileRepository
import com.yusufteker.planora.shared.api.CreateTaskRequest
import com.yusufteker.planora.shared.api.ItemDetails
import com.yusufteker.planora.shared.api.TaskType

class SubmitSmartInputUseCase(
    private val planRepository: PlanRepository,
    private val profileRepository: ProfileRepository
) {
    suspend operator fun invoke(text: String) {
        if (text.isBlank()) return

        // AI Simulation: Check if any follower's name is in the text
        val followers = profileRepository.getFollowingUsers().getOrNull() ?: emptyList()
        val mentionedUser = followers.find { text.contains(it.username, ignoreCase = true) }

        val participantsMap = if (mentionedUser != null) {
            mapOf(mentionedUser.id to mentionedUser.username)
        } else {
            emptyMap()
        }

        val request = CreateTaskRequest(
            title = if (mentionedUser != null) "AI Gen: Event with ${mentionedUser.username}" else "AI Gen: ${text.take(15)}...",
            description = text,
            startTime = getCurrentTimeMs(),
            type = if (mentionedUser != null) TaskType.EVENT else TaskType.NOTE,
            specificDetails = ItemDetails.Event(),
            participants = participantsMap
        )
        planRepository.createTask(request)
    }
}
