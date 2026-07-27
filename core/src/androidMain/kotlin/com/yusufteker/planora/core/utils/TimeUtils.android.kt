package com.yusufteker.planora.core.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

actual fun getCurrentTimeMs(): Long {
    return System.currentTimeMillis()
}

actual fun formatTime(epochMs: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(epochMs))
}

actual fun formatShortDate(epochMs: Long): String {
    val now = Calendar.getInstance()
    val target = Calendar.getInstance().apply { timeInMillis = epochMs }
    val pattern = if (now.get(Calendar.YEAR) == target.get(Calendar.YEAR)) "d MMM" else "d MMM yyyy"
    val sdf = SimpleDateFormat(pattern, Locale.getDefault())
    return sdf.format(Date(epochMs))
}

actual fun formatDayName(epochMs: Long): String {
    val sdf = SimpleDateFormat("EEEE", Locale.getDefault())
    return sdf.format(Date(epochMs))
}

actual fun formatFullDate(epochMs: Long): String {
    val sdf = SimpleDateFormat("d MMMM yyyy, EEEE", Locale.getDefault())
    return sdf.format(Date(epochMs))
}

actual fun isToday(epochMs: Long): Boolean {
    val now = Calendar.getInstance()
    val target = Calendar.getInstance().apply { timeInMillis = epochMs }
    return now.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)
}

actual fun isTomorrow(epochMs: Long): Boolean {
    val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
    val target = Calendar.getInstance().apply { timeInMillis = epochMs }
    return tomorrow.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
            tomorrow.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)
}

actual fun getRelativeTimeBucket(epochMs: Long): TimeBucket {
    if (isToday(epochMs)) return TimeBucket.TODAY

    val now = Calendar.getInstance()
    val target = Calendar.getInstance().apply { timeInMillis = epochMs }

    val startOfToday = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    if (target.before(startOfToday)) {
        return TimeBucket.PAST
    }

    val nowCal = Calendar.getInstance().apply { firstDayOfWeek = Calendar.MONDAY }
    val targetCal = Calendar.getInstance().apply {
        firstDayOfWeek = Calendar.MONDAY
        timeInMillis = epochMs
    }

    val currentWeek = nowCal.get(Calendar.WEEK_OF_YEAR)
    val currentYear = nowCal.get(Calendar.YEAR)
    val targetWeek = targetCal.get(Calendar.WEEK_OF_YEAR)
    val targetYear = targetCal.get(Calendar.YEAR)

    if (currentYear == targetYear && currentWeek == targetWeek) {
        return TimeBucket.THIS_WEEK
    }

    val currentMonth = nowCal.get(Calendar.MONTH)
    if (currentYear == targetYear && currentMonth == targetCal.get(Calendar.MONTH)) {
        return TimeBucket.THIS_MONTH
    }

    return TimeBucket.FUTURE
}
