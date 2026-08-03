package com.yusufteker.planora.feature.home.domain.use_case

import com.yusufteker.planora.feature.home.domain.repository.PlanRepository
import io.ktor.http.Url

sealed class DeepLinkResult {
    data class NavigateToEvent(
        val eventId: String? = null,
        val planRoomId: String? = null,
        val sharedTitle: String? = null,
        val sharedNote: String? = null,
        val sharedDate: Long? = null,
        val sharedSender: String? = null
    ) : DeepLinkResult()

    data class NavigateToTask(
        val taskId: String? = null,
        val sharedTitle: String? = null,
        val sharedNote: String? = null,
        val sharedDate: Long? = null,
        val sharedSender: String? = null
    ) : DeepLinkResult()

    data class NavigateToNote(
        val noteId: String? = null,
        val sharedNote: String? = null,
        val sharedSender: String? = null
    ) : DeepLinkResult()

    data class NavigateToRoom(
        val roomId: String
    ) : DeepLinkResult()

    data class NavigateToCalendar(
        val dateString: String? = null
    ) : DeepLinkResult()

    data object NavigateToPlanRooms : DeepLinkResult()

    data object InvalidOrIgnored : DeepLinkResult()
}

class ProcessDeepLinkUseCase(
    private val planRepository: PlanRepository
) {
    suspend operator fun invoke(deepLinkUrl: String): DeepLinkResult {
        if (deepLinkUrl.isEmpty()) return DeepLinkResult.InvalidOrIgnored

        try {
            val url = Url(deepLinkUrl)
            val isPlanoraScheme = url.protocol.name == "planora" && url.host == "share"
            val pathSegments = url.rawSegments.filter { it.isNotEmpty() }
            val isHttpScheme = (url.protocol.name == "http" || url.protocol.name == "https") &&
                               (url.host == "planora.yusufteker.com" || url.host == "pulse.yusufteker.com") &&
                               pathSegments.firstOrNull() == "share"

            if (!isPlanoraScheme && !isHttpScheme) {
                return DeepLinkResult.InvalidOrIgnored
            }

            val type = if (isHttpScheme) pathSegments.getOrNull(1) ?: "" else pathSegments.firstOrNull() ?: ""
            val title = url.parameters["title"]
            val note = url.parameters["note"]
            val dateString = url.parameters["date"]
            val date = dateString?.toLongOrNull()
            val sender = url.parameters["sender"]
            
            val eventId = url.parameters["eventId"]
            val taskId = url.parameters["taskId"]
            val roomId = url.parameters["roomId"]

            return when (type) {
                "event" -> {
                    DeepLinkResult.NavigateToEvent(
                        eventId = eventId,
                        sharedTitle = title,
                        sharedNote = note,
                        sharedDate = date,
                        sharedSender = sender
                    )
                }
                "joinEvent" -> {
                    // Logic to join the event automatically
                    if (eventId != null && roomId != null) {
                        try {
                            planRepository.joinTask(taskId = eventId, roomId = roomId)
                        } catch (e: Exception) {
                            io.github.aakira.napier.Napier.w("Failed to join task via deep link: ${e.message}")
                        }
                    }
                    DeepLinkResult.NavigateToEvent(
                        eventId = eventId,
                        planRoomId = roomId,
                        sharedSender = sender
                    )
                }
                "task" -> {
                    DeepLinkResult.NavigateToTask(
                        taskId = taskId,
                        sharedTitle = title,
                        sharedNote = note,
                        sharedDate = date,
                        sharedSender = sender
                    )
                }
                "note" -> {
                    DeepLinkResult.NavigateToNote(
                        sharedNote = note,
                        sharedSender = sender
                    )
                }
                "calendar" -> {
                    DeepLinkResult.NavigateToCalendar(dateString = url.parameters["date"])
                }
                "roomInvite" -> {
                    DeepLinkResult.NavigateToPlanRooms
                }
                "room", "joinRoom" -> {
                    if (roomId != null) {
                        DeepLinkResult.NavigateToRoom(roomId = roomId)
                    } else {
                        DeepLinkResult.NavigateToPlanRooms
                    }
                }
                else -> DeepLinkResult.InvalidOrIgnored
            }
        } catch (e: Exception) {
            io.github.aakira.napier.Napier.w("Error parsing deep link in UseCase: ${e.message}")
            return DeepLinkResult.InvalidOrIgnored
        }
    }
}
