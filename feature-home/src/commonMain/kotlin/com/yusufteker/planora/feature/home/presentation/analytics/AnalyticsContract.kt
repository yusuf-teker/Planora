package com.yusufteker.planora.feature.home.presentation.analytics

import com.yusufteker.planora.core.base.UiEffect
import com.yusufteker.planora.core.base.UiEvent
import com.yusufteker.planora.core.base.UiState
import com.yusufteker.planora.shared.api.TaskPriority
import com.yusufteker.planora.shared.api.TaskType

/**
 * Günlük görev tamamlama verilerini temsil eder.
 *
 * @property dayLabel Günün kısa adı (örn: Pzt, Sal, Çrş / Mon, Tue).
 * @property completedCount Tamamlanan görev adedi.
 * @property totalCount İlgili gün için planlanan toplam görev adedi.
 * @property isToday Bugün olup olmadığı.
 */
data class DayProductivity(
    val dayLabel: String,
    val completedCount: Int,
    val totalCount: Int,
    val isToday: Boolean = false
)

/**
 * Verimlilik ve İstatistikler ekranı UI durumu.
 *
 * @property isLoading Verilerin hesaplanma durumu.
 * @property isPremium Kullanıcının Premium abonelik durumu.
 * @property totalTasks Toplam görev sayısı.
 * @property completedTasks Tamamlanan görev sayısı.
 * @property completionRate Tamamlama başarı yüzdesi (0.0f - 1.0f).
 * @property currentStreakDays Kesintisiz görev tamamlama serisi (gün sayısı).
 * @property weeklyProductivity Son 7 günün günlük performans dağılımı.
 * @property priorityBreakdown Görevlerin öncelik dağılımı (Yüksek, Orta, Düşük).
 * @property typeBreakdown Plan türü dağılımı (Görev, Etkinlik, Not).
 * @property peakHourText En yüksek verimliliğin sağlandığı saat aralığı.
 * @property monthlyTrend Son 30 günün günlük tamamlama verisi (PRO). Her item bir günü temsil eder.
 * @property bestDayLabel En verimli gün adı (PRO).
 * @property worstDayLabel En düşük verimlilik gün adı (PRO).
 * @property longestStreakDays Tarihsel en uzun streak (PRO).
 * @property avgDailyCompletions Son 30 gün günlük ortalama tamamlama (PRO).
 */
data class AnalyticsState(
    val isLoading: Boolean = true,
    val isPremium: Boolean = false,
    val totalTasks: Int = 0,
    val completedTasks: Int = 0,
    val completionRate: Float = 0f,
    val currentStreakDays: Int = 0,
    val weeklyProductivity: List<DayProductivity> = emptyList(),
    val priorityBreakdown: Map<TaskPriority, Int> = emptyMap(),
    val typeBreakdown: Map<TaskType, Int> = emptyMap(),
    val peakHourText: String = "09:00 - 12:00",
    // ── PRO-only fields ──
    val monthlyTrend: List<DayProductivity> = emptyList(),
    val bestDayLabel: String = "",
    val worstDayLabel: String = "",
    val longestStreakDays: Int = 0,
    val avgDailyCompletions: Float = 0f
) : UiState

/**
 * Kullanıcı arayüzünden tetiklenen eylemler.
 */
sealed interface AnalyticsEvent : UiEvent {
    /** Geri butonuna tıklanması. */
    data object BackClicked : AnalyticsEvent
    /** Premium'a yükselt banner/butonuna tıklanması. */
    data object UpgradeClicked : AnalyticsEvent
}

/**
 * Tek seferlik navigasyon ve arayüz yan etkileri.
 */
sealed interface AnalyticsEffect : UiEffect {
    /** Önceki ekrana dönüş. */
    data object NavigateBack : AnalyticsEffect
    /** Planora Premium satın alma ekranına yönlendirme. */
    data object NavigateToPremium : AnalyticsEffect
}
