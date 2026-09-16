package com.yusufteker.planora.core.utils

import kotlinx.datetime.*

expect fun getCurrentTimeMs(): Long

/**
 * Formats epoch milliseconds into a "HH:mm" time string.
 */
expect fun formatTime(epochMs: Long): String

/**
 * Formats epoch milliseconds into a short date string like "1 Jul" or "1 Jul 2026".
 */
expect fun formatShortDate(epochMs: Long): String

/**
 * Formats epoch milliseconds into a full day name like "Tuesday".
 */
expect fun formatDayName(epochMs: Long): String

/**
 * Formats epoch milliseconds into a full date string like "1 July 2026, Tuesday".
 */
expect fun formatFullDate(epochMs: Long): String

/**
 * Checks whether the given epoch milliseconds falls on today's date.
 */
expect fun isToday(epochMs: Long): Boolean

/**
 * Checks whether the given epoch milliseconds falls on tomorrow's date.
 */
expect fun isTomorrow(epochMs: Long): Boolean

/**
 * Represents relative time buckets for grouping tasks.
 */
enum class TimeBucket {
    TODAY,
    THIS_WEEK,
    THIS_MONTH,
    FUTURE,
    PAST
}

/**
 * Returns a relative time bucket for grouping tasks.
 *
 * @param epochMs The epoch milliseconds of the target date.
 * @return A [TimeBucket] representing the relative time group.
 */
expect fun getRelativeTimeBucket(epochMs: Long): TimeBucket

/**
 * Combines today's date with the time-of-day (hour and minute) from [originalTimeMs].
 * If [originalTimeMs] is null, returns current time.
 *
 * @param originalTimeMs The original event/task epoch milliseconds.
 * @return An epoch milliseconds timestamp representing today with the original hour and minute.
 */
fun getTodayWithOriginalTime(originalTimeMs: Long?): Long {
    val nowMs = getCurrentTimeMs()
    val tz = TimeZone.currentSystemDefault()
    val todayDate = Instant.fromEpochMilliseconds(nowMs).toLocalDateTime(tz).date
    if (originalTimeMs == null) {
        return nowMs
    }
    val originalLocal = Instant.fromEpochMilliseconds(originalTimeMs).toLocalDateTime(tz)
    return LocalDateTime(
        year = todayDate.year,
        month = todayDate.month,
        dayOfMonth = todayDate.dayOfMonth,
        hour = originalLocal.hour,
        minute = originalLocal.minute,
        second = 0,
        nanosecond = 0
    ).toInstant(tz).toEpochMilliseconds()
}

