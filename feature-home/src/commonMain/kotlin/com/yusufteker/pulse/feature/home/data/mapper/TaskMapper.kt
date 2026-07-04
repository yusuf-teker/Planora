package com.yusufteker.pulse.feature.home.data.mapper

import com.yusufteker.pulse.core.database.PulsyDatabaseQueries
import com.yusufteker.pulse.shared.api.CreateTaskRequest
import com.yusufteker.pulse.shared.api.TaskDto
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun PulsyDatabaseQueries.insertTaskFromDto(task: TaskDto, isSynced: Long) {
    insertTask(
        id = task.id,
        creatorId = task.creatorId.toLong(),
        title = task.title,
        description = task.description,
        startTime = task.startTime,
        endTime = task.endTime,
        type = task.type.name,
        status = task.status.name,
        visibility = task.visibility.name,
        isRecurring = if (task.isRecurring) 1L else 0L,
        recurrenceRule = task.recurrenceRule,
        isFlexible = if (task.isFlexible) 1L else 0L,
        isOptional = if (task.isOptional) 1L else 0L,
        isPostponable = if (task.isPostponable) 1L else 0L,
        isAllDay = if (task.isAllDay) 1L else 0L,
        aiMetadata = task.aiMetadata?.let { Json.encodeToString(it) },
        reminders = if (task.reminders.isNotEmpty()) Json.encodeToString(task.reminders) else null,
        specificDetails = task.specificDetails?.let { Json.encodeToString(it) },
        tags = if (task.tags.isNotEmpty()) Json.encodeToString(task.tags) else null,
        color = task.color,
        parentId = task.parentId,
        participants = task.participants.takeIf { it.isNotEmpty() }?.let { Json.encodeToString(it) },
        isSynced = isSynced
    )
}

fun PulsyDatabaseQueries.insertTaskFromRequest(
    id: String,
    creatorId: Long,
    request: CreateTaskRequest,
    isSynced: Long
) {
    insertTask(
        id = id,
        creatorId = creatorId,
        title = request.title,
        description = request.description,
        startTime = request.startTime,
        endTime = request.endTime,
        type = request.type.name,
        status = request.status.name,
        visibility = request.visibility.name,
        isRecurring = if (request.isRecurring) 1L else 0L,
        recurrenceRule = request.recurrenceRule,
        isFlexible = if (request.isFlexible) 1L else 0L,
        isOptional = if (request.isOptional) 1L else 0L,
        isPostponable = if (request.isPostponable) 1L else 0L,
        isAllDay = if (request.isAllDay) 1L else 0L,
        aiMetadata = request.aiMetadata?.let { Json.encodeToString(it) },
        reminders = if (request.reminders.isNotEmpty()) Json.encodeToString(request.reminders) else null,
        specificDetails = request.specificDetails?.let { Json.encodeToString(it) },
        tags = if (request.tags.isNotEmpty()) Json.encodeToString(request.tags) else null,
        color = request.color,
        parentId = request.parentId,
        participants = request.participants.takeIf { it.isNotEmpty() }?.let { Json.encodeToString(it) },
        isSynced = isSynced
    )
}
