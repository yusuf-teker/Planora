package com.yusufteker.pulse.core.ai

import com.yusufteker.pulse.shared.ai.AiAvailabilityState
import com.yusufteker.pulse.shared.ai.AiChatContext
import com.yusufteker.pulse.shared.ai.AiChatResult
import com.yusufteker.pulse.shared.ai.AiIntent
import com.yusufteker.pulse.shared.ai.AiIntentPrompt
import com.yusufteker.pulse.shared.ai.LlmIntentJson
import com.yusufteker.pulse.shared.ai.LocalLlmDecision
import com.yusufteker.pulse.shared.api.TaskType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

/**
 * iOS AI Yöneticisi — 4 KADEMELİ FALLBACK ZİNCİRİ (Android ile birebir aynı mantık)
 *
 * ADIM 1 → Apple Intelligence (Foundation Models) : iOS 26+ ve uyumlu cihazda (emülatörde
 *                                                     ÇALIŞMAZ) IosAiBridge üzerinden Swift'e
 *                                                     devredilir, gerçek generative sonuç alınır.
 * ADIM 2 → İndirilebilir Local LLM                 : Apple Intelligence yoksa/uyumsuzsa, kullanıcıya
 *                                                     küçük bir modeli (örn. MLC-LLM / GGUF) indirmesini
 *                                                     teklif ederiz. Kabul ederse indirir, kullanırız.
 * ADIM 3 → Cloud (Gemini API)                      : Kullanıcı indirmeyi reddettiyse, cihaz uygun
 *                                                     değilse ya da indirme tamamlanmadıysa; internet
 *                                                     varsa ve kota dolmadıysa Gemini API'ye düşeriz.
 * ADIM 4 → RuleBasedNlpEngine                       : Hepsi başarısız olursa her zaman çalışan kural
 *                                                     tabanlı motor devreye girer. ASLA null dönmez.
 */
class IosAiManager(
    private val cloudAiManager: CloudAiManager
) : OfflineAiManager {

    private val ruleBasedEngine = RuleBasedNlpEngine()
    private val _downloadProgress = MutableStateFlow<Float?>(null)
    private var cachedAvailability: AiAvailabilityState? = null
    private var localLlmDecision: LocalLlmDecision = LocalLlmDecision.NOT_ASKED

    override suspend fun processMessage(input: String, context: AiChatContext): AiChatResult {
        step1_tryAppleIntelligence(input, context)?.let { return it }
        step2_tryDownloadableLocalLlm(input, context)?.let { return it }
        step3_tryCloudApi(input, context)?.let { return it }
        return step4_ruleBasedFallback(input, context)
    }

    // ── ADIM 1: Apple Intelligence ──────────────────────────

    private suspend fun step1_tryAppleIntelligence(input: String, context: AiChatContext): AiChatResult? {
        val isDeviceAvailable = IosAiBridge.isAvailable?.invoke() ?: false
        val bridgeParse = IosAiBridge.parse
        if (!isDeviceAvailable || bridgeParse == null) {
            println("IosAiManager: Apple Intelligence uygun değil. ADIM 2'ye geçiliyor.")
            return null
        }

        io.github.aakira.napier.Napier.d("ADIM 1 (iOS): Apple Intelligence deneniyor...", tag = "AiManagerLog")
        val jsonResult = callBridge(bridgeParse, input) ?: run {
            println("IosAiManager: Apple Intelligence null döndü. ADIM 2'ye geçiliyor.")
            return null
        }

        val intentJson = AiIntentPrompt.parse(jsonResult) ?: run {
            println("IosAiManager: Apple Intelligence JSON decode başarısız.")
            return null
        }

        return buildResultFromIntentJson(intentJson, input, context)
    }

    private suspend fun callBridge(
        bridge: (String, (String?) -> Unit) -> Unit,
        input: String
    ): String? = suspendCancellableCoroutine { cont ->
        bridge(input) { result -> cont.resume(result) {} }
    }

    /** Apple Intelligence VE Local LLM'in ortak sonuç işleme mantığı — aynı JSON şemasını üretiyorlar. */
    private suspend fun buildResultFromIntentJson(
        fmResult: LlmIntentJson,
        input: String,
        context: AiChatContext
    ): AiChatResult {
        val baseResult = ruleBasedEngine.processMessage(input, context)
        val intent = mapIntent(fmResult.type)

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
                startTime = dateTimeMs ?: baseResult.suggestedTaskRequest?.startTime ?: 0L // todo sonra düzeltilecek
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

    // ── ADIM 2: İndirilebilir Local LLM (LocalLLMClient / Gemma 3 1B GGUF) ──

    private suspend fun step2_tryDownloadableLocalLlm(input: String, context: AiChatContext): AiChatResult? {
        if (!isLocalLlmDownloadSupported()) return null
        if (localLlmDecision == LocalLlmDecision.DECLINED) return null

        if (localLlmDecision == LocalLlmDecision.NOT_ASKED) {
            cachedAvailability = AiAvailabilityState.PROMPT_DOWNLOAD
            return null
        }

        if (localLlmDecision == LocalLlmDecision.ACCEPTED && cachedAvailability == AiAvailabilityState.AVAILABLE) {
            io.github.aakira.napier.Napier.d("ADIM 2 (iOS): İndirilebilir Local LLM deneniyor...", tag = "AiManagerLog")
            return try {
                tryInvokeLocalLlm(input, context)
            } catch (e: Exception) {
                null
            }
        }
        return null
    }

    /** Swift tarafı bridge closure'larını kurduysa (Package eklendi) local LLM desteklenir. */
    private fun isLocalLlmDownloadSupported(): Boolean {
        return IosAiBridge.downloadLocalModel != null && IosAiBridge.generateLocal != null
    }

    private suspend fun tryInvokeLocalLlm(input: String, context: AiChatContext): AiChatResult? {
        val generate = IosAiBridge.generateLocal ?: return null
        val fullPrompt = "${AiIntentPrompt.SYSTEM_PROMPT}\n\nKullanıcı mesajı: $input"

        val jsonResult = suspendCancellableCoroutine<String?> { cont ->
            generate(fullPrompt) { result -> cont.resume(result) {} }
        } ?: return null

        val intentJson = AiIntentPrompt.parse(jsonResult) ?: return null
        return buildResultFromIntentJson(intentJson, input, context)
    }

    override suspend fun requestDownload() {
        if (!isLocalLlmDownloadSupported()) return
        val download = IosAiBridge.downloadLocalModel ?: return

        // Model diskte zaten varsa indirmeyi atla, direkt kullanılabilir yap.
        if (IosAiBridge.isLocalModelDownloaded?.invoke() == true) {
            localLlmDecision = LocalLlmDecision.ACCEPTED
            cachedAvailability = AiAvailabilityState.AVAILABLE
            return
        }

        localLlmDecision = LocalLlmDecision.ACCEPTED
        _downloadProgress.value = 0f

        suspendCancellableCoroutine<Unit> { cont ->
            download(
                { progress -> _downloadProgress.value = progress },
                { success ->
                    _downloadProgress.value = null
                    cachedAvailability = if (success) AiAvailabilityState.AVAILABLE else AiAvailabilityState.ERROR
                    cont.resume(Unit) {}
                }
            )
        }
    }

    fun declineLocalLlmDownload() {
        localLlmDecision = LocalLlmDecision.DECLINED
        cachedAvailability = AiAvailabilityState.BASIC_ONLY
    }

    // ── ADIM 3 / 4 ve availabilityState/release değişmedi ──

    private suspend fun step3_tryCloudApi(input: String, context: AiChatContext): AiChatResult? {
        io.github.aakira.napier.Napier.d("ADIM 3 (iOS): Cloud (Gemini API) deneniyor...", tag = "AiManagerLog")
        return cloudAiManager.processMessage(input, context)
    }

    private suspend fun step4_ruleBasedFallback(input: String, context: AiChatContext): AiChatResult {
        io.github.aakira.napier.Napier.d("ADIM 4 (iOS): Kural Tabanlı (RuleBasedNlpEngine) fallback çalışıyor...", tag = "AiManagerLog")
        return ruleBasedEngine.processMessage(input, context)
    }

    override fun availabilityState(): AiAvailabilityState {
        if (cachedAvailability != null) return cachedAvailability!!
        val appleAvailable = IosAiBridge.isAvailable?.invoke() ?: false
        cachedAvailability = when {
            appleAvailable -> AiAvailabilityState.AVAILABLE
            isLocalLlmDownloadSupported() && localLlmDecision != LocalLlmDecision.DECLINED -> AiAvailabilityState.PROMPT_DOWNLOAD
            else -> AiAvailabilityState.BASIC_ONLY
        }
        return cachedAvailability!!
    }

    override fun downloadProgress(): StateFlow<Float?> = _downloadProgress.asStateFlow()
    override fun release() {
        _downloadProgress.value = null
        cachedAvailability = null
    }
}

@Serializable
data class FoundationModelIntentResult(
    val type: String,          // "task" | "note" | "event"
    val title: String,
    val hasDeadline: Boolean,
    val deadline: String?,     // "YYYY-MM-DD"
    val time: String?          // "HH:mm"
)

// ═══════════════════════════════════════════════════════════
// iOS NLP Analiz Veri Sınıfları (mevcut, değişmedi)
// ═══════════════════════════════════════════════════════════

/** Apple Intelligence / NaturalLanguage analiz sonucu. */
/*data class AppleNlpAnalysis(
    val detectedLanguage: String?,
    val tokens: List<String>,
    val namedEntities: List<NamedEntity>,
    val sentiment: String?,
    val languageConfidence: Int
)*/

/** NSLinguisticTagger ile bulunan adlandırılmış varlık. */
/*data class NamedEntity(
    val text: String,
    val type: String, // "PersonalName", "PlaceName", "OrganizationName"
    val range: IntRange
)*/