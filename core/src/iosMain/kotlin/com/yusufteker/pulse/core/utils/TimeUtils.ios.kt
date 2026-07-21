package com.yusufteker.pulse.core.utils

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
    val calendar = NSCalendar.currentCalendar
    return calendar.isDateInToday(dateFromMs(epochMs))
}

actual fun isTomorrow(epochMs: Long): Boolean {
    val calendar = NSCalendar.currentCalendar
    return calendar.isDateInTomorrow(dateFromMs(epochMs))
}
