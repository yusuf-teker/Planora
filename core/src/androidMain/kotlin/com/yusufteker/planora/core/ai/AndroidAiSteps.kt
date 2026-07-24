package com.yusufteker.planora.core.ai

import android.content.Context
import android.content.pm.PackageManager
import com.yusufteker.planora.shared.ai.AiChatContext
import com.yusufteker.planora.shared.ai.AiChatResult
import com.yusufteker.planora.shared.ai.AiIntent
import com.yusufteker.planora.shared.ai.AiIntentPrompt
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

class AndroidGeminiNanoStep(
    private val appContext: Context
) : AiStep {
    override suspend fun process(input: String, context: AiChatContext): AiChatResult? {
        if (!isSystemAiAvailable()) return null

        Napier.d("AndroidGeminiNanoStep: Gemini Nano (AICore) deneniyor...", tag = "AiManagerLog")
        return try {
            tryInvokeAicoreGenerativeModel(input, context)
        } catch (e: Exception) {
            null
        }
    }

    private fun isSystemAiAvailable(): Boolean {
        return try {
            appContext.packageManager.getPackageInfo("com.google.android.aicore", 0) != null
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    private suspend fun tryInvokeAicoreGenerativeModel(input: String, context: AiChatContext): AiChatResult? {
        val aiManagerClass = Class.forName("com.google.ai.edge.aicore.AiManager")
        val getInstanceMethod = aiManagerClass.getMethod("getInstance", Context::class.java)
        getInstanceMethod.invoke(null, this.appContext)
        return null
    }
}
