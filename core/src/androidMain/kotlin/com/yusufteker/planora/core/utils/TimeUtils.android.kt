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
    val now = Calendar.getInstance()
    val target = Calendar.getInstance().apply { timeInMillis = epochMs }
    now.add(Calendar.DAY_OF_YEAR, 1)
    return now.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)
}
