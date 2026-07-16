package com.yusufteker.pulse.core.ai

import com.yusufteker.pulse.shared.ai.AiChatContext
import com.yusufteker.pulse.shared.ai.AiChatResult
import com.yusufteker.pulse.shared.ai.AiIntent
import com.yusufteker.pulse.shared.ai.AiIntentPrompt
import com.yusufteker.pulse.shared.ai.LlmIntentJson
import com.yusufteker.pulse.shared.api.TaskType
import io.github.aakira.napier.Napier
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import com.yusufteker.pulse.core.utils.getCurrentTimeMs
import kotlin.coroutines.resume

class IosAppleIntelligenceStep(
    private val ruleBasedEngine: RuleBasedNlpEngine
) : AiStep {
    override suspend fun process(input: String, context: AiChatContext): AiChatResult? {
        val isDeviceAvailable = IosAiBridge.isAvailable?.invoke() ?: false
        val bridgeParse = IosAiBridge.parse
        if (!isDeviceAvailable || bridgeParse == null) {
            println("IosAppleIntelligenceStep: Apple Intelligence uygun değil.")
            return null
        }

        Napier.d("IosAppleIntelligenceStep: Apple Intelligence deneniyor...", tag = "AiManagerLog")
        val jsonResult = callBridge(bridgeParse, input) ?: run {
            println("IosAppleIntelligenceStep: Apple Intelligence null döndü.")
            return null
        }

        val intentJson = AiIntentPrompt.parse(jsonResult) ?: run {
            println("IosAppleIntelligenceStep: Apple Intelligence JSON decode başarısız.")
            return null
        }

        return buildResultFromIntentJson(intentJson, input, context, ruleBasedEngine)
    }

    private suspend fun callBridge(
        bridge: (String, (String?) -> Unit) -> Unit,
        input: String
    ): String? = suspendCancellableCoroutine { cont ->
        bridge(input) { result -> cont.resume(result) }
    }
}

// Ortak fonksiyonlar
internal suspend fun buildResultFromIntentJson(
    fmResult: LlmIntentJson,
    input: String,
    context: AiChatContext,
    ruleBasedEngine: RuleBasedNlpEngine
): AiChatResult {
    val baseResult = ruleBasedEngine.processMessage(input, context)
    val intent = mapIntent(fmResult.type)

    val question = fmResult.clarificationQuestion
    if (intent == AiIntent.CHAT && question != null) {
        return baseResult.copy(
            intent = AiIntent.CHAT,
            replyText = question,
            shouldCreateTask = false,
            needsClarification = fmResult.needsClarification,
            extractedEntities = baseResult.extractedEntities
        )
    }

    if ((intent == AiIntent.CREATE_TASK || intent == AiIntent.CREATE_EVENT) && fmResult.deadline == null) {
        return baseResult.copy(
            intent = AiIntent.CHAT,
            replyText = "'${fmResult.title}' için bir zaman belirtmek ister misin? (örn: yarın, 15 Aralık)",
            shouldCreateTask = false,
            needsClarification = true,
            extractedEntities = baseResult.extractedEntities?.copy(title = fmResult.title)
        )
    }

    val dateTimeMs = parseDeadline(fmResult.deadline, fmResult.time)
    val mergedEntities = baseResult.extractedEntities?.copy(
        title = fmResult.title,
        type = mapTaskType(fmResult.type),
        dateTime = dateTimeMs ?: baseResult.extractedEntities?.dateTime
    )
    val replyText = mergedEntities?.let { ruleBasedEngine.generateResponse(intent, it) } ?: baseResult.replyText

    return baseResult.copy(
        intent = intent,
        extractedEntities = mergedEntities,
        replyText = replyText,
        shouldCreateTask = true,
        needsClarification = false,
        suggestedTaskRequest = baseResult.suggestedTaskRequest?.copy(
            title = fmResult.title,
            type = mapTaskType(fmResult.type),
            startTime = dateTimeMs ?: baseResult.suggestedTaskRequest?.startTime ?: 0L
        )
    )
}

private fun mapIntent(type: String): AiIntent = when (type) {
    "task" -> AiIntent.CREATE_TASK
    "event" -> AiIntent.CREATE_EVENT
    "note" -> AiIntent.CREATE_NOTE
    else -> AiIntent.CHAT
}

private fun mapTaskType(type: String): TaskType = when (type) {
    "event" -> TaskType.EVENT
    "note" -> TaskType.NOTE
    else -> TaskType.TASK
}

private fun parseDeadline(deadline: String?, time: String?): Long? {
    if (deadline == null) return null
    return try {
        val date = LocalDate.parse(deadline)
        val localTime = time?.let { LocalTime.parse(it) } ?: LocalTime(9, 0)
        LocalDateTime(date, localTime).toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
    } catch (e: Exception) {
        null
    }
}
