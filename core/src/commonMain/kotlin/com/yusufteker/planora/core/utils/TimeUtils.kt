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

/**
 * Computes the default deadline timestamp for a newly created task: 3 hours ahead of [nowMs]
 * with seconds and nanoseconds zeroed out.
 *
 * When [targetDate] is `null` or matches today's date in [timeZone], the date rolls over to
 * tomorrow automatically if adding 3 hours crosses midnight (e.g., 22:30 -> tomorrow 01:30).
 * When [targetDate] is a different explicitly selected date, [targetDate] is preserved and
 * paired with the `+3h` hour and minute.
 *
 * @param targetDate Optional calendar date chosen by the user.
 * @param nowMs Current epoch time in milliseconds (defaults to [getCurrentTimeMs]).
 * @param timeZone Time zone used for date/time conversion (defaults to system time zone).
 * @return Epoch milliseconds for the computed default task deadline.
 */
fun getDefaultTaskDeadlineMs(
    targetDate: LocalDate? = null,
    nowMs: Long = getCurrentTimeMs(),
    timeZone: TimeZone = TimeZone.currentSystemDefault()
): Long {
    val nowLdt = Instant.fromEpochMilliseconds(nowMs).toLocalDateTime(timeZone)
    val plusThreeLdt = Instant.fromEpochMilliseconds(nowMs + 3L * 60L * 60L * 1000L).toLocalDateTime(timeZone)
    val resolvedDate = if (targetDate == null || targetDate == nowLdt.date) plusThreeLdt.date else targetDate
    return LocalDateTime(
        year = resolvedDate.year,
        month = resolvedDate.month,
        dayOfMonth = resolvedDate.dayOfMonth,
        hour = plusThreeLdt.hour,
        minute = plusThreeLdt.minute,
        second = 0,
        nanosecond = 0
    ).toInstant(timeZone).toEpochMilliseconds()
}

