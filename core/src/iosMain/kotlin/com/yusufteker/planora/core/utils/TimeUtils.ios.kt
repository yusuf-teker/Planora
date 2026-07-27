package com.yusufteker.planora.core.utils

import io.ktor.client.request.invoke
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitDay
import platform.Foundation.NSCalendarUnitMonth
import platform.Foundation.NSCalendarUnitYear
import platform.Foundation.date
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.timeIntervalSince1970
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

actual fun getCurrentTimeMs(): Long {
    return (NSDate.date().timeIntervalSince1970 * 1000).toLong()
}

private fun dateFromMs(epochMs: Long): NSDate {
    return NSDate.dateWithTimeIntervalSince1970(epochMs / 1000.0)
}

actual fun formatTime(epochMs: Long): String {
    val formatter = NSDateFormatter()
    formatter.dateFormat = "HH:mm"
    return formatter.stringFromDate(dateFromMs(epochMs))
}

actual fun formatShortDate(epochMs: Long): String {
    val date = dateFromMs(epochMs)
    val calendar = NSCalendar.currentCalendar
    val nowYear = calendar.component(NSCalendarUnitYear, fromDate = NSDate.date())
    val targetYear = calendar.component(NSCalendarUnitYear, fromDate = date)
    val formatter = NSDateFormatter()
    formatter.dateFormat = if (nowYear == targetYear) "d MMM" else "d MMM yyyy"
    return formatter.stringFromDate(date)
}

actual fun formatDayName(epochMs: Long): String {
    val formatter = NSDateFormatter()
    formatter.dateFormat = "EEEE"
    return formatter.stringFromDate(dateFromMs(epochMs))
}

actual fun formatFullDate(epochMs: Long): String {
    val formatter = NSDateFormatter()
    formatter.dateFormat = "d MMMM yyyy, EEEE"
    return formatter.stringFromDate(dateFromMs(epochMs))
}

actual fun isToday(epochMs: Long): Boolean {
    val tz = kotlinx.datetime.TimeZone.currentSystemDefault()
    val today = kotlinx.datetime.Instant.fromEpochMilliseconds(getCurrentTimeMs()).toLocalDateTime(tz).date
    val target = kotlinx.datetime.Instant.fromEpochMilliseconds(epochMs).toLocalDateTime(tz).date
    return target == today
}

actual fun isTomorrow(epochMs: Long): Boolean {
    val tz = kotlinx.datetime.TimeZone.currentSystemDefault()
    val today = kotlinx.datetime.Instant.fromEpochMilliseconds(getCurrentTimeMs()).toLocalDateTime(tz).date
    val target = kotlinx.datetime.Instant.fromEpochMilliseconds(epochMs).toLocalDateTime(tz).date
    return target == today.plus(DatePeriod(days = 1))
}

actual fun getRelativeTimeBucket(epochMs: Long): TimeBucket {
    val nowMs = getCurrentTimeMs()
    if (isToday(epochMs)) return TimeBucket.TODAY

    try {
        val timeZone = kotlinx.datetime.TimeZone.currentSystemDefault()
        val targetDate = kotlinx.datetime.Instant.fromEpochMilliseconds(epochMs).toLocalDateTime(timeZone).date
        val today = kotlinx.datetime.Instant.fromEpochMilliseconds(nowMs).toLocalDateTime(timeZone).date

        if (targetDate < today) return TimeBucket.PAST

        val daysDiff = targetDate.toEpochDays() - today.toEpochDays()

        val daysUntilSunday = 7 - today.dayOfWeek.isoDayNumber
        if (daysDiff <= daysUntilSunday) return TimeBucket.THIS_WEEK

        if (targetDate.monthNumber == today.monthNumber && targetDate.year == today.year) {
            return TimeBucket.THIS_MONTH
        }

        return TimeBucket.FUTURE
    } catch (e: Exception) {
        return TimeBucket.FUTURE
    }
}
