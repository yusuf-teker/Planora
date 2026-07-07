package com.yusufteker.pulse.core.ai

import android.content.Context
import android.content.pm.PackageManager
import com.yusufteker.pulse.shared.ai.AiAvailabilityState
import com.yusufteker.pulse.shared.ai.AiChatContext
import com.yusufteker.pulse.shared.ai.AiChatResult
import com.yusufteker.pulse.shared.ai.LocalLlmDecision
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Android AI Yöneticisi — 4 KADEMELİ FALLBACK ZİNCİRİ
 *
 * ADIM 1 → Gemini Nano (AICore)     : Cihazda sistem AI'ı yerleşikse, tamamen offline + hızlı.
 * ADIM 2 → İndirilebilir Local LLM  : Nano yoksa ama cihaz uygunsa (Android 12+, 6GB+ RAM),
 *                                      kullanıcıya küçük bir modeli indirmesini teklif ederiz.
 *                                      Kabul ederse indirir, sonraki mesajlarda onu kullanırız.
 * ADIM 3 → Cloud (Gemini API)       : Kullanıcı indirmeyi reddettiyse, cihaz uygun değilse ya
 *                                      da indirme henüz tamamlanmadıysa; internet varsa ve
 *                                      ücretsiz kota dolmadıysa buluttaki Gemini'ye düşeriz.
 * ADIM 4 → RuleBasedNlpEngine       : Yukarıdakilerin hepsi başarısız olursa (internet yok,
 *                                      kota bitti, hata vs.) her zaman çalışan kural tabanlı
 *                                      motor devreye girer. Bu adım ASLA null dönmez.
 *
 * processMessage() bu 4 adımı sırayla dener; ilk sonuç veren adımda durur.
 */
class AndroidAiManager(
    private val context: Context,
    private val cloudAiManager: CloudAiManager
) : OfflineAiManager {

    private val ruleBasedEngine = RuleBasedNlpEngine()
    private val _downloadProgress = MutableStateFlow<Float?>(null)

    private var cachedAvailability: AiAvailabilityState? = null

    /** Kullanıcının local LLM indirme teklifine verdiği cevap (uygulama açıkken hafızada tutulur). */
    private var localLlmDecision: LocalLlmDecision = LocalLlmDecision.NOT_ASKED

    // ═══════════════════════════════════════════════════════
    // GENEL AKIŞ
    // ═══════════════════════════════════════════════════════

    override suspend fun processMessage(input: String, context: AiChatContext): AiChatResult {
        // ADIM 1: Gemini Nano (AICore)
        step1_tryGeminiNano(input, context)?.let { return it }

        // ADIM 2: İndirilebilir Local LLM (kullanıcı daha önce kabul ettiyse ve model inmişse)
        step2_tryDownloadableLocalLlm(input, context)?.let { return it }

        // ADIM 3: Cloud Gemini API (ücretsiz kota dahilinde)
        step3_tryCloudApi(input, context)?.let { return it }

        // ADIM 4: Rule-Based — garanti fallback, her zaman bir sonuç döner
        return step4_ruleBasedFallback(input, context)
    }

    // ── ADIM 1: Gemini Nano (AICore) ──────────────────────────

    private suspend fun step1_tryGeminiNano(input: String, context: AiChatContext): AiChatResult? {
        if (!isSystemAiAvailable()) return null

        io.github.aakira.napier.Napier.d("ADIM 1 (Android): Gemini Nano (AICore) deneniyor...", tag = "AiManagerLog")
        return try {
            tryInvokeAicoreGenerativeModel(input, context)
        } catch (e: Exception) {
            // Nano cihazda var görünüyor ama çağrı başarısız oldu → ADIM 2'ye düş
            null
        }
    }

    private fun isSystemAiAvailable(): Boolean {
        return try {
            // com.google.android.aicore → Google Play sistem güncellemesiyle gelen AI servisi
            context.packageManager.getPackageInfo("com.google.android.aicore", 0) != null
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    /**
     * AI Core GenerativeModel API'sini dener (reflection ile, SDK henüz tam entegre değil).
     *
     * TODO: Google AI Edge SDK entegrasyonu tamamlanınca gerçek çağrı buraya gelecek:
     * ```kotlin
     * val model = aiManager.getGenerativeModel(GenerativeModelConfig(model = Model.GEMINI_NANO, ...))
     * val response = model.generateContent(input)
     * return parseJsonResponse(response.text)
     * ```
     * Şimdilik SDK bağlı olmadığı için null dönüp ADIM 2'ye geçiyoruz.
     */
    private suspend fun tryInvokeAicoreGenerativeModel(input: String, context: AiChatContext): AiChatResult? {
        val aiManagerClass = Class.forName("com.google.ai.edge.aicore.AiManager")
        val getInstanceMethod = aiManagerClass.getMethod("getInstance", Context::class.java)
        getInstanceMethod.invoke(null, this.context)

        return null
    }

    // ── ADIM 2: İndirilebilir Local LLM ───────────────────────

    /**
     * Cihaz uygun (Android 12+, 6GB+ RAM) ve kullanıcı daha önce reddetmemişse indirme teklif edilir.
     *
     * - NOT_ASKED  → UI'a "teklif göster" sinyali verilir (availabilityState = PROMPT_DOWNLOAD),
     *                bu mesaj için ADIM 3'e düşülür. Kullanıcı karar verince sonraki mesajlarda
     *                bu adım aktif olur.
     * - ACCEPTED   → model inmişse local LLM ile işlenir.
     * - DECLINED   → bu adım tamamen atlanır, direkt ADIM 3'e geçilir.
     */
    private suspend fun step2_tryDownloadableLocalLlm(input: String, context: AiChatContext): AiChatResult? {
        if (!isEdgeAiDownloadSupported()) return null
        if (localLlmDecision == LocalLlmDecision.DECLINED) return null

        if (localLlmDecision == LocalLlmDecision.NOT_ASKED) {
            cachedAvailability = AiAvailabilityState.PROMPT_DOWNLOAD
            // UI bu state'i gözlemleyip kullanıcıya sormalı: requestDownload() ya da declineLocalLlmDownload()
            return null
        }

        if (localLlmDecision == LocalLlmDecision.ACCEPTED && cachedAvailability == AiAvailabilityState.AVAILABLE) {
            io.github.aakira.napier.Napier.d("ADIM 2 (Android): İndirilebilir Local LLM deneniyor...", tag = "AiManagerLog")
            return try {
                tryInvokeLocalLlm(input, context)
            } catch (e: Exception) {
                null
            }
        }

        // İndirme hâlâ devam ediyor ya da henüz tamamlanmadı
        return null
    }

    private fun isEdgeAiDownloadSupported(): Boolean {
        // Edge AI SDK'nın çalışabilmesi için cihazın belirli şartları sağlaması gerekir:
        // - Android 12+
        // - Yeterli RAM (genelde 6GB+)
        return try {
            val sdkInt = android.os.Build.VERSION.SDK_INT
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? android.app.ActivityManager
            val memInfo = android.app.ActivityManager.MemoryInfo()
            activityManager?.getMemoryInfo(memInfo)
            val totalRamGb = memInfo?.totalMem?.let { it / (1024 * 1024 * 1024) } ?: 0

            sdkInt >= 31 && totalRamGb >= 6
        } catch (e: Exception) {
            false
        }
    }

    /**
     * TODO: MediaPipe LLM Inference API / Gemma entegrasyonu tamamlanınca burada gerçek çağrı olacak.
     * Şimdilik null dönüp ADIM 3'e (Cloud API) geçiyoruz.
     */
    private suspend fun tryInvokeLocalLlm(input: String, context: AiChatContext): AiChatResult? {
        return null
    }

    /** Kullanıcı indirme teklifini kabul ettiğinde UI tarafından çağrılır. */
    override suspend fun requestDownload() {
        if (!isEdgeAiDownloadSupported()) return

        localLlmDecision = LocalLlmDecision.ACCEPTED
        _downloadProgress.value = 0f

        try {
            // TODO: Gerçek SDK entegrasyonunda -> aiManager.requestModelDownload(Model.GEMMA_NANO)
            // Şimdilik indirme sürecini simüle ediyoruz (UI/UX akışını test etmek için).
            for (progress in 1..10) {
                kotlinx.coroutines.delay(300)
                _downloadProgress.value = progress / 10f
            }
            cachedAvailability = AiAvailabilityState.AVAILABLE
            _downloadProgress.value = null
        } catch (e: Exception) {
            _downloadProgress.value = null
            cachedAvailability = AiAvailabilityState.ERROR
        }
    }

    /** Kullanıcı indirme teklifini reddettiğinde UI tarafından çağrılır. Bir daha sorulmaz. */
    fun declineLocalLlmDownload() {
        localLlmDecision = LocalLlmDecision.DECLINED
        cachedAvailability = AiAvailabilityState.BASIC_ONLY
    }

    // ── ADIM 3: Cloud (Gemini API) ────────────────────────────

    private suspend fun step3_tryCloudApi(input: String, context: AiChatContext): AiChatResult? {
        io.github.aakira.napier.Napier.d("ADIM 3 (Android): Cloud (Gemini API) deneniyor...", tag = "AiManagerLog")
        return cloudAiManager.processMessage(input, context)
    }

    // ── ADIM 4: Rule-Based (garanti fallback) ─────────────────

    private suspend fun step4_ruleBasedFallback(input: String, context: AiChatContext): AiChatResult {
        io.github.aakira.napier.Napier.d("ADIM 4 (Android): Kural Tabanlı (RuleBasedNlpEngine) fallback çalışıyor...", tag = "AiManagerLog")
        return ruleBasedEngine.processMessage(input, context)
    }

    // ── Durum & Yaşam Döngüsü ──────────────────────────────────

    override fun availabilityState(): AiAvailabilityState {
        if (cachedAvailability != null) return cachedAvailability!!

        cachedAvailability = when {
            isSystemAiAvailable() -> AiAvailabilityState.AVAILABLE
            isEdgeAiDownloadSupported() && localLlmDecision != LocalLlmDecision.DECLINED -> AiAvailabilityState.PROMPT_DOWNLOAD
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