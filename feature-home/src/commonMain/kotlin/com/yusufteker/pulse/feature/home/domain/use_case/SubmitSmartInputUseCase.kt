package com.yusufteker.pulse.feature.home.domain.use_case

import com.yusufteker.pulse.core.utils.getCurrentTimeMs
import com.yusufteker.pulse.feature.home.domain.repository.PlanRepository
import com.yusufteker.pulse.feature.home.domain.repository.ProfileRepository
import com.yusufteker.pulse.shared.api.CreateTaskRequest
import com.yusufteker.pulse.shared.api.ItemDetails
import com.yusufteker.pulse.shared.api.TaskType

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
            mapOf(mentionedUser.id to "PENDING")
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
