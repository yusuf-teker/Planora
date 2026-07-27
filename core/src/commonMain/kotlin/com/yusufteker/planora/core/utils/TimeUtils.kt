package com.yusufteker.planora.core.utils

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

