package com.yusufteker.pulse.core.utils

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
