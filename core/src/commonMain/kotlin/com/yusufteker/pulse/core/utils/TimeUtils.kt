package com.yusufteker.pulse.core.utils

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

expect fun getCurrentTimeMs(): Long

/**
 * Epoch millis -> "14:30" gibi saat:dakika formatı
 */
expect fun formatTime(epochMs: Long): String

/**
 * Epoch millis -> "1 Tem" gibi gün ay kısa formatı
 */
expect fun formatShortDate(epochMs: Long): String

/**
 * Epoch millis -> "Salı" gibi gün adı
 */
expect fun formatDayName(epochMs: Long): String

/**
 * Epoch millis -> "1 Temmuz 2026, Salı" gibi tam tarih
 */
expect fun formatFullDate(epochMs: Long): String

/**
 * Verilen epochMs bugün mü?
 */
expect fun isToday(epochMs: Long): Boolean

/**
 * Verilen epochMs yarın mı?
 */
expect fun isTomorrow(epochMs: Long): Boolean

/**
 * Returns a relative time bucket for grouping tasks.
 * Buckets: "Bugün", "Bu Hafta", "Bu Ay", "İleri Tarihli", "Geçmiş"
 */
fun getRelativeTimeBucket(epochMs: Long): String {
    val nowMs = getCurrentTimeMs()
    if (isToday(epochMs)) return "Bugün"
    
    // Fallback to kotlinx.datetime for complex logic
    try {
        val timeZone = kotlinx.datetime.TimeZone.currentSystemDefault()
        val targetDate = kotlinx.datetime.Instant.fromEpochMilliseconds(epochMs).toLocalDateTime(timeZone).date
        val today = kotlinx.datetime.Instant.fromEpochMilliseconds(nowMs).toLocalDateTime(timeZone).date
        
        if (targetDate < today) return "Geçmiş"
        
        val daysDiff = targetDate.toEpochDays() - today.toEpochDays()
        
        // This week (until Sunday)
        val daysUntilSunday = 7 - today.dayOfWeek.value
        if (daysDiff <= daysUntilSunday) return "Bu Hafta"
        
        // This month
        if (targetDate.monthNumber == today.monthNumber && targetDate.year == today.year) {
            return "Bu Ay"
        }
        
        return "İleri Tarihli"
    } catch (e: Exception) {
        return "İleri Tarihli"
    }
}
