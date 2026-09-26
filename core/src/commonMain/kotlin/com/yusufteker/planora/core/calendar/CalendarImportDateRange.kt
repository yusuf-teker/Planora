package com.yusufteker.planora.core.calendar

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.StringResource
import planora.core.generated.resources.Res
import planora.core.generated.resources.calendar_import_range_all
import planora.core.generated.resources.calendar_import_range_past_and_future
import planora.core.generated.resources.calendar_import_range_year

/**
 * Predefined date range filters for querying events from native system calendars.
 */
enum class CalendarImportDateRange(val labelRes: StringResource) {
    PAST_AND_FUTURE_30_DAYS(Res.string.calendar_import_range_past_and_future),
    THIS_YEAR(Res.string.calendar_import_range_year),
    ALL(Res.string.calendar_import_range_all);

    /**
     * Computes the [startEpochMillis, endEpochMillis] pair for this date range filter.
     */
    fun getEpochRange(nowMs: Long = com.yusufteker.planora.core.utils.getCurrentTimeMs()): Pair<Long, Long> {
        val oneDayMs = 86_400_000L
        return when (this) {
            PAST_AND_FUTURE_30_DAYS -> Pair(nowMs - 30L * oneDayMs, nowMs + 30L * oneDayMs)
            THIS_YEAR -> {
                try {
                    val tz = TimeZone.currentSystemDefault()
                    val currentDateTime = Instant.fromEpochMilliseconds(nowMs).toLocalDateTime(tz)
                    val startOfYear = LocalDateTime(currentDateTime.year, 1, 1, 0, 0, 0)
                    val endOfYear = LocalDateTime(currentDateTime.year, 12, 31, 23, 59, 59)
                    Pair(startOfYear.toInstant(tz).toEpochMilliseconds(), endOfYear.toInstant(tz).toEpochMilliseconds())
                } catch (e: Exception) {
                    Pair(nowMs - 180L * oneDayMs, nowMs + 185L * oneDayMs)
                }
            }
            ALL -> Pair(nowMs - 10L * 365L * oneDayMs, nowMs + 10L * 365L * oneDayMs)
        }
    }
}

